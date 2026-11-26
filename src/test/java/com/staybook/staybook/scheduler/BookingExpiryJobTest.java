package com.staybook.staybook.scheduler;

import com.staybook.staybook.booking.*;
import com.staybook.staybook.common.exception.BusinessException;
import com.staybook.staybook.room.*;
import com.staybook.staybook.setting.Setting;
import com.staybook.staybook.setting.SettingRepository;
import com.staybook.staybook.user.Role;
import com.staybook.staybook.user.User;
import com.staybook.staybook.user.UserRepository;
import com.staybook.staybook.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import java.time.*;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Import(BookingExpiryJobTest.TestClockConfiguration.class)
class BookingExpiryJobTest {

    @Autowired private BookingExpiryJob job;
    @Autowired private BookingService bookingService;
    @Autowired private BookingStateMachine stateMachine;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private BookingEventRepository eventRepository;
    @Autowired private AvailabilityService availabilityService;
    @Autowired private RoomTypeRepository roomTypeRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private SettingRepository settingRepository;
    @Autowired private MutableClock clock;

    private User guestUser;
    private RoomType standardType;
    private Room room101;
    private final Instant baseTime = Instant.parse("2026-12-01T03:00:00Z"); // 10:00:00 Asia/Ho_Chi_Minh

    @BeforeEach
    void setUp() {
        clock.setInstant(baseTime);
        ensureSetting("deposit.percent", "30");
        ensureSetting("hold.minutes", "30");
        ensureSetting("max.pending.per.guest", "10");
        ensureSetting("refund.hours", "48");

        guestUser = userRepository.save(new User(
            "Khách A",
            "khach" + System.nanoTime() + "@test.local",
            "0901234567",
            "hash",
            Role.GUEST,
            UserStatus.ACTIVE,
            LocalDateTime.now(clock)
        ));

        standardType = roomTypeRepository.save(new RoomType("Standard Test " + System.nanoTime(), 2, 500_000L, RoomTypeStatus.ACTIVE));
        room101 = roomRepository.save(new Room("T" + (System.nanoTime() % 10000), 1, standardType, RoomStatus.ACTIVE));
    }

    private void ensureSetting(String key, String value) {
        if (settingRepository.findByKey(key).isEmpty()) {
            settingRepository.save(new Setting(key, value));
        }
    }

    private Booking createBookingAtCurrentTime() {
        LocalDate in = LocalDate.of(2026, 12, 10);
        LocalDate out = LocalDate.of(2026, 12, 12);
        BookingForm form = new BookingForm();
        form.setRoomTypeId(standardType.getId());
        form.setCheckInDate(in);
        form.setCheckOutDate(out);
        form.setGuestCount(2);
        form.setGuestName(guestUser.getFullName());
        form.setGuestPhone(guestUser.getPhone());
        form.setGuestEmail(guestUser.getEmail());
        form.setNearCheckInConfirmed(true);
        return bookingService.createBooking(form, guestUser);
    }

    @Test
    @DisplayName("Đặt lúc 10:00 không cọc, lúc 10:31 chạy job -> EXPIRED; phòng xuất hiện lại khi tìm kiếm")
    void bookingExpiresAfterHoldTimeAndRoomBecomesAvailable() {
        // 1. Đặt lúc 10:00
        Booking booking = createBookingAtCurrentTime();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING_DEPOSIT);
        assertThat(booking.getHoldExpiresAt()).isEqualTo(LocalDateTime.of(2026, 12, 1, 10, 30, 0));

        // Kiểm tra phòng đang bị giữ chỗ -> không có phòng trống cho ngày này
        LocalDate in = LocalDate.of(2026, 12, 10);
        LocalDate out = LocalDate.of(2026, 12, 12);
        List<AvailabilityResult> availableBefore = availabilityService.findAvailable(in, out, 2).stream()
            .filter(r -> r.roomType().getId().equals(standardType.getId()))
            .toList();
        assertThat(availableBefore).isEmpty();

        // 2. Tua thời gian tới 10:31 (+31 phút) và chạy job
        clock.advanceMinutes(31);
        job.run();

        // 3. Trạng thái chuyển sang EXPIRED
        Booking expiredBooking = bookingRepository.findById(booking.getId()).orElseThrow();
        assertThat(expiredBooking.getStatus()).isEqualTo(BookingStatus.EXPIRED);

        // 4. Ghi nhận BookingEvent với actor SYSTEM
        List<BookingEvent> events = eventRepository.findByBookingIdOrderByAtAsc(booking.getId());
        BookingEvent expiryEvent = events.stream()
            .filter(e -> e.getToStatus() == BookingStatus.EXPIRED)
            .findFirst()
            .orElseThrow();
        assertThat(expiryEvent.getActor()).isEqualTo("SYSTEM");
        assertThat(expiryEvent.getFromStatus()).isEqualTo(BookingStatus.PENDING_DEPOSIT);

