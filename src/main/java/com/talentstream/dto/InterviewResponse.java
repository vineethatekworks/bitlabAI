package com.talentstream.dto;

public class InterviewResponse {
    private String questionNumber;
    private String question;
    private String analysis;
    private boolean completionStatus;
    private String overallFeedback;

    // Constructors
    public InterviewResponse() {
    }

    public InterviewResponse(String questionNumber, String question, String analysis, 
                           boolean completionStatus, String overallFeedback) {
        this.questionNumber = questionNumber;
        this.question = question;
        this.analysis = analysis;
        this.completionStatus = completionStatus;
        this.overallFeedback = overallFeedback;
    }

    // Getters and Setters
    public String getQuestionNumber() {
        return questionNumber;
    }

    public void setQuestionNumber(String questionNumber) {
        this.questionNumber = questionNumber;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnalysis() {
        return analysis;
    }

    public void setAnalysis(String analysis) {
        this.analysis = analysis;
    }

    public boolean isCompletionStatus() {
        return completionStatus;
    }

    public void setCompletionStatus(boolean completionStatus) {
        this.completionStatus = completionStatus;
    }

    public String getOverallFeedback() {
        return overallFeedback;
    }

    public void setOverallFeedback(String overallFeedback) {
        this.overallFeedback = overallFeedback;
    }

    // toString() method for debugging/logging
    @Override
    public String toString() {
        return "InterviewResponse{" +
                "questionNumber='" + questionNumber + '\'' +
                ", question='" + question + '\'' +
                ", analysis='" + analysis + '\'' +
                ", completionStatus=" + completionStatus +
                ", overallFeedback='" + overallFeedback + '\'' +
                '}';
    }
}