package com.marketplace.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.security.Principal;

/**
 * Đối tượng lưu thông tin người dùng đã xác thực trong SecurityContext.
 */
@Getter
@AllArgsConstructor
public class UserPrincipal implements Principal {

    private final Long id;
    private final String email;
    private final String role;

    @Override
    public String getName() {
        return email;
    }
}

