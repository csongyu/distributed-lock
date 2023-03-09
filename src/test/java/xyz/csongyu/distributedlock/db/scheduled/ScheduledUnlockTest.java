package xyz.csongyu.distributedlock.db.scheduled;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;

import xyz.csongyu.distributedlock.db.entity.Lock;
import xyz.csongyu.distributedlock.db.repository.LockRepository;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ScheduledUnlockTest {
    @SpyBean
    private ScheduledUnlock scheduledUnlock;

    @Autowired
    private LockRepository lockRepository;

    @Test
    void whenWaitFiveSeconds_thenScheduledIsCalledAtLeastOneTime() {
        await().atMost(Duration.ofSeconds(5L)).untilAsserted(() -> verify(this.scheduledUnlock, atLeast(1)).unLock());
    }

    @Test
    void givenExistExpiredLock_whenWaitFiveSeconds_thenUnlockSuccess() {
        // [given] exist expired lock
        IntStream.range(0, 2).forEach(i -> {
            final Lock expiredLock = new Lock();
            expiredLock.setLockName("EXPIRED_LOCK_NAME_" + i);
            expiredLock.setLockOwner("EXPIRED_LOCK_OWNER_" + i);
            expiredLock.setLastModifiedTime(Timestamp.from(Instant.now().minusSeconds(30L)));
            expiredLock.setLockStatus(Lock.LockStatus.OWN);
            this.lockRepository.save(expiredLock);
        });
        // [when] wait 5 seconds
        // [then] unlock expired lock
        await().atMost(Duration.ofSeconds(5L)).until(() -> this.lockRepository.count() == 0L);
    }
}