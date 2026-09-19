package com.smartdesk.agent;

public record AgentMetricsResponse(
        long totalRuns,
        long completedRuns,
        long failedRuns,
        double completionRate,
        long totalToolCalls,
        long successfulToolCalls,
        double toolSuccessRate,
        double averageToolDurationMs
) {

    public static AgentMetricsResponse from(AgentMetricsRow row) {
        double completionRate = row.getTotalRuns() == 0 ? 0.0
                : (double) row.getCompletedRuns() / row.getTotalRuns();
        double toolSuccessRate = row.getTotalToolCalls() == 0 ? 0.0
                : (double) row.getSuccessfulToolCalls() / row.getTotalToolCalls();
        return new AgentMetricsResponse(
                row.getTotalRuns(), row.getCompletedRuns(), row.getFailedRuns(), round(completionRate),
                row.getTotalToolCalls(), row.getSuccessfulToolCalls(), round(toolSuccessRate),
                round(row.getAverageToolDurationMs())
        );
    }

    private static double round(double value) {
        return Math.round(value * 10_000.0) / 10_000.0;
    }
}
