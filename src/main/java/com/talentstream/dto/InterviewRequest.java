package com.talentstream.dto;

import java.util.List;

public class InterviewRequest {
    private String applicantId;
    private List<String> skills;
    private List<Object> history;
    private String currentAnswer;

    public InterviewRequest() {}

    public InterviewRequest(String applicantId, List<String> skills, List<Object> history, String currentAnswer) {
        this.applicantId = applicantId;
        this.skills = skills;
        this.history = history;
        this.currentAnswer = currentAnswer;
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

    public List<Object> getHistory() {
        return history;
    }

    public void setHistory(List<Object> history) {
        this.history = history;
    }

    public String getCurrentAnswer() {
        return currentAnswer;
    }

    public void setCurrentAnswer(String currentAnswer) {
        this.currentAnswer = currentAnswer;
    }

    @Override
    public String toString() {
        return "InterviewRequest{" +
                "applicantId='" + applicantId + '\'' +
                ", skills=" + skills +
                ", history=" + history +
                ", currentAnswer='" + currentAnswer + '\'' +
                '}';
    }
}

