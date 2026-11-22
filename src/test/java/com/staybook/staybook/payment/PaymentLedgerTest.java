package com.staybook.staybook.payment;

import com.staybook.staybook.audit.AuditService;
import com.staybook.staybook.booking.Booking;
import com.staybook.staybook.booking.BookingRepository;
import com.staybook.staybook.booking.BookingStateMachine;
import com.staybook.staybook.booking.BookingStatus;
import com.staybook.staybook.common.exception.BusinessException;
import com.staybook.staybook.user.Role;
import com.staybook.staybook.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentLedgerTest {
    @Mock PaymentRepository paymentRepository;
    @Mock SurchargeRepository surchargeRepository;
    @Mock BookingRepository bookingRepository;
    @Mock BookingStateMachine stateMachine;
    @Mock AuditService auditService;

    private PaymentService service() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-22T10:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
        return new PaymentService(paymentRepository, surchargeRepository, bookingRepository,
            stateMachine, auditService, clock);
    }

    @Test
    void ledgerCalculatesPaidRemainingAndRefundDue() {
        Booking booking = org.mockito.Mockito.mock(Booking.class);
        User recordedBy = org.mockito.Mockito.mock(User.class);
        java.time.LocalDateTime recordedAt = java.time.LocalDateTime.of(2026, 11, 22, 10, 0);
        when(booking.getId()).thenReturn(10L);
        when(booking.getTotalRoomAmount()).thenReturn(1_000_000L);
        when(booking.getRefundEligibleAmount()).thenReturn(300_000L);
        when(paymentRepository.findByBookingIdOrderByRecordedAtAsc(10L)).thenReturn(List.of(
            new Payment(booking, PaymentType.DEPOSIT, 300_000, PaymentMethod.BANK_TRANSFER, "", recordedBy, recordedAt),
            new Payment(booking, PaymentType.EXTRA_PAYMENT, 500_000, PaymentMethod.CASH, "", recordedBy, recordedAt),
            new Payment(booking, PaymentType.REFUND, 100_000, PaymentMethod.CASH, "", recordedBy, recordedAt)
        ));
        Surcharge surcharge = new Surcharge(booking, "Dọn phòng", 100_000, recordedBy, recordedAt);
        when(surchargeRepository.findByBookingIdOrderByRecordedAtAsc(10L)).thenReturn(List.of(surcharge));

        PaymentLedger ledger = service().ledger(booking);

        assertEquals(700_000, ledger.paid());
        assertEquals(1_100_000, ledger.totalDue());
        assertEquals(400_000, ledger.remaining());
        assertEquals(200_000, ledger.refundDue());
    }

    @Test
    void confirmDepositAcceptsEnoughMoneyAndRecordsDeposit() {
        Booking booking = org.mockito.Mockito.mock(Booking.class);
        User staff = staff();
        when(booking.getId()).thenReturn(10L);
        when(booking.getDepositAmount()).thenReturn(300_000L);
        when(booking.getStatus()).thenReturn(BookingStatus.AWAITING_CONFIRMATION);
        when(booking.getCode()).thenReturn("BK-261122-TEST");
        when(bookingRepository.findByCodeForUpdate("BK-261122-TEST")).thenReturn(Optional.of(booking));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service().confirmDeposit("BK-261122-TEST", 300_000, PaymentMethod.BANK_TRANSFER, staff);

        verify(paymentRepository).save(any(Payment.class));
        verify(stateMachine).transition(booking, BookingStatus.CONFIRMED, staff.getEmail(), "Lễ tân xác nhận tiền cọc");
        verify(auditService).record(staff, "RECORD_PAYMENT", "BOOKING", booking.getId(),
            "Ghi DEPOSIT 300000 đồng cho đặt phòng BK-261122-TEST");
    }

    @Test
    void confirmDepositRejectsInsufficientMoney() {
        Booking booking = org.mockito.Mockito.mock(Booking.class);
        User staff = org.mockito.Mockito.mock(User.class);
        when(staff.getRole()).thenReturn(Role.STAFF);
        when(booking.getDepositAmount()).thenReturn(300_000L);
        when(booking.getStatus()).thenReturn(BookingStatus.AWAITING_CONFIRMATION);
        when(bookingRepository.findByCodeForUpdate("BK-261122-TEST")).thenReturn(Optional.of(booking));

        BusinessException error = assertThrows(BusinessException.class,
            () -> service().confirmDeposit("BK-261122-TEST", 299_999,
                PaymentMethod.BANK_TRANSFER, staff));

        assertEquals("Số tiền nhận chưa đủ tiền cọc.", error.getMessage());
        verify(paymentRepository, never()).save(any(Payment.class));
        verifyNoInteractions(stateMachine);
    }

    @Test
    void guestCannotConfirmDeposit() {
        User guest = org.mockito.Mockito.mock(User.class);
        when(guest.getRole()).thenReturn(Role.GUEST);

        assertThrows(BusinessException.class,
            () -> service().confirmDeposit("BK-261122-TEST", 300_000,
                PaymentMethod.BANK_TRANSFER, guest));
    }

    private User staff() {
        User user = org.mockito.Mockito.mock(User.class);
        when(user.getRole()).thenReturn(Role.STAFF);
        when(user.getEmail()).thenReturn("staff1@staybook.local");
        return user;
    }
}
