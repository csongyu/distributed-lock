package xyz.csongyu.distributedlock.db.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import xyz.csongyu.distributedlock.db.entity.Lock;
import xyz.csongyu.distributedlock.db.repository.LockRepository;
import xyz.csongyu.distributedlock.service.LockService;

@Slf4j
@Service
public class LockServiceDBImpl implements LockService<Optional<Lock.LockStatus>> {
    private final LockRepository lockRepository;

    public LockServiceDBImpl(final LockRepository lockRepository) {
        this.lockRepository = lockRepository;
    }

    @Override
    public Optional<Lock.LockStatus> getLock(final String lockName, final String lockRequester) {
        final Lock existLock = this.lockRepository.findByLockName(lockName);
        if (Objects.isNull(existLock)) {
            log.debug("no one owns this lock: {}, {} start to apply for this lock", lockName, lockRequester);
            final Lock newLock = new Lock();
            newLock.setLockName(lockName);
            newLock.setLockOwner(lockRequester);
            newLock.setLastModifiedTime(Timestamp.from(Instant.now()));
            newLock.setLockStatus(Lock.LockStatus.OWN);
            try {
                final Lock lock = this.lockRepository.save(newLock);
                log.debug("{} request lock {} success", lockRequester, lockName);
                return Optional.of(lock.getLockStatus());
            } catch (final DataIntegrityViolationException e) {
                log.debug("{} request lock {} failed", lockRequester, lockName, e);
                return Optional.empty();
            }
        } else {
            final String lockOwner = existLock.getLockOwner();
            if (Objects.equals(lockOwner, lockRequester)) {
                log.debug("{} already own this lock {}, no need to apply for again", lockRequester, lockName);
                this.lockRepository.updateLastModifiedTimeById(Timestamp.from(Instant.now()), existLock.getId());
                return Optional.of(existLock.getLockStatus());
            } else {
                log.debug("can't get lock {} because of being held by {}", lockName, lockOwner);
                return Optional.empty();
            }
        }
    }

    @Override
    public boolean releaseLock(final String lockName, final String lockOwner) {
        final long id = this.lockRepository.deleteByLockNameAndLockOwner(lockName, lockOwner);
        if (0L == id) {
            log.debug("can't release lock {} because {} does not hold the lock", lockName, lockOwner);
            return false;
        } else {
            log.debug("{} releases the lock {}", lockOwner, lockName);
            return true;
        }
    }

    @Override
    public boolean interruptLock(final String lockName) {
        final int size = this.lockRepository.updateLockStatusByLockName(Lock.LockStatus.INTERRUPT, lockName);
        if (size <= 0) {
            log.debug("can't interrupt the lock {}, because it's not exist", lockName);
            return false;
        } else {
            log.debug("interrupt lock {} success", lockName);
            return true;
        }
    }
}
