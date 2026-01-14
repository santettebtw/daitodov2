package ch.heigvd.tasklists;

import ch.heigvd.tasks.Task;

public record TaskList(Integer id, String name, Task... tasks) {}
