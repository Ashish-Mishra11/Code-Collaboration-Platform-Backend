package com.codesync.notification.service;

import com.codesync.notification.event.DeveloperApprovedEvent;
import com.codesync.notification.event.PaymentNotificationEvent;
import com.codesync.notification.event.UserLoginEvent;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

/**
 * Responsible for composing and dispatching HTML emails.
 *
 * Every method is annotated with {@code @Async} so the Kafka listener
 * is not blocked during SMTP round-trips.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    // ═══════════════════════════════════════════════════════════════════════
    // 1. User Login Notification
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Sends a login-confirmation email to the user who just signed in.
     */
    @Async
    public void sendUserLoginEmail(UserLoginEvent event) {
        try {
            String displayName = (event.getFullName() != null && !event.getFullName().isBlank())
                    ? event.getFullName() : event.getUserName();

            String timeStr = (event.getLoginTime() != null)
                    ? event.getLoginTime().format(FMT) : "Just now";

            String html = """
                    <html>
                    <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
                      <div style="max-width:600px; margin:auto; background:#fff; border-radius:10px;
                                  padding:30px; box-shadow:0 2px 8px rgba(0,0,0,0.1);">
                        <h2 style="color:#4f46e5;">New Login Detected — CodeSync</h2>
                        <p>Hi <strong>%s</strong>,</p>
                        <p>We detected a new login to your <strong>CodeSync</strong> account.</p>
                        <table style="width:100%%; border-collapse:collapse; margin:16px 0;">
                          <tr>
                            <td style="padding:8px; background:#f0f0f0; font-weight:bold;">Username</td>
                            <td style="padding:8px;">%s</td>
                          </tr>
                          <tr>
                            <td style="padding:8px; background:#f0f0f0; font-weight:bold;">Time</td>
                            <td style="padding:8px;">%s</td>
                          </tr>
                        </table>
                        <p>If this was <strong>not you</strong>, please change your password immediately.</p>
                        <p style="margin-top:24px;">— <em>The CodeSync Team</em></p>
                      </div>
                    </body>
                    </html>
                    """.formatted(displayName, event.getUserName(), timeStr);

            sendHtml(event.getEmail(),
                    "CodeSync: New Login to Your Account",
                    html);

            log.info("[Notification] Login email sent → {}", event.getEmail());

        } catch (Exception ex) {
            log.error("[Notification] Failed to send login email to {}: {}",
                    event.getEmail(), ex.getMessage(), ex);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 2. Developer Approved Notification
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Sends a congratulatory email when admin approves a developer application.
     */
    @Async
    public void sendDeveloperApprovedEmail(DeveloperApprovedEvent event) {
        try {
            String html = """
                    <html>
                    <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
                      <div style="max-width:600px; margin:auto; background:#fff; border-radius:10px;
                                  padding:30px; box-shadow:0 2px 8px rgba(0,0,0,0.1);">
                        <h2 style="color:#10b981;">🎉 Congratulations! You're Selected as a Developer</h2>
                        <p>Hi <strong>%s</strong>,</p>
                        <p>We are thrilled to inform you that your application to become a
                           <strong>Developer at CodeSync</strong> has been <span style="color:#10b981;">
                           <strong>APPROVED</strong></span>!</p>
                        <p>You applied from: <strong>%s</strong></p>
                        <p>To complete your registration and set up your developer account,
                           please click the button below:</p>
                        <div style="text-align:center; margin:28px 0;">
                          <a href="%s"
                             style="background:#4f46e5; color:#fff; padding:12px 28px;
                                    border-radius:6px; text-decoration:none; font-size:16px;">
                            Complete Registration →
                          </a>
                        </div>
                        <p style="color:#6b7280; font-size:13px;">
                          If the button doesn't work, copy and paste this link:<br/>
                          <a href="%s">%s</a>
                        </p>
                        <p>Welcome aboard! 🚀</p>
                        <p style="margin-top:24px;">— <em>The CodeSync Team</em></p>
                      </div>
                    </body>
                    </html>
                    """.formatted(event.getName(),
                    event.getCompanyName() != null ? event.getCompanyName() : "—",
                    event.getRegistrationLink(),
                    event.getRegistrationLink(),
                    event.getRegistrationLink());

            sendHtml(event.getEmail(),
                    "🎉 Congratulations! You are Selected as Developer at CodeSync",
                    html);

            log.info("[Notification] Developer-approved email sent → {}", event.getEmail());

        } catch (Exception ex) {
            log.error("[Notification] Failed to send developer-approved email to {}: {}",
                    event.getEmail(), ex.getMessage(), ex);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 3. Payment Notification (Success / Failure)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Sends a payment confirmation or failure email to the developer.
     */
    @Async
    public void sendPaymentNotificationEmail(PaymentNotificationEvent event) {
        try {
            boolean success = "SUCCESS".equalsIgnoreCase(event.getStatus());

            String statusBadge = success
                    ? "<span style='color:#10b981; font-weight:bold;'>✅ SUCCESS</span>"
                    : "<span style='color:#ef4444; font-weight:bold;'>❌ FAILED</span>";

            String heading = success
                    ? "✅ Payment Successful — CodeSync"
                    : "❌ Payment Failed — CodeSync";

            String timeStr = (event.getTransactionTime() != null)
                    ? event.getTransactionTime().format(FMT) : "—";

            String amountStr = (event.getAmount() != null)
                    ? "₹" + event.getAmount().toPlainString() : "—";

            String html = """
                    <html>
                    <body style="font-family: Arial, sans-serif; background-color:#f4f4f4; padding:20px;">
                      <div style="max-width:600px; margin:auto; background:#fff; border-radius:10px;
                                  padding:30px; box-shadow:0 2px 8px rgba(0,0,0,0.1);">
                        <h2 style="color:#4f46e5;">%s</h2>
                        <p>Hi <strong>%s</strong>,</p>
                        <p>Here is your payment summary:</p>
                        <table style="width:100%%; border-collapse:collapse; margin:16px 0;">
                          <tr>
                            <td style="padding:8px; background:#f0f0f0; font-weight:bold;">Status</td>
                            <td style="padding:8px;">%s</td>
                          </tr>
                          <tr>
                            <td style="padding:8px; background:#f0f0f0; font-weight:bold;">Tokens Purchased</td>
                            <td style="padding:8px;">%d</td>
                          </tr>
                          <tr>
                            <td style="padding:8px; background:#f0f0f0; font-weight:bold;">Amount Charged</td>
                            <td style="padding:8px;">%s</td>
                          </tr>
                          <tr>
                            <td style="padding:8px; background:#f0f0f0; font-weight:bold;">Date &amp; Time</td>
                            <td style="padding:8px;">%s</td>
                          </tr>
                          <tr>
                            <td style="padding:8px; background:#f0f0f0; font-weight:bold;">Remarks</td>
                            <td style="padding:8px;">%s</td>
                          </tr>
                        </table>
                        %s
                        <p style="margin-top:24px;">— <em>The CodeSync Team</em></p>
                      </div>
                    </body>
                    </html>
                    """.formatted(
                    heading,
                    event.getUserName() != null ? event.getUserName() : "Developer",
                    statusBadge,
                    event.getTokensPurchased() != null ? event.getTokensPurchased() : 0,
                    amountStr,
                    timeStr,
                    event.getRemarks() != null ? event.getRemarks() : "—",
                    success
                            ? "<p style='color:#10b981;'>Your execution tokens have been credited to your account. Happy coding! 🚀</p>"
                            : "<p style='color:#ef4444;'>Your payment could not be processed. Please try again or contact support.</p>"
            );

            String subject = success
                    ? "✅ CodeSync: Payment Successful — " + event.getTokensPurchased() + " tokens credited"
                    : "❌ CodeSync: Payment Failed — Please retry";

            sendHtml(event.getEmail(), subject, html);

            log.info("[Notification] Payment ({}) email sent → {}", event.getStatus(), event.getEmail());

        } catch (Exception ex) {
            log.error("[Notification] Failed to send payment email to {}: {}",
                    event.getEmail(), ex.getMessage(), ex);
        }
    }

    // ── Internal helper ───────────────────────────────────────────────────

    private void sendHtml(String to, String subject, String htmlBody) throws Exception {
        MimeMessage msg = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
        helper.setFrom(fromAddress);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);
        mailSender.send(msg);
    }
}
