package com.staybook.staybook.room;

import com.staybook.staybook.booking.BookingStatus;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {
    @Query("""
        select r from Room r
        where r.roomType.id = :roomTypeId and r.status = com.staybook.staybook.room.RoomStatus.ACTIVE
          and not exists (
            select b.id from Booking b
            where b.room = r and b.status in :heldStatuses
              and b.checkInDate < :checkOut and :checkIn < b.checkOutDate
          )
          and not exists (
            select rb.id from RoomBlock rb
            where rb.room = r and rb.fromDate < :checkOut and rb.toDate >= :checkIn
          )
        order by r.roomNumber
        """)
    List<Room> findAvailableRooms(@Param("roomTypeId") Long roomTypeId,
                                  @Param("checkIn") LocalDate checkIn,
                                  @Param("checkOut") LocalDate checkOut,
                                  @Param("heldStatuses") Collection<BookingStatus> heldStatuses);
}
