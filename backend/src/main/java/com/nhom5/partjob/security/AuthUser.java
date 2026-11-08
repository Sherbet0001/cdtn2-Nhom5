package com.nhom5.partjob.security;

import com.nhom5.partjob.domain.Role;

/** Nguoi dung dang dang nhap, lay tu JWT va CSDL; dung qua @AuthenticationPrincipal. */
public record AuthUser(Long id, String email, Role role) {
}
