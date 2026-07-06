package com.jobtracker.service;

import com.jobtracker.dto.GeneratedMessageDto;
import com.jobtracker.entity.GeneratedMessage;
import com.jobtracker.entity.Job;
import com.jobtracker.entity.UserProfile;
import com.jobtracker.repository.CoverLetterRepository;
import com.jobtracker.repository.GeneratedMessageRepository;
import com.jobtracker.repository.JobRepository;
import com.jobtracker.repository.ResumeRepository;
import com.jobtracker.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MessageGenerationService {

    private static final Set<String> VALID_TYPES = Set.of("HR_EMAIL", "LINKEDIN", "FOLLOWUP", "COVER_LETTER");

    private static final String SYSTEM_PROMPT = """
            You are a professional job application assistant specialising in the Australian job market. \
            Write personalised outreach messages and cover letters. Be genuine, professional, and direct. \
            Never use generic templates or filler phrases like "I hope this email finds you well". \
            Use Australian English spelling.""";

    private final ClaudeService claudeService;
    private final JobRepository jobRepository;
    private final UserProfileRepository userProfileRepository;
    private final GeneratedMessageRepository messageRepository;
    private final CoverLetterRepository coverLetterRepository;
    private final ResumeRepository resumeRepository;

    @Transactional
    public GeneratedMessageDto generate(Long jobId, String type) {
        if (!VALID_TYPES.contains(type)) {
            throw new IllegalArgumentException("Invalid type. Must be HR_EMAIL, LINKEDIN, FOLLOWUP, or COVER_LETTER");
        }

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        UserProfile profile = userProfileRepository.findById(1L).orElse(null);

        String resumeText = resumeRepository.findTopByOrderByCreatedAtDesc()
                .map(r -> r.getParsedText() != null ? r.getParsedText() : null)
                .orElse(null);

        String coverLetterText = coverLetterRepository.findTopByOrderByCreatedAtDesc()
                .map(cl -> cl.getParsedText() != null ? cl.getParsedText() : null)
                .orElse(null);

        String defaultTemplate = null;
        if ("HR_EMAIL".equals(type) && profile != null) {
            defaultTemplate = profile.getDefaultHrEmail();
        } else if ("LINKEDIN".equals(type) && profile != null) {
            defaultTemplate = profile.getDefaultLinkedinMessage();
        }

        String userMessage = buildUserMessage(job, profile, type, resumeText, coverLetterText, defaultTemplate);
        String systemPrompt = SYSTEM_PROMPT + "\n\n" + typeInstructions(type);

        String content = claudeService.call(systemPrompt, userMessage);

        GeneratedMessage msg = new GeneratedMessage();
        msg.setJobId(jobId);
        msg.setType(type);
        msg.setContent(content);
        msg = messageRepository.save(msg);

        return toDto(msg);
    }

    @Transactional(readOnly = true)
    public List<GeneratedMessageDto> list(Long jobId) {
        if (!jobRepository.existsById(jobId)) {
            throw new IllegalArgumentException("Job not found: " + jobId);
        }
        return messageRepository.findAllByJobIdOrderByCreatedAtDesc(jobId)
                .stream().map(this::toDto).toList();
    }

    private String buildUserMessage(Job job, UserProfile profile, String type,
                                     String resumeText, String coverLetterText, String defaultTemplate) {
        StringBuilder sb = new StringBuilder();

        // Job details
        sb.append("=== JOB DETAILS ===\n");
        sb.append("Job Title: ").append(job.getTitle()).append("\n");
        if (job.getCompany() != null) sb.append("Company: ").append(job.getCompany()).append("\n");
        if (job.getLocation() != null) sb.append("Location: ").append(job.getLocation()).append("\n");
        if (job.getSalary() != null) sb.append("Salary: ").append(job.getSalary()).append("\n");
        if (job.getDescription() != null) sb.append("\nJob Description:\n").append(job.getDescription()).append("\n");

        // Candidate profile fields
        if (profile != null) {
            sb.append("\n=== CANDIDATE PROFILE ===\n");
            if (profile.getName() != null) sb.append("Name: ").append(profile.getName()).append("\n");
            if (profile.getEmail() != null) sb.append("Email: ").append(profile.getEmail()).append("\n");
            if (profile.getPhone() != null) sb.append("Phone: ").append(profile.getPhone()).append("\n");
            if (profile.getLinkedinUrl() != null) sb.append("LinkedIn: ").append(profile.getLinkedinUrl()).append("\n");
            if (profile.getTargetRoles() != null) sb.append("Target Roles: ").append(profile.getTargetRoles()).append("\n");
            if (profile.getPreferredLocations() != null) sb.append("Preferred Locations: ").append(profile.getPreferredLocations()).append("\n");
            if (profile.getVisaNote() != null) sb.append("Visa/Work Rights: ").append(profile.getVisaNote()).append("\n");
            if (profile.getAvailability() != null) sb.append("Availability: ").append(profile.getAvailability()).append("\n");
            if (profile.getSalaryExpectation() != null) sb.append("Salary Expectation: ").append(profile.getSalaryExpectation()).append("\n");
        }

        // Resume — full background context for HR_EMAIL and LINKEDIN
        if (resumeText != null && !resumeText.isBlank() && (type.equals("HR_EMAIL") || type.equals("LINKEDIN") || type.equals("FOLLOWUP"))) {
            sb.append("\n=== CANDIDATE RESUME ===\n").append(resumeText).append("\n");
        }

        // Cover letter handling differs by type
        if (type.equals("COVER_LETTER")) {
            if (coverLetterText != null && !coverLetterText.isBlank()) {
                sb.append("\n=== ORIGINAL COVER LETTER (style and tone reference) ===\n")
                  .append("Use this to understand the candidate's natural voice, tone, and writing style. ")
                  .append("Write a brand new cover letter tailored to the JD above — do NOT copy-paste from this; write fresh content that sounds like the same person.\n")
                  .append(coverLetterText).append("\n");
            }
        } else {
            // For HR_EMAIL, LINKEDIN, FOLLOWUP — cover letter gives extra voice/background context
            if (coverLetterText != null && !coverLetterText.isBlank()) {
                sb.append("\n=== CANDIDATE COVER LETTER (additional context on background and writing style) ===\n")
                  .append(coverLetterText).append("\n");
            }
            // Default template to adapt
            if (defaultTemplate != null && !defaultTemplate.isBlank()) {
                String label = type.equals("HR_EMAIL")
                        ? "=== DEFAULT HR EMAIL TEMPLATE (adapt this for the specific job above) ==="
                        : "=== DEFAULT LINKEDIN INMAIL TEMPLATE (adapt this for the specific job above) ===";
                sb.append("\n").append(label).append("\n").append(defaultTemplate).append("\n");
            }
        }

        return sb.toString();
    }

    private String typeInstructions(String type) {
        return switch (type) {
            case "HR_EMAIL" -> """
                    You have the candidate's full resume and cover letter as context — use them to understand their background, \
                    key skills, and achievements deeply.
                    A default HR email template may also be provided — if so, preserve its tone, structure, and voice exactly, \
                    and only update the job-specific details (role title, company name, why this specific role and company appeal to the candidate, \
                    and one or two concrete achievements from the resume most relevant to this JD).
                    If no template is provided, write a cold email fresh in a genuine professional Australian voice.
                    Rules:
                    - Include a subject line starting with "Subject: "
                    - Keep the body to 150–200 words maximum
                    - Be direct and genuine — no filler phrases like "I hope this email finds you well"
                    - Draw on real details from the resume and cover letter, not generic claims
                    - End with a clear call to action
                    - IMPORTANT: Always write the complete email. If any detail is missing, use [placeholder] — never refuse or ask for more information""";
            case "LINKEDIN" -> """
                    You have the candidate's full resume and cover letter as context — use them to understand their background, \
                    key skills, and achievements deeply.
                    A default LinkedIn InMail template may also be provided — if so, preserve its tone, structure, length, and voice exactly, \
                    and only update the role, company, and one or two concrete details from the resume most relevant to this JD.
                    If no template is provided, write a LinkedIn InMail fresh in the candidate's natural voice.
                    Rules:
                    - Reference the specific role and company
                    - Be specific — draw on real details from the resume and cover letter, not generic claims
                    - Match the style and length of the provided template naturally
                    - Do NOT open with just "Hi" or "Hello" alone
                    - IMPORTANT: Always write the complete message. If any detail is missing, use [placeholder] — never refuse or ask for more information""";
            case "FOLLOWUP" -> """
                    Write a polite follow-up email after submitting an application. Include:
                    - Subject line starting with "Subject: "
                    - Brief reference to the application
                    - Reaffirm interest in the role with one specific reason drawn from the job description
                    - Request a status update politely
                    - Keep to 100–150 words""";
            case "COVER_LETTER" -> """
                    Write a brand new cover letter tailored specifically to the job description provided.
                    The candidate's original cover letter is provided as a style and tone reference only — study it to understand \
                    their natural voice, sentence rhythm, level of formality, and personality. Then write entirely new content for this JD.
                    Rules:
                    - PRESERVE the candidate's natural writing voice and tone from the original throughout
                    - PRESERVE all factual personal details: visa status, work rights, availability, start date, career objectives
                    - Write fresh content that directly addresses why the candidate is a strong fit for THIS specific role and company
                    - Reference specific requirements from the job description and connect them to the candidate's background
                    - If no original cover letter was provided, write one with a natural, genuine Australian tone based on the candidate information given
                    - Output ONLY the final cover letter text — no explanations, no preamble, no meta-commentary""";
            default -> "";
        };
    }

    private GeneratedMessageDto toDto(GeneratedMessage m) {
        return new GeneratedMessageDto(m.getId(), m.getJobId(), m.getType(), m.getContent(), m.getCreatedAt());
    }
}
