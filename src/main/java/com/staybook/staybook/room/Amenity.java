package com.staybook.staybook.room;

import jakarta.persistence.*;

@Entity
@Table(name = "amenities", uniqueConstraints = @UniqueConstraint(name = "uk_amenity_name", columnNames = "name"))
public class Amenity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 80) private String name;
    protected Amenity() {}
    public Amenity(String name) { this.name = name; }
}
