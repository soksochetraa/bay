package com.example.bay.model;

import java.io.Serializable;

public class Moderation implements Serializable {
    private String status; // "warned"
    private Long warnedAt;
    private Long expiresAt;
    private String warnedBy;
    private String warningMessage;

    public Moderation() {}

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getWarnedAt() { return warnedAt; }
    public void setWarnedAt(Long warnedAt) { this.warnedAt = warnedAt; }

    public Long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Long expiresAt) { this.expiresAt = expiresAt; }

    public String getWarnedBy() { return warnedBy; }
    public void setWarnedBy(String warnedBy) { this.warnedBy = warnedBy; }

    public String getWarningMessage() { return warningMessage; }
    public void setWarningMessage(String warningMessage) { this.warningMessage = warningMessage; }
}