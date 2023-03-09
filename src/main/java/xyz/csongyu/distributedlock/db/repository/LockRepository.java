package xyz.csongyu.distributedlock.db.repository;

import java.sql.Timestamp;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import xyz.csongyu.distributedlock.db.entity.Lock;

public interface LockRepository extends JpaRepository<Lock, Long> {
    Lock findByLockName(String lockName);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Lock lk set lk.lastModifiedTime = :lastModifiedTime where lk.id = :id")
    void updateLastModifiedTimeById(@Param("lastModifiedTime") Timestamp lastModifiedTime, @Param("id") Long id);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Lock lk set lk.lockStatus = :lockStatus where lk.lockName = :lockName")
    int updateLockStatusByLockName(@Param("lockStatus") Lock.LockStatus lockStatus, @Param("lockName") String lockName);

    @Transactional
    long deleteByLockNameAndLockOwner(String lockName, String lockOwner);

    @Transactional
    int deleteByLastModifiedTimeBefore(Timestamp boundary);
}
