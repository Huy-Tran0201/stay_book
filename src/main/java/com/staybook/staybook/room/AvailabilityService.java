package com.staybook.staybook.room;

import com.staybook.staybook.booking.BookingStatus;
import com.staybook.staybook.pricing.PricingService;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AvailabilityService {
    private static final java.util.Set<BookingStatus> HELD_STATUSES = EnumSet.of(
        BookingStatus.PENDING_DEPOSIT, BookingStatus.AWAITING_CONFIRMATION,
        BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN);

    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;
    private final RoomTypeImageRepository imageRepository;
    private final PricingService pricingService;

    public AvailabilityService(RoomTypeRepository roomTypeRepository, RoomRepository roomRepository,
            RoomTypeImageRepository imageRepository, PricingService pricingService) {
        this.roomTypeRepository = roomTypeRepository;
        this.roomRepository = roomRepository;
        this.imageRepository = imageRepository;
        this.pricingService = pricingService;
    }

    public List<AvailabilityResult> findAvailable(LocalDate checkIn, LocalDate checkOut, int guests) {
        if (checkIn == null || checkOut == null || !checkIn.isBefore(checkOut) || guests < 1) {
            throw new IllegalArgumentException("Thông tin tìm phòng không hợp lệ.");
        }
        return roomTypeRepository.findByStatusAndMaxGuestsGreaterThanEqualOrderByNameAsc(
                RoomTypeStatus.ACTIVE, guests).stream()
            .map(roomType -> {
                int count = roomRepository.findAvailableRooms(roomType.getId(), checkIn, checkOut,
                    HELD_STATUSES).size();
                if (count == 0) {
                    return null;
                }
                long total = pricingService.calculateRoomAmount(roomType, checkIn, checkOut);
                long nights = checkIn.until(checkOut).getDays();
                var images = imageRepository.findByRoomTypeIdOrderBySortOrder(roomType.getId());
                String imageFileName = images == null || images.isEmpty() ? null : images.get(0).getFileName();
                return new AvailabilityResult(roomType, count, total / nights, total, imageFileName);
            })
            .filter(java.util.Objects::nonNull)
            .toList();
    }
}
