package com.staybook.staybook.room;

import com.staybook.staybook.booking.*;
import com.staybook.staybook.common.exception.*;
import java.time.Clock;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class RoomAdminService {
    private static final Set<BookingStatus> HELD = EnumSet.of(BookingStatus.PENDING_DEPOSIT,
        BookingStatus.AWAITING_CONFIRMATION, BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN);
    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;
    private final AmenityRepository amenityRepository;
    private final RoomTypeImageRepository imageRepository;
    private final BookingRepository bookingRepository;
    private final ImageStorageService imageStorage;
    private final Clock clock;

    public RoomAdminService(RoomTypeRepository roomTypeRepository, RoomRepository roomRepository,
            AmenityRepository amenityRepository, RoomTypeImageRepository imageRepository,
            BookingRepository bookingRepository, ImageStorageService imageStorage, Clock clock) {
        this.roomTypeRepository = roomTypeRepository; this.roomRepository = roomRepository;
        this.amenityRepository = amenityRepository; this.imageRepository = imageRepository;
        this.bookingRepository = bookingRepository; this.imageStorage = imageStorage; this.clock = clock;
    }

    @Transactional
    public RoomType saveRoomType(Long id, RoomTypeForm form, MultipartFile[] images) {
        RoomType entity = id == null ? new RoomType(form.getName(), form.getMaxGuests(), form.getBasePrice(), RoomTypeStatus.ACTIVE)
            : roomTypeRepository.findByIdForUpdate(id).orElseThrow(() -> new BusinessException("Không tìm thấy loại phòng."));
        Set<Amenity> amenities = new HashSet<>(amenityRepository.findAllById(form.getAmenityIds()));
        if (amenities.size() != form.getAmenityIds().size()) throw new BusinessException("Danh sách tiện nghi không hợp lệ.");
        entity.update(form.getName(), form.getDescription(), form.getMaxGuests(), form.getAreaM2(), form.getBedType(),
            form.getBasePrice(), form.getWeekendSurchargePercent(), amenities);
        entity = roomTypeRepository.save(entity);
        long existing = imageRepository.countByRoomTypeId(entity.getId());
        int nextOrder = (int) existing + 1;
        if (images != null) {
            if (existing + Arrays.stream(images).filter(f -> f != null && !f.isEmpty()).count() > 5)
                throw new BusinessException("Mỗi loại phòng chỉ được tối đa 5 ảnh.");
            for (MultipartFile image : images) {
                if (image != null && !image.isEmpty())
                    imageRepository.save(new RoomTypeImage(entity, imageStorage.store(image), nextOrder++));
            }
        }
        return entity;
    }

    @Transactional
    public void deactivateRoomType(Long id) {
        RoomType roomType = roomTypeRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new BusinessException("Không tìm thấy loại phòng."));
        roomType.changeStatus(RoomTypeStatus.INACTIVE);
    }

    @Transactional
    public Room saveRoom(Long id, RoomForm form) {
        RoomType type = roomTypeRepository.findById(form.getRoomTypeId())
            .orElseThrow(() -> new BusinessException("Không tìm thấy loại phòng."));
        Room room = id == null ? new Room(form.getRoomNumber(), form.getFloor(), type, form.getStatus())
            : roomRepository.findById(id).orElseThrow(() -> new BusinessException("Không tìm thấy phòng."));
        if (id == null && roomRepository.existsByRoomNumberIgnoreCase(form.getRoomNumber()))
            throw new BusinessException("Số phòng đã tồn tại.");
        if (id != null && !room.getRoomNumber().equalsIgnoreCase(form.getRoomNumber())
                && roomRepository.existsByRoomNumberIgnoreCase(form.getRoomNumber()))
            throw new BusinessException("Số phòng đã tồn tại.");
        room.update(form.getRoomNumber(), form.getFloor(), type);
        if (id != null && form.getStatus() != room.getStatus()) changeRoomStatus(room, form.getStatus());
        return roomRepository.save(room);
    }

    @Transactional
    public void changeRoomStatus(Long id, RoomStatus status) {
        Room room = roomRepository.findById(id).orElseThrow(() -> new BusinessException("Không tìm thấy phòng."));
        changeRoomStatus(room, status);
    }

    private void changeRoomStatus(Room room, RoomStatus status) {
        if (status != RoomStatus.ACTIVE) {
            List<Booking> affected = bookingRepository.findActiveBookingsFromDate(room.getId(),
                LocalDate.now(clock), HELD);
            if (!affected.isEmpty())
                throw new RoomStatusChangeException("Không thể chuyển trạng thái vì phòng còn booking giữ chỗ từ hôm nay.",
                    affected);
        }
        room.changeStatus(status);
    }
}
