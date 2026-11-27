package com.staybook.staybook.config;

import com.staybook.staybook.booking.*;
import com.staybook.staybook.payment.PaymentRepository;
import com.staybook.staybook.payment.SurchargeRepository;
import com.staybook.staybook.payment.PaymentService;
import com.staybook.staybook.room.RoomTypeRepository;
import com.staybook.staybook.user.User;
import com.staybook.staybook.user.UserRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.staybook.staybook.common.exception.BusinessException;

@Controller
public class StaffController {
    private final BookingRepository bookings;
    private final BookingNightRepository nights;
    private final BookingEventRepository events;
    private final RoomTypeRepository roomTypes;
    private final PaymentRepository payments;
    private final SurchargeRepository surcharges;
    private final PaymentService paymentService;
    private final BookingService bookingService;
    private final UserRepository users;
    private final Clock clock;

    public StaffController(BookingRepository bookings, BookingNightRepository nights,
            BookingEventRepository events, RoomTypeRepository roomTypes,
            PaymentRepository payments, SurchargeRepository surcharges,
            PaymentService paymentService, BookingService bookingService,
            UserRepository users, Clock clock) {
        this.bookings = bookings;
        this.nights = nights;
        this.events = events;
        this.roomTypes = roomTypes;
        this.payments = payments;
        this.surcharges = surcharges;
        this.paymentService = paymentService;
        this.bookingService = bookingService;
        this.users = users;
        this.clock = clock;
    }

    @GetMapping("/staff/today")
    public String today(Model model) {
        LocalDate today = LocalDate.now(clock);
        LocalDateTime now = LocalDateTime.now(clock);
        List<Booking> confirmed = bookings.findByStatus(BookingStatus.CONFIRMED);
        List<Booking> checkedIn = bookings.findByStatus(BookingStatus.CHECKED_IN);
        List<Booking> awaiting = bookings.findByStatus(BookingStatus.AWAITING_CONFIRMATION);
        List<Booking> pending = bookings.findByStatus(BookingStatus.PENDING_DEPOSIT);

        model.addAttribute("checkIns", confirmed.stream()
            .filter(b -> b.getCheckInDate().equals(today)).toList());
        model.addAttribute("checkOuts", checkedIn.stream()
            .filter(b -> b.getCheckOutDate().equals(today)).toList());
        model.addAttribute("staying", checkedIn);
        model.addAttribute("awaiting", awaiting);
        model.addAttribute("pending", pending.stream()
            .filter(b -> b.getHoldExpiresAt() != null && b.getHoldExpiresAt().isAfter(now)).toList());

        List<Booking> overdueAwaiting = awaiting.stream()
            .filter(b -> events.findByBookingIdOrderByAtAsc(b.getId()).stream()
                .filter(e -> e.getToStatus() == BookingStatus.AWAITING_CONFIRMATION)
                .map(com.staybook.staybook.booking.BookingEvent::getAt)
                .findFirst()
                .map(reportedAt -> reportedAt.isBefore(now.minusHours(24)))
                .orElse(false)).toList();
        model.addAttribute("overdueCodes", overdueAwaiting.stream().map(Booking::getCode).toList());
        model.addAttribute("today", today);
        return "staff/today";
    }

