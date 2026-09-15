package hu.javadev.vigszinhaz.service;

import java.time.LocalDateTime;

public record TimeSlot(LocalDateTime dateTime, boolean ticketAvailable)
    implements Comparable<TimeSlot> {

  @Override
  public int compareTo(TimeSlot other) {
    return dateTime.compareTo(other.dateTime);
  }
}
