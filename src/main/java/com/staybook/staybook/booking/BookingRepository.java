package com.staybook.staybook.booking;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    boolean existsByCode(String code);
    @EntityGraph(attributePaths = {"user", "room", "roomType"})
    java.util.Optional<Booking> findByCode(String code);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"user", "room", "roomType"})
    @Query("select b from Booking b where b.code = :code")
    java.util.Optional<Booking> findByCodeForUpdate(@Param("code") String code);
    java.util.List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);
    java.util.List<Booking> findByStatusAndHoldExpiresAtBefore(BookingStatus status, java.time.LocalDateTime time);
    long countByUserIdAndStatusIn(Long userId, Collection<BookingStatus> statuses);
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
