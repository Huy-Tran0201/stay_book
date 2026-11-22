package com.staybook.staybook.payment;

public record PaymentLedger(long paid, long totalDue, long remaining, long refundDue) {}
