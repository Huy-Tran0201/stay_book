package com.staybook.staybook.room;

import com.staybook.staybook.common.exception.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminRoomController {
    private final RoomAdminService service;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;
    private final AmenityRepository amenityRepository;

    public AdminRoomController(RoomAdminService service, RoomTypeRepository roomTypeRepository,
                               RoomRepository roomRepository, AmenityRepository amenityRepository) {
        this.service = service; this.roomTypeRepository = roomTypeRepository;
        this.roomRepository = roomRepository; this.amenityRepository = amenityRepository;
    }

    @GetMapping("/room-types")
    public String roomTypes(Model model) {
        model.addAttribute("roomTypes", roomTypeRepository.findAll());
        return "admin/room-types";
    }

    @GetMapping("/room-types/new")
    public String newRoomType(Model model) {
        model.addAttribute("form", new RoomTypeForm());
        model.addAttribute("amenities", amenityRepository.findAll());
        return "admin/room-type-form";
    }

    @GetMapping("/room-types/{id}/edit")
    public String editRoomType(@PathVariable Long id, Model model) {
        RoomType type = roomTypeRepository.findById(id).orElseThrow(() -> new BusinessException("Không tìm thấy loại phòng."));
        model.addAttribute("form", RoomTypeForm.from(type)); model.addAttribute("id", id);
        model.addAttribute("amenities", amenityRepository.findAll());
        return "admin/room-type-form";
    }

    @PostMapping("/room-types/save")
    public String saveRoomType(@RequestParam(required = false) Long id, @Valid @ModelAttribute("form") RoomTypeForm form,
                               BindingResult result, @RequestParam(required = false) MultipartFile[] images,
                               Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) { model.addAttribute("id", id); model.addAttribute("amenities", amenityRepository.findAll()); return "admin/room-type-form"; }
        try {
            service.saveRoomType(id, form, images);
            redirect.addFlashAttribute("successMessage", "Đã lưu loại phòng.");
            return "redirect:/admin/room-types";
        } catch (BusinessException e) {
            result.reject("roomType.error", e.getMessage());
            model.addAttribute("id", id); model.addAttribute("amenities", amenityRepository.findAll());
            return "admin/room-type-form";
        }
    }

    @PostMapping("/room-types/{id}/deactivate")
    public String deactivateRoomType(@PathVariable Long id, RedirectAttributes redirect) {
        service.deactivateRoomType(id); redirect.addFlashAttribute("successMessage", "Đã ngừng hoạt động loại phòng.");
        return "redirect:/admin/room-types";
    }

    @GetMapping("/rooms")
    public String rooms(Model model) { model.addAttribute("rooms", roomRepository.findAllByOrderByRoomTypeNameAscRoomNumberAsc()); return "admin/rooms"; }

    @GetMapping("/rooms/new")
    public String newRoom(Model model) { addRoomFormModel(model, new RoomForm(), null); return "admin/room-form"; }

    @GetMapping("/rooms/{id}/edit")
    public String editRoom(@PathVariable Long id, Model model) {
        Room room = roomRepository.findById(id).orElseThrow(() -> new BusinessException("Không tìm thấy phòng."));
        RoomForm form = new RoomForm(); form.setRoomNumber(room.getRoomNumber()); form.setFloor(room.getFloor());
        form.setRoomTypeId(room.getRoomType().getId()); form.setStatus(room.getStatus());
        addRoomFormModel(model, form, id); return "admin/room-form";
    }

    @PostMapping("/rooms/save")
    public String saveRoom(@RequestParam(required = false) Long id, @Valid @ModelAttribute("form") RoomForm form,
                           BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) { addRoomFormModel(model, form, id); return "admin/room-form"; }
        try {
            service.saveRoom(id, form); redirect.addFlashAttribute("successMessage", "Đã lưu phòng.");
            return "redirect:/admin/rooms";
        } catch (RoomStatusChangeException e) {
            result.reject("room.error", e.getMessage()); model.addAttribute("affectedBookings", e.getAffectedBookings());
            addRoomFormModel(model, form, id); return "admin/room-form";
        } catch (BusinessException e) {
            result.reject("room.error", e.getMessage()); addRoomFormModel(model, form, id); return "admin/room-form";
        }
    }

    private void addRoomFormModel(Model model, RoomForm form, Long id) {
        model.addAttribute("form", form); model.addAttribute("id", id);
        model.addAttribute("roomTypes", roomTypeRepository.findAll());
        model.addAttribute("statuses", RoomStatus.values());
    }
}
