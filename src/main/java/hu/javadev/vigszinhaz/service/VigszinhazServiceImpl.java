package hu.javadev.vigszinhaz.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class VigszinhazServiceImpl implements VigszinhazService {

  private static final Logger LOG = LoggerFactory.getLogger(VigszinhazServiceImpl.class);
  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private static final String EVENT_SELECTOR = "#eventsList .ticketChooser";
  private static final String TIME_SLOT_SELECTOR = "time.ticketTime[datetime]";
  private static final String AVAILABLE_TICKET_SELECTOR =
      "a.buyTicketButton[href]:not(.disabled), a.button[href]:not(.disabled)";

  private final WebPageClient webPageClient;
  private final EmailService emailService;
  private final String emailText;
  private final String programUrl;
  private final Map<LocalDateTime, Boolean> previousTimeSlots = new HashMap<>();

  private SortedSet<TimeSlot> timeSlots = new TreeSet<>();
  private boolean initialized;

  public VigszinhazServiceImpl(
      WebPageClient webPageClient,
      EmailService emailService,
      @Value("${vigszinhaz.notification.text}") String emailText,
      @Value("${vigszinhaz.monitor.url}") String programUrl) {
    this.webPageClient = webPageClient;
    this.emailService = emailService;
    this.emailText = emailText;
    this.programUrl = programUrl;
  }

  @Override
  public synchronized SortedSet<TimeSlot> getTimeSlots() {
    return new TreeSet<>(timeSlots);
  }

  @Override
  public synchronized SortedSet<LocalDateTime> getAvailableTimeSlots() {
    return timeSlots.stream()
        .filter(TimeSlot::ticketAvailable)
        .map(TimeSlot::dateTime)
        .collect(Collectors.toCollection(TreeSet::new));
  }

  @Override
  @Scheduled(
      fixedDelayString = "${vigszinhaz.monitor.interval:600000}",
      initialDelayString = "${vigszinhaz.monitor.initial-delay:0}")
  public synchronized void checkForNewTimeSlots() {
    SortedSet<TimeSlot> currentTimeSlots;
    try {
      currentTimeSlots = parseTimeSlots(webPageClient.load(programUrl));
    } catch (IOException e) {
      LOG.error("Failed to fetch the page: {}", programUrl, e);
      return;
    }

    timeSlots = currentTimeSlots;
    if (!initialized) {
      currentTimeSlots.forEach(timeSlot ->
          previousTimeSlots.put(timeSlot.dateTime(), timeSlot.ticketAvailable()));
      initialized = true;
      LOG.info("Initial check completed, {} time slots stored", currentTimeSlots.size());
      return;
    }

    SortedSet<LocalDateTime> newTimeSlots = currentTimeSlots.stream()
        .filter(this::isNewlyAvailable)
        .map(TimeSlot::dateTime)
        .collect(Collectors.toCollection(TreeSet::new));
    if (newTimeSlots.isEmpty()) {
      LOG.info("No new time slot found");
      return;
    }

    emailService.sendEmail(createEmailText(newTimeSlots));
    currentTimeSlots.forEach(timeSlot ->
        previousTimeSlots.put(timeSlot.dateTime(), timeSlot.ticketAvailable()));
    LOG.info("{} new time slots sent in the notification", newTimeSlots.size());
  }

  private SortedSet<TimeSlot> parseTimeSlots(String html) {
    SortedSet<TimeSlot> timeSlots = new TreeSet<>();
    for (Element event : Jsoup.parse(html).select(EVENT_SELECTOR)) {
      Element element = event.select(TIME_SLOT_SELECTOR).first();
      if (element == null) {
        continue;
      }
      String dateTime = element.attr("datetime");
      try {
        LocalDateTime parsedDateTime = LocalDateTime.parse(dateTime, DATE_TIME_FORMATTER);
        boolean ticketAvailable = !event.select(AVAILABLE_TICKET_SELECTOR).isEmpty()
            && "Jegyvásárlás".equals(event.select(AVAILABLE_TICKET_SELECTOR).first().text().trim());
        timeSlots.add(new TimeSlot(parsedDateTime, ticketAvailable));
      } catch (DateTimeParseException e) {
        LOG.warn("Invalid time slot on the page: {}", dateTime);
      }
    }
    return timeSlots;
  }

  private boolean isNewlyAvailable(TimeSlot timeSlot) {
    Boolean previouslyAvailable = previousTimeSlots.get(timeSlot.dateTime());
    return timeSlot.ticketAvailable() && !Boolean.TRUE.equals(previouslyAvailable)
        && timeSlot.dateTime().getMonthValue() >= Month.NOVEMBER.getValue();
  }

  private String createEmailText(SortedSet<LocalDateTime> newTimeSlots) {
    String formattedTimeSlots = newTimeSlots.stream()
        .map(DATE_TIME_FORMATTER::format)
        .collect(Collectors.joining(System.lineSeparator()));
    return emailText
    + System.lineSeparator()
        + formattedTimeSlots
        + System.lineSeparator()
        + System.lineSeparator()
        + programUrl;
  }
}
