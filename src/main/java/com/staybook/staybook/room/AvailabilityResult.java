package com.staybook.staybook.room;

public record AvailabilityResult(
    RoomType roomType,
    int availableRoomCount,
    long averageNightPrice,
    long totalPrice,
    String imageFileName
) {
}
