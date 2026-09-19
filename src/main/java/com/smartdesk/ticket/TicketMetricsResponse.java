package com.smartdesk.ticket;

public record TicketMetricsResponse(
        long totalCount,
        long openCount,
        long inProgressCount,
        long resolvedCount,
        long closedCount,
        long urgentOpenCount,
        double averageResolutionHours
) {

    public static TicketMetricsResponse from(TicketMetricsRow row) {
        return new TicketMetricsResponse(
                row.getTotalCount(),
                row.getOpenCount(),
                row.getInProgressCount(),
                row.getResolvedCount(),
                row.getClosedCount(),
                row.getUrgentOpenCount(),
                round(row.getAverageResolutionHours())
        );
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
