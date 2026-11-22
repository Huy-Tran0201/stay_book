package com.staybook.staybook.payment;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SurchargeRepository extends JpaRepository<Surcharge, Long> {
    java.util.List<Surcharge> findByBookingIdOrderByRecordedAtAsc(Long bookingId);
}
