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
    public BookingEvent(Booking booking, BookingStatus fromStatus, BookingStatus toStatus,
            String actor, String reason, LocalDateTime at) {
        this.booking = booking; this.fromStatus = fromStatus; this.toStatus = toStatus;
        this.actor = actor; this.reason = reason; this.at = at;
    }
    public BookingStatus getFromStatus() { return fromStatus; }
    public BookingStatus getToStatus() { return toStatus; }
    public String getActor() { return actor; }
    public String getReason() { return reason; }
    public LocalDateTime getAt() { return at; }
}
