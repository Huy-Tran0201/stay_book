package com.staybook.staybook.room;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.staybook.staybook.booking.*;
import com.staybook.staybook.common.exception.RoomStatusChangeException;
import java.time.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

class RoomAdminServiceTest {
    @Test
    void rejectsMaintenanceWhenHeldBookingStartsTodayOrLater() {
        RoomTypeRepository types = mock(RoomTypeRepository.class);
        RoomRepository rooms = mock(RoomRepository.class);
        AmenityRepository amenities = mock(AmenityRepository.class);
        RoomTypeImageRepository images = mock(RoomTypeImageRepository.class);
        BookingRepository bookings = mock(BookingRepository.class);
        ImageStorageService storage = mock(ImageStorageService.class);
        Room room = new Room("101", 1, new RoomType("Deluxe", 2, 100000, RoomTypeStatus.ACTIVE), RoomStatus.ACTIVE);
        Booking booking = mock(Booking.class);
        when(rooms.findById(1L)).thenReturn(Optional.of(room));
        when(bookings.findActiveBookingsFromDate(eq(null), any(LocalDate.class), anyCollection()))
            .thenReturn(List.of(booking));
        RoomAdminService service = new RoomAdminService(types, rooms, amenities, images, bookings, storage,
            Clock.fixed(Instant.parse("2026-11-10T01:05:39Z"), ZoneId.of("Asia/Ho_Chi_Minh")));

        assertThrows(RoomStatusChangeException.class, () -> service.changeRoomStatus(1L, RoomStatus.MAINTENANCE));
        org.junit.jupiter.api.Assertions.assertEquals(RoomStatus.ACTIVE, room.getStatus());
    }

    @Test
    void rejectsSixthImage() throws Exception {
        RoomTypeRepository types = mock(RoomTypeRepository.class);
        RoomRepository rooms = mock(RoomRepository.class);
        AmenityRepository amenities = mock(AmenityRepository.class);
        RoomTypeImageRepository images = mock(RoomTypeImageRepository.class);
        BookingRepository bookings = mock(BookingRepository.class);
        ImageStorageService storage = mock(ImageStorageService.class);
        RoomType type = mock(RoomType.class);
        when(type.getId()).thenReturn(1L);
        when(types.findByIdForUpdate(1L)).thenReturn(Optional.of(type));
        when(types.save(type)).thenReturn(type);
        when(images.countByRoomTypeId(1L)).thenReturn(5L);
        RoomAdminService service = new RoomAdminService(types, rooms, amenities, images, bookings, storage, Clock.systemUTC());
        RoomTypeForm form = new RoomTypeForm();
        form.setName("Deluxe"); form.setMaxGuests(2); form.setAreaM2(20); form.setBedType("Queen"); form.setBasePrice(100000);
        byte[] content;
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "png", output); content = output.toByteArray();
        MockMultipartFile sixth = new MockMultipartFile("images", "sixth.png", "image/png", content);
        assertThrows(com.staybook.staybook.common.exception.BusinessException.class,
            () -> service.saveRoomType(1L, form, new MockMultipartFile[]{sixth}));
    }
}
