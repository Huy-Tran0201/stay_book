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
    public Surcharge(Booking booking, String name, long amount, User recordedBy, LocalDateTime recordedAt) {
        this.booking = booking; this.name = name; this.amount = amount;
        this.recordedBy = recordedBy; this.recordedAt = recordedAt;
    }
    public String getName() { return name; }
    public long getAmount() { return amount; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
}
