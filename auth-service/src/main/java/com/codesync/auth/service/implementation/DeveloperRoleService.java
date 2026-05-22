package com.codesync.auth.service.implementation;

import java.io.IOException;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.codesync.auth.dto.RegisterDeveloperDto;
import com.codesync.auth.entity.DeveloperApplicationReceived;
import com.codesync.auth.kafka.AuthNotificationPublisher;
import com.codesync.auth.kafka.DeveloperApprovedEvent;
import com.codesync.auth.repository.DeveloperApplication;

import jakarta.mail.internet.MimeMessage;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class DeveloperRoleService {

    private DeveloperApplication developerApplication;
    private JavaMailSender mailSender;
    private AuthNotificationPublisher notificationPublisher;

    // ---- Developer submits initial application ----
    public String registerDeveloperApplications(RegisterDeveloperDto registerDeveloperDto) {

        if (developerApplication.findByEmail(registerDeveloperDto.getEmail()).isPresent()) {
            return "Your Application already exists in the database. Please wait for admin approval.";
        }

        DeveloperApplicationReceived entity = new DeveloperApplicationReceived();
        entity.setName(registerDeveloperDto.getName());
        entity.setEmail(registerDeveloperDto.getEmail());

        MultipartFile resume = registerDeveloperDto.getResume();
        if (resume.isEmpty()) {
            return "Resume file is empty";
        }
        if (!"application/pdf".equals(resume.getContentType())) {
            return "Only PDF files are allowed";
        }
        try {
            entity.setResumePdf(resume.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Problem with resume pdf part");
        }
        entity.setCompanyName(registerDeveloperDto.getCompanyName());

        developerApplication.save(entity);
        generateMailToAdmin(registerDeveloperDto);
        return "Your Application Registered successfully — now wait for Admin approval.";
    }

    /**
     * Direct email to admin when a developer applies (synchronous — admin must
     * know immediately).  Attaches the resume PDF.
     */
    public void generateMailToAdmin(RegisterDeveloperDto registerDeveloperDto) {
        try {
            String adminEmail = "am3410352@gmail.com";

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);

            helper.setTo(adminEmail);
            helper.setSubject("New Developer Application Received");

            String htmlBody = "<html><body>"
                    + "<h2>New Developer Application</h2>"
                    + "<p><strong>Name:</strong> " + registerDeveloperDto.getName() + "</p>"
                    + "<p><strong>Email:</strong> " + registerDeveloperDto.getEmail() + "</p>"
                    + "<p><strong>Company:</strong> " + registerDeveloperDto.getCompanyName() + "</p>"
                    + "<br>"
                    + "<p>Please log in to your Admin Dashboard (http://localhost:3000/admin) "
                    + "to manually review and explicitly approve or reject this applicant.</p>"
                    + "<br><br><p>Regards,<br>CodeSync System</p>"
                    + "</body></html>";

            helper.setText(htmlBody, true);

            // Attach resume PDF
            helper.addAttachment(
                    registerDeveloperDto.getResume().getOriginalFilename(),
                    registerDeveloperDto.getResume());

            mailSender.send(mimeMessage);

        } catch (Exception e) {
            throw new RuntimeException("Failed to send email to admin", e);
        }
    }

    /**
     * Admin approves a developer.
     *
     * Publishes a {@link DeveloperApprovedEvent} to Kafka so the
     * Notification Service dispatches the congratulatory email asynchronously.
     * Falls back to direct SMTP if Kafka is unavailable.
     */
    public String generateMailToDeveloper(String to) {
        String registrationLink = "http://localhost:3000/register-developer?email=" + to;

        // Fetch name and company from DB
        String name = developerApplication.findByEmail(to)
                .map(DeveloperApplicationReceived::getName)
                .orElse(to);

        String company = developerApplication.findByEmail(to)
                .map(DeveloperApplicationReceived::getCompanyName)
                .orElse(null);

        // ── Publish via Kafka → Notification Service handles the email ──
        try {
            DeveloperApprovedEvent event = DeveloperApprovedEvent.builder()
                    .name(name)
                    .email(to)
                    .companyName(company)
                    .registrationLink(registrationLink)
                    .build();

            notificationPublisher.publishDeveloperApproved(event);
            System.out.println("[Kafka] Developer-approved event published for: " + to);

        } catch (Exception ex) {
            // Fallback: direct mail when Kafka is unavailable
            System.out.println("[Kafka] Unavailable — falling back to direct email for: " + to);
            sendDirectApprovalMail(to, name, registrationLink);
        }

        // Update the application status in database so it no longer appears in the pending admin panel
        developerApplication.findByEmail(to).ifPresent(app -> {
            app.setStatus("APPROVED");
            developerApplication.save(app);
        });

        return "Approval mail dispatched to the developer.";
    }

    /** Direct-mail fallback used when Kafka is down. */
    private void sendDirectApprovalMail(String to, String name, String registrationLink) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Congratulations! You are Selected as Developer at CodeSync \uD83C\uDF89");
            String body = "Dear " + name + ",\n\n"
                    + "Congratulations! \uD83C\uDF89\n\n"
                    + "You have been selected as a Developer for CodeSync.\n\n"
                    + "To complete your registration, please visit:\n\n"
                    + registrationLink + "\n\n"
                    + "Welcome aboard! \uD83D\uDE80\n\n"
                    + "Best Regards,\nCodeSync Team";
            message.setText(body);
            mailSender.send(message);
        } catch (Exception ex) {
            System.out.println("[Mail] Fallback direct mail also failed: " + ex.getMessage());
        }
    }

    // ---- Admin rejects: update application status ----
    public String rejectHandler(String email) {
        return developerApplication.findByEmail(email).map(app -> {
            app.setStatus("REJECTED");
            developerApplication.save(app);
            return "Developer application rejected and updated in database.";
        }).orElse("No application found for that email.");
    }
}
