package ch.heigvd.tasklists;

import io.javalin.http.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskListsController {
  private final ConcurrentMap<Integer, TaskList> lists;
  private final AtomicInteger uniqueId;

  public TaskListsController(ConcurrentMap<Integer, TaskList> lists, AtomicInteger uniqueId) {
    this.lists = lists;
    this.uniqueId = uniqueId;
  }

  public void create(Context ctx) {
    TaskList newTaskList =
        ctx.bodyValidator(TaskList.class)
            .check(obj -> obj.name() != null, "Missing name")
            .get();

    newTaskList =
        new TaskList(
            uniqueId.getAndIncrement(),
            newTaskList.name(),
            newTaskList.tasks());

    lists.put(newTaskList.id(), newTaskList);

    ctx.status(HttpStatus.CREATED);
    ctx.json(newTaskList);
  }

  public void getOne(Context ctx) {
    Integer id = ctx.pathParamAsClass("id", Integer.class).get();

    TaskList list = lists.get(id);

    if (list == null) {
      throw new NotFoundResponse();
    }

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
    ctx.json(lists);
  }

  public void update(Context ctx) {
    Integer id = ctx.pathParamAsClass("id", Integer.class).get();

    if (!lists.containsKey(id)) {
      throw new NotFoundResponse();
    }

    TaskList updateTaskList =
        ctx.bodyValidator(TaskList.class)
            .check(obj -> obj.name() != null, "Missing name")
            .get();

    lists.put(id, updateTaskList);

    ctx.json(updateTaskList);
  }

  public void delete(Context ctx) {
    Integer id = ctx.pathParamAsClass("id", Integer.class).get();

    if (!lists.containsKey(id)) {
      throw new NotFoundResponse();
    }

    lists.remove(id);

    ctx.status(HttpStatus.NO_CONTENT);
  }
}
