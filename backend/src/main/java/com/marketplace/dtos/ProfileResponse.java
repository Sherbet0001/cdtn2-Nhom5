package com.marketplace.dtos;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileResponse {

    private Long id;
    private Long userId;
    private String email;
    private String role;
    private String status;
    private String fullName;
    private String phone;
    private Long locationId;
    private String locationName;
    private String bio;
    private String school;
    private String major;
    private String cvContent;
    private String companyName;
    private LocalDateTime updatedAt;
}

