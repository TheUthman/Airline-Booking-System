package com.airline.notificationservice.client;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "notifications.smtp", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SmtpEmailClient implements EmailClient {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailClient.class);

    private final JavaMailSender mailSender;
    private final String fromEmail;

    public SmtpEmailClient(
            JavaMailSender mailSender,
            @Value("${notifications.smtp.from:Airline Booking <noreply@tigerairlines.com>}") String fromEmail) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
    }

    @Override
    public void sendEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            log.info("Sending email via SMTP (MailHog) to [{}], subject: [{}]", to, subject);
            mailSender.send(message);
            log.info("Email successfully delivered via SMTP to [{}]", to);
        } catch (Exception ex) {
            log.error("Failed to send email via SMTP to [{}]: {}", to, ex.getMessage());
            throw new RuntimeException("SMTP delivery failed: " + ex.getMessage(), ex);
        }
    }
}
