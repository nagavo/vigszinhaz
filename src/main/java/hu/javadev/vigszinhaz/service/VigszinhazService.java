package hu.javadev.vigszinhaz.service;

import java.time.LocalDateTime;
import java.util.SortedSet;

public interface VigszinhazService {

  SortedSet<LocalDateTime> getAvailableTimeSlots();

  void checkForNewTimeSlots();
}
