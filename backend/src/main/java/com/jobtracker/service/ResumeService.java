package com.jobtracker.service;

import com.jobtracker.dto.ResumeDto;
import com.jobtracker.entity.Resume;
import com.jobtracker.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResumeService {

    private final ResumeRepository repository;
    private final S3Service s3Service;
    private final FileParserService fileParserService;

    @Transactional
    public ResumeDto upload(MultipartFile file) throws IOException {
        // Single-resume policy: delete existing before saving new
        repository.findTopByOrderByCreatedAtDesc().ifPresent(existing -> {
            s3Service.delete(existing.getFileUrl());
            repository.delete(existing);
        });

        String parsedText = fileParserService.parse(file);
        String fileUrl = s3Service.upload(
                "resumes",
                file.getOriginalFilename(),
                file.getInputStream(),
                file.getSize(),
                file.getContentType()
        );
        Resume resume = new Resume();
        resume.setFileName(file.getOriginalFilename());
        resume.setFileUrl(fileUrl);
        resume.setParsedText(parsedText);
        return toDto(repository.save(resume));
    }

    @Transactional(readOnly = true)
    public List<ResumeDto> findAll() {
        return repository.findAllByOrderByCreatedAtDesc().stream().map(this::toDto).toList();
    }

    @Transactional
    public void delete(Long id) {
        Resume resume = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Resume not found: " + id));
        s3Service.delete(resume.getFileUrl());
        repository.delete(resume);
    }

    private ResumeDto toDto(Resume r) {
        return new ResumeDto(r.getId(), r.getFileName(), r.getFileUrl(), r.getParsedText(), r.getCreatedAt());
    }
}
