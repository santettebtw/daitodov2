package ch.heigvd.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for persisting and loading application data from JSON files.
 * https://www.baeldung.com/jackson-object-mapper-tutorial
 */
public class PersistenceService {
  private static final String DATA_FILE = "data.json";
  private final ObjectMapper objectMapper;
  private final Path dataFilePath;

  public PersistenceService() {
    this.objectMapper = new ObjectMapper();
    // add time parsing modile (like with javalin)
    this.objectMapper.registerModule(new JavaTimeModule());
    this.dataFilePath = Paths.get(DATA_FILE);
  }

  public ApplicationData load() {
    if (!Files.exists(dataFilePath)) {
      return new ApplicationData(new ConcurrentHashMap<>(), new ConcurrentHashMap<>());
    }

    try {
      return objectMapper.readValue(dataFilePath.toFile(), ApplicationData.class);
    } catch (IOException e) {
      System.err.println("Failed to load data: " + e.getMessage());
      return new ApplicationData(new ConcurrentHashMap<>(), new ConcurrentHashMap<>());
    }
  }

  public void save(ApplicationData data) {
    try {
      objectMapper.writeValue(dataFilePath.toFile(), data);
    } catch (IOException e) {
      throw new RuntimeException("Failed to save data", e);
    }
  }
}
