package com.staybook.staybook.pricing;

import java.util.List;

public record PricingResult(List<NightPrice> nights, long totalAmount, long depositAmount) {
    public PricingResult {
        nights = List.copyOf(nights);
    }
}
