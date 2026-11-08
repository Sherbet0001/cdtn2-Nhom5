package com.nhom5.partjob.controller;

import com.nhom5.partjob.domain.JobType;
import com.nhom5.partjob.dto.JobDtos.*;
import com.nhom5.partjob.security.AuthUser;
import com.nhom5.partjob.service.JobService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping
    public Page<JobResponse> search(@RequestParam(required = false) String q,
                                    @RequestParam(required = false) Long locationId,
                                    @RequestParam(required = false) JobType jobType,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "8") int size) {
        if (page < 0 || size < 1 || size > 50) {
            throw com.nhom5.partjob.exception.ApiException.badRequest("Phân trang không hợp lệ");
        }
        return jobService.search(q, locationId, jobType, page, size);
    }

    @GetMapping("/mine")
    public List<JobResponse> getMine(@AuthenticationPrincipal AuthUser user) {
        return jobService.getMine(user.id());
    }

    @GetMapping("/{jobId}")
    public JobResponse getPublic(@PathVariable Long jobId) {
        return jobService.getPublic(jobId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JobResponse create(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody JobRequest request) {
        return jobService.create(user.id(), request);
    }

    @PutMapping("/{jobId}")
    public JobResponse update(@AuthenticationPrincipal AuthUser user, @PathVariable Long jobId,
                              @Valid @RequestBody JobRequest request) {
        return jobService.update(user.id(), jobId, request);
    }

    @PatchMapping("/{jobId}/status")
    public JobResponse updateStatus(@AuthenticationPrincipal AuthUser user, @PathVariable Long jobId,
                                    @Valid @RequestBody StatusRequest request) {
        return jobService.updateStatus(user.id(), jobId, request.status());
    }
}
