package com.aidentx.iam.identity_management.service;

import com.aidentx.iam.identity_management.model.enums.IdentityStatus;

import java.util.Map;
import java.util.Set;

/**
 * Identity Lifecycle State Machine — defines and enforces valid status transitions.
 * <p>
 * In IGA, identity lifecycle governance prevents invalid state changes that
 * could create compliance violations or security gaps. This state machine
 * encodes enterprise identity lifecycle rules:
 * <pre>
 *   PENDING ──► ACTIVE ──► SUSPENDED ──► ACTIVE (reinstatement)
 *                  │                        │
 *                  ▼                        ▼
 *              INACTIVE ──► ACTIVE     TERMINATED ──► ARCHIVED
 *                  │
 *                  ▼
 *              TERMINATED ──► ARCHIVED
 *              EXPIRED ──► ARCHIVED
 * </pre>
 * <p>
 * Key governance rules enforced:
 * <ul>
 *   <li>ARCHIVED is a terminal state — no transitions out</li>
 *   <li>TERMINATED requires an explicit path — no direct ACTIVE → ARCHIVED skip</li>
 *   <li>Suspended accounts can be reactivated (security hold → clearance)</li>
 *   <li>Expired identities cannot be reactivated — they must go through re-onboarding</li>
 * </ul>
 */
public final class IdentityLifecycleStateMachine {

    private IdentityLifecycleStateMachine() {
        // Utility class — no instantiation
    }

    /**
     * Defines valid transitions for each status.
     * Key = current status, Value = set of valid target statuses.
     */
    private static final Map<IdentityStatus, Set<IdentityStatus>> VALID_TRANSITIONS = Map.of(
            IdentityStatus.PENDING, Set.of(
                    IdentityStatus.ACTIVE,
                    IdentityStatus.TERMINATED  // Onboarding rejected before activation
            ),
            IdentityStatus.ACTIVE, Set.of(
                    IdentityStatus.INACTIVE,
                    IdentityStatus.SUSPENDED,
                    IdentityStatus.TERMINATED,
                    IdentityStatus.EXPIRED
            ),
            IdentityStatus.INACTIVE, Set.of(
                    IdentityStatus.ACTIVE,      // Reactivation
                    IdentityStatus.TERMINATED,
                    IdentityStatus.EXPIRED
            ),
            IdentityStatus.SUSPENDED, Set.of(
                    IdentityStatus.ACTIVE,      // Reinstatement after security review
                    IdentityStatus.TERMINATED
            ),
            IdentityStatus.TERMINATED, Set.of(
                    IdentityStatus.ARCHIVED
            ),
            IdentityStatus.EXPIRED, Set.of(
                    IdentityStatus.ARCHIVED
            ),
            IdentityStatus.ARCHIVED, Set.of(
                    // Terminal — no transitions out
            )
    );

    /**
     * Checks whether a status transition is valid according to lifecycle rules.
     *
     * @param from Current identity status
     * @param to   Desired target status
     * @return true if the transition is allowed
     */
    public static boolean isValidTransition(IdentityStatus from, IdentityStatus to) {
        if (from == to) {
            return true; // No-op transitions are always valid
        }
        Set<IdentityStatus> allowed = VALID_TRANSITIONS.get(from);
        return allowed != null && allowed.contains(to);
    }

    /**
     * Returns the set of valid target statuses from a given status.
     *
     * @param from Current identity status
     * @return Set of statuses that can be transitioned to, never null
     */
    public static Set<IdentityStatus> getValidTargets(IdentityStatus from) {
        return VALID_TRANSITIONS.getOrDefault(from, Set.of());
    }

    /**
     * Validates a transition and throws if invalid.
     *
     * @param from Current identity status
     * @param to   Desired target status
     * @throws InvalidStatusTransitionException if the transition violates lifecycle rules
     */
    public static void validateTransition(IdentityStatus from, IdentityStatus to) {
        if (!isValidTransition(from, to)) {
            throw new InvalidStatusTransitionException(from, to);
        }
    }

    /**
     * Exception thrown when an invalid lifecycle transition is attempted.
     */
    public static class InvalidStatusTransitionException extends RuntimeException {
        private final IdentityStatus from;
        private final IdentityStatus to;

        public InvalidStatusTransitionException(IdentityStatus from, IdentityStatus to) {
            super(String.format(
                    "Invalid identity status transition: %s → %s. Valid targets from %s: %s",
                    from.name(), to.name(), from.name(),
                    VALID_TRANSITIONS.getOrDefault(from, Set.of())));
            this.from = from;
            this.to = to;
        }

        public IdentityStatus getFrom() {
            return from;
        }

        public IdentityStatus getTo() {
            return to;
        }
    }
}
