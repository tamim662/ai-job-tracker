package com.jobtracker;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.Paths;

@SpringBootApplication
public class JobTrackerApplication {

    public static void main(String[] args) {
        loadDotEnv();
        SpringApplication.run(JobTrackerApplication.class, args);
    }

    private static void loadDotEnv() {
        // Try project root from backend/ subdir first, then current directory (JAR case)
        for (String dir : new String[]{"../", "./"}) {
            if (!Paths.get(dir, ".env").toFile().exists()) continue;
            try {
                Dotenv.configure()
                        .directory(dir)
                        .load()
                        .entries()
                        .forEach(e -> {
                            // Never override a variable already set by the OS or IDE run config
                            if (System.getenv(e.getKey()) == null) {
                                System.setProperty(e.getKey(), e.getValue());
                            }
                        });
            } catch (Exception ignored) {}
            return;
        }
    }
}
