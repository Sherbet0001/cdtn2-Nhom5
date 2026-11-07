package com.vlu.freelance.dto;

public class ApplicationRequest {
    private String coverLetter;

    public ApplicationRequest() {}

    public ApplicationRequest(String coverLetter) {
        this.coverLetter = coverLetter;
    }

    public String getCoverLetter() { return coverLetter; }
    public void setCoverLetter(String coverLetter) { this.coverLetter = coverLetter; }
}
