package com.staybook.staybook.payment;

import org.springframework.data.repository.Repository;

public interface PaymentRepository extends Repository<Payment, Long> {
    Payment save(Payment payment);
    java.util.List<Payment> findByBookingIdOrderByRecordedAtAsc(Long bookingId);
}
