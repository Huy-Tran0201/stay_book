package com.staybook.staybook.scheduler;

import com.staybook.staybook.booking.Booking;
import com.staybook.staybook.booking.BookingRepository;
import com.staybook.staybook.booking.BookingService;
import com.staybook.staybook.booking.BookingStatus;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BookingExpiryJob {
    private static final Logger log = LoggerFactory.getLogger(BookingExpiryJob.class);

    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final Clock clock;

    public BookingExpiryJob(BookingRepository bookingRepository, BookingService bookingService, Clock clock) {
        this.bookingRepository = bookingRepository;
        this.bookingService = bookingService;
        this.clock = clock;
    }

    @Scheduled(fixedDelay = 60000)
    public void run() {
        expirePendingBookings();
    }

    public void expirePendingBookings() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<Booking> expiredCandidates = bookingRepository.findByStatusAndHoldExpiresAtBefore(
                BookingStatus.PENDING_DEPOSIT, now);
        for (Booking candidate : expiredCandidates) {
            try {
                bookingService.expireBooking(candidate.getId(), now);
            } catch (Exception ex) {
                log.error("Lỗi khi xử lý hết hạn cho đặt phòng ID {}: {}", candidate.getId(), ex.getMessage(), ex);
            }
        }
    }
}
