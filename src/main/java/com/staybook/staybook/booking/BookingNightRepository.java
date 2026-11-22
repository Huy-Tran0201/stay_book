package com.staybook.staybook.booking;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingNightRepository extends JpaRepository<BookingNight, Long> {
    java.util.List<BookingNight> findByBookingIdOrderByDateAsc(Long bookingId);
}
