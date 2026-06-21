package com.aidentx.iam.contracts.dto.policy;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * DTO representing a configurable synchronization reaction.
 * Maps a synchronization situation to a lifecycle action to execute.
 * <p>
 * Follows the MidPoint synchronization reaction pattern where each
 * resource defines what should happen when a specific correlation
 * situation is encountered during import/reconciliation.
 * <p>
 * Valid situations: "unmatched", "unlinked", "linked", "disputed", "deleted"
 * Valid actions: "add-focus", "link", "sync", "unlink", "inactivate-focus",
 *               "delete-focus", "none"
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SynchronizationReactionDTO(
        /** The synchronization situation that triggers this reaction */
        String situation,
        /** The action to execute when the situation is encountered */
        String action
) {

    // ========================
    //  Situation Constants
    // ========================

    public static final String SITUATION_UNMATCHED = "unmatched";
    public static final String SITUATION_UNLINKED = "unlinked";
    public static final String SITUATION_LINKED = "linked";
    public static final String SITUATION_DISPUTED = "disputed";
    public static final String SITUATION_DELETED = "deleted";

    // ========================
    //  Action Constants
    // ========================

    /** Create a new identity (focal object) from inbound mappings and link the shadow */
    public static final String ACTION_ADD_FOCUS = "add-focus";

    /** Link the shadow to the correlated identity */
    public static final String ACTION_LINK = "link";

    /** Synchronize (update) the linked identity's attributes from the shadow */
    public static final String ACTION_SYNC = "sync";

    /** Unlink the shadow from its identity (but don't disable/delete the identity) */
    public static final String ACTION_UNLINK = "unlink";

    /** Inactivate (disable) the linked identity */
    public static final String ACTION_INACTIVATE_FOCUS = "inactivate-focus";

    /** Delete the linked identity entirely */
    public static final String ACTION_DELETE_FOCUS = "delete-focus";

    /** Do nothing — skip this situation */
    public static final String ACTION_NONE = "none";
}
