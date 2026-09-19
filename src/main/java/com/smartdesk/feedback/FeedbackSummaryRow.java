package com.smartdesk.feedback;

public class FeedbackSummaryRow {

    private long totalCount;
    private long helpfulCount;
    private long notHelpfulCount;

    public long getTotalCount() { return totalCount; }
    public void setTotalCount(long totalCount) { this.totalCount = totalCount; }
    public long getHelpfulCount() { return helpfulCount; }
    public void setHelpfulCount(long helpfulCount) { this.helpfulCount = helpfulCount; }
    public long getNotHelpfulCount() { return notHelpfulCount; }
    public void setNotHelpfulCount(long notHelpfulCount) { this.notHelpfulCount = notHelpfulCount; }
}