    @GetMapping("/staff/bookings")
    public String bookings(@RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) LocalDate checkIn,
            @RequestParam(required = false) LocalDate checkOut,
            @RequestParam(required = false) Long roomTypeId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        String searchText = search == null || search.isBlank() ? null : search.trim();
        model.addAttribute("result", bookings.search(status, checkIn, checkOut, roomTypeId, searchText, pageable));
        model.addAttribute("roomTypes", roomTypes.findAll());
        model.addAttribute("statuses", BookingStatus.values());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("checkIn", checkIn);
        model.addAttribute("checkOut", checkOut);
        model.addAttribute("roomTypeId", roomTypeId);
        model.addAttribute("search", searchText);
        return "staff/bookings";
    }

    @GetMapping("/staff/bookings/{code}")
    public String bookingDetail(@PathVariable String code, Model model) {
        Booking booking = bookings.findByCode(code)
            .orElseThrow(() -> new com.staybook.staybook.common.exception.ResourceNotFoundException("Không tìm thấy đặt phòng."));
        populateDetail(model, booking);
        return "staff/booking-detail";
    }

    @PostMapping("/staff/bookings/{code}/confirm-deposit")
    public String confirmDeposit(@PathVariable String code,
            @RequestParam long amount,
            @RequestParam com.staybook.staybook.payment.PaymentMethod method,
            Authentication auth, RedirectAttributes redirectAttributes) {
        try {
            paymentService.confirmDeposit(code, amount, method, currentUser(auth));
            redirectAttributes.addFlashAttribute("successMessage", "Đã xác nhận tiền cọc.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/staff/bookings/" + code;
    }

    @PostMapping("/staff/bookings/{code}/reject-deposit")
    public String rejectDeposit(@PathVariable String code,
            @RequestParam(defaultValue = "Lễ tân từ chối tiền cọc") String reason,
            Authentication auth, RedirectAttributes redirectAttributes) {
        try {
            paymentService.rejectDeposit(code, reason, currentUser(auth));
            redirectAttributes.addFlashAttribute("successMessage", "Đã từ chối tiền cọc.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/staff/bookings/" + code;
    }

    @PostMapping("/staff/bookings/{code}/check-in")
    public String checkIn(@PathVariable String code,
            @RequestParam(required = false) String idDocumentType,
            @RequestParam(required = false) String idDocumentLast4,
            Authentication auth, RedirectAttributes redirectAttributes) {
        try {
            bookingService.checkIn(code, idDocumentType, idDocumentLast4, currentUser(auth));
            redirectAttributes.addFlashAttribute("successMessage", "Nhận phòng thành công.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/staff/bookings/" + code;
    }

    @PostMapping("/staff/bookings/{code}/surcharges")
    public String addSurcharge(@PathVariable String code,
            @RequestParam String name,
            @RequestParam long amount,
            Authentication auth, RedirectAttributes redirectAttributes) {
        try {
            paymentService.addSurcharge(code, name, amount, currentUser(auth));
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm phụ phí thành công.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/staff/bookings/" + code;
    }

    @PostMapping("/staff/bookings/{code}/payments")
    public String addPayment(@PathVariable String code,
            @RequestParam(defaultValue = "EXTRA_PAYMENT") com.staybook.staybook.payment.PaymentType type,
            @RequestParam long amount,
            @RequestParam com.staybook.staybook.payment.PaymentMethod method,
            @RequestParam(required = false) String note,
            Authentication auth, RedirectAttributes redirectAttributes) {
        try {
            paymentService.addPayment(code, type, amount, method, note, currentUser(auth));
            redirectAttributes.addFlashAttribute("successMessage",
                type == com.staybook.staybook.payment.PaymentType.REFUND
                    ? "Đã ghi nhận hoàn tiền thành công." : "Đã ghi nhận thanh toán thành công.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/staff/bookings/" + code;
    }

    @PostMapping("/staff/bookings/{code}/check-out")
    public String checkOut(@PathVariable String code,
            Authentication auth, RedirectAttributes redirectAttributes) {
        try {
            bookingService.checkOut(code, currentUser(auth));
            redirectAttributes.addFlashAttribute("successMessage", "Trả phòng thành công.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/staff/bookings/" + code;
    }

    @PostMapping("/staff/bookings/{code}/cancel")
    public String cancel(@PathVariable String code,
            @RequestParam CancelCause cause,
            @RequestParam(required = false) String reason,
            Authentication auth, RedirectAttributes redirectAttributes) {
        try {
            bookingService.staffCancel(code, cause, reason, currentUser(auth));
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy đặt phòng thành công.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/staff/bookings/" + code;
    }

    @PostMapping("/staff/bookings/{code}/refund")
    public String refund(@PathVariable String code,
            @RequestParam long amount,
            @RequestParam com.staybook.staybook.payment.PaymentMethod method,
            @RequestParam(required = false) String note,
            Authentication auth, RedirectAttributes redirectAttributes) {
        try {
            paymentService.recordRefund(code, amount, method, note, currentUser(auth));
            redirectAttributes.addFlashAttribute("successMessage", "Đã ghi nhận hoàn tiền thành công.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/staff/bookings/" + code;
    }

    @PostMapping("/staff/bookings/{code}/no-show")
    public String noShow(@PathVariable String code,
            Authentication auth, RedirectAttributes redirectAttributes) {
        try {
            bookingService.noShow(code, currentUser(auth));
            redirectAttributes.addFlashAttribute("successMessage", "Đã ghi nhận khách không đến.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/staff/bookings/" + code;
    }

    private void populateDetail(Model model, Booking booking) {
        model.addAttribute("booking", booking);
        model.addAttribute("nights", nights.findByBookingIdOrderByDateAsc(booking.getId()));
        model.addAttribute("payments", payments.findByBookingIdOrderByRecordedAtAsc(booking.getId()));
        model.addAttribute("surcharges", surcharges.findByBookingIdOrderByRecordedAtAsc(booking.getId()));
        model.addAttribute("events", events.findByBookingIdOrderByAtAsc(booking.getId()));
        model.addAttribute("ledger", paymentService.ledger(booking));
        LocalDate today = LocalDate.now(clock);
        LocalDateTime now = LocalDateTime.now(clock);
        model.addAttribute("today", today);
        model.addAttribute("now", now);
        model.addAttribute("canCheckIn", booking.getStatus() == BookingStatus.CONFIRMED && !today.isBefore(booking.getCheckInDate()));
        model.addAttribute("canNoShow", booking.getStatus() == BookingStatus.CONFIRMED && !now.isBefore(booking.getCheckInDate().plusDays(1).atStartOfDay()));
        model.addAttribute("guestCancelRefund", bookingService.previewCancelRefund(booking, CancelCause.GUEST_REQUEST));
        model.addAttribute("facilityCancelRefund", bookingService.previewCancelRefund(booking, CancelCause.FACILITY_CANCEL));
    }

    private User currentUser(Authentication auth) {
        return users.findByEmail(auth.getName())
            .orElseThrow(() -> new BusinessException("Không tìm thấy tài khoản."));
    }
}
