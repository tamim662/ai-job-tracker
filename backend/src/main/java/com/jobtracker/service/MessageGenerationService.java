package com.jobtracker.service;

import com.jobtracker.dto.GeneratedMessageDto;
import com.jobtracker.entity.GeneratedMessage;
import com.jobtracker.entity.Job;
import com.jobtracker.entity.UserProfile;
import com.jobtracker.repository.CoverLetterRepository;
import com.jobtracker.repository.GeneratedMessageRepository;
import com.jobtracker.repository.JobRepository;
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

    @Transactional
    public GeneratedMessageDto generate(Long jobId, String type) {
        if (!VALID_TYPES.contains(type)) {
            throw new IllegalArgumentException("Invalid type. Must be HR_EMAIL, LINKEDIN, FOLLOWUP, or COVER_LETTER");
        }

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        UserProfile profile = userProfileRepository.findById(1L).orElse(null);

        String baseTemplate = null;
        if ("COVER_LETTER".equals(type)) {
            baseTemplate = coverLetterRepository.findTopByOrderByCreatedAtDesc()
                    .map(cl -> cl.getParsedText() != null ? cl.getParsedText() : null)
                    .orElse(null);
        } else if ("HR_EMAIL".equals(type) && profile != null) {
            baseTemplate = profile.getDefaultHrEmail();
        } else if ("LINKEDIN".equals(type) && profile != null) {
            baseTemplate = profile.getDefaultLinkedinMessage();
        }

        String userMessage = buildUserMessage(job, profile, type, baseTemplate);
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

    private String buildUserMessage(Job job, UserProfile profile, String type, String baseTemplate) {
        StringBuilder sb = new StringBuilder();
        sb.append("Job Title: ").append(job.getTitle()).append("\n");
        if (job.getCompany() != null) sb.append("Company: ").append(job.getCompany()).append("\n");
        if (job.getLocation() != null) sb.append("Location: ").append(job.getLocation()).append("\n");
        if (job.getDescription() != null) sb.append("Job Description:\n").append(job.getDescription()).append("\n\n");

        if (profile != null) {
            sb.append("Candidate:\n");
            if (profile.getName() != null) sb.append("Name: ").append(profile.getName()).append("\n");
            if (profile.getTargetRoles() != null) sb.append("Target Roles: ").append(profile.getTargetRoles()).append("\n");
            if (profile.getPreferredLocations() != null) sb.append("Locations: ").append(profile.getPreferredLocations()).append("\n");
        }

        if (baseTemplate != null && !baseTemplate.isBlank()) {
            String label = switch (type) {
                case "HR_EMAIL" -> "BASE HR EMAIL TEMPLATE (adapt this for the specific job — do not rewrite from scratch)";
                case "LINKEDIN" -> "BASE LINKEDIN INMAIL TEMPLATE (adapt this for the specific job — do not rewrite from scratch)";
                default -> "BASE COVER LETTER (edit this — do not rewrite from scratch)";
            };
            sb.append("\n").append(label).append(":\n").append(baseTemplate);
        }

        return sb.toString();
    }

    private String typeInstructions(String type) {
        return switch (type) {
            case "HR_EMAIL" -> """
                    Write a cold email to the hiring manager or HR department.
                    If a base HR email template is provided, adapt it for this specific job — preserve the candidate's tone and structure, only update job-specific details (role, company, why this role appeals).
                    If no template is provided, write one fresh. Either way:
                    - Include a subject line starting with "Subject: "
                    - Keep the body to 150-200 words maximum
                    - Be genuine, professional, and direct — no filler phrases
                    - End with a clear call to action
                    - IMPORTANT: Always write the complete email. If any candidate details are missing, use square bracket placeholders like [Your Name], [Your Background] — never ask for more information or refuse to write""";
            case "LINKEDIN" -> """
                    Write a LinkedIn InMail message.
                    If a base LinkedIn InMail template is provided, adapt it for this specific job — preserve the candidate's voice, structure, and tone. Only update the role, company, and relevant context.
                    If no template is provided, write one fresh. Either way:
                    - Reference the specific role and company
                    - Be genuine, professional, and specific — do NOT start with "Hi" or "Hello" alone
                    - Match the length and style of the provided template naturally
                    - IMPORTANT: Always write the complete message. If any candidate details are missing, use square bracket placeholders like [Your Name], [Your Background], [Specific Achievement] — never ask for more information or refuse to write""";
            case "FOLLOWUP" -> """
                    Write a polite follow-up email after submitting an application. Include:
                    - Subject line starting with "Subject: "
                    - Brief reference to the application
                    - Reaffirm interest in the role with one specific reason
                    - Request a status update
                    - Keep to 100-150 words""";
            case "COVER_LETTER" -> """
                    You are editing a cover letter for a specific job application, NOT writing one from scratch.
                    The candidate's base cover letter is provided (if available). Customise it for this particular role.
                    Rules:
                    - PRESERVE all personal sections exactly: visa status, work rights, availability, start date, career objectives, personal voice and tone
                    - PRESERVE the candidate's writing style throughout
                    - ONLY update the job-specific sections: why this role and company appeal to them, which specific skills match this JD
                    - If no base cover letter was provided, write one with a natural, genuine Australian tone based on the candidate information given
                    - Output ONLY the final cover letter text — no explanations, no preamble""";
            default -> "";
        };
    }

    private GeneratedMessageDto toDto(GeneratedMessage m) {
        return new GeneratedMessageDto(m.getId(), m.getJobId(), m.getType(), m.getContent(), m.getCreatedAt());
    }
}
