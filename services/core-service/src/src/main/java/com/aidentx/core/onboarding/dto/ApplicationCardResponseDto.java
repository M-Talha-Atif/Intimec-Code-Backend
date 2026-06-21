package com.aidentx.core.onboarding.dto;

public class ApplicationCardResponseDto {

    private String iconPath;
    private String id;
    private String name;
    private String category;
    private int users;
    private boolean orphan;
    private int orphanCount;
    private boolean risky;
    private int riskyCount;
    private String source;
    private int flows;
    private String status;
    private long lastSyncAccounts;
    private long lastSyncEntitlements;
    private long onboardedDate;
    private String aiStatus;

    // Getters and Setters for all fields...

    public String geticonPath() { return iconPath; }
    public void seticonPath(String iconPath) { this.iconPath = iconPath; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public int getUsers() { return users; }
    public void setUsers(int users) { this.users = users; }
    public boolean isOrphan() { return orphan; }
    public void setOrphan(boolean orphan) { this.orphan = orphan; }
    public int getOrphanCount() { return orphanCount; }
    public void setOrphanCount(int orphanCount) { this.orphanCount = orphanCount; }
    public boolean isRisky() { return risky; }
    public void setRisky(boolean risky) { this.risky = risky; }
    public int getRiskyCount() { return riskyCount; }
    public void setRiskyCount(int riskyCount) { this.riskyCount = riskyCount; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public int getFlows() { return flows; }
    public void setFlows(int flows) { this.flows = flows; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public long getLastSyncAccounts() { return lastSyncAccounts; }
    public void setLastSyncAccounts(long lastSyncAccounts) { this.lastSyncAccounts = lastSyncAccounts; }
    public long getLastSyncEntitlements() { return lastSyncEntitlements; }
    public void setLastSyncEntitlements(long lastSyncEntitlements) { this.lastSyncEntitlements = lastSyncEntitlements; }
    public long getOnboardedDate() { return onboardedDate; }
    public void setOnboardedDate(long onboardedDate) { this.onboardedDate = onboardedDate; }
    public String getAiStatus() { return aiStatus; }
    public void setAiStatus(String aiStatus) { this.aiStatus = aiStatus; }
}