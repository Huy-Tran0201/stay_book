package com.staybook.staybook.booking;

import com.staybook.staybook.common.exception.BusinessException;
import com.staybook.staybook.pricing.PricingResult;
import com.staybook.staybook.pricing.PricingService;
import com.staybook.staybook.room.*;
import com.staybook.staybook.setting.SettingRepository;
import com.staybook.staybook.user.User;
import com.staybook.staybook.audit.AuditService;
import com.staybook.staybook.payment.Payment;
import com.staybook.staybook.payment.PaymentRepository;
import com.staybook.staybook.payment.PaymentType;
import com.staybook.staybook.common.exception.ResourceNotFoundException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;

@Service
public class BookingService {
    private static final Set<BookingStatus> HELD = EnumSet.of(
        BookingStatus.PENDING_DEPOSIT, BookingStatus.AWAITING_CONFIRMATION,
        BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN);
    private static final String NO_ROOM = "Loại phòng này vừa hết phòng trong các ngày bạn chọn. Vui lòng chọn ngày hoặc loại phòng khác.";
    private final BookingRepository bookingRepository;
    private final BookingNightRepository nightRepository;
    private final BookingEventRepository eventRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;
    private final PricingService pricingService;
    private final SettingRepository settingRepository;
    private final Clock clock;
    private final CancellationPolicy cancellationPolicy;
    private final AuditService auditService;
    private final PaymentRepository paymentRepository;

