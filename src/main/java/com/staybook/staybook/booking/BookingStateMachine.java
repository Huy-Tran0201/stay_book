package com.staybook.staybook.booking;

import com.staybook.staybook.common.exception.BusinessException;
import com.staybook.staybook.user.User;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingStateMachine {
    private final BookingRepository bookingRepository;
    private final BookingEventRepository eventRepository;
    private final Clock clock;
    public BookingStateMachine(BookingRepository b, BookingEventRepository e, Clock clock) {
        bookingRepository = b; eventRepository = e; this.clock = clock;
    }
    @Transactional
    public Booking submitPaidNotice(String code, User actor) {
        Booking booking = owned(code, actor);
        if (booking.getStatus() != BookingStatus.PENDING_DEPOSIT)
            throw new BusinessException("Đặt phòng không còn ở trạng thái chờ cọc.");
        if (booking.getHoldExpiresAt() != null && LocalDateTime.now(clock).isAfter(booking.getHoldExpiresAt()))
            throw new BusinessException("Thời gian giữ chỗ đã hết. Vui lòng đặt lại.");
        return transition(booking, BookingStatus.AWAITING_CONFIRMATION, actor.getEmail(), "Khách báo đã chuyển khoản");
    }
    @Transactional
    public Booking transition(Booking booking, BookingStatus target, String actor, String reason) {
        BookingStatus from = booking.getStatus();
        if (!allowed(from, target)) throw new BusinessException("Chuyển trạng thái đặt phòng không hợp lệ.");
        booking.changeStatus(target); bookingRepository.save(booking);
        eventRepository.save(new BookingEvent(booking, from, target, actor, reason, LocalDateTime.now(clock)));
        return booking;
    }
    private boolean allowed(BookingStatus from, BookingStatus to) {
        return switch (from) {
            case PENDING_DEPOSIT -> EnumSet.of(BookingStatus.AWAITING_CONFIRMATION, BookingStatus.CONFIRMED,
                BookingStatus.CANCELLED, BookingStatus.EXPIRED).contains(to);
            case AWAITING_CONFIRMATION -> EnumSet.of(BookingStatus.CONFIRMED, BookingStatus.CANCELLED).contains(to);
            case CONFIRMED -> EnumSet.of(BookingStatus.CHECKED_IN, BookingStatus.CANCELLED, BookingStatus.NO_SHOW).contains(to);
            case CHECKED_IN -> to == BookingStatus.CHECKED_OUT;
            default -> false;
        };
    }
    private Booking owned(String code, User actor) {
        Booking b = bookingRepository.findByCodeForUpdate(code)
            .orElseThrow(() -> new BusinessException("Không tìm thấy đặt phòng."));
        if (b.getUser() == null || actor == null || !Objects.equals(b.getUser().getId(), actor.getId()))
            throw new BusinessException("Bạn không có quyền với đặt phòng này.");
        return b;
    }
}
