package com.talentstream.dto;


public class QuestionHistory {
    private int questionNumber;
    private String question;
    private boolean completed;
    private String feedback;

    public QuestionHistory() {}

    public QuestionHistory(int questionNumber, String question, boolean completed, String feedback) {
        this.questionNumber = questionNumber;
        this.question = question;
        this.completed = completed;
        this.feedback = feedback;
    }

    public int getQuestionNumber() {
        return questionNumber;
    }

    public void setQuestionNumber(int questionNumber) {
        this.questionNumber = questionNumber;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    @Override
    public String toString() {
        return "QuestionHistory{" +
                "questionNumber=" + questionNumber +
                ", question='" + question + '\'' +
                ", completed=" + completed +
                ", feedback='" + feedback + '\'' +
                '}';
    }
}
