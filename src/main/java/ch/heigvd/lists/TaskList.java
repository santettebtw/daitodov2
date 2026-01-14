package ch.heigvd.lists;

import ch.heigvd.tasks.Task;

public record TaskList(Integer id, String name, Task... tasks) {}