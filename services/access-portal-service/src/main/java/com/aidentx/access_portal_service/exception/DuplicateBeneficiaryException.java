package com.aidentx.access_portal_service.exception;

import lombok.Getter;
import java.util.List;

@Getter
public class DuplicateBeneficiaryException extends RuntimeException {
    private final List<String> conflictingUserNames;
    private final List<String> conflictingUserIds;

    public DuplicateBeneficiaryException(String message, List<String> conflictingUserNames, List<String> conflictingUserIds) {
        super(message);
        this.conflictingUserNames = conflictingUserNames;
        this.conflictingUserIds = conflictingUserIds;
    }
}