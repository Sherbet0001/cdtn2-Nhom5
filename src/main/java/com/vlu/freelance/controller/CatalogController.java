package com.vlu.freelance.controller;

import com.vlu.freelance.entity.Category;
import com.vlu.freelance.entity.Location;
import com.vlu.freelance.repository.CategoryRepository;
import com.vlu.freelance.repository.LocationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Catalog Module", description = "Danh mục ngành nghề và địa điểm (FR-09)")
public class CatalogController {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private LocationRepository locationRepository;

    @GetMapping("/categories")
    @Operation(summary = "Lấy danh sách tất cả ngành nghề (FR-09)")
    public ResponseEntity<List<Category>> getCategories() {
        return ResponseEntity.ok(categoryRepository.findAll());
    }

    @GetMapping("/locations")
    @Operation(summary = "Lấy danh sách tất cả địa điểm làm việc (FR-09)")
    public ResponseEntity<List<Location>> getLocations() {
        return ResponseEntity.ok(locationRepository.findAll());
    }
}
