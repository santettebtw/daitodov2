package ch.heigvd.tasklists;

import java.util.List;

public record TaskListRequest(String name, List<Integer> taskIds) {}
