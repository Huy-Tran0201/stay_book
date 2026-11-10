package com.staybook.staybook.room;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import com.staybook.staybook.booking.BookingStatus;
import com.staybook.staybook.pricing.PricingResult;
import com.staybook.staybook.pricing.PricingService;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AvailabilityServiceTest {
    private RoomTypeRepository roomTypes;
    private RoomRepository rooms;
    private RoomTypeImageRepository images;
    private PricingService pricing;
    private AvailabilityService service;
    private RoomType standard;

    @BeforeEach
    void setUp() {
        roomTypes = mock(RoomTypeRepository.class);
        rooms = mock(RoomRepository.class);
        images = mock(RoomTypeImageRepository.class);
        pricing = mock(PricingService.class);
        service = new AvailabilityService(roomTypes, rooms, images, pricing);
        standard = new RoomType("Standard", 2, 500_000, RoomTypeStatus.ACTIVE);
        when(roomTypes.findByStatusAndMaxGuestsGreaterThanEqualOrderByNameAsc(RoomTypeStatus.ACTIVE, 2))
            .thenReturn(List.of(standard));
        when(pricing.calculateRoomAmount(any(), any(), any())).thenReturn(1_000_000L);
    }

    @Test
    void adjacentBookingStillLeavesRoomAvailable() {
        Room room = new Room("101", 1, standard, RoomStatus.ACTIVE);
        when(rooms.findAvailableRooms(any(), eq(LocalDate.of(2026, 11, 10)),
            eq(LocalDate.of(2026, 11, 12)), any())).thenReturn(List.of(room));

        assertEquals(1, service.findAvailable(LocalDate.of(2026, 11, 10),
            LocalDate.of(2026, 11, 12), 2).get(0).availableRoomCount());
    }

    @Test
    void overlappingBookingMakesRoomUnavailable() {
        when(rooms.findAvailableRooms(any(), any(), any(), any())).thenReturn(List.of());

        assertEquals(0, service.findAvailable(LocalDate.of(2026, 11, 10),
            LocalDate.of(2026, 11, 12), 2).size());
    }

    @Test
    void maintenanceAndBlockedRoomsAreExcludedByAvailabilityQuery() {
        when(rooms.findAvailableRooms(any(), any(), any(), eq(EnumSet.of(
            BookingStatus.PENDING_DEPOSIT, BookingStatus.AWAITING_CONFIRMATION,
            BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN)))).thenReturn(List.of());

        assertEquals(0, service.findAvailable(LocalDate.of(2026, 11, 10),
            LocalDate.of(2026, 11, 12), 2).size());
    }

    @Test
    void roomTypeWithInsufficientCapacityIsNotQueried() {
        when(roomTypes.findByStatusAndMaxGuestsGreaterThanEqualOrderByNameAsc(RoomTypeStatus.ACTIVE, 3))
            .thenReturn(List.of());

        assertEquals(0, service.findAvailable(LocalDate.of(2026, 11, 10),
            LocalDate.of(2026, 11, 12), 3).size());
        verifyNoInteractions(rooms, pricing);
    }
}
