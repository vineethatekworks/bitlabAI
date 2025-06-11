package com.talentstream.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.talentstream.dto.InterviewRequest;
import com.talentstream.dto.InterviewResponse;

@Service
public class InterviewService {

	private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=";
	private static final Set<String> VALID_DIFFICULTIES = Set.of("easy", "medium", "hard");
	private static final String DEFAULT_DIFFICULTY = "easy";
	private static final int MAX_QUESTIONS_PER_SKILL = 4;
	private static final String DEFAULT_FEEDBACK = "No feedback provided";

	@Value("${gemini.api.key}")
	private String apiKey;

	private final HttpClient httpClient = HttpClient.newHttpClient();
	private final Gson gson = new GsonBuilder().create();

	public InterviewResponse generateNextQuestion(InterviewRequest request) {

		List<Map<String, Object>> history = request.getHistory();
		List<String> skills = request.getSkills();
		int currentSkillQuestionNumber = 1;

		if (skills == null || skills.isEmpty()) {
			throw new IllegalArgumentException("At least one skill is required");
		}

		if (history == null || history.isEmpty()) {
			return firstQuestion(skills.get(0), 0, history);
		}

		Map<String, Object> lastEntry = getHistoryEntry(history, -2);
		validateLastEntry(lastEntry);

		int globalQuestionNumber = ((Number) lastEntry.get("questionNumber")).intValue() + 1;
		String question = (String) lastEntry.get("question");
		String difficulty = (String) lastEntry.get("currentDifficulty");
		int currentSkillIndex = ((Number) lastEntry.get("currentSkillIndex")).intValue();
		String currentSkill = skills.get(currentSkillIndex);
		currentSkillQuestionNumber = ((Number) lastEntry.get("currentSkillQuestionNumber")).intValue() + 1;

		String evalPrompt = generatePromptForEvaluation(currentSkill, difficulty, history, question,
				currentSkillQuestionNumber);
		System.out.println("evalPrompt:" + evalPrompt);

		List<String> responseLines = callGemini(evalPrompt);
		System.out.println("responseLines:" + responseLines);

		JsonObject responseJson = parseAIResponse(responseLines);
		String action = responseJson.get("action").getAsString().toLowerCase();
		String feedback = responseJson.has("feedback") ? responseJson.get("feedback").getAsString() : DEFAULT_FEEDBACK;
		int score = responseJson.get("score").getAsInt();

		if (currentSkillQuestionNumber == MAX_QUESTIONS_PER_SKILL) {
			return handleNextSkill(history, skills, currentSkillIndex, globalQuestionNumber, feedback,
					currentSkillQuestionNumber, score);
		}

		switch (action) {
		case "next_skill":
			return handleNextSkill(history, skills, currentSkillIndex, globalQuestionNumber, feedback,
					currentSkillQuestionNumber, score);
		case "simpler_question":
			return buildQuestionResponse(history, currentSkill, globalQuestionNumber, currentSkillQuestionNumber,
					downgradeDifficulty(difficulty), feedback, currentSkillIndex, score);
		case "next_question":
			return buildQuestionResponse(history, currentSkill, globalQuestionNumber, currentSkillQuestionNumber,
					upgradeDifficulty(difficulty), feedback, currentSkillIndex, score);
		case "end":
			return completeInterview(history, globalQuestionNumber, feedback,score);
		default:
			return buildQuestionResponse(history, currentSkill, globalQuestionNumber, currentSkillQuestionNumber,
					difficulty, feedback, currentSkillIndex, score);
		}

	}

	private InterviewResponse firstQuestion(String skill, int skillIndex, List<Map<String, Object>> history) {
		String prompt = generateQuestion("easy", skill, history);
		JsonObject responseJson = parseAIResponse(callGemini(prompt));
		return buildInterviewResponse(responseJson.get("question").getAsString(), skill, 1, 1, "easy", skillIndex, null,
				false, 0);
	}

	private InterviewResponse handleNextSkill(List<Map<String, Object>> history, List<String> skills,
			int currentSkillIndex, int globalQuestionNumber, String feedback, int currentSkillQuestionNumber,
			int score) {
		int nextSkillIndex = currentSkillIndex + 1;
		currentSkillQuestionNumber = 1;
		if (nextSkillIndex >= skills.size()) {
			return completeInterview(history, globalQuestionNumber, feedback,score);
		}
		String nextSkill = skills.get(nextSkillIndex);
		return buildQuestionResponse(history, nextSkill, globalQuestionNumber, currentSkillQuestionNumber, "easy",
				feedback, nextSkillIndex, score);
	}

