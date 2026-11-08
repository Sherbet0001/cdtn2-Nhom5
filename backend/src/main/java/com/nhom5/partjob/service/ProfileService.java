package com.nhom5.partjob.service;

import com.nhom5.partjob.domain.Location;
import com.nhom5.partjob.domain.Profile;
import com.nhom5.partjob.domain.Role;
import com.nhom5.partjob.domain.User;
import com.nhom5.partjob.dto.ProfileDtos.*;
import com.nhom5.partjob.exception.ApiException;
import com.nhom5.partjob.repository.LocationRepository;
import com.nhom5.partjob.repository.ProfileRepository;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Locale;

/** Xem va cap nhat ho so ca nhan, CV (FR-05, 11, 16). */
@Service
public class ProfileService {

    private static final long MAX_CV_FILE_SIZE = 5L * 1024 * 1024;

    private final ProfileRepository profileRepository;
    private final LocationRepository locationRepository;

    public ProfileService(ProfileRepository profileRepository, LocationRepository locationRepository) {
        this.profileRepository = profileRepository;
        this.locationRepository = locationRepository;
    }

    @Transactional(readOnly = true)
    public ProfileResponse getMine(Long userId) {
        return toResponse(findProfile(userId));
    }

    @Transactional
    public ProfileResponse updateMine(Long userId, ProfileUpdateRequest req) {
        Profile profile = findProfile(userId);
        Role role = profile.getUser().getRole();

        profile.setFullName(req.fullName().trim());
        profile.setPhone(blankToNull(req.phone()));
        profile.setBio(blankToNull(req.bio()));
        profile.setLocation(resolveLocation(req.locationId()));

        if (role == Role.STUDENT) {
            profile.setSchool(blankToNull(req.school()));
            profile.setMajor(blankToNull(req.major()));
            profile.setCvContent(blankToNull(req.cvContent()));
        } else if (role == Role.EMPLOYER) {
            profile.setCompanyName(blankToNull(req.companyName()));
        }
        return toResponse(profileRepository.save(profile));
    }

    @Transactional
    public ProfileResponse uploadCvFile(Long userId, MultipartFile file) {
        Profile profile = findProfile(userId);
        if (profile.getUser().getRole() != Role.STUDENT) {
            throw ApiException.forbidden("Chỉ sinh viên mới được tải tệp CV lên");
        }
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("Vui lòng chọn tệp CV");
        }
        if (file.getSize() > MAX_CV_FILE_SIZE) {
            throw ApiException.badRequest("Tệp CV không được vượt quá 5 MB");
        }

        String fileName = safeFileName(file.getOriginalFilename());
        String extension = fileExtension(fileName);
        String contentType = contentTypeFor(extension);
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException exception) {
            throw ApiException.badRequest("Không thể đọc tệp CV đã chọn");
        }
        if (!hasValidSignature(extension, content)) {
            throw ApiException.badRequest("Nội dung tệp không khớp định dạng PDF, DOC hoặc DOCX");
        }

        profile.setCvFile(fileName, contentType, content);
        return toResponse(profileRepository.save(profile));
    }

    @Transactional(readOnly = true)
    public CvDocument getCvFile(Long userId) {
        Profile profile = findProfile(userId);
        if (profile.getUser().getRole() != Role.STUDENT) {
            throw ApiException.forbidden("Chỉ sinh viên mới có tệp CV");
        }
        if (profile.getCvFileData() == null || profile.getCvFileName() == null) {
            throw ApiException.notFound("Bạn chưa tải tệp CV lên");
        }
        return new CvDocument(profile.getCvFileName(), profile.getCvFileContentType(), profile.getCvFileData());
    }

    private Profile findProfile(Long userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ"));
    }

    private Location resolveLocation(Long locationId) {
        if (locationId == null) {
            return null;
        }
        return locationRepository.findById(locationId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy địa điểm"));
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static String safeFileName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            throw ApiException.badRequest("Tên tệp CV không hợp lệ");
        }
        String fileName = originalName.replace('\\', '/');
        fileName = fileName.substring(fileName.lastIndexOf('/') + 1).trim();
        if (fileName.isBlank() || fileName.length() > 255) {
            throw ApiException.badRequest("Tên tệp CV tối đa 255 ký tự");
        }
        return fileName;
    }

    private static String fileExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0) {
            throw ApiException.badRequest("Chỉ chấp nhận tệp PDF, DOC hoặc DOCX");
        }
        String extension = fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!extension.equals("pdf") && !extension.equals("doc") && !extension.equals("docx")) {
            throw ApiException.badRequest("Chỉ chấp nhận tệp PDF, DOC hoặc DOCX");
        }
        return extension;
    }

    private static String contentTypeFor(String extension) {
        return switch (extension) {
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            default -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        };
    }

    private static boolean hasValidSignature(String extension, byte[] content) {
        if (extension.equals("pdf")) {
            return content.length >= 5 && content[0] == '%' && content[1] == 'P'
                    && content[2] == 'D' && content[3] == 'F' && content[4] == '-';
        }
        if (extension.equals("doc")) {
            return content.length >= 8 && (content[0] & 0xff) == 0xd0 && (content[1] & 0xff) == 0xcf
                    && (content[2] & 0xff) == 0x11 && (content[3] & 0xff) == 0xe0
                    && (content[4] & 0xff) == 0xa1 && (content[5] & 0xff) == 0xb1
                    && (content[6] & 0xff) == 0x1a && (content[7] & 0xff) == 0xe1;
        }
        return content.length >= 4 && content[0] == 'P' && content[1] == 'K'
                && content[2] == 3 && content[3] == 4;
    }

    private static ProfileResponse toResponse(Profile p) {
        User u = p.getUser();
        Location l = p.getLocation();
        LocationInfo loc = (l == null) ? null : new LocationInfo(l.getId(), l.getCity(), l.getDistrict());
        return new ProfileResponse(p.getId(), u.getId(), u.getEmail(), u.getRole(), p.getFullName(), p.getPhone(),
                loc, p.getBio(), p.getSchool(), p.getMajor(), p.getCvContent(), p.getCvFileName(),
                p.getCompanyName(), p.getUpdatedAt());
    }
}
