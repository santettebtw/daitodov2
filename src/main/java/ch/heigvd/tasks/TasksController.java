package ch.heigvd.tasks;

import ch.heigvd.tasklists.TaskListsController;
import io.javalin.http.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class TasksController {
  private final ConcurrentMap<Integer, Task> tasks;
  private final AtomicInteger nextTaskId;
  private final ConcurrentMap<Integer, LocalDateTime> tasksCache;
  private TaskListsController taskListsController;

  // Magic number used to store the tasks list last modification date
  // As the ID for tasks starts from 1, it is safe to reserve the value -1 for all tasks
  private final Integer RESERVED_ID_TO_IDENTIFY_ALL_TASKS = -1;

  public TasksController(
      ConcurrentMap<Integer, Task> tasks,
      AtomicInteger nextTaskId,
      ConcurrentMap<Integer, LocalDateTime> tasksCache) {
    this.tasks = tasks;
    this.nextTaskId = nextTaskId;
    this.tasksCache = tasksCache;
  }

  /** Set the task lists controller for cache invalidation. */
  public void setTaskListsController(TaskListsController taskListsController) {
    this.taskListsController = taskListsController;
  }

  public void create(Context ctx) {
    Task newTask =
        ctx.bodyValidator(Task.class)
            .check(obj -> obj.description() != null, "Missing description")
            .check(obj -> obj.dueDate() != null, "Missing due date")
            .get();

    newTask =
        new Task(
            nextTaskId.getAndIncrement(),
            newTask.description(),
            LocalDate.now(),
            newTask.dueDate(),
            newTask.priority(),
            newTask.status());

    tasks.put(newTask.id(), newTask);

    LocalDateTime now = LocalDateTime.now();
    tasksCache.put(newTask.id(), now);

    tasksCache.remove(RESERVED_ID_TO_IDENTIFY_ALL_TASKS);

    ctx.status(HttpStatus.CREATED);

    ctx.header("Last-Modified", String.valueOf(now));

    ctx.json(newTask);
  }

  public void getOne(Context ctx) {
    Integer id = ctx.pathParamAsClass("id", Integer.class).get();

    Task task = tasks.get(id);

    if (task == null) {
      throw new NotFoundResponse();
    }

    LocalDateTime lastKnownModification =
        ctx.headerAsClass("If-Modified-Since", LocalDateTime.class).getOrDefault(null);

    if (lastKnownModification != null
        && tasksCache.containsKey(id)
        && tasksCache.get(id).equals(lastKnownModification)) {
      throw new NotModifiedResponse();
    }

    LocalDateTime now;
    if (tasksCache.containsKey(task.id())) {
      now = tasksCache.get(task.id());
    } else {
      now = LocalDateTime.now();
      tasksCache.put(task.id(), now);
    }

    ctx.header("Last-Modified", String.valueOf(now));

    ctx.json(task);
  }

  public void getMany(Context ctx) {
    String dueDate = ctx.queryParam("dueDate");
    String statusParam = ctx.queryParam("status");
    String priorityParam = ctx.queryParam("priority");

    Task.Status status = null;
    Task.Priority priority = null;

    if (statusParam != null) {
      try {
        status = Task.Status.valueOf(statusParam.toUpperCase());
      } catch (IllegalArgumentException e) {
        throw new BadRequestResponse("Invalid status: " + statusParam);
      }
    }

    if (priorityParam != null) {
      try {
        priority = Task.Priority.valueOf(priorityParam.toUpperCase());
      } catch (IllegalArgumentException e) {
        throw new BadRequestResponse("Invalid priority: " + priorityParam);
      }
    }

    List<Task> filteredTasks = new ArrayList<>();

    // Apply filters with AND logic (all provided filters must match)
    for (Task task : this.tasks.values()) {
      boolean matches = true;
      if (dueDate != null && !task.dueDate().toString().equals(dueDate)) {
        matches = false;
      }
      if (status != null && !task.status().equals(status)) {
        matches = false;
      }
      if (priority != null && !task.priority().equals(priority)) {
        matches = false;
      }
      if (matches) {
        filteredTasks.add(task);
      }
    }

    ctx.json(filteredTasks);
  }

  public void getAll(Context ctx) {
    LocalDateTime lastKnownModification =
        ctx.headerAsClass("If-Modified-Since", LocalDateTime.class).getOrDefault(null);

    if (lastKnownModification != null
        && tasksCache.containsKey(RESERVED_ID_TO_IDENTIFY_ALL_TASKS)
        && tasksCache.get(RESERVED_ID_TO_IDENTIFY_ALL_TASKS).equals(lastKnownModification)) {
      throw new NotModifiedResponse();
    }

    LocalDateTime now;
    if (tasksCache.containsKey(RESERVED_ID_TO_IDENTIFY_ALL_TASKS)) {
      now = tasksCache.get(RESERVED_ID_TO_IDENTIFY_ALL_TASKS);
    } else {
      now = LocalDateTime.now();
      tasksCache.put(RESERVED_ID_TO_IDENTIFY_ALL_TASKS, now);
    }

    ctx.header("Last-Modified", String.valueOf(now));

    ctx.json(tasks);
  }

  public void update(Context ctx) {
    Integer id = ctx.pathParamAsClass("id", Integer.class).get();

    if (!tasks.containsKey(id)) {
      throw new NotFoundResponse();
    }

    LocalDateTime lastKnownModification =
        ctx.headerAsClass("If-Unmodified-Since", LocalDateTime.class).getOrDefault(null);

    if (lastKnownModification != null
        && tasksCache.containsKey(id)
        && !tasksCache.get(id).equals(lastKnownModification)) {
      throw new PreconditionFailedResponse();
    }

    Task existingTask = tasks.get(id);
    Task updateTask =
        ctx.bodyValidator(Task.class)
            .check(obj -> obj.description() != null, "Missing description")
            .check(obj -> obj.dueDate() != null, "Missing due date")
            .get();

    Task updatedTask =
        new Task(
            existingTask.id(),
            updateTask.description(),
            existingTask.createdAt(),
            updateTask.dueDate(),
            updateTask.priority(),
            updateTask.status());

    tasks.put(id, updatedTask);

    LocalDateTime now = LocalDateTime.now();
    tasksCache.put(updatedTask.id(), now);

    tasksCache.remove(RESERVED_ID_TO_IDENTIFY_ALL_TASKS);

    // invalidate cache for task lists containing this task
    if (taskListsController != null) {
      taskListsController.invalidateCacheForTask(id);
    }

    ctx.header("Last-Modified", String.valueOf(now));

    ctx.json(updatedTask);
  }

  public void delete(Context ctx) {
    Integer id = ctx.pathParamAsClass("id", Integer.class).get();

    if (!tasks.containsKey(id)) {
      throw new NotFoundResponse();
    }

    LocalDateTime lastKnownModification =
        ctx.headerAsClass("If-Unmodified-Since", LocalDateTime.class).getOrDefault(null);

    if (lastKnownModification != null
        && tasksCache.containsKey(id)
        && !tasksCache.get(id).equals(lastKnownModification)) {
      throw new PreconditionFailedResponse();
    }

    tasks.remove(id);

    tasksCache.remove(id);
    tasksCache.remove(RESERVED_ID_TO_IDENTIFY_ALL_TASKS);

    // invalidate cache for task lists containing this task
    if (taskListsController != null) {
      taskListsController.invalidateCacheForTask(id);
    }

    ctx.status(HttpStatus.NO_CONTENT);
  }
}
