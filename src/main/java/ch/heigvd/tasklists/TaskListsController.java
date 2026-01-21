package ch.heigvd.tasklists;

import ch.heigvd.tasks.Task;
import io.javalin.http.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskListsController {
  private final ConcurrentMap<Integer, TaskList> lists;
  private final ConcurrentMap<Integer, Task> tasks;
  private final AtomicInteger uniqueId;
  private final ConcurrentMap<Integer, LocalDateTime> taskListsCache;

  private final Integer RESERVED_ID_TO_IDENTIFY_ALL_TASK_LISTS = -1;

  public TaskListsController(
      ConcurrentMap<Integer, TaskList> lists,
      ConcurrentMap<Integer, Task> tasks,
      AtomicInteger uniqueId,
      ConcurrentMap<Integer, LocalDateTime> taskListsCache) {
    this.lists = lists;
    this.tasks = tasks;
    this.uniqueId = uniqueId;
    this.taskListsCache = taskListsCache;
  }

  private TaskListResponse toResponse(TaskList taskList) {
    List<Task> resolvedTasks = new ArrayList<>();
    if (taskList.taskIds() != null) {
      for (Integer taskId : taskList.taskIds()) {
        Task task = tasks.get(taskId);
        if (task != null) {
          resolvedTasks.add(task);
        }
      }
    }
    return new TaskListResponse(taskList.id(), taskList.name(), resolvedTasks);
  }

  private void validateTaskIds(List<Integer> taskIds) {
    if (taskIds != null) {
      for (Integer taskId : taskIds) {
        if (!tasks.containsKey(taskId)) {
          throw new BadRequestResponse("Task with ID " + taskId + " does not exist");
        }
      }
    }
  }

  /**
   * Invalidate cache for task lists that contain the specified task ID. This should be called when
   * a task is updated or deleted.
   */
  public void invalidateCacheForTask(Integer taskId) {
    for (TaskList list : lists.values()) {
      if (list.taskIds() != null && list.taskIds().contains(taskId)) {
        taskListsCache.remove(list.id());
      }
    }
    // also invalidate the "all task lists" cache
    taskListsCache.remove(RESERVED_ID_TO_IDENTIFY_ALL_TASK_LISTS);
  }

  public void create(Context ctx) {
    TaskListRequest request =
        ctx.bodyValidator(TaskListRequest.class)
            .check(obj -> obj.name() != null, "Missing name")
            .get();

    validateTaskIds(request.taskIds());

    TaskList newTaskList =
        new TaskList(
            uniqueId.getAndIncrement(),
            request.name(),
            request.taskIds() != null ? new ArrayList<>(request.taskIds()) : new ArrayList<>());

    lists.put(newTaskList.id(), newTaskList);

    LocalDateTime now = LocalDateTime.now();
    taskListsCache.put(newTaskList.id(), now);

    taskListsCache.remove(RESERVED_ID_TO_IDENTIFY_ALL_TASK_LISTS);

    ctx.status(HttpStatus.CREATED);

    ctx.header("Last-Modified", String.valueOf(now));

    ctx.json(toResponse(newTaskList));
  }

  public void getOne(Context ctx) {
    Integer id = ctx.pathParamAsClass("id", Integer.class).get();

    TaskList list = lists.get(id);

    if (list == null) {
      throw new NotFoundResponse();
    }

    LocalDateTime lastKnownModification =
        ctx.headerAsClass("If-Modified-Since", LocalDateTime.class).getOrDefault(null);

    if (lastKnownModification != null
        && taskListsCache.containsKey(id)
        && taskListsCache.get(id).equals(lastKnownModification)) {
      throw new NotModifiedResponse();
    }

    LocalDateTime now;
    if (taskListsCache.containsKey(list.id())) {
      now = taskListsCache.get(list.id());
    } else {
      now = LocalDateTime.now();
      taskListsCache.put(list.id(), now);
    }

    ctx.header("Last-Modified", String.valueOf(now));

    ctx.json(toResponse(list));
  }

  public void getMany(Context ctx) {
    String name = ctx.queryParam("name");

    List<TaskListResponse> responses = new ArrayList<>();

    for (TaskList list : this.lists.values()) {
      // check if list name contains it (case-insensitive)
      if (name != null
          && name.trim().length() > 0
          && !list.name().toLowerCase().contains(name.toLowerCase().trim())) {
        continue;
      }

      responses.add(toResponse(list));
    }

    ctx.json(responses);
  }

  public void getAll(Context ctx) {
    LocalDateTime lastKnownModification =
        ctx.headerAsClass("If-Modified-Since", LocalDateTime.class).getOrDefault(null);

    if (lastKnownModification != null
        && taskListsCache.containsKey(RESERVED_ID_TO_IDENTIFY_ALL_TASK_LISTS)
        && taskListsCache
            .get(RESERVED_ID_TO_IDENTIFY_ALL_TASK_LISTS)
            .equals(lastKnownModification)) {
      throw new NotModifiedResponse();
    }

    LocalDateTime now;
    if (taskListsCache.containsKey(RESERVED_ID_TO_IDENTIFY_ALL_TASK_LISTS)) {
      now = taskListsCache.get(RESERVED_ID_TO_IDENTIFY_ALL_TASK_LISTS);
    } else {
      now = LocalDateTime.now();
      taskListsCache.put(RESERVED_ID_TO_IDENTIFY_ALL_TASK_LISTS, now);
    }

    // Add the last modification date to the response
    ctx.header("Last-Modified", String.valueOf(now));

    List<TaskListResponse> responses = new ArrayList<>();
    for (TaskList list : lists.values()) {
      responses.add(toResponse(list));
    }

    ctx.json(responses);
  }

  public void update(Context ctx) {
    Integer id = ctx.pathParamAsClass("id", Integer.class).get();

    if (!lists.containsKey(id)) {
      throw new NotFoundResponse();
    }

    LocalDateTime lastKnownModification =
        ctx.headerAsClass("If-Unmodified-Since", LocalDateTime.class).getOrDefault(null);

    if (lastKnownModification != null
        && taskListsCache.containsKey(id)
        && !taskListsCache.get(id).equals(lastKnownModification)) {
      throw new PreconditionFailedResponse();
    }

    TaskListRequest request =
        ctx.bodyValidator(TaskListRequest.class)
            .check(obj -> obj.name() != null, "Missing name")
            .get();

    validateTaskIds(request.taskIds());

    TaskList updatedTaskList =
        new TaskList(
            id,
            request.name(),
            request.taskIds() != null ? new ArrayList<>(request.taskIds()) : new ArrayList<>());

    lists.put(id, updatedTaskList);

    LocalDateTime now = LocalDateTime.now();
    taskListsCache.put(updatedTaskList.id(), now);

    taskListsCache.remove(RESERVED_ID_TO_IDENTIFY_ALL_TASK_LISTS);

    ctx.header("Last-Modified", String.valueOf(now));

    ctx.json(toResponse(updatedTaskList));
  }

  public void delete(Context ctx) {
    Integer id = ctx.pathParamAsClass("id", Integer.class).get();

    if (!lists.containsKey(id)) {
      throw new NotFoundResponse();
    }

    LocalDateTime lastKnownModification =
        ctx.headerAsClass("If-Unmodified-Since", LocalDateTime.class).getOrDefault(null);

    if (lastKnownModification != null
        && taskListsCache.containsKey(id)
        && !taskListsCache.get(id).equals(lastKnownModification)) {
      throw new PreconditionFailedResponse();
    }

    lists.remove(id);

    taskListsCache.remove(id);

    taskListsCache.remove(RESERVED_ID_TO_IDENTIFY_ALL_TASK_LISTS);

    ctx.status(HttpStatus.NO_CONTENT);
  }
}
