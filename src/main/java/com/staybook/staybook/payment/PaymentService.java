package com.staybook.staybook.payment;

import com.staybook.staybook.audit.AuditService;
import com.staybook.staybook.booking.Booking;
import com.staybook.staybook.booking.BookingRepository;
import com.staybook.staybook.booking.BookingStateMachine;
import com.staybook.staybook.booking.BookingStatus;
import com.staybook.staybook.booking.CancelCause;
import com.staybook.staybook.common.exception.BusinessException;
import com.staybook.staybook.common.exception.ResourceNotFoundException;
import com.staybook.staybook.user.Role;
import com.staybook.staybook.user.User;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final SurchargeRepository surchargeRepository;
    private final BookingRepository bookingRepository;
    private final BookingStateMachine stateMachine;
    private final AuditService auditService;
    private final Clock clock;

    public PaymentService(PaymentRepository paymentRepository, SurchargeRepository surchargeRepository,
            BookingRepository bookingRepository, BookingStateMachine stateMachine,
            AuditService auditService, Clock clock) {
        this.paymentRepository = paymentRepository;
        this.surchargeRepository = surchargeRepository;
        this.bookingRepository = bookingRepository;
        this.stateMachine = stateMachine;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PaymentLedger ledger(Booking booking) {
        long paid = paymentRepository.findByBookingIdOrderByRecordedAtAsc(booking.getId()).stream()
            .mapToLong(payment -> switch (payment.getType()) {
                case DEPOSIT, EXTRA_PAYMENT, ADJUSTMENT -> payment.getAmount();
                case REFUND -> -payment.getAmount();
            }).sum();
        long surchargeTotal = surchargeRepository.findByBookingIdOrderByRecordedAtAsc(booking.getId()).stream()
            .mapToLong(Surcharge::getAmount).sum();
        long totalDue = booking.getTotalRoomAmount() + surchargeTotal;
        long refundPaid = paymentRepository.findByBookingIdOrderByRecordedAtAsc(booking.getId()).stream()
            .filter(payment -> payment.getType() == PaymentType.REFUND)
            .mapToLong(Payment::getAmount).sum();
        long refundDue = Math.max(0, booking.getRefundEligibleAmount() - refundPaid);
        return new PaymentLedger(paid, totalDue, totalDue - paid, refundDue);
    }

    @Transactional
    public Payment recordPayment(Booking booking, PaymentType type, long amount,
            PaymentMethod method, String note, User recordedBy) {
        requireStaff(recordedBy);
        if (booking == null) throw new BusinessException("Không tìm thấy đặt phòng.");
        if (type != PaymentType.ADJUSTMENT && amount <= 0)
            throw new BusinessException("Số tiền phải lớn hơn 0.");
        if (type == PaymentType.REFUND && amount < 0)
            throw new BusinessException("Số tiền hoàn không hợp lệ.");
        LocalDateTime now = LocalDateTime.now(clock);
        Payment payment = paymentRepository.save(
            new Payment(booking, type, amount, method, note, recordedBy, now));
        auditService.record(recordedBy, "RECORD_PAYMENT", "BOOKING", booking.getId(),
            "Ghi " + type + " " + amount + " đồng cho đặt phòng " + booking.getCode());
        return payment;
    }

    @Transactional
    public Booking confirmDeposit(String code, long amount, PaymentMethod method, User actor) {
        requireStaff(actor);
        Booking booking = bookingRepository.findByCodeForUpdate(code)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đặt phòng."));
        if (!EnumSet.of(BookingStatus.PENDING_DEPOSIT, BookingStatus.AWAITING_CONFIRMATION)
                .contains(booking.getStatus()))
            throw new BusinessException("Đặt phòng không còn chờ xác nhận cọc.");
        if (amount < booking.getDepositAmount())
            throw new BusinessException("Số tiền nhận chưa đủ tiền cọc.");

        recordPayment(booking, PaymentType.DEPOSIT, amount, method, "Xác nhận tiền cọc", actor);
        stateMachine.transition(booking, BookingStatus.CONFIRMED, actor.getEmail(), "Lễ tân xác nhận tiền cọc");
        return booking;
    }

    @Transactional
    public Booking rejectDeposit(String code, String reason, User actor) {
        requireStaff(actor);
        Booking booking = bookingRepository.findByCodeForUpdate(code)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đặt phòng."));
        if (!EnumSet.of(BookingStatus.PENDING_DEPOSIT, BookingStatus.AWAITING_CONFIRMATION)
                .contains(booking.getStatus()))
            throw new BusinessException("Đặt phòng không còn chờ xác nhận cọc.");
        String safeReason = reason == null || reason.isBlank() ? "Lễ tân từ chối tiền cọc" : reason.trim();
        LocalDateTime now = LocalDateTime.now(clock);
        stateMachine.transition(booking, BookingStatus.CANCELLED, actor.getEmail(), safeReason);
        booking.cancel(CancelCause.DEPOSIT_REJECTED, safeReason, now, 0, false);
        bookingRepository.save(booking);
        auditService.record(actor, "REJECT_DEPOSIT", "BOOKING", booking.getId(),
            "Từ chối cọc cho đặt phòng " + booking.getCode() + ": " + safeReason);
        return booking;
    }

    @Transactional
    public Surcharge addSurcharge(String code, String name, long amount, User actor) {
        requireStaff(actor);
        if (name == null || name.isBlank()) {
            throw new BusinessException("Tên phụ phí không được để trống.");
        }
        if (amount <= 0) {
            throw new BusinessException("Số tiền phụ phí phải lớn hơn 0.");
        }
        Booking booking = bookingRepository.findByCodeForUpdate(code)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đặt phòng."));
        if (EnumSet.of(BookingStatus.CANCELLED, BookingStatus.CHECKED_OUT,
                BookingStatus.NO_SHOW, BookingStatus.EXPIRED).contains(booking.getStatus())) {
            throw new BusinessException("Không thể thêm phụ phí cho đặt phòng ở trạng thái này.");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        Surcharge surcharge = surchargeRepository.save(
            new Surcharge(booking, name.trim(), amount, actor, now));
        auditService.record(actor, "ADD_SURCHARGE", "BOOKING", booking.getId(),
            "Thêm phụ phí " + name.trim() + " " + amount + " đồng cho đặt phòng " + booking.getCode());
        return surcharge;
    }

    @Transactional
    public Payment addPayment(String code, PaymentType type, long amount,
                              PaymentMethod method, String note, User actor) {
        requireStaff(actor);
        if (amount <= 0) {
            throw new BusinessException("Số tiền phải lớn hơn 0.");
        }
        if (method == null) {
            throw new BusinessException("Hình thức thanh toán không hợp lệ.");
        }
        Booking booking = bookingRepository.findByCodeForUpdate(code)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đặt phòng."));
        PaymentType paymentType = (type != null) ? type : PaymentType.EXTRA_PAYMENT;
        if (paymentType == PaymentType.REFUND) {
            PaymentLedger currentLedger = ledger(booking);
            if (currentLedger.refundDue() <= 0) {
                throw new BusinessException("Đặt phòng không có số tiền cần hoàn.");
            }
            if (amount > currentLedger.refundDue()) {
                throw new BusinessException("Số tiền hoàn vượt quá số tiền cần hoàn.");
            }
        }
        String safeNote = (note != null && !note.isBlank()) ? note.trim()
            : (paymentType == PaymentType.EXTRA_PAYMENT ? "Thanh toán thêm" : paymentType.name());
        return recordPayment(booking, paymentType, amount, method, safeNote, actor);
    }

    @Transactional
    public Payment recordRefund(String code, long amount, PaymentMethod method, String note, User actor) {
        return addPayment(code, PaymentType.REFUND, amount, method,
            (note != null && !note.isBlank()) ? note : "Hoàn tiền cho khách", actor);
    }

    private void requireStaff(User actor) {
        if (actor == null || (actor.getRole() != Role.STAFF && actor.getRole() != Role.ADMIN))
            throw new BusinessException("Chỉ lễ tân hoặc quản trị mới được ghi nhận thanh toán.");
    }
}
