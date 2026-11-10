package com.staybook.staybook.room;

import jakarta.validation.constraints.*;
import java.util.HashSet;
import java.util.Set;

public class RoomForm {
    @NotBlank(message = "Số phòng không được để trống")
    @Size(max = 20, message = "Số phòng tối đa 20 ký tự")
    private String roomNumber;
    @Min(value = 0, message = "Tầng không hợp lệ")
    private int floor;
    @NotNull(message = "Vui lòng chọn loại phòng")
    private Long roomTypeId;
    private RoomStatus status = RoomStatus.ACTIVE;
    public String getRoomNumber() { return roomNumber; } public void setRoomNumber(String v) { roomNumber = v; }
    public int getFloor() { return floor; } public void setFloor(int v) { floor = v; }
    public Long getRoomTypeId() { return roomTypeId; } public void setRoomTypeId(Long v) { roomTypeId = v; }
    public RoomStatus getStatus() { return status; } public void setStatus(RoomStatus v) { status = v; }
}
