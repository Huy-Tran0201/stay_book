package com.staybook.staybook.booking;

import com.staybook.staybook.room.Room;
import com.staybook.staybook.room.RoomType;
import com.staybook.staybook.room.RoomTypeStatus;
import com.staybook.staybook.room.RoomStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CancellationPolicyTest {
    private final CancellationPolicy policy = new CancellationPolicy();

    @Test
    void refundedWhenCancelledOneMinuteBeforeDeadline() {
        Booking booking = confirmedBooking();
        LocalDateTime oneMinuteBefore = LocalDate.of(2026, 11, 22).atTime(13, 59);

        assertEquals(480000L, policy.refundEligibleAmount(booking, oneMinuteBefore, 48));
    }

    @Test
    void refundedExactlyAtDeadline() {
        Booking booking = confirmedBooking();
        LocalDateTime deadline = LocalDate.of(2026, 11, 22).atTime(14, 0);

        assertEquals(480000L, policy.refundEligibleAmount(booking, deadline, 48));
    }

    @Test
    void notRefundedOneMinuteAfterDeadline() {
        Booking booking = confirmedBooking();
        LocalDateTime oneMinuteAfter = LocalDate.of(2026, 11, 22).atTime(14, 1);

        assertEquals(0L, policy.refundEligibleAmount(booking, oneMinuteAfter, 48));
    }

    @Test
    void pendingDepositNeverProducesRefund() {
        Booking booking = confirmedBooking();
        booking.changeStatus(BookingStatus.PENDING_DEPOSIT);

        assertEquals(0L, policy.refundEligibleAmount(
            booking, LocalDate.of(2026, 11, 20).atTime(10, 0), 48));
    }

    private Booking confirmedBooking() {
        RoomType type = new RoomType("Deluxe", 3, 800000L, RoomTypeStatus.ACTIVE);
        Room room = new Room("201", 2, type, RoomStatus.ACTIVE);
        Booking booking = new Booking("BK-261122-TEST", "Guest", "0900000000", type, room,
            LocalDate.of(2026, 11, 24), LocalDate.of(2026, 11, 26),
            1600000L, 480000L, BookingStatus.CONFIRMED,
            BookingSource.GUEST_ONLINE, LocalDateTime.of(2026, 11, 20, 10, 0));
        return booking;
    }
}
