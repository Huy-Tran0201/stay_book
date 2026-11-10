package com.staybook.staybook.room;

import jakarta.validation.constraints.*;
import java.util.HashSet;
import java.util.Set;

public class RoomTypeForm {
    @NotBlank(message = "Tên loại phòng không được để trống")
    @Size(max = 100, message = "Tên loại phòng tối đa 100 ký tự")
    private String name;
    @Size(max = 1000, message = "Mô tả tối đa 1.000 ký tự")
    private String description;
    @Min(value = 1, message = "Sức chứa từ 1 đến 10 khách")
    @Max(value = 10, message = "Sức chứa từ 1 đến 10 khách")
    private int maxGuests;
    @Min(value = 1, message = "Diện tích phải lớn hơn 0")
    private int areaM2;
    @NotBlank(message = "Loại giường không được để trống")
    private String bedType;
    @Positive(message = "Giá cơ bản phải lớn hơn 0")
    private long basePrice;
    @Min(value = 0, message = "Phụ thu không được âm")
    @Max(value = 100, message = "Phụ thu tối đa 100%")
    private int weekendSurchargePercent = 10;
    private Set<Long> amenityIds = new HashSet<>();
    public String getName() { return name; } public void setName(String v) { name = v; }
    public String getDescription() { return description; } public void setDescription(String v) { description = v; }
    public int getMaxGuests() { return maxGuests; } public void setMaxGuests(int v) { maxGuests = v; }
    public int getAreaM2() { return areaM2; } public void setAreaM2(int v) { areaM2 = v; }
    public String getBedType() { return bedType; } public void setBedType(String v) { bedType = v; }
    public long getBasePrice() { return basePrice; } public void setBasePrice(long v) { basePrice = v; }
    public int getWeekendSurchargePercent() { return weekendSurchargePercent; }
    public void setWeekendSurchargePercent(int v) { weekendSurchargePercent = v; }
    public Set<Long> getAmenityIds() { return amenityIds; } public void setAmenityIds(Set<Long> v) { amenityIds = v; }
    public static RoomTypeForm from(RoomType entity) {
        RoomTypeForm form = new RoomTypeForm(); form.name = entity.getName(); form.description = entity.getDescription();
        form.maxGuests = entity.getMaxGuests(); form.areaM2 = entity.getAreaM2(); form.bedType = entity.getBedType();
        form.basePrice = entity.getBasePrice(); form.weekendSurchargePercent = entity.getWeekendSurchargePercent();
        entity.getAmenities().forEach(a -> form.amenityIds.add(a.getId())); return form;
    }
}
