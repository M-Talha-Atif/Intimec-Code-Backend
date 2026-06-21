package com.aidentx.iam.entitlement_management.service;

import java.util.*;

/**
 * Builds the immutable compliance evidence for a single identity evaluation.
 *
 * <p>Captures the actual field values from the identity and the per-group,
 * per-condition evaluation results. This evidence is stored as JSONB in the
 * execution detail record — an immutable receipt proving why the decision
 * was made even if rule conditions or identity data change later.
 *
 * <p>Output structure:
 * <pre>
 * {
 *   "identityFields": {
 *     "department": "Engineering",
 *     "jobTitle": "Senior Software Engineer",
 *     "location": "NYC"
 *   },
 *   "groupsEvaluated": [
 *     {
 *       "groupIndex": 0,
 *       "groupOperator": "AND",
 *       "negated": false,
 *       "groupMatched": true,
 *       "conditions": [
 *         {
 *           "field": "department",
 *           "operator": "EQUALS",
 *           "expectedValue": "Engineering",
 *           "actualValue": "Engineering",
 *           "matched": true
 *         }
 *       ]
 *     }
 *   ],
 *   "overallMatch": true,
 *   "shadowFound": true,
 *   "shadowId": "22222222-..."
 * }
 * </pre>
 */
public class MatchEvidenceBuilder {

    private final Map<String, Object> identityFields = new LinkedHashMap<>();
    private final List<Map<String, Object>> groupsEvaluated = new ArrayList<>();
    private boolean overallMatch;
    private boolean shadowFound;
    private String shadowId;

    /**
     * Adds an identity field value to the evidence.
     *
     * @param field the field name
     * @param value the actual value from the identity
     */
    public void addField(String field, Object value) {
        identityFields.put(field, value);
    }

    /**
     * Sets whether the identity matched the overall rule (all groups combined).
     *
     * @param matched true if the identity matched
     */
    public void overallMatch(boolean matched) {
        this.overallMatch = matched;
    }

    /**
     * Sets shadow resolution result.
     *
     * @param found    whether a shadow was found
     * @param shadowId the shadow UUID if found, null otherwise
     */
    public void shadowFound(boolean found, UUID shadowId) {
        this.shadowFound = found;
        this.shadowId = shadowId != null ? shadowId.toString() : null;
    }

       /**
     * Creates a new group evidence builder.
     */
    public GroupEvidenceBuilder startGroup(int groupIndex, String groupOperator, boolean negated) {
        return new GroupEvidenceBuilder(groupIndex, groupOperator, negated);
    }

    /**
     * Adds a completed group evidence to the groups list.
     */
    public void addGroupResult(GroupEvidenceBuilder groupBuilder) {
        groupsEvaluated.add(groupBuilder.build());
    }

    /**
     * Builds the final evidence map for JSONB storage.
     *
     * @return the complete evidence map
     */
    public Map<String, Object> build() {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("identityFields", new LinkedHashMap<>(identityFields));
        evidence.put("groupsEvaluated", new ArrayList<>(groupsEvaluated));
        evidence.put("overallMatch", overallMatch);
        evidence.put("shadowFound", shadowFound);
        if (shadowId != null) {
            evidence.put("shadowId", shadowId);
        }
        return evidence;
    }

    // ===================================================================
    //  Group Evidence Builder
    // ===================================================================

  /**
     * Builder for a single group's evidence, supporting nested sub-groups.
     */
    public static class GroupEvidenceBuilder {
        private final int groupIndex;
        private final String groupOperator;
        private final boolean negated;
        private final List<Map<String, Object>> conditionResults = new ArrayList<>();
        private final List<Map<String, Object>> subGroupResults = new ArrayList<>();
        private boolean groupMatched;

        public GroupEvidenceBuilder(int groupIndex, String groupOperator, boolean negated) {
            this.groupIndex = groupIndex;
            this.groupOperator = groupOperator;
            this.negated = negated;
        }

        /**
         * Adds a leaf condition evaluation result.
         */
        public void addConditionResult(String field, String operator,
                                        Object expectedValue, Object actualValue, boolean matched) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("field", field);
            result.put("operator", operator);
            result.put("expectedValue", expectedValue);
            result.put("actualValue", actualValue);
            result.put("matched", matched);
            conditionResults.add(result);
        }

        /**
         * Adds a fully-built nested sub-group evidence map.
         */
        public void addSubGroupResult(Map<String, Object> subGroupEvidence) {
            subGroupResults.add(subGroupEvidence);
        }

        public void setGroupMatched(boolean matched) {
            this.groupMatched = matched;
        }

        /**
         * Builds the evidence map for this group, including nested sub-groups.
         */
        public Map<String, Object> build() {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("groupIndex", groupIndex);
            result.put("groupOperator", groupOperator);
            result.put("negated", negated);
            result.put("conditions", conditionResults);
            if (!subGroupResults.isEmpty()) {
                result.put("conditionGroups", subGroupResults);
            }
            result.put("groupMatched", groupMatched);
            return result;
        }
    }


}