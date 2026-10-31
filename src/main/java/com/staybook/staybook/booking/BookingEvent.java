package com.staybook.staybook.booking;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "booking_events", indexes = @Index(name = "ix_booking_event_booking_at", columnList = "booking_id,event_at"))
public class BookingEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "booking_id") private Booking booking;
    @Enumerated(EnumType.STRING) @Column(name = "from_status", length = 30) private BookingStatus fromStatus;
    @Enumerated(EnumType.STRING) @Column(name = "to_status", nullable = false, length = 30) private BookingStatus toStatus;
    @Column(nullable = false, length = 120) private String actor;
    @Column(length = 500) private String reason;
    @Column(name = "event_at", nullable = false) private LocalDateTime at;
    protected BookingEvent() {}
}
