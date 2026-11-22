package com.staybook.staybook.booking;

import java.time.LocalDateTime;

public class CancellationPolicy {

    public long refundEligibleAmount(Booking booking, LocalDateTime cancelAt, long refundHours) {
        return refundEligibleAmount(booking, cancelAt, refundHours, booking == null ? 0 : booking.getDepositAmount());
    }

    public long refundEligibleAmount(Booking booking, LocalDateTime cancelAt, long refundHours,
                                     long recordedDepositAmount) {
        if (booking == null || cancelAt == null || booking.getStatus() == BookingStatus.PENDING_DEPOSIT) {
            return 0;
        }

        LocalDateTime checkInAt = booking.getCheckInDate().atTime(14, 0);
        LocalDateTime refundDeadline = checkInAt.minusHours(refundHours);
        if (!cancelAt.isAfter(refundDeadline)) {
            return Math.max(0, recordedDepositAmount);
        }
        return 0;
    }
}
