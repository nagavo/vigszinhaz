package hu.javadev.vigszinhaz.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Set;
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
  private static final String TIME_SLOT_SELECTOR =
      "#eventsList time.ticketTime[datetime]";

  private final WebPageClient webPageClient;
  private final EmailService emailService;
  private final String programUrl;
  private final Set<LocalDateTime> seenTimeSlots = new HashSet<>();

  private SortedSet<LocalDateTime> availableTimeSlots = new TreeSet<>();
  private boolean initialized;

  public VigszinhazServiceImpl(
      WebPageClient webPageClient,
      EmailService emailService,
      @Value("${vigszinhaz.monitor.url}") String programUrl) {
    this.webPageClient = webPageClient;
    this.emailService = emailService;
    this.programUrl = programUrl;
  }

  @Override
  public synchronized SortedSet<LocalDateTime> getAvailableTimeSlots() {
    return new TreeSet<>(availableTimeSlots);
  }

  @Override
  @Scheduled(
      fixedDelayString = "${vigszinhaz.monitor.interval:600000}",
      initialDelayString = "${vigszinhaz.monitor.initial-delay:0}")
  public synchronized void checkForNewTimeSlots() {
    SortedSet<LocalDateTime> currentTimeSlots;
    try {
      currentTimeSlots = parseTimeSlots(webPageClient.load(programUrl));
    } catch (IOException e) {
      LOG.error("Failed to fetch the page: {}", programUrl, e);
      return;
    }

    availableTimeSlots = currentTimeSlots;
    if (!initialized) {
      seenTimeSlots.addAll(currentTimeSlots);
      initialized = true;
      LOG.info("Initial check completed, {} time slots stored", currentTimeSlots.size());
      return;
    }

    SortedSet<LocalDateTime> newTimeSlots = currentTimeSlots.stream()
        .filter(timeSlot -> !seenTimeSlots.contains(timeSlot))
        .collect(Collectors.toCollection(TreeSet::new));
    if (newTimeSlots.isEmpty()) {
      LOG.info("No new time slot found");
      return;
    }

    emailService.sendEmail(createEmailText(newTimeSlots));
    seenTimeSlots.addAll(newTimeSlots);
    LOG.info("{} new time slots sent in the notification", newTimeSlots.size());
  }

  private SortedSet<LocalDateTime> parseTimeSlots(String html) {
    SortedSet<LocalDateTime> timeSlots = new TreeSet<>();
    for (Element element : Jsoup.parse(html).select(TIME_SLOT_SELECTOR)) {
      String dateTime = element.attr("datetime");
      try {
        timeSlots.add(LocalDateTime.parse(dateTime, DATE_TIME_FORMATTER));
      } catch (DateTimeParseException e) {
        LOG.warn("Invalid time slot on the page: {}", dateTime);
      }
    }
    return timeSlots;
  }

  private String createEmailText(SortedSet<LocalDateTime> newTimeSlots) {
    String formattedTimeSlots = newTimeSlots.stream()
        .map(DATE_TIME_FORMATTER::format)
        .collect(Collectors.joining(System.lineSeparator()));
    return "A new performance date has been added:"
    + System.lineSeparator()
        + formattedTimeSlots
        + System.lineSeparator()
        + System.lineSeparator()
        + programUrl;
  }
}
