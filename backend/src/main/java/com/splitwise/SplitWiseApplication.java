package com.splitwise;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @SpringBootApplication is a meta-annotation that combines:
 * - @Configuration: Marks this class as a source of bean definitions
 * - @EnableAutoConfiguration: Tells Spring Boot to auto-configure beans based on classpath
 * - @ComponentScan: Scans this package and sub-packages for @Component, @Service, @Repository, etc.
 *
 * @EnableScheduling: Needed for recurring expense processing (cron jobs).
 * Alternative: Use Quartz Scheduler (heavier, supports clustering) — overkill for us.
 */
@SpringBootApplication
@EnableScheduling
public class SplitWiseApplication {

    public static void main(String[] args) {
        applyDotEnv();
        SpringApplication.run(SplitWiseApplication.class, args);
    }

    /**
     * Reads the first existing {@code .env} from {@code backend/.env} (repo root cwd) or {@code ./.env} (when cwd is {@code backend/}).
     * Does not overwrite keys already supplied by {@code export ...} / IDE Run env.
     */
    private static void applyDotEnv() {
        String[] directories = { "backend", "." };
        for (String dir : directories) {
            if (!Files.exists(Path.of(dir, ".env"))) {
                continue;
            }
            Dotenv dotenv = Dotenv.configure()
                    .directory(dir)
                    .filename(".env")
                    .ignoreIfMalformed()
                    .ignoreIfMissing()
                    .load();
            dotenv.entries().forEach(e -> {
                String key = e.getKey();
                if (key != null && !key.isBlank() && isUnset(key.trim())) {
                    String value = e.getValue();
                    System.setProperty(key.trim(), value != null ? value : "");
                }
            });
            return;
        }
    }

    private static boolean isUnset(String key) {
        String env = System.getenv(key);
        if (env != null && !env.isEmpty()) {
            return false;
        }
        String sys = System.getProperty(key);
        return sys == null || sys.isEmpty();
    }
}
