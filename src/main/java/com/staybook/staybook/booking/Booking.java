package com.staybook.staybook.booking;

import com.staybook.staybook.room.Room;
import com.staybook.staybook.room.RoomType;
import com.staybook.staybook.user.User;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings", uniqueConstraints = @UniqueConstraint(name = "uk_booking_code", columnNames = "code"),
       indexes = {@Index(name = "ix_booking_room_dates_status", columnList = "room_id,check_in_date,check_out_date,status"),
                  @Index(name = "ix_booking_status_hold", columnList = "status,hold_expires_at")})
public class Booking {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 20) private String code;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id") private User user;
    @Column(nullable = false, length = 120) private String guestName;
    @Column(nullable = false, length = 20) private String guestPhone;
    @Column(length = 160) private String guestEmail;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "room_type_id") private RoomType roomType;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "room_id") private Room room;
    @Column(nullable = false) private LocalDate checkInDate;
    @Column(nullable = false) private LocalDate checkOutDate;
    @Column(nullable = false) private int guestCount;
    @Column(nullable = false) private long totalRoomAmount;
    @Column(nullable = false) private long depositAmount;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private BookingStatus status;
    private LocalDateTime holdExpiresAt;
    @Column(length = 500) private String note;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private BookingSource source;
    @Enumerated(EnumType.STRING) @Column(length = 30) private CancelCause cancelCause;
    @Column(length = 500) private String cancelReason;
    private LocalDateTime cancelledAt;
    private long refundEligibleAmount;
    @Column(nullable = false) private boolean refundCheckNeeded;
    @Column(length = 100) private String idDocumentNote;
    private LocalDateTime checkedInAt;
    private LocalDateTime checkedOutAt;
    private LocalDateTime reminderSentAt;
    @Column(nullable = false) private LocalDateTime createdAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "created_by_id") private User createdBy;
    protected Booking() {}
    public Booking(String code, String guestName, String guestPhone, RoomType roomType, Room room,
                   LocalDate checkInDate, LocalDate checkOutDate, long totalRoomAmount, long depositAmount,
                   BookingStatus status, BookingSource source, LocalDateTime createdAt) {
        this.code = code; this.guestName = guestName; this.guestPhone = guestPhone; this.roomType = roomType;
        this.room = room; this.checkInDate = checkInDate; this.checkOutDate = checkOutDate;
        this.totalRoomAmount = totalRoomAmount; this.depositAmount = depositAmount; this.status = status;
        this.source = source; this.createdAt = createdAt;
    }
    public Long getId() { return id; } public String getCode() { return code; } public Room getRoom() { return room; }
    public LocalDate getCheckInDate() { return checkInDate; } public LocalDate getCheckOutDate() { return checkOutDate; }
    public BookingStatus getStatus() { return status; } public void setUser(User v) { user = v; }
    public void setGuestCount(int v) { guestCount = v; } public void setNote(String v) { note = v; }
}
