package com.staybook.staybook.room;

import jakarta.persistence.*;

@Entity
@Table(name = "rooms", uniqueConstraints = @UniqueConstraint(name = "uk_room_number", columnNames = "room_number"))
public class Room {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "room_number", nullable = false, length = 20) private String roomNumber;
    @Column(nullable = false) private int floor;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "room_type_id") private RoomType roomType;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private RoomStatus status;
    protected Room() {}
    public Room(String roomNumber, int floor, RoomType roomType, RoomStatus status) {
        this.roomNumber = roomNumber; this.floor = floor; this.roomType = roomType; this.status = status;
    }
    public Long getId() { return id; } public String getRoomNumber() { return roomNumber; }
    public RoomType getRoomType() { return roomType; } public RoomStatus getStatus() { return status; }
}
