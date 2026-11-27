
package com.fairgig.factory.report;

public final class Report {
    private final String title;
    private final String content;   // plain‑text for this demo

    public Report(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public String getTitle() { return title; }
    public String getContent() { return content; }

    @Override
    public String toString() {
        return "Report: " + title + "\n" + content;
    }
}