package com.vlu.freelance.dto;

public class AuthResponse {
    private String accessToken;
    private String tokenType = "Bearer";
    private long expiresIn = 7200; // 2 giờ theo đặc tả NFR-02 Buổi 5
    private UserSummary user;

    public AuthResponse() {}

    public AuthResponse(String accessToken, UserSummary user) {
        this.accessToken = accessToken;
        this.tokenType = "Bearer";
        this.expiresIn = 7200;
        this.user = user;
    }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }
    public long getExpiresIn() { return expiresIn; }
    public void setExpiresIn(long expiresIn) { this.expiresIn = expiresIn; }
    public UserSummary getUser() { return user; }
    public void setUser(UserSummary user) { this.user = user; }
}
