package hu.javadev.vigszinhaz.service;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSendException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class ResendEmailService implements EmailService {

  private static final Logger LOG = LoggerFactory.getLogger(ResendEmailService.class);
  private static final String RESEND_EMAILS_URL = "https://api.resend.com/emails";

  private final RestClient restClient;
  private final String apiKey;
  private final String mailFrom;
  private final String mailTo;
  private final String mailSubject;

  public ResendEmailService(
      RestClient.Builder restClientBuilder,
      @Value("${resend.api-key}") String apiKey,
      @Value("${resend.from}") String mailFrom,
      @Value("${vigszinhaz.notification.recipient}") String mailTo,
      @Value("${vigszinhaz.notification.subject}") String mailSubject) {
    this.restClient = restClientBuilder.build();
    this.apiKey = apiKey;
    this.mailFrom = mailFrom;
    this.mailTo = mailTo;
    this.mailSubject = mailSubject;
  }

  @Override
  public void sendEmail(String text) {
    LOG.info("Sending email to {}", mailTo);
    try {
      restClient.post()
          .uri(RESEND_EMAILS_URL)
          .header("Authorization", "Bearer " + apiKey)
          .body(Map.of(
              "from", mailFrom,
              "to", new String[] {mailTo},
              "subject", mailSubject,
              "text", text))
          .retrieve()
          .toBodilessEntity();
    } catch (RestClientException e) {
      throw new MailSendException("Failed to send notification through Resend", e);
    }
  }
}
