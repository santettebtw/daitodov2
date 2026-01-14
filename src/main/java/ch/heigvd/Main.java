package ch.heigvd;

import ch.heigvd.persistence.ApplicationData;
import ch.heigvd.persistence.PersistenceService;
import ch.heigvd.tasklists.TaskList;
import ch.heigvd.tasklists.TaskListsController;
import ch.heigvd.tasks.Task;
import ch.heigvd.tasks.TasksController;
import io.javalin.Javalin;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
  public static final int PORT = 8080;

  public static void main(String[] args) {
    PersistenceService persistence = new PersistenceService();
    ApplicationData data = persistence.load();

    ConcurrentMap<Integer, Task> tasks = new ConcurrentHashMap<>(data.tasks());
    ConcurrentMap<Integer, TaskList> taskLists = new ConcurrentHashMap<>(data.taskLists());

    AtomicInteger nextTaskId =
        new AtomicInteger(tasks.keySet().stream().mapToInt(i -> i).max().orElse(0) + 1);
    AtomicInteger nextTaskListId =
        new AtomicInteger(taskLists.keySet().stream().mapToInt(i -> i).max().orElse(0) + 1);

    TasksController tasksController = new TasksController(tasks, nextTaskId);
    TaskListsController taskListsController = new TaskListsController(taskLists, nextTaskListId);

    Javalin app = Javalin.create();

    app.get("/tasks", tasksController::getAll);
    app.get("/tasks/{id}", tasksController::getOne);
    app.post("/tasks", tasksController::create);
    app.put("/tasks", tasksController::update);
    app.delete("/tasks/{id}", tasksController::delete);

    app.get("/tasklists", taskListsController::getAll);
    app.get("/tasklists/{id}", taskListsController::getOne);
    app.post("/tasklists", taskListsController::create);
    app.put("/tasklists/{id}", taskListsController::update);
    app.delete("/tasklists/{id}", taskListsController::delete);

    // https://docs.oracle.com/javase/8/docs/technotes/guides/lang/hook-design.html
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  System.out.println("Saving data...");
                  persistence.save(new ApplicationData(tasks, taskLists));
                }));

    app.start(PORT);
  }
}
