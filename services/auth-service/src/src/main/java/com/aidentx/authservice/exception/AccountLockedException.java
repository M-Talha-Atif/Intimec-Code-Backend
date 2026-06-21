package com.aidentx.authservice.exception;

import com.aidentx.authservice.config.AppConstants;

/**
 * Exception thrown when an attempt to log in is made while the user's account is locked.
 */
public class AccountLockedException extends RuntimeException {

    /**
     * Constructs a new AccountLockedException with a default message.
     */
    public AccountLockedException() {
        super(AppConstants.WRONG_USERNAME_OR_PASSWORD);
    }

    /**
     * Constructs a new AccountLockedException with a formatted message.
     *
     * @param lockedUntil the timestamp until which the account is locked
     */
    public AccountLockedException(String lockedUntil) {
        super(String.format(AppConstants.ACCOUNT_LOCKED, lockedUntil));
    }
}
