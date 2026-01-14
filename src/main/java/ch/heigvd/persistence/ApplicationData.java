package ch.heigvd.persistence;

import ch.heigvd.list.TaskList;
import ch.heigvd.task.Task;
import java.util.Map;

/** Data model to hold all application state for persistence. */
public record ApplicationData(Map<Integer, Task> tasks, Map<Integer, TaskList> taskLists) {
  public ApplicationData {
    if (tasks == null) {
      tasks = new java.util.concurrent.ConcurrentHashMap<>();
    }
    if (taskLists == null) {
      taskLists = new java.util.concurrent.ConcurrentHashMap<>();
    }
  }
}
