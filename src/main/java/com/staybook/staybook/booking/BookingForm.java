package com.staybook.staybook.booking;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public class BookingForm {
    @NotNull private Long roomTypeId;
    @NotNull private LocalDate checkInDate;
    @NotNull private LocalDate checkOutDate;
    @Min(1) private int guestCount;
    @NotBlank @Size(max = 120) private String guestName;
    @NotBlank @Size(max = 20) private String guestPhone;
    @Email @Size(max = 160) private String guestEmail;
    @Size(max = 500) private String note;
    private boolean nearCheckInConfirmed;

    public Long getRoomTypeId() { return roomTypeId; } public void setRoomTypeId(Long v) { roomTypeId = v; }
    public LocalDate getCheckInDate() { return checkInDate; } public void setCheckInDate(LocalDate v) { checkInDate = v; }
    public LocalDate getCheckOutDate() { return checkOutDate; } public void setCheckOutDate(LocalDate v) { checkOutDate = v; }
    public int getGuestCount() { return guestCount; } public void setGuestCount(int v) { guestCount = v; }
    public String getGuestName() { return guestName; } public void setGuestName(String v) { guestName = v; }
    public String getGuestPhone() { return guestPhone; } public void setGuestPhone(String v) { guestPhone = v; }
    public String getGuestEmail() { return guestEmail; } public void setGuestEmail(String v) { guestEmail = v; }
    public String getNote() { return note; } public void setNote(String v) { note = v; }
    public boolean isNearCheckInConfirmed() { return nearCheckInConfirmed; }
    public void setNearCheckInConfirmed(boolean v) { nearCheckInConfirmed = v; }
}
