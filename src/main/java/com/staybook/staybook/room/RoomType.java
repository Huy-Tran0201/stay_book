package com.staybook.staybook.room;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "room_types")
public class RoomType {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 100) private String name;
    @Column(length = 1000) private String description;
    @Column(nullable = false) private int maxGuests;
    private int areaM2;
    @Column(length = 100) private String bedType;
    @Column(nullable = false) private long basePrice;
    @Column(nullable = false) private int weekendSurchargePercent = 10;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private RoomTypeStatus status;
    @ManyToMany
    @JoinTable(name = "room_type_amenities",
            joinColumns = @JoinColumn(name = "room_type_id"),
            inverseJoinColumns = @JoinColumn(name = "amenity_id"))
    private Set<Amenity> amenities = new HashSet<>();
    protected RoomType() {}
    public RoomType(String name, int maxGuests, long basePrice, RoomTypeStatus status) {
        this.name = name; this.maxGuests = maxGuests; this.basePrice = basePrice; this.status = status;
    }
    public Long getId() { return id; } public String getName() { return name; }
    public String getDescription() { return description; }
    public int getMaxGuests() { return maxGuests; } public long getBasePrice() { return basePrice; }
    public int getAreaM2() { return areaM2; } public String getBedType() { return bedType; }
    public int getWeekendSurchargePercent() { return weekendSurchargePercent; }
    public RoomTypeStatus getStatus() { return status; }
    public Set<Amenity> getAmenities() { return amenities; }
    public void update(String name, String description, int maxGuests, int areaM2, String bedType,
                       long basePrice, int weekendSurchargePercent, Set<Amenity> amenities) {
        this.name = name; this.description = description; this.maxGuests = maxGuests;
        this.areaM2 = areaM2; this.bedType = bedType; this.basePrice = basePrice;
        this.weekendSurchargePercent = weekendSurchargePercent;
        this.amenities.clear(); this.amenities.addAll(amenities);
    }
    public void changeStatus(RoomTypeStatus status) { this.status = status; }
}
