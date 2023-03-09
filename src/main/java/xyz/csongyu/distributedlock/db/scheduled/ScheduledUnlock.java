package xyz.csongyu.distributedlock.db.scheduled;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import xyz.csongyu.distributedlock.db.repository.LockRepository;

@Component
public class ScheduledUnlock {
    private final LockRepository lockRepository;

    @Value("${distributed-lock.db.scheduled.lock-expired-seconds}")
    private int expiredSeconds;

    public ScheduledUnlock(final LockRepository lockRepository) {
        this.lockRepository = lockRepository;
    }

    @Scheduled(cron = "${distributed-lock.db.scheduled.unlock-cron}")
    public void unLock() {
        final LocalDateTime xSecondsBefore = LocalDateTime.now().minusSeconds(this.expiredSeconds);
        final Timestamp boundary = Timestamp.valueOf(xSecondsBefore);
        this.lockRepository.deleteByLastModifiedTimeBefore(boundary);
    }
}