	private InterviewResponse buildQuestionResponse(List<Map<String, Object>> history, String skill,
			int globalQuestionNumber, int skillQuestionNumber, String difficulty, String feedback, int skillIndex,
			int score) {
		String prompt = generateQuestion(difficulty, skill, history);
		JsonObject responseJson = parseAIResponse(callGemini(prompt));
		return buildInterviewResponse(responseJson.get("question").getAsString(), skill, globalQuestionNumber,
				skillQuestionNumber, difficulty, skillIndex, feedback, false, score);
	}

	private InterviewResponse completeInterview(List<Map<String, Object>> history, int questionNumber, String feedback, int lastScore) {
	    List<String> analyses = new ArrayList<>();
	    analyses.add(feedback);
	    
	    double totalScore = lastScore; 
	    int scoreCount = 1;         

	    for (Map<String, Object> entry : history) {
	        Object analysisObj = entry.get("analysis");
	        if (analysisObj != null) {
	            analyses.add(analysisObj.toString());
	        }

	        if (entry.containsKey("score")) {
	            int score = ((Number) entry.get("score")).intValue();
	            totalScore += score;
	            scoreCount++;
	        }
	    }

	    int averageScore = (int) Math.round(totalScore / scoreCount);
	    
	    System.out.println("Calculated average score (including last answer): " + averageScore);
	    System.out.println("Analyses: " + analyses);

	    // Generate feedback
	    String overallFeedbackPrompt = generateOverAllFeedback(analyses);
	    JsonObject responseJson = parseAIResponse(callGemini(overallFeedbackPrompt));
	    String overallFeedback = responseJson.get("OverallFeedback").getAsString();

	    return new InterviewResponse(
	            0, null, null, true, 
	            overallFeedback, null, 0, 0, null,
	            averageScore);
	}

	private InterviewResponse buildInterviewResponse(String question, String skill, int questionNumber,
			int skillQuestionNumber, String difficulty, int skillIndex, String feedback, boolean isComplete,
			int score) {
		InterviewResponse response = new InterviewResponse();
		response.setQuestion(question);
		response.setQuestionNumber(questionNumber);
		response.setCurrentSkillQuestionNumber(skillQuestionNumber);
		response.setSkill(skill);
		response.setCurrentDifficulty(difficulty);
		response.setCurrentSkillIndex(skillIndex);
		response.setAnalysis(feedback);
		response.setScore(score);
		response.setCompletionStatus(isComplete);
		System.out.println("response:" + response);
		return response;
	}

	private String generatePromptForEvaluation(String skill, String difficulty, List<Map<String, Object>> history,
			String question, int currentSkillQuestionNumber) {
		difficulty = validateDifficulty(difficulty);
		Map<String, Object> lastEntry = (Map<String, Object>) history.get(history.size() - 1);
		String currentAnswer = (String) lastEntry.get("currentAnswer");

		return "You're an expert technical interviewer evaluating a fresher's answer to a " + difficulty
				+ " level question on the topic of " + skill + " for this skill.\n\n"
				+ "Candidate's most recent answer: \"" + currentAnswer + " For the question :" + question + ")\n\n"
				+ "Evaluate this response with empathy and technical insight give feedback.\n" + "Evaluation Rules:\n"
				+ "DO NOT make comments like 'Let's move on' or 'Try another topic'. Focus only on this answer.\n\n"
				+ "Evaluation Rules:\n"
				+ "Based on the answer's quality, clarity, and relevance, determine the appropriate next action:\n\n"
				+ "- If the answer is reasonably good: continue at the same level (action: next_question)\n"
				+ "- If the answer lacks clarity or has mistakes: simplify and ask an easier question (action: simpler_question)\n"
				+ "- If the answer is irrelevant , very poor, off-topic, or the candidate says \"I don't know\":\n"
				+ "  - If difficulty is 'hard' or 'medium': ask a simpler question (action: simpler_question)\n"
				+ "  - If difficulty is 'easy': move to the next skill (action: next_skill)\n\n"
				+ "Provide constructive feedback in **exactly two lines** (strictly).\n"
				+ "Do NOT include the action in the feedback.\n\n"
				+ "Respond with ONLY raw JSON in this exact format:\n" + "{\n"
				+ "  \"action\": \"next_question|simpler_question|next_skill|end\",\n"
				+ "  \"feedback\": \"Give only two-line feedback strictly about the candidate's answer. DO NOT mention what action will be taken. Stricly suggest what the candidate can improve or learn.\"\n"
				+ "  \"score\":  \"include score out of 10 according to the feedback\"\n" + "}\n";
	}

