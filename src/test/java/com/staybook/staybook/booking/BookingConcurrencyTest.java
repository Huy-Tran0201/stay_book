package com.staybook.staybook.booking;

import com.staybook.staybook.room.*;
import com.staybook.staybook.setting.*;
import com.staybook.staybook.user.*;
import java.time.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class BookingConcurrencyTest {
    @Autowired private BookingService bookingService;
    @Autowired private RoomTypeRepository roomTypes;
    @Autowired private RoomRepository rooms;
    @Autowired private UserRepository users;
    @Autowired private SettingRepository settings;
    @Autowired private BookingRepository bookings;

    @Test
    void onlyOneConcurrentRequestGetsTheLastRoom() throws Exception {
        RoomType type = roomTypes.save(new RoomType("Concurrent", 2, 800000, RoomTypeStatus.ACTIVE));
        rooms.save(new Room("901", 9, type, RoomStatus.ACTIVE));
        settings.save(new Setting("deposit.percent", "30"));
        settings.save(new Setting("hold.minutes", "30"));
        settings.save(new Setting("max.pending.per.guest", "3"));
        settings.save(new Setting("refund.hours", "48"));
        User first = users.save(new User("A", "a" + System.nanoTime() + "@test.local", "0900000001",
            "hash", Role.GUEST, UserStatus.ACTIVE, LocalDateTime.now()));
        User second = users.save(new User("B", "b" + System.nanoTime() + "@test.local", "0900000002",
            "hash", Role.GUEST, UserStatus.ACTIVE, LocalDateTime.now()));
        BookingForm form = form(type.getId());
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Boolean> attempt = () -> {
            ready.countDown(); start.await();
            try { bookingService.createBooking(form, Thread.currentThread().getName().contains("1") ? first : second); return true; }
            catch (RuntimeException ex) { return false; }
        };
        Future<Boolean> one = executor.submit(attempt);
        Future<Boolean> two = executor.submit(attempt);
        ready.await(); start.countDown();
        int successful = (one.get(10, TimeUnit.SECONDS) ? 1 : 0) + (two.get(10, TimeUnit.SECONDS) ? 1 : 0);
        executor.shutdownNow();
        assertThat(successful).isEqualTo(1);
        assertThat(bookings.count()).isEqualTo(1);
    }

    private BookingForm form(Long typeId) {
        BookingForm form = new BookingForm();
        form.setRoomTypeId(typeId); form.setCheckInDate(LocalDate.of(2026, 11, 25));
        form.setCheckOutDate(LocalDate.of(2026, 11, 27)); form.setGuestCount(1);
        form.setGuestName("Guest"); form.setGuestPhone("0900000000"); form.setNearCheckInConfirmed(true);
        return form;
    }
}
