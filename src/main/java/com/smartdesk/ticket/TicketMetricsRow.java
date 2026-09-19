package com.smartdesk.ticket;

public class TicketMetricsRow {

    private long totalCount;
    private long openCount;
    private long inProgressCount;
    private long resolvedCount;
    private long closedCount;
    private long urgentOpenCount;
    private double averageResolutionHours;

    public long getTotalCount() { return totalCount; }
    public void setTotalCount(long totalCount) { this.totalCount = totalCount; }
    public long getOpenCount() { return openCount; }
    public void setOpenCount(long openCount) { this.openCount = openCount; }
    public long getInProgressCount() { return inProgressCount; }
    public void setInProgressCount(long inProgressCount) { this.inProgressCount = inProgressCount; }
    public long getResolvedCount() { return resolvedCount; }
    public void setResolvedCount(long resolvedCount) { this.resolvedCount = resolvedCount; }
    public long getClosedCount() { return closedCount; }
    public void setClosedCount(long closedCount) { this.closedCount = closedCount; }
    public long getUrgentOpenCount() { return urgentOpenCount; }
    public void setUrgentOpenCount(long urgentOpenCount) { this.urgentOpenCount = urgentOpenCount; }
    public double getAverageResolutionHours() { return averageResolutionHours; }
    public void setAverageResolutionHours(double averageResolutionHours) { this.averageResolutionHours = averageResolutionHours; }
}
