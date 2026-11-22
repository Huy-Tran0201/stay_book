package com.staybook.staybook.booking;

import com.staybook.staybook.common.exception.BusinessException;
import com.staybook.staybook.pricing.PricingService;
import com.staybook.staybook.room.RoomTypeRepository;
import com.staybook.staybook.setting.SettingRepository;
import com.staybook.staybook.user.User;
import com.staybook.staybook.user.UserRepository;
import jakarta.validation.Valid;
import java.time.*;
import java.util.Map;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class BookingController {
    private final BookingService bookingService;
    private final BookingStateMachine stateMachine;
    private final RoomTypeRepository roomTypes;
    private final UserRepository users;
    private final PricingService pricing;
    private final BookingRepository bookings;
    private final SettingRepository settings;
    private final Clock clock;
    public BookingController(BookingService bookingService, BookingStateMachine stateMachine,
            RoomTypeRepository roomTypes, UserRepository users, PricingService pricing, BookingRepository bookings,
            SettingRepository settings, Clock clock) {
        this.bookingService = bookingService; this.stateMachine = stateMachine; this.roomTypes = roomTypes;
        this.users = users; this.pricing = pricing; this.bookings = bookings; this.settings = settings; this.clock = clock;
    }

    @GetMapping("/booking/new")
    public String newBooking(@RequestParam Long roomTypeId,
            @RequestParam(name = "checkIn", required = false) LocalDate checkInDate,
            @RequestParam(name = "checkOut", required = false) LocalDate checkOutDate,
            @RequestParam int guests, Authentication auth, Model model) {
        User user = currentUser(auth);
        var type = roomTypes.findById(roomTypeId).orElseThrow(() -> new BusinessException("Không tìm thấy loại phòng."));
        BookingForm form = new BookingForm(); form.setRoomTypeId(roomTypeId);
        form.setCheckInDate(checkInDate); form.setCheckOutDate(checkOutDate); form.setGuestCount(guests);
        form.setGuestName(user.getFullName()); form.setGuestPhone(user.getPhone()); form.setGuestEmail(user.getEmail());
        model.addAttribute("form", form); model.addAttribute("roomType", type);
        if (checkInDate == null || checkOutDate == null || !checkInDate.isBefore(checkOutDate)) {
            model.addAttribute("message", "Ngày trả phải sau ngày nhận.");
            model.addAttribute("nearCheckIn", false);
            return "booking/new";
        }
        model.addAttribute("pricing", pricing.calculate(type, checkInDate, checkOutDate));
        model.addAttribute("nearCheckIn", LocalDateTime.now(clock).plusHours(48).isAfter(checkInDate.atTime(14, 0)));
        return "booking/new";
    }

    @PostMapping("/booking")
    public String create(@Valid @ModelAttribute("form") BookingForm form, BindingResult errors,
            Authentication auth, Model model, RedirectAttributes redirectAttributes) {
        if (errors.hasErrors()) return newBooking(form.getRoomTypeId(), form.getCheckInDate(), form.getCheckOutDate(),
            form.getGuestCount(), auth, model);
        try {
            Booking booking = bookingService.createBooking(form, currentUser(auth));
            return "redirect:/booking/" + booking.getCode() + "/payment";
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("message", ex.getMessage());
            return "redirect:/booking/new?roomTypeId=" + form.getRoomTypeId()
                + "&checkIn=" + form.getCheckInDate()
                + "&checkOut=" + form.getCheckOutDate()
                + "&guests=" + form.getGuestCount();
        }
    }

    @GetMapping("/booking/{code}/payment")
    public String payment(@PathVariable String code, Authentication auth, Model model) {
        Booking booking = bookings.findByCode(code).orElseThrow(() -> new BusinessException("Không tìm thấy đặt phòng."));
        if (booking.getUser() == null || !booking.getUser().getEmail().equals(currentUser(auth).getEmail()))
            throw new BusinessException("Bạn không có quyền với đặt phòng này.");
        model.addAttribute("booking", booking);
        String bankName = setting("bank.name", "");
        String account = setting("bank.account", "");
        String holder = setting("bank.holder", "StayBook");
        model.addAttribute("bank", Map.of("name", bankName.isBlank() ? "Ngân hàng" : bankName,
            "account", account.isBlank() ? "Liên hệ lễ tân" : account, "holder", holder));
        if (!bankName.isBlank() && !account.isBlank()) {
            model.addAttribute("qrUrl", "https://img.vietqr.io/image/"
                + URLEncoder.encode(bankName, StandardCharsets.UTF_8) + "-" + account
                + "-compact2.png?amount=" + booking.getDepositAmount()
                + "&addInfo=" + URLEncoder.encode(booking.getCode(), StandardCharsets.UTF_8));
        }
        return "booking/payment";
    }

    @PostMapping("/booking/{code}/paid-notice")
    public String paidNotice(@PathVariable String code, Authentication auth) {
        try {
            stateMachine.submitPaidNotice(code, currentUser(auth));
        } catch (BusinessException ex) {
            return "redirect:/booking/" + code + "/payment?error";
        }
        return "redirect:/booking/" + code + "/payment?submitted";
    }

    @GetMapping("/my-bookings")
    public String myBookings(Authentication auth, Model model) {
        model.addAttribute("bookings", bookings.findByUserIdOrderByCreatedAtDesc(currentUser(auth).getId()));
        return "booking/my-bookings";
    }

    @GetMapping("/my-bookings/{code}")
    public String bookingDetail(@PathVariable String code, Authentication auth, Model model) {
        User user = currentUser(auth);
        Booking booking = ownedBooking(code, user);
        model.addAttribute("booking", booking);
        return "booking/detail";
    }

    @PostMapping("/my-bookings/{code}/cancel")
    public String cancel(@PathVariable String code, @RequestParam(defaultValue = "Khách yêu cầu hủy") String reason,
            Authentication auth, Model model) {
        try {
            bookingService.cancelBooking(code, currentUser(auth), reason);
            return "redirect:/my-bookings/" + code + "?cancelled";
        } catch (BusinessException ex) {
            model.addAttribute("message", ex.getMessage());
            model.addAttribute("booking", ownedBooking(code, currentUser(auth)));
            return "booking/detail";
        }
    }

    private Booking ownedBooking(String code, User user) {
        Booking booking = bookings.findByCode(code)
            .orElseThrow(() -> new BusinessException("Không tìm thấy đặt phòng."));
        if (booking.getUser() == null || !booking.getUser().getId().equals(user.getId()))
            throw new BusinessException("Không tìm thấy đặt phòng.");
        return booking;
    }

    private User currentUser(Authentication auth) {
        if (auth == null) throw new BusinessException("Vui lòng đăng nhập để đặt phòng.");
        return users.findByEmail(auth.getName()).orElseThrow(() -> new BusinessException("Không tìm thấy tài khoản."));
    }
    private String setting(String key, String fallback) {
        return settings.findByKey(key).map(s -> s.getValue()).orElse(fallback);
    }
}
