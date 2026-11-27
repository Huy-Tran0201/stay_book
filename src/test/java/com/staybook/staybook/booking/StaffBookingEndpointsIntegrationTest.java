package com.staybook.staybook.booking;

import com.staybook.staybook.payment.*;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StaffBookingEndpointsIntegrationTest {

    private static final String STAFF_EMAIL = "staff-endpoint-test@staybook.local";
    private static final String GUEST_EMAIL = "guest-endpoint-test@staybook.local";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private RoomTypeRepository roomTypeRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private SurchargeRepository surchargeRepository;

    private User staff;
    private User guest;
    private RoomType roomType;
    private Room room;

    @BeforeEach
    void setUp() {
        staff = userRepository.findByEmail(STAFF_EMAIL).orElseGet(() ->
                userRepository.save(new User("Staff Test", STAFF_EMAIL, "0901112222",
                        "hash", Role.STAFF, UserStatus.ACTIVE, LocalDateTime.now()))
        );
        guest = userRepository.findByEmail(GUEST_EMAIL).orElseGet(() ->
                userRepository.save(new User("Guest Test", GUEST_EMAIL, "0903334444",
                        "hash", Role.GUEST, UserStatus.ACTIVE, LocalDateTime.now()))
        );

        roomType = roomTypeRepository.save(new RoomType("Deluxe Endpoint Test", 3, 800_000L, RoomTypeStatus.ACTIVE));
        room = roomRepository.save(new Room("EP" + (System.nanoTime() % 10000), 2, roomType, RoomStatus.ACTIVE));
    }

    private Booking createBooking(BookingStatus status, LocalDate checkIn, LocalDate checkOut) {
        String code = "BK-EP-" + (System.nanoTime() % 100000);
        Booking booking = new Booking(code, "Khach Test", "0909998888",
                roomType, room, checkIn, checkOut, 2_560_000L, 768_000L,
                status, BookingSource.GUEST_ONLINE, LocalDateTime.now());
        booking.setUser(guest);
        return bookingRepository.save(booking);
    }

    @Test
    @DisplayName("Khách (GUEST) không được phép gọi POST /staff/** (403 Forbidden)")
    @WithMockUser(username = GUEST_EMAIL, roles = "GUEST")
    void guestCannotAccessStaffEndpoints() throws Exception {
        Booking booking = createBooking(BookingStatus.CONFIRMED, LocalDate.now(), LocalDate.now().plusDays(2));

        mockMvc.perform(post("/staff/bookings/" + booking.getCode() + "/check-in").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Thiếu CSRF token bị từ chối 403 Forbidden")
    @WithMockUser(username = STAFF_EMAIL, roles = "STAFF")
    void requestWithoutCsrfIsForbidden() throws Exception {
        Booking booking = createBooking(BookingStatus.CONFIRMED, LocalDate.now(), LocalDate.now().plusDays(2));

        mockMvc.perform(post("/staff/bookings/" + booking.getCode() + "/check-in"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Lễ tân hoàn thành trọn vòng đời: nhận phòng → phụ phí → thanh toán → trả phòng")
    @WithMockUser(username = STAFF_EMAIL, roles = "STAFF")
    void fullStaffLifecycleCheckInSurchargePaymentCheckOut() throws Exception {
        // 1. Tạo booking CONFIRMED hôm nay, đã có cọc 768.000 ₫
        Booking booking = createBooking(BookingStatus.CONFIRMED, LocalDate.now(), LocalDate.now().plusDays(3));
        paymentRepository.save(new Payment(booking, PaymentType.DEPOSIT, 768_000L, PaymentMethod.BANK_TRANSFER,
                "Cọc", staff, LocalDateTime.now()));

        // 2. Nhận phòng (Check-in) kèm CCCD 4 số cuối
        mockMvc.perform(post("/staff/bookings/" + booking.getCode() + "/check-in")
                        .with(csrf())
                        .param("idDocumentType", "CCCD")
                        .param("idDocumentLast4", "9988"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("successMessage", "Nhận phòng thành công."));

        Booking checkedIn = bookingRepository.findByCode(booking.getCode()).orElseThrow();
        assertEquals(BookingStatus.CHECKED_IN, checkedIn.getStatus());
        assertNotNull(checkedIn.getCheckedInAt());
        assertEquals("CCCD - ****9988", checkedIn.getIdDocumentNote());

        // 3. Thêm phụ phí 150.000 ₫
        mockMvc.perform(post("/staff/bookings/" + booking.getCode() + "/surcharges")
                        .with(csrf())
                        .param("name", "Giặt ủi")
                        .param("amount", "150000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("successMessage", "Đã thêm phụ phí thành công."));

        // 4. Thử trả phòng khi chưa thu đủ tiền → báo lỗi "Còn phải thu 1.942.000 ₫."
        mockMvc.perform(post("/staff/bookings/" + booking.getCode() + "/check-out")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("errorMessage", "Còn phải thu 1.942.000 ₫."));

        // 5. Thanh toán thêm 1.942.000 ₫
        mockMvc.perform(post("/staff/bookings/" + booking.getCode() + "/payments")
                        .with(csrf())
                        .param("type", "EXTRA_PAYMENT")
                        .param("amount", "1942000")
                        .param("method", "CASH")
                        .param("note", "Thu tiền mặt khi trả phòng"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("successMessage", "Đã ghi nhận thanh toán thành công."));

        // 6. Trả phòng thành công
        mockMvc.perform(post("/staff/bookings/" + booking.getCode() + "/check-out")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("successMessage", "Trả phòng thành công."));

        Booking checkedOut = bookingRepository.findByCode(booking.getCode()).orElseThrow();
        assertEquals(BookingStatus.CHECKED_OUT, checkedOut.getStatus());
        assertNotNull(checkedOut.getCheckedOutAt());
    }

    @Test
    @DisplayName("Hủy do cơ sở: hoàn 100% cọc và ghi nhận hoàn tiền Payment(REFUND)")
    @WithMockUser(username = STAFF_EMAIL, roles = "STAFF")
    void facilityCancelAndRefundFlow() throws Exception {
        Booking booking = createBooking(BookingStatus.CONFIRMED, LocalDate.now().plusDays(2), LocalDate.now().plusDays(4));
        paymentRepository.save(new Payment(booking, PaymentType.DEPOSIT, 768_000L, PaymentMethod.BANK_TRANSFER,
                "Cọc", staff, LocalDateTime.now()));

        // Lễ tân hủy do cơ sở
        mockMvc.perform(post("/staff/bookings/" + booking.getCode() + "/cancel")
                        .with(csrf())
                        .param("cause", "FACILITY_CANCEL")
                        .param("reason", "Khách sạn sửa chữa đột xuất"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("successMessage", "Đã hủy đặt phòng thành công."));

        Booking cancelled = bookingRepository.findByCode(booking.getCode()).orElseThrow();
        assertEquals(BookingStatus.CANCELLED, cancelled.getStatus());
        assertEquals(768_000L, cancelled.getRefundEligibleAmount());

        // Lễ tân xác nhận đã hoàn tiền
        mockMvc.perform(post("/staff/bookings/" + booking.getCode() + "/refund")
                        .with(csrf())
                        .param("amount", "768000")
                        .param("method", "BANK_TRANSFER")
                        .param("note", "Đã CK hoàn cọc"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("successMessage", "Đã ghi nhận hoàn tiền thành công."));

        // Kiểm tra đã có bản ghi REFUND
        var refundPayments = paymentRepository.findByBookingIdOrderByRecordedAtAsc(cancelled.getId()).stream()
                .filter(p -> p.getType() == PaymentType.REFUND)
                .toList();
        assertEquals(1, refundPayments.size());
        assertEquals(768_000L, refundPayments.get(0).getAmount());
    }
}
