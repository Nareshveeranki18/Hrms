package com.hrms.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendTemplatedEmailRequest;
import software.amazon.awssdk.services.ses.model.SendTemplatedEmailResponse;
import software.amazon.awssdk.services.ses.model.SesException;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Value("${app.storage.type:local}")
    private String storageType;

    @Value("${aws.ses.sender-email}")
    private String senderEmail;

    @Value("${aws.region}")
    private String region;

    public String sendTemplatedEmail(String toAddress, String templateName, String templateDataJson) {
        if ("local".equalsIgnoreCase(storageType)) {
            log.info("[LOCAL EMAIL MOCK] To: {} | Template: {} | Data: {}", toAddress, templateName, templateDataJson);
            return "local-mock-email-id";
        }

        try (SesClient sesClient = SesClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build()) {

            SendTemplatedEmailRequest request = SendTemplatedEmailRequest.builder()
                    .source(senderEmail)
                    .destination(d -> d.toAddresses(toAddress))
                    .template(templateName)
                    .templateData(templateDataJson)
                    .build();

            SendTemplatedEmailResponse response = sesClient.sendTemplatedEmail(request);
            return response.messageId();
        } catch (SesException e) {
            throw new RuntimeException("Failed to send email via SES: " + e.awsErrorDetails().errorMessage());
        }
    }
}