package com.staybook.staybook.room;

import jakarta.persistence.*;

@Entity
@Table(name = "room_type_images", uniqueConstraints = @UniqueConstraint(name = "uk_room_type_image_order", columnNames = {"room_type_id", "sort_order"}))
public class RoomTypeImage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "room_type_id") private RoomType roomType;
    @Column(nullable = false, length = 255) private String fileName;
    @Column(name = "sort_order", nullable = false) private int sortOrder;
    protected RoomTypeImage() {}
    public RoomTypeImage(RoomType roomType, String fileName, int sortOrder) {
        this.roomType = roomType; this.fileName = fileName; this.sortOrder = sortOrder;
    }
    public Long getId() { return id; }
    public String getFileName() { return fileName; }
    public int getSortOrder() { return sortOrder; }
}
