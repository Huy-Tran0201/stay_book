package com.staybook.staybook.config;

import com.staybook.staybook.common.exception.BusinessException;
import com.staybook.staybook.pricing.PricingService;
import com.staybook.staybook.room.RoomTypeRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class HomeController {
    private final RoomTypeRepository roomTypeRepository;
    private final PricingService pricingService;

    public HomeController(RoomTypeRepository roomTypeRepository, PricingService pricingService) {
        this.roomTypeRepository = roomTypeRepository;
        this.pricingService = pricingService;
    }

    @GetMapping("/")
    public String home() {
        return "public/home";
    }

    @GetMapping("/room-types/{id}")
    public String roomType(@PathVariable Long id, Model model) {
        var roomType = roomTypeRepository.findById(id)
            .orElseThrow(() -> new BusinessException("Không tìm thấy loại phòng."));
        model.addAttribute("roomType", roomType);
        model.addAttribute("pricing", pricingService.referencePrices(roomType));
        return "public/room-type";
    }
}
