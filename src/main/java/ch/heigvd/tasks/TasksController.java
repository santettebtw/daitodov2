package ch.heigvd.tasks;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.NotFoundResponse;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class TasksController {
    private final ConcurrentMap<Integer, Task> tasks;

    private final AtomicInteger uniqueId = new AtomicInteger(1);

    public TasksController(ConcurrentMap<Integer, Task> lists) {
        this.tasks = lists;
    }
    
    public void create(Context ctx) {
        Task newTask =
                ctx.bodyValidator(Task.class)
                        .check(obj -> obj.description() != null, "Missing description")
                        .check(obj -> obj.dueDate() != null, "Missing due date")
                        .get();

        newTask =
                new Task(
                        uniqueId.getAndIncrement(),
                        newTask.description(),
                        LocalDate.now(),
                        newTask.dueDate(),
                        newTask.priority(),
                        newTask.status());

        tasks.put(newTask.id(), newTask);

        ctx.status(HttpStatus.CREATED);
        ctx.json(newTask);
    }

    public void getOne(Context ctx) {
        Integer id = ctx.pathParamAsClass("id", Integer.class).get();

        Task task = tasks.get(id);

        if (task == null) {
            throw new NotFoundResponse();
        }

        ctx.json(task);
    }

    public void getMany(Context ctx) {
        String dueDate = ctx.queryParam("dueDate");
        Task.Status status = Task.Status.valueOf(ctx.queryParam("status"));
        Task.Priority priority = Task.Priority.valueOf(ctx.queryParam("priority"));

        List<Task> tasks = new ArrayList<>();

        for (Task task : this.tasks.values()) {
            if (task.dueDate().toString().equals(dueDate)) {
                tasks.add(task);
            } else if (task.status().equals(status)){
                tasks.add(task);
            } else if (task.priority().equals(priority)) {
                tasks.add(task);
            }
        }

        ctx.json(tasks);
    }

    public void getAll(Context ctx) {
        ctx.json(tasks);
    }

    public void update(Context ctx) {
        Integer id = ctx.pathParamAsClass("id", Integer.class).get();

        if (!tasks.containsKey(id)) {
            throw new NotFoundResponse();
        }

        Task updateTask =
                ctx.bodyValidator(Task.class)
                        .check(obj -> obj.description() != null, "Missing description")
                        .check(obj -> obj.dueDate() != null, "Missing due date")
                        .get();

        tasks.put(id, updateTask);

        ctx.json(updateTask);
    }

    public void delete(Context ctx) {
        Integer id = ctx.pathParamAsClass("id", Integer.class).get();

        if (!tasks.containsKey(id)) {
            throw new NotFoundResponse();
        }

        tasks.remove(id);

        ctx.status(HttpStatus.NO_CONTENT);
    }
}
