package com.airline.notificationservice.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class CompositeEmailClient implements EmailClient {

    private static final Logger log = LoggerFactory.getLogger(CompositeEmailClient.class);

    private final MailgunClient mailgunClient;
    private final SmtpEmailClient smtpEmailClient;
    private final DevEmailClient devEmailClient;

    public CompositeEmailClient(
            @Autowired(required = false) MailgunClient mailgunClient,
            @Autowired(required = false) SmtpEmailClient smtpEmailClient,
            @Autowired(required = false) DevEmailClient devEmailClient) {
        this.mailgunClient = mailgunClient;
        this.smtpEmailClient = smtpEmailClient;
        this.devEmailClient = devEmailClient;
    }

    @Override
    public void sendEmail(String to, String subject, String htmlBody) {
        boolean dispatched = false;

        // 1. Attempt delivery via Mailgun if configured
        if (mailgunClient != null) {
            try {
                log.info("Sending email via Mailgun to [{}]", to);
                mailgunClient.sendEmail(to, subject, htmlBody);
                dispatched = true;
            } catch (Exception ex) {
                log.warn("Mailgun delivery failed for recipient [{}]: {}. Attempting SMTP fallback.", to, ex.getMessage());
            }
        }

        // 2. Send via SMTP / MailHog (works as mirror or fallback for dev/testing)
        if (smtpEmailClient != null) {
            try {
                log.info("Sending email via SMTP (MailHog) to [{}]", to);
                smtpEmailClient.sendEmail(to, subject, htmlBody);
                dispatched = true;
            } catch (Exception ex) {
                log.warn("SMTP (MailHog) delivery failed for recipient [{}]: {}", to, ex.getMessage());
            }
        }

        // 3. Fallback to console logger if no active provider succeeded
        if (!dispatched && devEmailClient != null) {
            log.info("Logging email via DevEmailClient console for recipient [{}]", to);
            devEmailClient.sendEmail(to, subject, htmlBody);
            dispatched = true;
        }

        if (!dispatched) {
            log.error("Failed to deliver email to [{}] through all configured email providers", to);
            throw new IllegalStateException("Failed to dispatch email: no email client succeeded");
        }
    }
}
