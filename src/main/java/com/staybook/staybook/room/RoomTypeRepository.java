package com.staybook.staybook.room;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;

public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select rt from RoomType rt where rt.id = :id")
    Optional<RoomType> findByIdForUpdate(@Param("id") Long id);
    @Override
    @EntityGraph(attributePaths = "amenities")
    Optional<RoomType> findById(Long id);
    boolean existsByNameIgnoreCase(String name);
    @EntityGraph(attributePaths = "amenities")
    List<RoomType> findByStatusAndMaxGuestsGreaterThanEqualOrderByNameAsc(RoomTypeStatus status, int maxGuests);
}
