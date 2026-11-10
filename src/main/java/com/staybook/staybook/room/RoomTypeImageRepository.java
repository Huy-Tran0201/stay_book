package com.staybook.staybook.room;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomTypeImageRepository extends JpaRepository<RoomTypeImage, Long> {
    long countByRoomTypeId(Long roomTypeId);
    java.util.List<RoomTypeImage> findByRoomTypeIdOrderBySortOrder(Long roomTypeId);
}
