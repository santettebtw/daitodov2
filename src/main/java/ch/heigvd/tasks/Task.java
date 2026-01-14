package ch.heigvd.tasks;

import java.time.LocalDate;

public record Task(
    Integer id,
    String description,
    LocalDate createdAt,
    LocalDate dueDate,
    Priority priority,
    Status status) {

  public enum Priority {
    LOW,
    MEDIUM,
    HIGH
  };

  public enum Status {
    TODO,
    DOING,
    DONE
  };
}
