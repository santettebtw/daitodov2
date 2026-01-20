package ch.heigvd.tasklists;

import io.javalin.http.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskListsController {
  private final ConcurrentMap<Integer, TaskList> lists;
  private final AtomicInteger uniqueId;
  private final ConcurrentMap<Integer, LocalDateTime> taskListsCache;

  private final Integer RESERVED_ID_TO_IDENTIFY_ALL_TASK_LISTS = -1;

  public TaskListsController(
      ConcurrentMap<Integer, TaskList> lists,
      AtomicInteger uniqueId,
      ConcurrentMap<Integer, LocalDateTime> taskListsCache) {
    this.lists = lists;
    this.uniqueId = uniqueId;
    this.taskListsCache = taskListsCache;
  }

  public void create(Context ctx) {
    TaskList newTaskList =
        ctx.bodyValidator(TaskList.class).check(obj -> obj.name() != null, "Missing name").get();

    newTaskList = new TaskList(uniqueId.getAndIncrement(), newTaskList.name(), newTaskList.tasks());

    lists.put(newTaskList.id(), newTaskList);

    LocalDateTime now = LocalDateTime.now();
    taskListsCache.put(newTaskList.id(), now);

    taskListsCache.remove(RESERVED_ID_TO_IDENTIFY_ALL_TASK_LISTS);

    ctx.status(HttpStatus.CREATED);

    ctx.header("Last-Modified", String.valueOf(now));

    ctx.json(newTaskList);
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

    ctx.json(list);
  }

  public void getMany(Context ctx) {
    String name = ctx.queryParam("name");

    List<TaskList> lists = new ArrayList<>();

    for (TaskList list : this.lists.values()) {
      if (name != null && !list.name().equalsIgnoreCase(name)) {
        continue;
      }

      lists.add(list);
    }

    ctx.json(lists);
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

    ctx.json(lists);
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

    TaskList updateTaskList =
        ctx.bodyValidator(TaskList.class).check(obj -> obj.name() != null, "Missing name").get();

    lists.put(id, updateTaskList);

    LocalDateTime now = LocalDateTime.now();
    taskListsCache.put(updateTaskList.id(), now);

    taskListsCache.remove(RESERVED_ID_TO_IDENTIFY_ALL_TASK_LISTS);

    ctx.header("Last-Modified", String.valueOf(now));

    ctx.json(updateTaskList);
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
