package com.smartdesk.agent;

public class AgentMetricsRow {

    private long totalRuns;
    private long completedRuns;
    private long failedRuns;
    private long totalToolCalls;
    private long successfulToolCalls;
    private double averageToolDurationMs;

    public long getTotalRuns() { return totalRuns; }
    public void setTotalRuns(long totalRuns) { this.totalRuns = totalRuns; }
    public long getCompletedRuns() { return completedRuns; }
    public void setCompletedRuns(long completedRuns) { this.completedRuns = completedRuns; }
    public long getFailedRuns() { return failedRuns; }
    public void setFailedRuns(long failedRuns) { this.failedRuns = failedRuns; }
    public long getTotalToolCalls() { return totalToolCalls; }
    public void setTotalToolCalls(long totalToolCalls) { this.totalToolCalls = totalToolCalls; }
    public long getSuccessfulToolCalls() { return successfulToolCalls; }
    public void setSuccessfulToolCalls(long successfulToolCalls) { this.successfulToolCalls = successfulToolCalls; }
    public double getAverageToolDurationMs() { return averageToolDurationMs; }
    public void setAverageToolDurationMs(double averageToolDurationMs) { this.averageToolDurationMs = averageToolDurationMs; }
}
