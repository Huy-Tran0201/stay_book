package com.staybook.staybook.pricing;

import java.time.LocalDate;

public record NightPrice(LocalDate date, long basePrice, long weekendSurcharge, long nightPrice) {
}
