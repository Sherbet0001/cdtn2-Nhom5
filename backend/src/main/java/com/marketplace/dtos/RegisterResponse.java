package com.marketplace.dtos;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterResponse {

    private Long userId;
    private String email;
    private String fullName;
    private String role;
    private String status;
}
