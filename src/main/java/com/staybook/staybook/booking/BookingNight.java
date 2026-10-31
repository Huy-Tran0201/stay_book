package com.staybook.staybook.booking;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "booking_nights", uniqueConstraints = @UniqueConstraint(name = "uk_booking_night_date", columnNames = {"booking_id", "night_date"}))
public class BookingNight {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "booking_id") private Booking booking;
    @Column(name = "night_date", nullable = false) private LocalDate date;
    @Column(nullable = false) private long price;
    protected BookingNight() {}
    public BookingNight(Booking booking, LocalDate date, long price) { this.booking = booking; this.date = date; this.price = price; }
    public Long getId() { return id; } public LocalDate getDate() { return date; } public long getPrice() { return price; }
}
