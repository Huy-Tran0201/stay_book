package com.staybook.staybook.room;

import com.staybook.staybook.user.User;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "room_blocks", indexes = @Index(name = "ix_room_block_room_dates", columnList = "room_id,from_date,to_date"))
public class RoomBlock {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "room_id") private Room room;
    @Column(name = "from_date", nullable = false) private LocalDate fromDate;
    @Column(name = "to_date", nullable = false) private LocalDate toDate;
    @Column(nullable = false, length = 255) private String reason;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "created_by_id") private User createdBy;
    protected RoomBlock() {}
}
