package com.jobtracker.service;

import com.jobtracker.dto.CreateJobRequest;
import com.jobtracker.dto.JobDto;
import com.jobtracker.dto.UpdateJobRequest;
import com.jobtracker.entity.Application;
import com.jobtracker.entity.Job;
import com.jobtracker.repository.ApplicationRepository;
import com.jobtracker.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class JobService {

    private static final Set<String> VALID_STATUSES = Set.of(
            "SAVED", "RESUME_MATCHED", "READY_TO_APPLY", "APPLIED", "HR_CONTACTED",
            "INTERVIEW_SCHEDULED", "INTERVIEW_DONE", "OFFER", "REJECTED", "CLOSED"
    );

    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;

    @Transactional
    public JobDto create(CreateJobRequest req) {
        Job job = new Job();
        applyFields(job, req.title(), req.company(), req.location(), req.platform(),
                req.jobUrl(), req.description(), req.salary(), req.jobType(),
                req.postedDate(), req.closingDate());
        job = jobRepository.save(job);

        Application app = new Application();
        app.setJobId(job.getId());
        app.setStatus("SAVED");
        app = applicationRepository.save(app);

        return toDto(job, app);
    }

    @Transactional(readOnly = true)
    public List<JobDto> findAll() {
        return jobRepository.findAllByOrderBySavedDateDesc().stream()
                .map(job -> {
                    Application app = applicationRepository.findByJobId(job.getId()).orElse(null);
                    return toDto(job, app);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public JobDto findById(Long id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + id));
        Application app = applicationRepository.findByJobId(id).orElse(null);
        return toDto(job, app);
    }

    @Transactional
    public JobDto update(Long id, UpdateJobRequest req) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + id));
        applyFields(job, req.title(), req.company(), req.location(), req.platform(),
                req.jobUrl(), req.description(), req.salary(), req.jobType(),
                req.postedDate(), req.closingDate());
        job = jobRepository.save(job);
        Application app = applicationRepository.findByJobId(id).orElse(null);
        return toDto(job, app);
    }

    @Transactional
    public void delete(Long id) {
        if (!jobRepository.existsById(id)) {
            throw new IllegalArgumentException("Job not found: " + id);
        }
        jobRepository.deleteById(id);
    }

    @Transactional
    public void updateStatus(Long jobId, String status) {
        if (!VALID_STATUSES.contains(status)) {
            throw new IllegalArgumentException("Invalid status: " + status);
        }
        if (!jobRepository.existsById(jobId)) {
            throw new IllegalArgumentException("Job not found: " + jobId);
        }
        Application app = applicationRepository.findByJobId(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found for job: " + jobId));
        app.setStatus(status);
        applicationRepository.save(app);
    }

    private void applyFields(Job job, String title, String company, String location,
                              String platform, String jobUrl, String description,
                              String salary, String jobType,
                              java.time.LocalDate postedDate, java.time.LocalDate closingDate) {
        job.setTitle(title);
        job.setCompany(company);
        job.setLocation(location);
        job.setPlatform(platform);
        job.setJobUrl(jobUrl);
        job.setDescription(description);
        job.setSalary(salary);
        job.setJobType(jobType);
        job.setPostedDate(postedDate);
        job.setClosingDate(closingDate);
    }

    private JobDto toDto(Job job, Application app) {
        return new JobDto(
                job.getId(),
                job.getTitle(),
                job.getCompany(),
                job.getLocation(),
                job.getPlatform(),
                job.getJobUrl(),
                job.getDescription(),
                job.getSalary(),
                job.getJobType(),
                job.getPostedDate(),
                job.getClosingDate(),
                job.getSavedDate(),
                app != null ? app.getId() : null,
                app != null ? app.getStatus() : null
        );
    }
}