	private String generateQuestion(String difficulty, String skill, List<Map<String, Object>> history) {
		return "You are an adaptive technical interviewer. Please respond with ONLY the JSON output, no additional text or code blocks.\n\n"
				+ "Given:\n" + "- skill: " + skill + "\n" + "- history: " + history + "\n" + "- difficulty: "
				+ difficulty + "\n\n" + "Process:\n" + "Generate a new " + difficulty
				+ " level **conceptual** or **theoretical** question based on the fundamental principles of " + skill
				+ ".\n" + "This should simulate a real-time interview experience"
				+ "Avoid any repetition — DO NOT repeat or rephrase any question from the history.\n"
				+ "Avoid code-related or implementation-specific questions; focus on explaining 'how' and 'why' things work.\n"
				+ "Strictly return a JSON object in this format:\n" + "{\n"
				+ "  \"question\": \"<your generated question>\",\n" + "  \"skill\": \"<current skill>\"\n" + "}";
	}

	private String generateOverAllFeedback(List<String> analyses) {
		System.out.println("analyses :" + analyses);
		return "You're an expert technical interviewer. Based on these individual feedback points i.e:" + analyses
				+ "	\n\\nRead all feedbacks Generate a concise overall feedback (3-4 sentences) summarizing the candidate's performance remember he is a fresher \" +\r\n"
				+ "	\"across all technical skills assessed. Highlight strengths and areas for improvement. \" +\r\n"
				+ "	\"Provide only the feedback text without any additional formatting or headings. \" +\r\n"
				+ "	\"For example: 'Candidate shows strong Java skills but needs improvement in JavaScript. Overall, a solid foundation.'\";"
				+ "Strictly return a JSON object in this format:\n" + "{\n"
				+ " \"OverallFeedback\":\"<your generated Overallfeedback\"";
		}

	private JsonObject parseAIResponse(List<String> response) {
		System.out.println("response :" + response);
		String combined = String.join(" ", response).trim();
		String cleaned = combined.replaceAll("```json", "").replaceAll("```", "").trim();
		int start = cleaned.indexOf("{");
		int end = cleaned.lastIndexOf("}");
		if (start == -1 || end == -1 || end <= start) {
			throw new RuntimeException("Invalid JSON content in response");
		}
		String jsonPart = cleaned.substring(start, end + 1).trim();
		System.out.println("jsonpart:" + jsonPart);
		return JsonParser.parseString(jsonPart).getAsJsonObject();
	}

	private List<String> callGemini(String prompt) {
		try {
			Map<String, Object> content = Map.of("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))));
			HttpRequest request = HttpRequest.newBuilder().uri(URI.create(GEMINI_URL + apiKey))
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(gson.toJson(content))).build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() != 200) {
				throw new RuntimeException("Gemini API returned status: " + response.statusCode());
			}

			JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
			if (json.has("error")) {
				throw new RuntimeException("Gemini API error: " + json.get("error").toString());
			}

			JsonArray candidates = json.getAsJsonArray("candidates");
			if (candidates == null || candidates.isEmpty()) {
				throw new RuntimeException("No candidates in Gemini response");
			}

			JsonObject cont = candidates.get(0).getAsJsonObject().getAsJsonObject("content");
			JsonArray parts = cont.getAsJsonArray("parts");
			if (parts == null || parts.isEmpty()) {
				throw new RuntimeException("No parts in Gemini response content");
			}

			String text = parts.get(0).getAsJsonObject().get("text").getAsString();

			return Arrays.stream(text.split("\n")).map(String::trim).filter(line -> !line.isBlank())
					.collect(Collectors.toList());

		} catch (Exception e) {
			throw new RuntimeException("Error calling Gemini API: " + e.getMessage());
		}
	}

	private String upgradeDifficulty(String difficulty) {
		difficulty = validateDifficulty(difficulty);
		switch (difficulty) {
		case "easy":
			return "medium";
		case "medium":
			return "hard";
		default:
			return "hard";
		}
	}

	private String downgradeDifficulty(String difficulty) {
		difficulty = validateDifficulty(difficulty);
		switch (difficulty) {
		case "hard":
			return "medium";
		case "medium":
			return "easy";
		default:
			return "easy";
		}
	}

	private String validateDifficulty(String difficulty) {
		return VALID_DIFFICULTIES.contains(difficulty.toLowerCase()) ? difficulty.toLowerCase() : DEFAULT_DIFFICULTY;
	}

	private Map<String, Object> getHistoryEntry(List<Map<String, Object>> history, int fromLast) {
		int index = history.size() + fromLast;
		if (index < 0 || index >= history.size()) {
			throw new IndexOutOfBoundsException("Invalid history index");
		}
		return history.get(index);
	}

	private void validateLastEntry(Map<String, Object> entry) {
		if (entry == null || entry.size() < 2) {
			throw new IllegalStateException("Missing or incomplete last question entry");
		}
	}

}