    public BookingService(BookingRepository bookingRepository, BookingNightRepository nightRepository,
            BookingEventRepository eventRepository, RoomTypeRepository roomTypeRepository,
            RoomRepository roomRepository, PricingService pricingService,
            SettingRepository settingRepository, Clock clock, AuditService auditService,
            PaymentRepository paymentRepository) {
        this.bookingRepository = bookingRepository; this.nightRepository = nightRepository;
        this.eventRepository = eventRepository; this.roomTypeRepository = roomTypeRepository;
        this.roomRepository = roomRepository; this.pricingService = pricingService;
        this.settingRepository = settingRepository; this.clock = clock;
        this.cancellationPolicy = new CancellationPolicy();
        this.auditService = auditService;
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public Booking createBooking(BookingForm form, User user) {
        if (form == null || user == null) throw new BusinessException("Thông tin đặt phòng không hợp lệ.");
        validateDates(form.getCheckInDate(), form.getCheckOutDate());
        RoomType roomType = roomTypeRepository.findByIdForUpdate(form.getRoomTypeId())
            .orElseThrow(() -> new BusinessException("Không tìm thấy loại phòng."));
        validateRequest(form, roomType);
        long pendingLimit = setting("max.pending.per.guest", 3);
        long current = bookingRepository.countByUserIdAndStatusIn(user.getId(),
            EnumSet.of(BookingStatus.PENDING_DEPOSIT, BookingStatus.AWAITING_CONFIRMATION));
        if (current >= pendingLimit) throw new BusinessException(
            "Bạn đã có quá nhiều đặt phòng đang chờ. Vui lòng thanh toán hoặc hủy bớt đặt phòng đang chờ.");
        Room room = roomRepository.findAvailableRooms(roomType.getId(), form.getCheckInDate(),
                form.getCheckOutDate(), HELD).stream().findFirst().orElseThrow(() -> new BusinessException(NO_ROOM));
        PricingResult pricing = pricingService.calculate(roomType, form.getCheckInDate(), form.getCheckOutDate());
        LocalDateTime now = LocalDateTime.now(clock);
        Booking booking = new Booking(nextCode(now), form.getGuestName().trim(), form.getGuestPhone().trim(),
            roomType, room, form.getCheckInDate(), form.getCheckOutDate(), pricing.totalAmount(),
            pricing.depositAmount(), BookingStatus.PENDING_DEPOSIT, BookingSource.GUEST_ONLINE, now);
        booking.setUser(user); booking.setGuestCount(form.getGuestCount()); booking.setGuestEmail(form.getGuestEmail());
        booking.setNote(form.getNote());
        booking.setHoldExpiresAt(now.plusMinutes(setting("hold.minutes", 30)));
        booking = bookingRepository.save(booking);
        for (var night : pricing.nights()) nightRepository.save(new BookingNight(booking, night.date(), night.nightPrice()));
        eventRepository.save(new BookingEvent(booking, null, BookingStatus.PENDING_DEPOSIT,
            user.getEmail(), "Tạo đặt phòng", now));
        return booking;
    }

    @Transactional
    public Booking cancelBooking(String code, User user, String reason) {
        Booking booking = bookingRepository.findByCodeForUpdate(code)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đặt phòng."));
        if (booking.getUser() == null || user == null || !Objects.equals(booking.getUser().getId(), user.getId()))
            throw new ResourceNotFoundException("Không tìm thấy đặt phòng.");
        if (!EnumSet.of(BookingStatus.PENDING_DEPOSIT, BookingStatus.AWAITING_CONFIRMATION,
                BookingStatus.CONFIRMED).contains(booking.getStatus()))
            throw new BusinessException("Đặt phòng này không thể hủy.");
        LocalDateTime now = LocalDateTime.now(clock);
        boolean checkNeeded = booking.getStatus() == BookingStatus.AWAITING_CONFIRMATION;
        long refund = cancellationPolicy.refundEligibleAmount(
            booking, now, setting("refund.hours", 48), recordedDepositAmount(booking));
        BookingStatus old = booking.getStatus();
        booking.cancel(CancelCause.GUEST_REQUEST, reason, now, refund, checkNeeded);
        bookingRepository.save(booking);
        eventRepository.save(new BookingEvent(booking, old, BookingStatus.CANCELLED,
            user.getEmail(), reason, now));
        auditService.record(user, "CANCEL_BOOKING",
            "Hủy đặt phòng " + booking.getCode() + ", số tiền dự kiến hoàn " + refund + " đồng");
        return booking;
    }

    @Transactional(readOnly = true)
    public Booking findOwnedBooking(String code, User user) {
        Booking booking = bookingRepository.findByCode(code)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đặt phòng."));
        if (booking.getUser() == null || user == null || !Objects.equals(booking.getUser().getId(), user.getId())) {
            throw new ResourceNotFoundException("Không tìm thấy đặt phòng.");
        }
        return booking;
    }

    @Transactional(readOnly = true)
    public long refundPreview(String code, User user) {
        Booking booking = findOwnedBooking(code, user);
        if (!EnumSet.of(BookingStatus.PENDING_DEPOSIT, BookingStatus.AWAITING_CONFIRMATION,
                BookingStatus.CONFIRMED).contains(booking.getStatus())) {
            return 0;
        }
        return cancellationPolicy.refundEligibleAmount(
            booking, LocalDateTime.now(clock), setting("refund.hours", 48),
            recordedDepositAmount(booking));
    }

    private long recordedDepositAmount(Booking booking) {
        return paymentRepository.findByBookingIdOrderByRecordedAtAsc(booking.getId()).stream()
            .filter(payment -> payment.getType() == PaymentType.DEPOSIT)
            .mapToLong(com.staybook.staybook.payment.Payment::getAmount)
            .sum();
    }

    @Transactional
    @Scheduled(fixedDelay = 60000)
    public void expirePendingBookings() {
        LocalDateTime now = LocalDateTime.now(clock);
        for (Booking booking : bookingRepository.findByStatusAndHoldExpiresAtBefore(
                BookingStatus.PENDING_DEPOSIT, now)) {
            booking.changeStatus(BookingStatus.EXPIRED);
            bookingRepository.save(booking);
            eventRepository.save(new BookingEvent(booking, BookingStatus.PENDING_DEPOSIT,
                BookingStatus.EXPIRED, "SYSTEM", "Hết hạn giữ chỗ", now));
        }
    }

    private void validateDates(LocalDate in, LocalDate out) {
        LocalDate today = LocalDate.now(clock);
        if (in == null || out == null || in.isBefore(today) || !in.isBefore(out))
            throw new BusinessException("Ngày nhận/trả phòng không hợp lệ.");
        if (in.until(out).getDays() > 30) throw new BusinessException("Thời gian ở tối đa là 30 đêm.");
    }
    private void validateRequest(BookingForm form, RoomType roomType) {
        if (form.getGuestCount() < 1 || form.getGuestCount() > roomType.getMaxGuests())
            throw new BusinessException("Số khách vượt quá sức chứa của loại phòng.");
        LocalDateTime nearLimit = form.getCheckInDate().atTime(14, 0).minusHours(setting("refund.hours", 48));
        if (LocalDateTime.now(clock).isAfter(nearLimit) && !form.isNearCheckInConfirmed())
            throw new BusinessException("Đặt phòng này sẽ không được hoàn cọc nếu hủy.");
    }
    private long setting(String key, long fallback) {
        return settingRepository.findByKey(key).map(s -> {
            try { return Long.parseLong(s.getValue()); }
            catch (NumberFormatException e) { throw new BusinessException("Cấu hình " + key + " không hợp lệ."); }
        }).orElse(fallback);
    }
    private String nextCode(LocalDateTime now) {
        String prefix = "BK-" + now.format(DateTimeFormatter.ofPattern("yyMMdd")) + "-";
        for (int i = 0; i < 20; i++) {
            String code = prefix + String.format("%04X", new Random().nextInt(0x10000));
            if (!bookingRepository.existsByCode(code)) return code;
        }
        throw new BusinessException("Không thể tạo mã đặt phòng, vui lòng thử lại.");
    }
}
