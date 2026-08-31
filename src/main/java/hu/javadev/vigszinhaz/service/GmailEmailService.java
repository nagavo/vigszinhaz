package hu.javadev.vigszinhaz.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class GmailEmailService implements EmailService {

  private static final Logger LOG = LoggerFactory.getLogger(GmailEmailService.class);

  private final JavaMailSender mailSender;
  private final String mailFrom;
  private final String mailTo;
  private final String mailSubject;

  public GmailEmailService(
      JavaMailSender mailSender,
      @Value("${spring.mail.username}") String mailFrom,
      @Value("${vigszinhaz.notification.recipient}") String mailTo,
      @Value("${vigszinhaz.notification.subject}") String mailSubject) {
    this.mailSender = mailSender;
    this.mailFrom = mailFrom;
    this.mailTo = mailTo;
    this.mailSubject = mailSubject;
  }

  @Override
  public void sendEmail(String text) {
    LOG.info("Sending email to {}", mailTo);
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(mailFrom);
    message.setTo(mailTo);
    message.setSubject(mailSubject);
    message.setText(text);
    mailSender.send(message);
  }
}
