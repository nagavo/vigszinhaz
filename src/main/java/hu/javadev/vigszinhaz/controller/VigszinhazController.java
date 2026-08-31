package hu.javadev.vigszinhaz.controller;

import hu.javadev.vigszinhaz.service.VigszinhazService;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class VigszinhazController {

  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private final VigszinhazService vigszinhazService;

  public VigszinhazController(VigszinhazService vigszinhazService) {
    this.vigszinhazService = vigszinhazService;
  }

  @GetMapping("/time-slots")
  public List<String> getTimeSlots() {
    return vigszinhazService.getAvailableTimeSlots().stream()
        .map(DATE_TIME_FORMATTER::format)
        .toList();
  }
}
