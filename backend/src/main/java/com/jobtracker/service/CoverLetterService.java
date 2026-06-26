package com.jobtracker.service;

import com.jobtracker.dto.CoverLetterDto;
import com.jobtracker.entity.CoverLetter;
import com.jobtracker.repository.CoverLetterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CoverLetterService {

    private final CoverLetterRepository repository;
    private final S3Service s3Service;
    private final FileParserService fileParserService;

    @Transactional
    public CoverLetterDto upload(MultipartFile file) throws IOException {
        // Single cover-letter policy: delete existing before saving new
        repository.findTopByOrderByCreatedAtDesc().ifPresent(existing -> {
            s3Service.delete(existing.getFileUrl());
            repository.delete(existing);
        });

        String parsedText = fileParserService.parse(file);
        String fileUrl = s3Service.upload(
                "cover-letters",
                file.getOriginalFilename(),
                file.getInputStream(),
                file.getSize(),
                file.getContentType()
        );
        CoverLetter cl = new CoverLetter();
        cl.setFileName(file.getOriginalFilename());
        cl.setFileUrl(fileUrl);
        cl.setParsedText(parsedText);
        return toDto(repository.save(cl));
    }

    @Transactional(readOnly = true)
    public List<CoverLetterDto> findAll() {
        return repository.findAllByOrderByCreatedAtDesc().stream().map(this::toDto).toList();
    }

    @Transactional
    public void delete(Long id) {
        CoverLetter cl = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cover letter not found: " + id));
        s3Service.delete(cl.getFileUrl());
        repository.delete(cl);
    }

    private CoverLetterDto toDto(CoverLetter cl) {
        return new CoverLetterDto(cl.getId(), cl.getFileName(), cl.getFileUrl(), cl.getParsedText(), cl.getCreatedAt());
    }
}
