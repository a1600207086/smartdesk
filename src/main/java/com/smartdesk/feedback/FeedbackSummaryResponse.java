package com.smartdesk.feedback;

public record FeedbackSummaryResponse(
        long totalCount,
        long helpfulCount,
        long notHelpfulCount,
        double helpfulRate
) {
    public static FeedbackSummaryResponse from(FeedbackSummaryRow row) {
        double rate = row.getTotalCount() == 0
                ? 0.0
                : (double) row.getHelpfulCount() / row.getTotalCount();
        return new FeedbackSummaryResponse(
                row.getTotalCount(),
                row.getHelpfulCount(),
                row.getNotHelpfulCount(),
                Math.round(rate * 10_000.0) / 10_000.0
        );
    }
}
