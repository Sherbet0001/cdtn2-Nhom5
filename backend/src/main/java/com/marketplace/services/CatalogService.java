package com.marketplace.services;

import com.marketplace.dtos.CategoryResponse;
import com.marketplace.dtos.LocationResponse;
import com.marketplace.repositories.CategoryRepository;
import com.marketplace.repositories.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Dịch vụ cung cấp danh mục Ngành nghề và Địa điểm phục vụ hiển thị trên giao diện.
 */
@Service
@RequiredArgsConstructor
public class CatalogService {

    private final CategoryRepository categoryRepository;
    private final LocationRepository locationRepository;

    /**
     * Lấy danh sách toàn bộ ngành nghề việc làm (FR-09).
     *
     * @return Danh sách ngành nghề sắp xếp theo ID tăng dần
     */
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAllByOrderByIdAsc()
                .stream()
                .map(c -> CategoryResponse.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách toàn bộ địa điểm làm việc/sinh sống (FR-05, FR-09).
     *
     * @return Danh sách địa điểm sắp xếp theo ID tăng dần
     */
    @Transactional(readOnly = true)
    public List<LocationResponse> getAllLocations() {
        return locationRepository.findAllByOrderByIdAsc()
                .stream()
                .map(l -> LocationResponse.builder()
                        .id(l.getId())
                        .city(l.getCity())
                        .district(l.getDistrict())
                        .build())
                .collect(Collectors.toList());
    }
}

