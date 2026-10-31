package com.staybook.staybook.review;

import com.staybook.staybook.booking.Booking;
import com.staybook.staybook.room.RoomType;
import com.staybook.staybook.user.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews", uniqueConstraints = @UniqueConstraint(name = "uk_review_booking", columnNames = "booking_id"))
public class Review {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "booking_id") private Booking booking;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id") private User user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "room_type_id") private RoomType roomType;
    @Column(nullable = false) private int stars;
    @Column(length = 1000) private String comment;
    @Column(nullable = false) private boolean hidden;
    @Column(nullable = false) private LocalDateTime createdAt;
    protected Review() {}
}
