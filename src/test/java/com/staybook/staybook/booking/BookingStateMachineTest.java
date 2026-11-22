package com.staybook.staybook.booking;

import com.staybook.staybook.common.exception.BusinessException;
import com.staybook.staybook.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingStateMachineTest {
    @Mock BookingRepository bookingRepository;
    @Mock BookingEventRepository eventRepository;

    @Test
    void paidNoticeChangesOwnerBookingToAwaitingConfirmation() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-22T10:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
        BookingStateMachine stateMachine = new BookingStateMachine(bookingRepository, eventRepository, clock);
        User owner = org.mockito.Mockito.mock(User.class);
        User actor = org.mockito.Mockito.mock(User.class);
        Booking booking = org.mockito.Mockito.mock(Booking.class);

        when(owner.getId()).thenReturn(1L);
        when(actor.getId()).thenReturn(1L);
        when(actor.getEmail()).thenReturn("guest1@staybook.local");
        when(booking.getUser()).thenReturn(owner);
        when(booking.getStatus()).thenReturn(BookingStatus.PENDING_DEPOSIT);
        when(booking.getHoldExpiresAt()).thenReturn(LocalDateTime.now(clock).plusMinutes(30));
        when(bookingRepository.findByCodeForUpdate("BK-261122-TEST")).thenReturn(Optional.of(booking));

        stateMachine.submitPaidNotice("BK-261122-TEST", actor);

        verify(booking).changeStatus(BookingStatus.AWAITING_CONFIRMATION);
        verify(bookingRepository).save(booking);
        verify(eventRepository).save(any(BookingEvent.class));
    }

    @Test
    void paidNoticeRejectsAnotherGuest() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-22T10:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
        BookingStateMachine stateMachine = new BookingStateMachine(bookingRepository, eventRepository, clock);
        User owner = org.mockito.Mockito.mock(User.class);
        User actor = org.mockito.Mockito.mock(User.class);
        Booking booking = org.mockito.Mockito.mock(Booking.class);

        when(owner.getId()).thenReturn(1L);
        when(actor.getId()).thenReturn(2L);
        when(booking.getUser()).thenReturn(owner);
        when(bookingRepository.findByCodeForUpdate("BK-261122-TEST")).thenReturn(Optional.of(booking));

        assertThrows(BusinessException.class,
            () -> stateMachine.submitPaidNotice("BK-261122-TEST", actor));
    }
}
