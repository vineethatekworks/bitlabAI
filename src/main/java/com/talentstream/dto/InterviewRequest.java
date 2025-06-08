package com.talentstream.dto;

import java.util.List;
import java.util.Map;

public class InterviewRequest {
    private String applicantId;
    private List<String> skills;
    private List<Map<String, Object>> history;

    public InterviewRequest() {}

    public InterviewRequest(String applicantId, List<String> skills, List<Map<String, Object>> history) {
        this.applicantId = applicantId;
        this.skills = skills;
        this.history = history;
    }

    public String getApplicantId() {
        return applicantId;
    }

    public void setApplicantId(String applicantId) {
        this.applicantId = applicantId;
    }

    public List<String> getSkills() {
        return skills;
    }

    public void setSkills(List<String> skills) {
        this.skills = skills;
    }

    public List<Map<String, Object>> getHistory() {
        return history;
    }

    public void setHistory(List<Map<String, Object>> history) {
        this.history = history;
    }

   

    @Override
    public String toString() {
        return "InterviewRequest{" +
                "applicantId='" + applicantId + '\'' +
                ", skills=" + skills +
                ", history=" + history +
                '}';
    }
}

