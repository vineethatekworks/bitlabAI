package com.talentstream.dto;

public class InterviewResponse {
    private int questionNumber;
    private String question;
    private String analysis;
    private boolean completionStatus;
    private String overallFeedback;
    private String skill;
    private int CurrentSkillQuestionNumber;
    private String CurrentDifficulty;
    private int CurrentSkillIndex;
    // Constructors
    public InterviewResponse() {
    }

    public InterviewResponse(int questionNumber, String question, String analysis, 
                           boolean completionStatus, String overallFeedback) {
        this.questionNumber = questionNumber;
        this.question = question;
        this.analysis = analysis;
        this.completionStatus = completionStatus;
        this.overallFeedback = overallFeedback;
    }

    // Getters and Setters
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

	public String getSkill() {
		return skill;
	}

	public void setSkill(String skill) {
		this.skill = skill;
	}

	public int getCurrentSkillQuestionNumber() {
		return CurrentSkillQuestionNumber;
	}

	public void setCurrentSkillQuestionNumber(int currentSkillQuestionNumber) {
		CurrentSkillQuestionNumber = currentSkillQuestionNumber;
	}

	public String getCurrentDifficulty() {
		return CurrentDifficulty;
	}

	public void setCurrentDifficulty(String currentDifficulty) {
		CurrentDifficulty = currentDifficulty;
	}

	public int getCurrentSkillIndex() {
		return CurrentSkillIndex;
	}

	public void setCurrentSkillIndex(int currentSkillIndex) {
		CurrentSkillIndex = currentSkillIndex;
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