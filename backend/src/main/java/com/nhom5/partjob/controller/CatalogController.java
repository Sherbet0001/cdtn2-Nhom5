package com.nhom5.partjob.controller;

import com.nhom5.partjob.dto.ProfileDtos.LocationInfo;
import com.nhom5.partjob.repository.LocationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/locations")
@Tag(name = "Danh mục")
public class CatalogController {

    private final LocationRepository locationRepository;

    public CatalogController(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    @Operation(summary = "Lấy danh sách địa điểm dùng trong hồ sơ (FR-05, FR-16)")
    @GetMapping
    public List<LocationInfo> getLocations() {
        return locationRepository.findAllByOrderByCityAscDistrictAsc().stream()
                .map(location -> new LocationInfo(location.getId(), location.getCity(), location.getDistrict()))
                .toList();
    }
}
