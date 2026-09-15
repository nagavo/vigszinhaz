package hu.javadev.vigszinhaz.service;

import java.time.LocalDateTime;
import java.util.SortedSet;

public interface VigszinhazService {

  SortedSet<TimeSlot> getTimeSlots();

  SortedSet<LocalDateTime> getAvailableTimeSlots();

  void checkForNewTimeSlots();
}
