package com.marketplace.services;

import com.marketplace.dtos.CategoryRequest;
import com.marketplace.dtos.CategoryResponse;
import com.marketplace.dtos.LocationRequest;
import com.marketplace.dtos.LocationResponse;
import com.marketplace.entities.Category;
import com.marketplace.entities.Location;
import com.marketplace.exceptions.AppException;
import com.marketplace.repositories.CategoryRepository;
import com.marketplace.repositories.JobRepository;
import com.marketplace.repositories.LocationRepository;
import com.marketplace.repositories.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Dịch vụ cung cấp và quản lý danh mục Ngành nghề và Địa điểm (FR-05, FR-09, FR-28, FR-29).
 */
@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class CatalogService {

    private final CategoryRepository categoryRepository;
    private final LocationRepository locationRepository;
    private final JobRepository jobRepository;
    private final ProfileRepository profileRepository;

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

    /**
     * Thêm mới một danh mục ngành nghề (FR-28).
     *
     * @param request Tên danh mục mới
     * @return Thông tin danh mục vừa tạo
     * @throws AppException Mã lỗi DUPLICATE_RESOURCE (409) nếu tên danh mục đã tồn tại
     */
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        String trimmedName = request.getName().trim();
        if (categoryRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new AppException("DUPLICATE_RESOURCE", "Danh mục ngành nghề '" + trimmedName + "' đã tồn tại");
        }
        Category saved = categoryRepository.save(Category.builder().name(trimmedName).build());
        return CategoryResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .build();
    }

    /**
     * Cập nhật tên một danh mục ngành nghề (FR-28).
     *
     * @param id      ID danh mục cần sửa
     * @param request Tên danh mục mới
     * @return Thông tin danh mục sau khi cập nhật
     * @throws AppException Mã lỗi CATEGORY_NOT_FOUND (404) hoặc DUPLICATE_RESOURCE (409)
     */
    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException("CATEGORY_NOT_FOUND", "Danh mục ngành nghề không tồn tại"));

        String trimmedName = request.getName().trim();
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(trimmedName, id)) {
            throw new AppException("DUPLICATE_RESOURCE", "Danh mục ngành nghề '" + trimmedName + "' đã tồn tại");
        }

        category.setName(trimmedName);
        Category updated = categoryRepository.save(category);
        return CategoryResponse.builder()
                .id(updated.getId())
                .name(updated.getName())
                .build();
    }

    /**
     * Xóa một danh mục ngành nghề nếu chưa có tin tuyển dụng nào sử dụng (FR-28).
     *
     * @param id ID danh mục cần xóa
     * @throws AppException Mã lỗi CATEGORY_NOT_FOUND (404) hoặc RESOURCE_IN_USE (409) nếu còn tin đang dùng
     */
    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException("CATEGORY_NOT_FOUND", "Danh mục ngành nghề không tồn tại"));

        // QT-ADMIN (FR-28): Chặn xóa nếu còn tin tuyển dụng tham chiếu tới danh mục này (ON DELETE RESTRICT -> 409)
        if (jobRepository.existsByCategoryId(id)) {
            throw new AppException(
                    "RESOURCE_IN_USE",
                    "Không thể xóa danh mục ngành nghề '" + category.getName() + "' vì đang có tin tuyển dụng sử dụng"
            );
        }

        categoryRepository.delete(category);
    }

    /**
     * Thêm mới một địa điểm làm việc (FR-29).
     *
     * @param request Tỉnh/Thành phố và Quận/Huyện
     * @return Thông tin địa điểm vừa tạo
     * @throws AppException Mã lỗi DUPLICATE_RESOURCE (409) nếu cặp (city, district) đã tồn tại
     */
    @Transactional
    public LocationResponse createLocation(LocationRequest request) {
        String trimmedCity = request.getCity().trim();
        String trimmedDistrict = request.getDistrict().trim();

        if (locationRepository.existsByCityIgnoreCaseAndDistrictIgnoreCase(trimmedCity, trimmedDistrict)) {
            throw new AppException(
                    "DUPLICATE_RESOURCE",
                    "Địa điểm '" + trimmedDistrict + ", " + trimmedCity + "' đã tồn tại"
            );
        }

        Location saved = locationRepository.save(
                Location.builder()
                        .city(trimmedCity)
                        .district(trimmedDistrict)
                        .build()
        );
        return LocationResponse.builder()
                .id(saved.getId())
                .city(saved.getCity())
                .district(saved.getDistrict())
                .build();
    }

    /**
     * Cập nhật thông tin một địa điểm làm việc (FR-29).
     *
     * @param id      ID địa điểm cần sửa
     * @param request Tỉnh/Thành phố và Quận/Huyện mới
     * @return Thông tin địa điểm sau khi cập nhật
     * @throws AppException Mã lỗi LOCATION_NOT_FOUND (404) hoặc DUPLICATE_RESOURCE (409)
     */
    @Transactional
    public LocationResponse updateLocation(Long id, LocationRequest request) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new AppException("LOCATION_NOT_FOUND", "Địa điểm làm việc không tồn tại"));

        String trimmedCity = request.getCity().trim();
        String trimmedDistrict = request.getDistrict().trim();

        if (locationRepository.existsByCityIgnoreCaseAndDistrictIgnoreCaseAndIdNot(trimmedCity, trimmedDistrict, id)) {
            throw new AppException(
                    "DUPLICATE_RESOURCE",
                    "Địa điểm '" + trimmedDistrict + ", " + trimmedCity + "' đã tồn tại"
            );
        }

        location.setCity(trimmedCity);
        location.setDistrict(trimmedDistrict);
        Location updated = locationRepository.save(location);
        return LocationResponse.builder()
                .id(updated.getId())
                .city(updated.getCity())
                .district(updated.getDistrict())
                .build();
    }

    /**
     * Xóa một địa điểm làm việc nếu không còn tin tuyển dụng hoặc hồ sơ người dùng nào tham chiếu (FR-29).
     *
     * @param id ID địa điểm cần xóa
     * @throws AppException Mã lỗi LOCATION_NOT_FOUND (404) hoặc RESOURCE_IN_USE (409) nếu còn bản ghi sử dụng
     */
    @Transactional
    public void deleteLocation(Long id) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new AppException("LOCATION_NOT_FOUND", "Địa điểm làm việc không tồn tại"));

        // QT-ADMIN (FR-29): Chặn xóa nếu còn tin tuyển dụng hoặc hồ sơ cá nhân tham chiếu tới địa điểm này (409)
        if (jobRepository.existsByLocationId(id) || profileRepository.existsByLocationId(id)) {
            throw new AppException(
                    "RESOURCE_IN_USE",
                    "Không thể xóa địa điểm '" + location.getDistrict() + ", " + location.getCity()
                            + "' vì đang có tin tuyển dụng hoặc hồ sơ người dùng sử dụng"
            );
        }

        locationRepository.delete(location);
    }
}

