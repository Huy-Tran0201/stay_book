package com.staybook.staybook.payment;

import com.staybook.staybook.booking.Booking;
import com.staybook.staybook.user.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "surcharges")
public class Surcharge {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "booking_id") private Booking booking;
    @Column(nullable = false, length = 120) private String name;
    @Column(nullable = false) private long amount;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "recorded_by_id") private User recordedBy;
    @Column(nullable = false) private LocalDateTime recordedAt;
    protected Surcharge() {}
}
