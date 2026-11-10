package com.staybook.staybook.common.exception;

import com.staybook.staybook.booking.Booking;
import java.util.List;

public class RoomStatusChangeException extends BusinessException {
    private final List<Booking> affectedBookings;
    public RoomStatusChangeException(String message, List<Booking> affectedBookings) {
        super(message); this.affectedBookings = affectedBookings;
    }
    public List<Booking> getAffectedBookings() { return affectedBookings; }
}
