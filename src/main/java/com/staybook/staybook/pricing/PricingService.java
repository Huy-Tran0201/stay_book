package com.staybook.staybook.pricing;

import com.staybook.staybook.room.RoomType;
import com.staybook.staybook.room.SeasonPrice;
import com.staybook.staybook.room.SeasonPriceRepository;
import com.staybook.staybook.setting.Setting;
import com.staybook.staybook.setting.SettingRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PricingService {
    private static final String DEPOSIT_PERCENT_KEY = "deposit.percent";
    private final SeasonPriceRepository seasonPriceRepository;
    private final SettingRepository settingRepository;

    public PricingService(SeasonPriceRepository seasonPriceRepository, SettingRepository settingRepository) {
        this.seasonPriceRepository = seasonPriceRepository;
        this.settingRepository = settingRepository;
    }

    public PricingResult calculate(RoomType roomType, LocalDate checkIn, LocalDate checkOut) {
        List<NightPrice> nights = calculateNights(roomType, checkIn, checkOut);
        long total = nights.stream().mapToLong(NightPrice::nightPrice).sum();
        return new PricingResult(nights, total, roundDeposit(total, readDepositPercent()));
    }

    public long calculateRoomAmount(RoomType roomType, LocalDate checkIn, LocalDate checkOut) {
        return calculateNights(roomType, checkIn, checkOut).stream()
            .mapToLong(NightPrice::nightPrice).sum();
    }

    private List<NightPrice> calculateNights(RoomType roomType, LocalDate checkIn, LocalDate checkOut) {
        if (roomType == null || checkIn == null || checkOut == null || !checkIn.isBefore(checkOut)) {
            throw new IllegalArgumentException("Khoảng ngày ở không hợp lệ.");
        }

        List<NightPrice> nights = new ArrayList<>();
        for (LocalDate date = checkIn; date.isBefore(checkOut); date = date.plusDays(1)) {
            long basePrice = findBasePrice(roomType, date);
            long surcharge = isWeekendNight(date)
                ? percentageOf(basePrice, roomType.getWeekendSurchargePercent())
                : 0;
            long nightPrice = Math.addExact(basePrice, surcharge);
            nights.add(new NightPrice(date, basePrice, surcharge, nightPrice));
        }
        return nights;
    }

    public PricingReference referencePrices(RoomType roomType) {
        long weekdayPrice = roomType.getBasePrice();
        long weekendPrice = Math.addExact(
            weekdayPrice, percentageOf(weekdayPrice, roomType.getWeekendSurchargePercent()));
        return new PricingReference(weekdayPrice, weekendPrice);
    }

    private long findBasePrice(RoomType roomType, LocalDate date) {
        for (SeasonPrice season : seasonPriceRepository.findAll()) {
            if (belongsToRoomType(season, roomType)
                    && !date.isBefore(season.getFromDate())
                    && !date.isAfter(season.getToDate())) {
                return season.getBasePrice();
            }
        }
        return roomType.getBasePrice();
    }

    private boolean belongsToRoomType(SeasonPrice season, RoomType roomType) {
        if (season.getRoomType() == roomType) {
            return true;
        }
        return season.getRoomType() != null && season.getRoomType().getId() != null
                && season.getRoomType().getId().equals(roomType.getId());
    }

    private int readDepositPercent() {
        Setting setting = settingRepository.findByKey(DEPOSIT_PERCENT_KEY)
            .orElseThrow(() -> new IllegalStateException("Thiếu cấu hình " + DEPOSIT_PERCENT_KEY + "."));
        try {
            return Integer.parseInt(setting.getValue());
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("Cấu hình " + DEPOSIT_PERCENT_KEY + " không hợp lệ.", exception);
        }
    }

    private static boolean isWeekendNight(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.FRIDAY || date.getDayOfWeek() == DayOfWeek.SATURDAY;
    }

    private static long percentageOf(long amount, int percent) {
        if (percent < 0) {
            throw new IllegalArgumentException("Tỷ lệ phụ thu không được âm.");
        }
        return BigDecimal.valueOf(amount)
            .multiply(BigDecimal.valueOf(percent))
            .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
            .longValueExact();
    }

    private static long roundDeposit(long total, int percent) {
        if (percent < 0 || percent > 100) {
            throw new IllegalStateException("Tỷ lệ cọc không hợp lệ.");
        }
        BigDecimal deposit = BigDecimal.valueOf(total)
            .multiply(BigDecimal.valueOf(percent))
            .divide(BigDecimal.valueOf(100), 0, RoundingMode.CEILING);
        return deposit.divide(BigDecimal.valueOf(1000), 0, RoundingMode.CEILING)
            .multiply(BigDecimal.valueOf(1000)).longValueExact();
    }
}
