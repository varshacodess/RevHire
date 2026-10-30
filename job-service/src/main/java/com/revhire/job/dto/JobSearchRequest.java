package com.revhire.job.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class JobSearchRequest {
    private String keywords;
    private String title;
    private String location;
    private String skill;
    private String employmentType;
    @Min(0)
    private Integer minExperienceYears;
    @Min(0)
    private Integer maxExperienceYears;
    @Min(0)
    private int page = 0;
    @Min(1)
    @Max(100)
    private int size = 20;
    private JobSortField sortBy = JobSortField.CREATED_AT;
    private JobSortDirection direction = JobSortDirection.DESC;

    public enum JobSortDirection {
        ASC, DESC
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getSkill() {
        return skill;
    }

    public void setSkill(String skill) {
        this.skill = skill;
    }

    public String getEmploymentType() {
        return employmentType;
    }

    public void setEmploymentType(String employmentType) {
        this.employmentType = employmentType;
    }

    public Integer getMinExperienceYears() {
        return minExperienceYears;
    }

    public void setMinExperienceYears(Integer minExperienceYears) {
        this.minExperienceYears = minExperienceYears;
    }

    public Integer getMaxExperienceYears() {
        return maxExperienceYears;
    }

    public void setMaxExperienceYears(Integer maxExperienceYears) {
        this.maxExperienceYears = maxExperienceYears;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public JobSortField getSortBy() {
        return sortBy;
    }

    public void setSortBy(JobSortField sortBy) {
        this.sortBy = sortBy;
    }

    public JobSortDirection getDirection() {
        return direction;
    }

    public void setDirection(JobSortDirection direction) {
        this.direction = direction;
    }
}
