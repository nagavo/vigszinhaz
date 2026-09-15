package hu.javadev.vigszinhaz.controller;

import hu.javadev.vigszinhaz.service.VigszinhazService;
import hu.javadev.vigszinhaz.service.TimeSlot;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class VigszinhazController {

  private final VigszinhazService vigszinhazService;

  public VigszinhazController(VigszinhazService vigszinhazService) {
    this.vigszinhazService = vigszinhazService;
  }

  @GetMapping("/time-slots")
  public SortedSet<TimeSlot> getTimeSlots(@RequestParam(value = "ticketAvailable", required = false) boolean ticketAvailable) {
    SortedSet<TimeSlot> timeSlots = vigszinhazService.getTimeSlots();
    return ticketAvailable ? timeSlots.stream().filter(TimeSlot::ticketAvailable).collect(Collectors.toCollection(TreeSet::new)) : timeSlots;
  }
}
