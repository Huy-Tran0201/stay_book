package com.staybook.staybook.room;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "season_prices", indexes = @Index(name = "ix_season_price_type_dates", columnList = "room_type_id,from_date,to_date"))
public class SeasonPrice {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "room_type_id") private RoomType roomType;
    @Column(nullable = false, length = 100) private String name;
    @Column(name = "from_date", nullable = false) private LocalDate fromDate;
    @Column(name = "to_date", nullable = false) private LocalDate toDate;
    @Column(nullable = false) private long basePrice;
    protected SeasonPrice() {}
    public SeasonPrice(RoomType roomType, String name, LocalDate fromDate, LocalDate toDate, long basePrice) {
        this.roomType = roomType;
        this.name = name;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.basePrice = basePrice;
    }

    public RoomType getRoomType() { return roomType; }
    public String getName() { return name; }
    public LocalDate getFromDate() { return fromDate; }
    public LocalDate getToDate() { return toDate; }
    public long getBasePrice() { return basePrice; }
}
