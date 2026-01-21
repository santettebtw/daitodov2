package ch.heigvd.tasklists;

import java.util.List;

public record TaskList(Integer id, String name, List<Integer> taskIds) {}
