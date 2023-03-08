package xyz.csongyu.distributedlock.db.service;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import xyz.csongyu.distributedlock.db.entity.Lock;
import xyz.csongyu.distributedlock.db.repository.LockRepository;
import xyz.csongyu.distributedlock.service.LockService;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class LockServiceDBImplTest {
    @Autowired
    private LockService<Optional<Lock.LockStatus>> lockService;

    @Autowired
    private LockRepository lockRepository;

    @Test
    void givenNoOneHoldLock_whenAcquireLock_thenSuccess() {
        final String lockName = "LOCK_NAME_*";
        final String lockRequester = "LOCK_REQUESTER_*";

        // [given] no one holds the lock
        // [when] someone acquires the lock
        final Optional<Lock.LockStatus> lockStatus = this.lockService.getLock(lockName, lockRequester);
        // [then] someone got the lock
        Assertions.assertTrue(lockStatus.isPresent());
        Assertions.assertEquals(Lock.LockStatus.OWN, lockStatus.get());

        Assertions.assertEquals(1, this.lockRepository.count());
        final Lock result = this.lockRepository.findByLockName(lockName);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(lockRequester, result.getLockOwner());
        Assertions.assertEquals(Lock.LockStatus.OWN, result.getLockStatus());
    }

    @Test
    void givenSomeoneHoldLock_whenAcquireLockAgain_thenSuccess() {
        final String lockName = "LOCK_NAME_*";
        final String lockRequester = "LOCK_REQUESTER_*";

        // [given] someone holds the lock
        this.lockService.getLock(lockName, lockRequester);
        final Timestamp before = this.lockRepository.findByLockName(lockName).getLastModifiedTime();
        // [when] someone acquires the lock again
        final Optional<Lock.LockStatus> lockStatus = this.lockService.getLock(lockName, lockRequester);
        // [then] someone got the lock
        Assertions.assertTrue(lockStatus.isPresent());
        Assertions.assertEquals(Lock.LockStatus.OWN, lockStatus.get());

        Assertions.assertEquals(1, this.lockRepository.count());
        final Lock result = this.lockRepository.findByLockName(lockName);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(lockRequester, result.getLockOwner());
        Assertions.assertEquals(Lock.LockStatus.OWN, result.getLockStatus());
        Assertions.assertTrue(before.compareTo(result.getLastModifiedTime()) < 0);
    }

    @Test
    void givenOtherThreadsHoldLock_whenCurrentThreadAcquireLock_thenFailed() {
        final String lockName = "LOCK_NAME_*";
        final String currentLockRequester = "LOCK_REQUESTER_THREAD_CURRENT";
        final String otherLockRequester = "LOCK_REQUESTER_THREAD_OTHER";

        // [given] the other thread holds the lock
        this.lockService.getLock(lockName, otherLockRequester);
        // [when] the current thread acquires the lock
        final Optional<Lock.LockStatus> lockStatus = this.lockService.getLock(lockName, currentLockRequester);
        // [then] the current thread cannot acquire the lock
        Assertions.assertTrue(lockStatus.isEmpty());
    }

    @Test
    void givenNoOneHoldLock_whenAcquireLockAtTheSameTime_thenOnlyOneSuccess() {
        final String lockName = "LOCK_NAME_*";
        final String lockRequesterPrefix = "LOCK_REQUEST_THREAD_";

        // [given] no one hold the lock
        // [when] they acquire the lock at the same time
        final Map<String, FutureTask<Optional<Lock.LockStatus>>> futureTasks = new HashMap<>();
        final CountDownLatch countDownLatch = new CountDownLatch(8);
        IntStream.range(0, 8).forEach(i -> {
            final String lockRequester = lockRequesterPrefix + i;
            final FutureTask<Optional<Lock.LockStatus>> futureTask = new FutureTask<>(() -> {
                countDownLatch.await();
                return this.lockService.getLock(lockName, lockRequester);
            });
            futureTasks.put(lockRequester, futureTask);
            new Thread(futureTask).start();
            countDownLatch.countDown();
        });
        // [then] only one thread acquires the lock
        final List<String> lockOwner = futureTasks.entrySet().stream().filter(entry -> {
            try {
                final Optional<Lock.LockStatus> lockStatus = entry.getValue().get();
                return lockStatus.isPresent() && Lock.LockStatus.OWN.equals(lockStatus.get());
            } catch (final InterruptedException | ExecutionException e) {
                return false;
            }
        }).map(Map.Entry::getKey).collect(Collectors.toList());
        Assertions.assertEquals(1, lockOwner.size());

        Assertions.assertEquals(1, this.lockRepository.count());
        final Lock result = this.lockRepository.findByLockName(lockName);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(lockOwner.get(0), result.getLockOwner());
    }

    @Test
    public void givenSomeoneHoldLock_whenReleaseLock_thenSuccess() {
        final String lockName = "LOCK_NAME_*";
        final String lockRequester = "LOCK_REQUESTER_*";

        // [given] someone holds the lock
        this.lockService.getLock(lockName, lockRequester);
        // [when] someone releases the lock
        final boolean result = this.lockService.releaseLock(lockName, lockRequester);
        // [then] someone no longer owns the lock
        Assertions.assertTrue(result);

        Assertions.assertEquals(0, this.lockRepository.count());
    }

    @Test
    void givenOtherThreadsHoldLock_whenCurrentThreadReleaseLock_thenFailed() {
        final String lockName = "LOCK_NAME_*";
        final String currentLockRequester = "LOCK_REQUESTER_THREAD_CURRENT";
        final String otherLockRequester = "LOCK_REQUESTER_THREAD_OTHER";

        // [given] the other thread holds the lock
        this.lockService.getLock(lockName, otherLockRequester);
        // [when] the current thread release the lock
        final boolean result = this.lockService.releaseLock(lockName, currentLockRequester);
        // [then] the other thread still owns the lock
        Assertions.assertFalse(result);

        Assertions.assertEquals(1, this.lockRepository.count());
        final Lock lock = this.lockRepository.findByLockName(lockName);
        Assertions.assertNotNull(lock);
        Assertions.assertEquals(otherLockRequester, lock.getLockOwner());
        Assertions.assertEquals(Lock.LockStatus.OWN, lock.getLockStatus());
    }

    @Test
    void givenLockExist_whenInterruptLock_thenSuccess() {
        final String lockName = "LOCK_NAME_*";
        final String lockRequester = "LOCK_REQUESTER_*";

        // [given] lock exist
        this.lockService.getLock(lockName, lockRequester);
        // [when] interrupt the lock
        final boolean result = this.lockService.interruptLock(lockName);
        // [then] lock status is changed
        Assertions.assertTrue(result);

        Assertions.assertEquals(1, this.lockRepository.count());
        final Lock lock = this.lockRepository.findByLockName(lockName);
        Assertions.assertNotNull(lock);
        Assertions.assertEquals(lockRequester, lock.getLockOwner());
        Assertions.assertEquals(Lock.LockStatus.INTERRUPT, lock.getLockStatus());
    }

    @Test
    void givenLockNotExist_whenInterruptLock_thenFailed() {
        // [given] lock not exist
        // [when] interrupt the lock
        final boolean result = this.lockService.interruptLock("NOT_EXIST");
        // [then] nothing will happen
        Assertions.assertFalse(result);
    }
}