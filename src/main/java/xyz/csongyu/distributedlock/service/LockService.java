package xyz.csongyu.distributedlock.service;

import javax.validation.constraints.NotBlank;

import org.springframework.validation.annotation.Validated;

@Validated
public interface LockService<T> {
    T getLock(@NotBlank String lockName, @NotBlank String lockRequester);

    boolean releaseLock(@NotBlank String lockName, @NotBlank String lockOwner);

    boolean interruptLock(@NotBlank String lockName);
}
