package xyz.csongyu.distributedlock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DistributedLockApplication {
    public static void main(final String[] args) {
        SpringApplication.run(DistributedLockApplication.class, args);
    }
}
