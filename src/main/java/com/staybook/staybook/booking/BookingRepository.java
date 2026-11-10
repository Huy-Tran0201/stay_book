package com.staybook.staybook.booking;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    boolean existsByRoomTypeId(Long roomTypeId);
    boolean existsByRoomId(Long roomId);
    @Query("select b from Booking b where b.room.id = :roomId and b.status in :statuses and b.checkOutDate > :fromDate order by b.checkInDate")
    List<Booking> findActiveBookingsFromDate(@Param("roomId") Long roomId,
                                             @Param("fromDate") LocalDate fromDate,
                                             @Param("statuses") Collection<BookingStatus> statuses);
    @Query("""
        select b from Booking b
        where b.room.id = :roomId
          and b.status in :heldStatuses
          and b.checkInDate < :checkOut
          and :checkIn < b.checkOutDate
        """)
    List<Booking> findHeldOverlapping(@Param("roomId") Long roomId,
                                      @Param("checkIn") LocalDate checkIn,
                                      @Param("checkOut") LocalDate checkOut,
                                      @Param("heldStatuses") Collection<BookingStatus> heldStatuses);
}
