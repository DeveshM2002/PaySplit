package com.splitwise;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

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
        SpringApplication.run(SplitWiseApplication.class, args);
    }
}
