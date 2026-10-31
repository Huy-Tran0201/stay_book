package com.staybook.staybook.booking;

import com.staybook.staybook.room.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class BookingRepositoryDataJpaTest {
    @Autowired private RoomTypeRepository roomTypeRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private BookingNightRepository bookingNightRepository;

    @Test
    void savesBookingWithBookingNight() {
        Room room = createRoom();
        Booking booking = bookingRepository.save(new Booking(
                "BK-261101-0001", "Nguyen Van A", "0900000000", room.getRoomType(), room,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 3),
                1600000L, 480000L, BookingStatus.PENDING_DEPOSIT,
                BookingSource.GUEST_ONLINE, LocalDateTime.of(2026, 10, 31, 10, 0)));
        bookingNightRepository.save(new BookingNight(booking, LocalDate.of(2026, 11, 1), 800000L));

        Booking loaded = bookingRepository.findById(booking.getId()).orElseThrow();
        assertThat(loaded.getCode()).isEqualTo("BK-261101-0001");
        assertThat(bookingNightRepository.findAll()).singleElement()
                .extracting(BookingNight::getPrice).isEqualTo(800000L);
    }

    @Test
    void overlapUsesHalfOpenIntervalAndAdjacentDatesDoNotOverlap() {
        Room room = createRoom();
        Booking booking = bookingRepository.save(new Booking(
                "BK-261107-0001", "Nguyen Van B", "0900000001", room.getRoomType(), room,
                LocalDate.of(2026, 11, 7), LocalDate.of(2026, 11, 8),
                800000L, 240000L, BookingStatus.CONFIRMED,
                BookingSource.STAFF_ASSISTED, LocalDateTime.of(2026, 10, 31, 10, 0)));
        List<Booking> adjacent = bookingRepository.findHeldOverlapping(
                room.getId(), LocalDate.of(2026, 11, 8), LocalDate.of(2026, 11, 9),
                List.of(BookingStatus.PENDING_DEPOSIT, BookingStatus.AWAITING_CONFIRMATION,
                        BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN));
        assertThat(adjacent).isEmpty();

        List<Booking> overlapping = bookingRepository.findHeldOverlapping(
                room.getId(), LocalDate.of(2026, 11, 7), LocalDate.of(2026, 11, 9),
                List.of(BookingStatus.PENDING_DEPOSIT, BookingStatus.AWAITING_CONFIRMATION,
                        BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN));
        assertThat(overlapping).containsExactly(booking);
    }

    private Room createRoom() {
        RoomType type = roomTypeRepository.save(new RoomType("Deluxe", 2, 800000L, RoomTypeStatus.ACTIVE));
        return roomRepository.save(new Room("101", 1, type, RoomStatus.ACTIVE));
    }
}
