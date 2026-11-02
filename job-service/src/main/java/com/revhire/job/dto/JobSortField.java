package com.revhire.job.dto;

public enum JobSortField {
    CREATED_AT("createdAt"),
    TITLE("title"),
    LOCATION("location");

    private final String property;

    JobSortField(String property) {
        this.property = property;
    }

    public String property() {
        return property;
    }
}
