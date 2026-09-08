package com.airline.notificationservice.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(EmailClient.class)
public class DevEmailClient implements EmailClient {

    private static final Logger log = LoggerFactory.getLogger(DevEmailClient.class);

    @Override
    public void sendEmail(String to, String subject, String htmlBody) {
        log.warn("=== [DEV EMAIL CLIENT - NO MAILGUN API KEY CONFIGURED] ===");
        log.info("To: {}", to);
        log.info("Subject: {}", subject);
        log.info("Body preview: {}", htmlBody.length() > 200 ? htmlBody.substring(0, 200) + "..." : htmlBody);
        log.warn("==========================================================");
    }
}
