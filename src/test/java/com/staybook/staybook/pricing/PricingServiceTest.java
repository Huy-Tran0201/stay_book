package com.staybook.staybook.pricing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.staybook.staybook.room.RoomType;
import com.staybook.staybook.room.RoomTypeStatus;
import com.staybook.staybook.room.SeasonPrice;
import com.staybook.staybook.room.SeasonPriceRepository;
import com.staybook.staybook.setting.Setting;
import com.staybook.staybook.setting.SettingRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PricingServiceTest {
    private SeasonPriceRepository seasons;
    private SettingRepository settings;
    private PricingService service;

    @BeforeEach
    void setUp() {
        seasons = mock(SeasonPriceRepository.class);
        settings = mock(SettingRepository.class);
        when(seasons.findAll()).thenReturn(List.of());
        when(settings.findByKey("deposit.percent")).thenReturn(Optional.of(new Setting("deposit.percent", "30")));
        service = new PricingService(seasons, settings);
    }

    @Test
    void calculatesNightPricesAndWeekendSurcharge() {
        RoomType roomType = new RoomType("Standard", 2, 500_000, RoomTypeStatus.ACTIVE);

        PricingResult result = service.calculate(roomType,
            LocalDate.of(2026, 11, 12), LocalDate.of(2026, 11, 16));

        assertEquals(4, result.nights().size());
        assertEquals(500_000, result.nights().get(0).nightPrice());
        assertEquals(50_000, result.nights().get(1).weekendSurcharge());
        assertEquals(550_000, result.nights().get(1).nightPrice());
        assertEquals(2_100_000, result.totalAmount());
        assertEquals(630_000, result.depositAmount());
    }

    @Test
    void matchesProjectExampleOne() {
        RoomType roomType = new RoomType("Deluxe", 3, 800_000, RoomTypeStatus.ACTIVE);

        PricingResult result = service.calculate(roomType,
            LocalDate.of(2026, 11, 6), LocalDate.of(2026, 11, 9));

        assertEquals(2_560_000, result.totalAmount());
        assertEquals(768_000, result.depositAmount());
        assertEquals(List.of(880_000L, 880_000L, 800_000L),
            result.nights().stream().map(NightPrice::nightPrice).toList());
    }

    @Test
    void matchesProjectExampleTwoWithSeasonPrice() {
        RoomType roomType = new RoomType("Deluxe", 3, 800_000, RoomTypeStatus.ACTIVE);
        SeasonPrice season = new SeasonPrice(roomType, "Cao điểm cuối năm",
            LocalDate.of(2026, 12, 20), LocalDate.of(2027, 1, 5), 1_000_000);
        when(seasons.findAll()).thenReturn(List.of(season));

        PricingResult result = service.calculate(roomType,
            LocalDate.of(2026, 12, 19), LocalDate.of(2026, 12, 22));

        assertEquals(2_880_000, result.totalAmount());
        assertEquals(864_000, result.depositAmount());
        assertEquals(List.of(880_000L, 1_000_000L, 1_000_000L),
            result.nights().stream().map(NightPrice::nightPrice).toList());
    }

    @Test
    void appliesSeasonAtBothInclusiveBoundariesAndUsesSeasonPriceForSurcharge() {
        RoomType roomType = new RoomType("Deluxe", 3, 800_000, RoomTypeStatus.ACTIVE);
        SeasonPrice season = new SeasonPrice(roomType, "Cao điểm",
            LocalDate.of(2026, 12, 20), LocalDate.of(2027, 1, 5), 1_000_000);
        when(seasons.findAll()).thenReturn(List.of(season));

        PricingResult result = service.calculate(roomType,
            LocalDate.of(2026, 12, 20), LocalDate.of(2027, 1, 7));

        assertEquals(18, result.nights().size());
        assertEquals(1_000_000, result.nights().get(0).basePrice());
        assertEquals(1_000_000, result.nights().get(16).basePrice());
        assertEquals(1_000_000, result.nights().get(16).nightPrice());
        assertEquals(800_000, result.nights().get(17).basePrice());
    }

    @Test
    void handlesSingleNightMonthAndYearBoundaries() {
        RoomType roomType = new RoomType("Standard", 2, 1_234_000, RoomTypeStatus.ACTIVE);

        PricingResult result = service.calculate(roomType,
            LocalDate.of(2026, 12, 31), LocalDate.of(2027, 1, 1));

        assertEquals(LocalDate.of(2026, 12, 31), result.nights().get(0).date());
        assertEquals(1_234_000, result.totalAmount());
        assertEquals(371_000, result.depositAmount());
    }

    @Test
    void roundsWeekendSurchargeToNearestDong() {
        RoomType roomType = new RoomType("Standard", 2, 123, RoomTypeStatus.ACTIVE);
        PricingResult result = service.calculate(roomType,
            LocalDate.of(2026, 11, 13), LocalDate.of(2026, 11, 14));

        assertEquals(12, result.nights().get(0).weekendSurcharge());
        assertEquals(135, result.totalAmount());
    }

    @Test
    void rejectsInvalidDateRange() {
        RoomType roomType = new RoomType("Standard", 2, 500_000, RoomTypeStatus.ACTIVE);

        assertThrows(IllegalArgumentException.class, () ->
            service.calculate(roomType, LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 10)));
    }
}
