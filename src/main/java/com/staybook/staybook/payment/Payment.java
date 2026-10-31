package com.staybook.staybook.payment;

import com.staybook.staybook.booking.Booking;
import com.staybook.staybook.user.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments", indexes = @Index(name = "ix_payment_recorded_at", columnList = "recorded_at"))
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "booking_id") private Booking booking;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private PaymentType type;
    @Column(nullable = false) private long amount;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private PaymentMethod method;
    @Column(length = 500) private String note;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "recorded_by_id") private User recordedBy;
    @Column(nullable = false) private LocalDateTime recordedAt;
    protected Payment() {}
}
