package com.staybook.staybook.booking;

import com.staybook.staybook.audit.AuditService;
import com.staybook.staybook.common.exception.BusinessException;
import com.staybook.staybook.payment.Payment;
import com.staybook.staybook.payment.PaymentLedger;
import com.staybook.staybook.payment.PaymentMethod;
import com.staybook.staybook.payment.PaymentRepository;
import com.staybook.staybook.payment.PaymentService;
import com.staybook.staybook.payment.PaymentType;
import com.staybook.staybook.payment.Surcharge;
import com.staybook.staybook.payment.SurchargeRepository;
import com.staybook.staybook.pricing.PricingService;
import com.staybook.staybook.room.Room;
import com.staybook.staybook.room.RoomRepository;
import com.staybook.staybook.room.RoomStatus;
import com.staybook.staybook.room.RoomType;
import com.staybook.staybook.room.RoomTypeRepository;
import com.staybook.staybook.room.RoomTypeStatus;
import com.staybook.staybook.setting.SettingRepository;
import com.staybook.staybook.user.Role;
import com.staybook.staybook.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffBookingLifecycleTest {

    @Mock BookingRepository bookingRepository;
    @Mock BookingNightRepository nightRepository;
    @Mock BookingEventRepository eventRepository;
    @Mock RoomTypeRepository roomTypeRepository;
    @Mock RoomRepository roomRepository;
    @Mock PricingService pricingService;
    @Mock SettingRepository settingRepository;
    @Mock AuditService auditService;
    @Mock PaymentRepository paymentRepository;
    @Mock PaymentService paymentService;
    @Mock BookingStateMachine stateMachine;

    private Clock clock;
    private BookingService bookingService;
    private User staff;
    private RoomType roomType;
    private Room room;

    @BeforeEach
    void setUp() {
        // Cố định thời điểm: 2026-11-22 lúc 10:00 (Thứ 2)
        clock = Clock.fixed(Instant.parse("2026-11-22T03:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh")); // 10:00 VN
        bookingService = new BookingService(bookingRepository, nightRepository, eventRepository,
            roomTypeRepository, roomRepository, pricingService, settingRepository, clock,
            auditService, paymentRepository, stateMachine, paymentService);

        staff = mock(User.class);
        lenient().when(staff.getRole()).thenReturn(Role.STAFF);
        lenient().when(staff.getEmail()).thenReturn("staff1@staybook.local");

        roomType = new RoomType("Deluxe", 3, 800_000L, RoomTypeStatus.ACTIVE);
        room = new Room("201", 2, roomType, RoomStatus.ACTIVE);
    }

    @Test
    @DisplayName("Nhận phòng bị từ chối nếu chưa xác nhận cọc (PENDING_DEPOSIT hoặc AWAITING_CONFIRMATION)")
    void checkInFailsWhenNotConfirmed() {
        Booking booking = createBooking(BookingStatus.AWAITING_CONFIRMATION, LocalDate.of(2026, 11, 22), LocalDate.of(2026, 11, 25));
        when(bookingRepository.findByCodeForUpdate("BK-TEST")).thenReturn(Optional.of(booking));

        BusinessException ex = assertThrows(BusinessException.class,
            () -> bookingService.checkIn("BK-TEST", "CCCD", "1234", staff));

        assertEquals("Đặt phòng chưa được xác nhận cọc.", ex.getMessage());
    }

    @Test
    @DisplayName("Nhận phòng bị từ chối nếu hôm nay chưa đến ngày nhận phòng")
    void checkInFailsWhenBeforeCheckInDate() {
        // Hôm nay là 22/11, ngày nhận phòng là 23/11
        Booking booking = createBooking(BookingStatus.CONFIRMED, LocalDate.of(2026, 11, 23), LocalDate.of(2026, 11, 25));
        when(bookingRepository.findByCodeForUpdate("BK-TEST")).thenReturn(Optional.of(booking));

        BusinessException ex = assertThrows(BusinessException.class,
            () -> bookingService.checkIn("BK-TEST", "CCCD", "1234", staff));

        assertEquals("Chưa đến ngày nhận phòng.", ex.getMessage());
    }

    @Test
    @DisplayName("Nhận phòng thành công: ghi checkedInAt, lưu loại giấy tờ + 4 số cuối (BR-26), chuyển CHECKED_IN")
    void checkInSucceedsOnCheckInDateWithOptionalDocument() {
        // Hôm nay 22/11, ngày nhận 22/11
        Booking booking = createBooking(BookingStatus.CONFIRMED, LocalDate.of(2026, 11, 22), LocalDate.of(2026, 11, 25));
        when(bookingRepository.findByCodeForUpdate("BK-TEST")).thenReturn(Optional.of(booking));

        Booking checkedIn = bookingService.checkIn("BK-TEST", "CCCD", "012345678912", staff);

        // Đảm bảo chỉ lưu loại giấy tờ và 4 số cuối
        assertEquals("CCCD - ****8912", checkedIn.getIdDocumentNote());
        assertNotNull(checkedIn.getCheckedInAt());
        verify(stateMachine).transition(booking, BookingStatus.CHECKED_IN, staff.getEmail(), "Lễ tân thực hiện nhận phòng");
        verify(auditService).record(eq(staff), eq("CHECK_IN"), eq("BOOKING"), any(), any());
    }

    @Test
    @DisplayName("Ví dụ trả phòng từ Prompt 8B: tiền phòng 2.560.000 ₫, cọc 768.000 ₫, phụ phí 150.000 ₫ -> còn phải thu 1.942.000 ₫. Không trả phòng được khi chưa thu đủ")
    void checkOutFailsWhenRemainingDueNotZero() {
        Booking booking = createBooking(BookingStatus.CHECKED_IN, LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 23));
        when(bookingRepository.findByCodeForUpdate("BK-TEST")).thenReturn(Optional.of(booking));

        // Ledger: totalRoom = 2.560.000, surcharge = 150.000, paid = 768.000 => remaining = 1.942.000
        when(paymentService.ledger(booking)).thenReturn(new PaymentLedger(768_000, 2_710_000, 1_942_000, 0));

        BusinessException ex = assertThrows(BusinessException.class,
            () -> bookingService.checkOut("BK-TEST", staff));

        assertEquals("Còn phải thu 1.942.000 ₫.", ex.getMessage());
    }

    @Test
    @DisplayName("Trả phòng thành công khi remaining == 0, ghi checkedOutAt và chuyển CHECKED_OUT")
    void checkOutSucceedsWhenRemainingIsZero() {
        Booking booking = createBooking(BookingStatus.CHECKED_IN, LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 23));
        when(bookingRepository.findByCodeForUpdate("BK-TEST")).thenReturn(Optional.of(booking));

        when(paymentService.ledger(booking)).thenReturn(new PaymentLedger(2_710_000, 2_710_000, 0, 0));

        Booking checkedOut = bookingService.checkOut("BK-TEST", staff);

        assertNotNull(checkedOut.getCheckedOutAt());
        verify(stateMachine).transition(booking, BookingStatus.CHECKED_OUT, staff.getEmail(), "Lễ tân thực hiện trả phòng");
        verify(auditService).record(eq(staff), eq("CHECK_OUT"), eq("BOOKING"), any(), any());
    }

    @Test
    @DisplayName("Trả phòng sớm vẫn được nhưng không hoàn tiền đêm chưa ở, chỉ cần remaining == 0")
    void earlyCheckOutAllowedWithoutRefundingRemainingNights() {
        // Ngày trả dự kiến 25/11, hôm nay 22/11 -> trả sớm 3 ngày
        Booking booking = createBooking(BookingStatus.CHECKED_IN, LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 25));
        booking.setNote("Khách về sớm");
        when(bookingRepository.findByCodeForUpdate("BK-TEST")).thenReturn(Optional.of(booking));

        when(paymentService.ledger(booking)).thenReturn(new PaymentLedger(booking.getTotalRoomAmount(), booking.getTotalRoomAmount(), 0, 0));

        Booking checkedOut = bookingService.checkOut("BK-TEST", staff);

        assertNotNull(checkedOut.getCheckedOutAt());
        assertEquals(2_560_000L, checkedOut.getTotalRoomAmount()); // Giá tiền phòng các đêm giữ nguyên
        verify(stateMachine).transition(booking, BookingStatus.CHECKED_OUT, staff.getEmail(), "Lễ tân thực hiện trả phòng");
    }

    @Test
    @DisplayName("Không đến (No-show) bị từ chối nếu trước 00:00 ngày kế tiếp ngày nhận")
    void noShowFailsBeforeNextDayMidnight() {
        // Nhận phòng 22/11, hôm nay là 22/11 lúc 10:00 (chưa đến 00:00 ngày 23/11)
        Booking booking = createBooking(BookingStatus.CONFIRMED, LocalDate.of(2026, 11, 22), LocalDate.of(2026, 11, 25));
        when(bookingRepository.findByCodeForUpdate("BK-TEST")).thenReturn(Optional.of(booking));

        BusinessException ex = assertThrows(BusinessException.class,
            () -> bookingService.noShow("BK-TEST", staff));

        assertEquals("Chỉ được ghi nhận không đến từ 00:00 ngày kế tiếp ngày nhận phòng.", ex.getMessage());
    }

    @Test
    @DisplayName("Không đến (No-show) thành công từ 00:00 ngày kế tiếp ngày nhận: cọc giữ lại (refundEligibleAmount = 0)")
    void noShowSucceedsFromNextDayMidnight() {
        // Nhận phòng 20/11, hôm nay là 22/11 (đã qua 00:00 ngày 21/11)
        Booking booking = createBooking(BookingStatus.CONFIRMED, LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 23));
        when(bookingRepository.findByCodeForUpdate("BK-TEST")).thenReturn(Optional.of(booking));

        Booking noShowBooking = bookingService.noShow("BK-TEST", staff);

        assertEquals(0L, noShowBooking.getRefundEligibleAmount());
        verify(stateMachine).transition(booking, BookingStatus.NO_SHOW, staff.getEmail(), "Khách không đến nhận phòng");
        verify(auditService).record(eq(staff), eq("NO_SHOW"), eq("BOOKING"), any(), any());
    }

    @Test
    @DisplayName("Booking đã NO_SHOW không nhận phòng lại được")
    void noShowBookingCannotCheckInAgain() {
        Booking booking = createBooking(BookingStatus.NO_SHOW, LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 23));
        when(bookingRepository.findByCodeForUpdate("BK-TEST")).thenReturn(Optional.of(booking));

        BusinessException ex = assertThrows(BusinessException.class,
            () -> bookingService.checkIn("BK-TEST", "CCCD", "1234", staff));

        assertEquals("Đặt phòng đã không đến, không thể nhận phòng.", ex.getMessage());
    }

    @Test
    @DisplayName("Hủy bởi lễ tân do cơ sở hủy: hoàn 100% tiền cọc đã thu")
    void staffCancelFacilityCancelProduces100PercentDepositRefund() {
        Booking booking = createBooking(BookingStatus.CONFIRMED, LocalDate.of(2026, 11, 22), LocalDate.of(2026, 11, 25));
        when(bookingRepository.findByCodeForUpdate("BK-TEST")).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingIdOrderByRecordedAtAsc(booking.getId())).thenReturn(List.of(
            new Payment(booking, PaymentType.DEPOSIT, 768_000, PaymentMethod.BANK_TRANSFER, "", staff, LocalDateTime.now(clock))
        ));

        Booking cancelled = bookingService.staffCancel("BK-TEST", CancelCause.FACILITY_CANCEL, "Cơ sở sửa chữa", staff);

        assertEquals(CancelCause.FACILITY_CANCEL, cancelled.getCancelCause());
        assertEquals(768_000L, cancelled.getRefundEligibleAmount()); // Hoàn 100%
        verify(stateMachine).transition(booking, BookingStatus.CANCELLED, staff.getEmail(), "Cơ sở sửa chữa");
    }

    @Test
    @DisplayName("Hủy bởi lễ tân do khách yêu cầu: hoàn theo chính sách (quá hạn 48h không được hoàn cọc)")
    void staffCancelGuestRequestFollowsCancellationPolicy() {
        // Nhận phòng 22/11 lúc 14:00, thời điểm hủy là 22/11 lúc 10:00 (còn 4h nữa nhận phòng -> quá hạn 48h)
        Booking booking = createBooking(BookingStatus.CONFIRMED, LocalDate.of(2026, 11, 22), LocalDate.of(2026, 11, 25));
        when(bookingRepository.findByCodeForUpdate("BK-TEST")).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingIdOrderByRecordedAtAsc(booking.getId())).thenReturn(List.of(
            new Payment(booking, PaymentType.DEPOSIT, 768_000, PaymentMethod.BANK_TRANSFER, "", staff, LocalDateTime.now(clock))
        ));

        Booking cancelled = bookingService.staffCancel("BK-TEST", CancelCause.GUEST_REQUEST, "Khách đổi lịch", staff);

        assertEquals(CancelCause.GUEST_REQUEST, cancelled.getCancelCause());
        assertEquals(0L, cancelled.getRefundEligibleAmount()); // Không được hoàn theo chính sách
        verify(stateMachine).transition(booking, BookingStatus.CANCELLED, staff.getEmail(), "Khách đổi lịch");
    }

    @Test
    @DisplayName("Các chuyển trạng thái không hợp lệ bị từ chối")
    void invalidStatusTransitionsAreRejected() {
        // Không thể check-out khi chưa CHECKED_IN (đang ở CONFIRMED)
        Booking booking = createBooking(BookingStatus.CONFIRMED, LocalDate.of(2026, 11, 22), LocalDate.of(2026, 11, 25));
        when(bookingRepository.findByCodeForUpdate("BK-TEST")).thenReturn(Optional.of(booking));

        BusinessException ex = assertThrows(BusinessException.class,
            () -> bookingService.checkOut("BK-TEST", staff));
        assertEquals("Chuyển trạng thái đặt phòng không hợp lệ.", ex.getMessage());

        // Không thể no-show khi booking đang ở trạng thái CHECKED_IN
        Booking checkedInBooking = createBooking(BookingStatus.CHECKED_IN, LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 23));
        when(bookingRepository.findByCodeForUpdate("BK-CHECKEDIN")).thenReturn(Optional.of(checkedInBooking));

        BusinessException ex2 = assertThrows(BusinessException.class,
            () -> bookingService.noShow("BK-CHECKEDIN", staff));
        assertEquals("Đặt phòng không ở trạng thái đã xác nhận.", ex2.getMessage());
    }

    private Booking createBooking(BookingStatus status, LocalDate checkIn, LocalDate checkOut) {
        Booking booking = new Booking("BK-TEST", "Nguyễn Văn A", "0901234567", roomType, room,
            checkIn, checkOut, 2_560_000L, 768_000L, status,
            BookingSource.GUEST_ONLINE, LocalDateTime.now(clock));
        return booking;
    }
}
