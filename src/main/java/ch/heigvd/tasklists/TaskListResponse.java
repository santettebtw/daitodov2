package ch.heigvd.tasklists;

import ch.heigvd.tasks.Task;
import java.util.List;

public record TaskListResponse(Integer id, String name, List<Task> tasks) {}
