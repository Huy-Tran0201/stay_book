package com.staybook.staybook.config;

import com.staybook.staybook.common.exception.BusinessException;
import com.staybook.staybook.pricing.PricingService;
import com.staybook.staybook.room.Amenity;
import com.staybook.staybook.room.AmenityRepository;
import com.staybook.staybook.room.AvailabilityResult;
import com.staybook.staybook.room.AvailabilityService;
import com.staybook.staybook.room.RoomTypeImageRepository;
import com.staybook.staybook.room.RoomTypeRepository;
import com.staybook.staybook.setting.SettingRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.context.MessageSource;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {
    private final RoomTypeRepository roomTypeRepository;
    private final PricingService pricingService;
    private final AvailabilityService availabilityService;
    private final AmenityRepository amenityRepository;
    private final SettingRepository settingRepository;
    private final RoomTypeImageRepository imageRepository;
    private final Clock clock;
    private final MessageSource messageSource;

    public HomeController(RoomTypeRepository roomTypeRepository, PricingService pricingService,
            AvailabilityService availabilityService, AmenityRepository amenityRepository,
            SettingRepository settingRepository, RoomTypeImageRepository imageRepository, Clock clock,
            MessageSource messageSource) {
        this.roomTypeRepository = roomTypeRepository;
        this.pricingService = pricingService;
        this.availabilityService = availabilityService;
        this.amenityRepository = amenityRepository;
        this.settingRepository = settingRepository;
        this.imageRepository = imageRepository;
        this.clock = clock;
        this.messageSource = messageSource;
    }

    @GetMapping("/")
    public String home(Model model) {
        LocalDate today = LocalDate.now(clock);
        model.addAttribute("checkIn", today);
        model.addAttribute("checkOut", today.plusDays(1));
        model.addAttribute("guests", 1);
        return "public/home";
    }

    @GetMapping("/rooms")
    public String rooms(@RequestParam(required = false) LocalDate checkIn,
            @RequestParam(required = false) LocalDate checkOut,
            @RequestParam(required = false) Integer guests,
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice,
            @RequestParam(required = false) Integer minCapacity,
            @RequestParam(name = "amenity", required = false) Set<Long> amenityIds,
            @RequestParam(required = false, defaultValue = "priceAsc") String sort,
            Model model) {
        LocalDate today = LocalDate.now(clock);
        checkIn = checkIn == null ? today : checkIn;
        checkOut = checkOut == null ? checkIn.plusDays(1) : checkOut;
        guests = guests == null ? 1 : guests;
        addSearchModel(model, checkIn, checkOut, guests, minPrice, maxPrice, minCapacity, amenityIds, sort);

        String validationMessage = validateSearch(checkIn, checkOut, guests, today);
        if (validationMessage != null) {
            model.addAttribute("validationMessage",
                messageSource.getMessage(validationMessage, null, java.util.Locale.forLanguageTag("vi")));
            model.addAttribute("results", List.of());
        } else {
            List<AvailabilityResult> results = availabilityService.findAvailable(checkIn, checkOut, guests)
                .stream()
                .filter(result -> minPrice == null || result.totalPrice() >= minPrice)
                .filter(result -> maxPrice == null || result.totalPrice() <= maxPrice)
                .filter(result -> minCapacity == null || result.roomType().getMaxGuests() >= minCapacity)
                .filter(result -> amenityIds == null || amenityIds.isEmpty()
                    || result.roomType().getAmenities().stream().map(Amenity::getId).collect(Collectors.toSet())
                        .containsAll(amenityIds))
                .sorted(resultComparator(sort))
                .toList();
            model.addAttribute("results", results);
        }
        model.addAttribute("amenities", amenityRepository.findAll());
        return "public/rooms";
    }

    @GetMapping("/room-types/{id}")
    public String roomType(@PathVariable Long id,
            @RequestParam(required = false) LocalDate checkIn,
            @RequestParam(required = false) LocalDate checkOut,
            @RequestParam(required = false) Integer guests,
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice,
            @RequestParam(required = false) Integer minCapacity,
            @RequestParam(name = "amenity", required = false) Set<Long> amenityIds,
            @RequestParam(required = false, defaultValue = "priceAsc") String sort, Model model) {
        var roomType = roomTypeRepository.findById(id)
            .orElseThrow(() -> new BusinessException("Không tìm thấy loại phòng."));
        model.addAttribute("roomType", roomType);
        model.addAttribute("pricing", pricingService.referencePrices(roomType));
        LocalDate today = LocalDate.now(clock);
        model.addAttribute("checkIn", checkIn == null ? today : checkIn);
        model.addAttribute("checkOut", checkOut == null ? today.plusDays(1) : checkOut);
        model.addAttribute("guests", guests == null ? 1 : guests);
        addSearchModel(model, checkIn == null ? today : checkIn, checkOut == null ? today.plusDays(1) : checkOut,
            guests == null ? 1 : guests, minPrice, maxPrice, minCapacity, amenityIds, sort);
        model.addAttribute("settings", settingRepository.findAll().stream()
            .collect(Collectors.toMap(setting -> setting.getKey(), setting -> setting.getValue(),
                (first, ignored) -> first)));
        model.addAttribute("images", imageRepository.findByRoomTypeIdOrderBySortOrder(id));
        return "public/room-type";
    }

    private void addSearchModel(Model model, LocalDate checkIn, LocalDate checkOut, int guests,
            Long minPrice, Long maxPrice, Integer minCapacity, Set<Long> amenityIds, String sort) {
        model.addAttribute("checkIn", checkIn);
        model.addAttribute("checkOut", checkOut);
        model.addAttribute("guests", guests);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("minCapacity", minCapacity);
        model.addAttribute("selectedAmenities", amenityIds == null ? Set.of() : amenityIds);
        model.addAttribute("sort", sort);
    }

    private String validateSearch(LocalDate checkIn, LocalDate checkOut, int guests, LocalDate today) {
        if (checkIn.isBefore(today)) return "search.date.past";
        if (!checkIn.isBefore(checkOut)) return "search.date.order";
        if (checkIn.plusDays(30).isBefore(checkOut)) return "search.date.max";
        if (guests < 1) return "search.guests.invalid";
        return null;
    }

    private Comparator<AvailabilityResult> resultComparator(String sort) {
        return switch (sort) {
            case "priceDesc" -> Comparator.comparingLong(AvailabilityResult::totalPrice).reversed();
            case "capacity" -> Comparator.comparingInt(result -> result.roomType().getMaxGuests());
            default -> Comparator.comparingLong(AvailabilityResult::totalPrice);
        };
    }
}
