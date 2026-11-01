package com.marketplace.controllers;

import com.marketplace.dtos.ApiResponse;
import com.marketplace.dtos.CategoryResponse;
import com.marketplace.dtos.LocationResponse;
import com.marketplace.services.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller cung cấp danh mục Ngành nghề và Địa điểm công khai (FR-05, FR-09).
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategories() {
        List<CategoryResponse> categories = catalogService.getAllCategories();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách ngành nghề thành công", categories));
    }

    @GetMapping("/locations")
    public ResponseEntity<ApiResponse<List<LocationResponse>>> getLocations() {
        List<LocationResponse> locations = catalogService.getAllLocations();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách địa điểm thành công", locations));
    }
}

