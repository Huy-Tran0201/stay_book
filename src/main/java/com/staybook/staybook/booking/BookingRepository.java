package com.staybook.staybook.booking;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
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
