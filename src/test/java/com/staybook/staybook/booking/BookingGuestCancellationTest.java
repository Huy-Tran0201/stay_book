package com.staybook.staybook.booking;

import com.staybook.staybook.audit.AuditService;
import com.staybook.staybook.common.exception.BusinessException;
import com.staybook.staybook.common.exception.ResourceNotFoundException;
import com.staybook.staybook.payment.PaymentRepository;
import com.staybook.staybook.pricing.PricingService;
import com.staybook.staybook.room.Room;
import com.staybook.staybook.room.RoomRepository;
import com.staybook.staybook.room.RoomStatus;
import com.staybook.staybook.room.RoomType;
import com.staybook.staybook.room.RoomTypeRepository;
import com.staybook.staybook.room.RoomTypeStatus;
import com.staybook.staybook.setting.SettingRepository;
import com.staybook.staybook.user.User;
import com.staybook.staybook.user.UserStatus;
import com.staybook.staybook.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.mock;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class BookingGuestCancellationTest {
    @Mock BookingRepository bookingRepository;
    @Mock BookingNightRepository nightRepository;
    @Mock BookingEventRepository eventRepository;
    @Mock RoomTypeRepository roomTypeRepository;
    @Mock RoomRepository roomRepository;
    @Mock PricingService pricingService;
    @Mock SettingRepository settingRepository;
    @Mock AuditService auditService;
    @Mock PaymentRepository paymentRepository;

    private BookingService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-22T10:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
        service = new BookingService(bookingRepository, nightRepository, eventRepository,
            roomTypeRepository, roomRepository, pricingService, settingRepository, clock,
            auditService, paymentRepository);
        lenient().when(paymentRepository.findByBookingIdOrderByRecordedAtAsc(org.mockito.ArgumentMatchers.anyLong()))
            .thenReturn(List.of());
    }

    @Test
    void anotherGuestCannotCancelBooking() {
        User owner = mock(User.class);
        User other = mock(User.class);
        when(owner.getId()).thenReturn(1L);
        when(other.getId()).thenReturn(2L);
        Booking booking = booking(BookingStatus.CONFIRMED);
        booking.setUser(owner);
        when(bookingRepository.findByCodeForUpdate("BK-TEST")).thenReturn(Optional.of(booking));

        assertThrows(ResourceNotFoundException.class,
            () -> service.cancelBooking("BK-TEST", other, "Khách yêu cầu hủy"));
    }

    @Test
    void checkedInBookingCannotBeCancelled() {
        User guest = mock(User.class);
        when(guest.getId()).thenReturn(1L);
        Booking booking = booking(BookingStatus.CHECKED_IN);
        booking.setUser(guest);
        when(bookingRepository.findByCodeForUpdate("BK-TEST")).thenReturn(Optional.of(booking));

        assertThrows(BusinessException.class,
            () -> service.cancelBooking("BK-TEST", guest, "Khách yêu cầu hủy"));
    }

    @Test
    void pendingDepositCancellationHasNoRefund() {
        User guest = mock(User.class);
        when(guest.getId()).thenReturn(1L);
        when(guest.getEmail()).thenReturn("a@test.local");
        Booking booking = booking(BookingStatus.PENDING_DEPOSIT);
        booking.setUser(guest);
        when(bookingRepository.findByCodeForUpdate("BK-TEST")).thenReturn(Optional.of(booking));
        when(settingRepository.findByKey("refund.hours")).thenReturn(Optional.empty());

        Booking cancelled = service.cancelBooking("BK-TEST", guest, "Khách yêu cầu hủy");

        assert cancelled.getRefundEligibleAmount() == 0;
        assert cancelled.getStatus() == BookingStatus.CANCELLED;
        verify(eventRepository).save(org.mockito.ArgumentMatchers.any(BookingEvent.class));
    }

    @Test
    void anotherGuestCannotViewBooking() {
        User owner = mock(User.class);
        User other = mock(User.class);
        when(owner.getId()).thenReturn(1L);
        when(other.getId()).thenReturn(2L);
        Booking booking = booking(BookingStatus.CONFIRMED);
        booking.setUser(owner);
        when(bookingRepository.findByCode("BK-TEST")).thenReturn(Optional.of(booking));

        assertThrows(ResourceNotFoundException.class,
            () -> service.findOwnedBooking("BK-TEST", other));
    }

    @Test
    void awaitingConfirmationCancellationNeedsRefundCheck() {
        User guest = mock(User.class);
        when(guest.getId()).thenReturn(1L);
        when(guest.getEmail()).thenReturn("a@test.local");
        Booking booking = booking(BookingStatus.AWAITING_CONFIRMATION);
        booking.setUser(guest);
        when(bookingRepository.findByCodeForUpdate("BK-TEST")).thenReturn(Optional.of(booking));
        when(settingRepository.findByKey("refund.hours")).thenReturn(Optional.empty());

        Booking cancelled = service.cancelBooking("BK-TEST", guest, "Khách yêu cầu hủy");

        assert cancelled.isRefundCheckNeeded();
        assert cancelled.getStatus() == BookingStatus.CANCELLED;
    }

    private Booking booking(BookingStatus status) {
        RoomType type = new RoomType("Deluxe", 3, 800000L, RoomTypeStatus.ACTIVE);
        Room room = new Room("201", 2, type, RoomStatus.ACTIVE);
        return new Booking("BK-TEST", "Guest", "0900000000", type, room,
            LocalDate.of(2026, 11, 24), LocalDate.of(2026, 11, 26),
            1600000L, 480000L, status, BookingSource.GUEST_ONLINE,
            LocalDateTime.of(2026, 11, 20, 10, 0));
    }
}