        // 5. Sau khi EXPIRED, phòng xuất hiện lại khi tìm kiếm
        List<AvailabilityResult> availableAfter = availabilityService.findAvailable(in, out, 2).stream()
            .filter(r -> r.roomType().getId().equals(standardType.getId()))
            .toList();
        assertThat(availableAfter).isNotEmpty();
        assertThat(availableAfter.get(0).availableRoomCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Chạy job hai lần không lỗi và không tạo thêm sự kiện lặp")
    void runningJobTwiceDoesNotErrorOrDuplicate() {
        Booking booking = createBookingAtCurrentTime();
        clock.advanceMinutes(31);

        // Lần 1
        job.run();
        Booking expiredFirst = bookingRepository.findById(booking.getId()).orElseThrow();
        assertThat(expiredFirst.getStatus()).isEqualTo(BookingStatus.EXPIRED);
        int eventCount = eventRepository.findByBookingIdOrderByAtAsc(booking.getId()).size();

        // Lần 2: Chạy lại job không lỗi
        job.run();
        Booking expiredSecond = bookingRepository.findById(booking.getId()).orElseThrow();
        assertThat(expiredSecond.getStatus()).isEqualTo(BookingStatus.EXPIRED);
        assertThat(eventRepository.findByBookingIdOrderByAtAsc(booking.getId())).hasSize(eventCount);
    }

    @Test
    @DisplayName("Khách bấm 'đã chuyển khoản' trước khi job chạy thì KHÔNG bị hết hạn")
    void guestPaidNoticeBeforeJobPreventsExpiry() {
        Booking booking = createBookingAtCurrentTime();

        // Khách bấm "Tôi đã chuyển khoản" lúc 10:15 (trước hạn 10:30)
        clock.advanceMinutes(15);
        Booking awaitingBooking = stateMachine.submitPaidNotice(booking.getCode(), guestUser);
        assertThat(awaitingBooking.getStatus()).isEqualTo(BookingStatus.AWAITING_CONFIRMATION);

        // Đến 10:31 chạy job
        clock.advanceMinutes(16); // 10:15 + 16m = 10:31
        job.run();

        // Booking không bị hết hạn, vẫn là AWAITING_CONFIRMATION
        Booking afterJob = bookingRepository.findById(booking.getId()).orElseThrow();
        assertThat(afterJob.getStatus()).isEqualTo(BookingStatus.AWAITING_CONFIRMATION);
    }

    @Test
    @DisplayName("Khách bấm đúng lúc quá hạn thì bị từ chối")
    void guestPaidNoticeAtOrAfterExpiryIsRejected() {
        Booking booking = createBookingAtCurrentTime();

        // Đến 10:31 (quá hạn 10:30) khách mới bấm
        clock.advanceMinutes(31);

        BusinessException ex = assertThrows(BusinessException.class, () ->
            stateMachine.submitPaidNotice(booking.getCode(), guestUser)
        );
        assertThat(ex.getMessage()).contains("Thời gian giữ chỗ đã hết");

        // Trạng thái vẫn là PENDING_DEPOSIT cho đến khi job chạy
        Booking current = bookingRepository.findById(booking.getId()).orElseThrow();
        assertThat(current.getStatus()).isEqualTo(BookingStatus.PENDING_DEPOSIT);

        // Sau đó job chạy sẽ chuyển sang EXPIRED
        job.run();
        Booking afterJob = bookingRepository.findById(booking.getId()).orElseThrow();
        assertThat(afterJob.getStatus()).isEqualTo(BookingStatus.EXPIRED);
    }

    @Test
    @DisplayName("Booking AWAITING_CONFIRMATION không bao giờ bị job hủy")
    void awaitingConfirmationBookingIsNeverExpiredByJob() {
        Booking booking = createBookingAtCurrentTime();
        stateMachine.submitPaidNotice(booking.getCode(), guestUser);

        Booking awaiting = bookingRepository.findById(booking.getId()).orElseThrow();
        assertThat(awaiting.getStatus()).isEqualTo(BookingStatus.AWAITING_CONFIRMATION);

        // Tua thời gian 24 giờ sau
        clock.advanceMinutes(24 * 60);
        job.run();

        Booking stillAwaiting = bookingRepository.findById(booking.getId()).orElseThrow();
        assertThat(stillAwaiting.getStatus()).isEqualTo(BookingStatus.AWAITING_CONFIRMATION);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestClockConfiguration {
        @Bean("testClock")
        @Primary
        MutableClock clock() {
            return new MutableClock(Instant.parse("2026-12-01T03:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
        }
    }

    static final class MutableClock extends Clock {
        private Instant current;
        private final ZoneId zone;

        MutableClock(Instant current, ZoneId zone) {
            this.current = current;
            this.zone = zone;
        }

        void setInstant(Instant instant) {
            this.current = instant;
        }

        void advanceMinutes(long minutes) {
            this.current = this.current.plusSeconds(minutes * 60);
        }

        @Override public ZoneId getZone() { return zone; }
        @Override public Clock withZone(ZoneId zone) { return new MutableClock(current, zone); }
        @Override public Instant instant() { return current; }
    }
}
