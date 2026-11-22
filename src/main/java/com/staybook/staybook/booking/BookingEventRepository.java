package com.staybook.staybook.booking;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingEventRepository extends JpaRepository<BookingEvent, Long> {
    java.util.List<BookingEvent> findByBookingIdOrderByAtAsc(Long bookingId);
}
