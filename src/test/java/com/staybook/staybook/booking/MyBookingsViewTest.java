package com.staybook.staybook.booking;

import com.staybook.staybook.room.*;
import com.staybook.staybook.user.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MyBookingsViewTest {

    private static final String GUEST_EMAIL = "mybooking-test@staybook.local";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private RoomTypeRepository roomTypeRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private BookingRepository bookingRepository;

    private User guest;
    private RoomType roomType;
    private Room room;

    @BeforeEach
    void setUp() {
        guest = userRepository.findByEmail(GUEST_EMAIL).orElseGet(() ->
            userRepository.save(new User("Test Guest", GUEST_EMAIL, "0901234567",
                "hash", Role.GUEST, UserStatus.ACTIVE, LocalDateTime.now()))
        );

        roomType = roomTypeRepository.save(new RoomType("Deluxe View Test", 2, 700_000L, RoomTypeStatus.ACTIVE));
        room = roomRepository.save(new Room("MV" + (System.nanoTime() % 10000), 1, roomType, RoomStatus.ACTIVE));
    }

    @Test
    @DisplayName("Chưa đăng nhập truy cập /my-bookings thì chuyển hướng đến /login")
    void unauthenticatedRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/my-bookings"))
            .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = GUEST_EMAIL, roles = "GUEST")
    @DisplayName("Khách đã đăng nhập có đặt phòng (kể cả EXPIRED) truy cập /my-bookings trả về 200 OK và không bị lỗi 500")
    void myBookingsRendersSuccessfullyWithExpiredBooking() throws Exception {
        Booking booking = new Booking(
            "BK-" + System.currentTimeMillis(),
            guest.getFullName(),
            guest.getPhone(),
            roomType,
            room,
            LocalDate.now().plusDays(2),
            LocalDate.now().plusDays(4),
            1_400_000L,
            420_000L,
            BookingStatus.EXPIRED,
            BookingSource.GUEST_ONLINE,
            LocalDateTime.now()
        );
        booking.setUser(guest);
        booking.setGuestCount(2);
        booking.setHoldExpiresAt(LocalDateTime.now().minusMinutes(5));
        bookingRepository.save(booking);

        mockMvc.perform(get("/my-bookings"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Lịch sử đặt phòng")))
            .andExpect(content().string(containsString(booking.getCode())))
            .andExpect(content().string(containsString("Đặt phòng đã hết hạn giữ chỗ. Vui lòng đặt lại.")))
            .andExpect(content().string(containsString("Đặt lại")));
    }
}
