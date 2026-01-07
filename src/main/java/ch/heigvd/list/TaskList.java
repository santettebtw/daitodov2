package ch.heigvd.list;

public record TaskList(Integer id, String name, Task... tasks) {}