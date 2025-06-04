package com.talentstream.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
import com.google.gson.JsonSyntaxException;
import com.talentstream.dto.InterviewRequest;
import com.talentstream.dto.InterviewResponse;

@Service
public class InterviewService2 {

	private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=";
	private static final Set<String> VALID_DIFFICULTIES = Set.of("easy", "medium", "hard");
	private static final String DEFAULT_DIFFICULTY = "easy";

	@Value("${gemini.api.key}")
	private String apiKey;

	private final HttpClient httpClient = HttpClient.newHttpClient();
	private final Gson gson = new GsonBuilder().create();

	public InterviewResponse generateNextQuestion(InterviewRequest request) {
		List<Object> history = request.getHistory();
		List<String> skills = request.getSkills();

		String currentSkill = null;
		int globalQuestionNumber = 0;
		String difficulty = null;
		int currentSkillQuestionNumber = 0;
		int currentSkillIndex = 0;

		if (skills == null || skills.isEmpty()) {
			throw new RuntimeException("At least one skill is required");
		}

		if (history == null || history.isEmpty()) {
			currentSkill = skills.get(currentSkillIndex);
			globalQuestionNumber = 1;
			currentSkillQuestionNumber = 1;
			difficulty = "easy";

			String prompt = generateQuestion(difficulty, currentSkill, history);
			List<String> aiResponse = callGemini(prompt);
			JsonObject responseJson = parseAIResponse(aiResponse);

			InterviewResponse response = new InterviewResponse();
			response.setQuestionNumber(globalQuestionNumber);
			response.setCurrentSkillQuestionNumber(currentSkillQuestionNumber);
			response.setQuestion(responseJson.get("question").getAsString());
			response.setSkill(currentSkill);
			response.setCurrentDifficulty(difficulty);
			response.setCurrentSkillIndex(currentSkillIndex);
			return response;
		} else {
			Map<String, Object> lastEntry = (Map<String, Object>) history.get(history.size() - 2);
			System.out.println("lastEntry"+lastEntry);
			currentSkill = (String) lastEntry.get("skill");
			System.out.println("currentSkill" + currentSkill);
			
			globalQuestionNumber = ((Number) lastEntry.get("questionNumber")).intValue() + 1;
			System.out.println("globalQuestionNumber"+globalQuestionNumber);
			
			currentSkillQuestionNumber = ((Number) lastEntry.get("currentSkillQuestionNumber")).intValue() + 1;
			System.out.println("currentSkillQuestionNumber"+currentSkillQuestionNumber);
			
			difficulty = (String) lastEntry.get("currentDifficulty");
			System.out.println("CurrentDifficulty"+difficulty);
			
			currentSkillIndex = ((Number) lastEntry.get("currentSkillIndex")).intValue();
			System.out.println("currentSkillIndex"+currentSkillIndex);

			String evalPrompt = generatePromptForEvaluation(currentSkill, difficulty, history);
			System.out.println("evalPrompt: " + evalPrompt);
			try {
				List<String> responseLines = callGemini(evalPrompt);
				System.out.println("responseLines: " + responseLines);

				JsonObject responseJson = parseAIResponse(responseLines);
				String action = responseJson.get("action").getAsString().toLowerCase();
				String feedback = responseJson.has("feedback") ? responseJson.get("feedback").getAsString()
						: "No feedback provided";

				System.out.println("Gemini response - action: " + action + ", feedback: " + feedback);

				switch (action) {
				case "next_skill":
					return handleNextSkill(history, skills, currentSkillIndex, globalQuestionNumber, feedback);
				case "simpler_question":
					return handleSimplerQuestion(history, currentSkill, globalQuestionNumber, feedback, difficulty,
							currentSkillIndex);
				case "next_question":
					return handleNextQuestion(history, currentSkill, globalQuestionNumber, feedback, difficulty,
							currentSkillIndex);
				case "end":
					return completeInterview(history, globalQuestionNumber, feedback);
				default:
					return handleDefaultAction(history, currentSkill, globalQuestionNumber, feedback, currentSkillIndex,
							difficulty);
				}
			} catch (JsonSyntaxException e) {
				throw new RuntimeException("Failed to parse evaluation response");
			} catch (Exception e) {
				throw new RuntimeException("Evaluation failed: " + e.getMessage());
			}
		}
	}

	private InterviewResponse handleNextSkill(List<Object> history, List<String> skills, int currentSkillIndex,
			int questionNumber, String feedback) {
		int nextSkillIndex = currentSkillIndex + 1;
		if (nextSkillIndex >= skills.size()) {
			return completeInterview(history, questionNumber, feedback);
		}

		String nextSkill = skills.get(nextSkillIndex);
		String prompt = generateQuestion("easy", nextSkill, history);
		List<String> aiResponse = callGemini(prompt);
		JsonObject responseJson = parseAIResponse(aiResponse);

		InterviewResponse response = new InterviewResponse();
		response.setQuestionNumber(questionNumber);
		response.setCurrentSkillQuestionNumber(1);
		response.setQuestion(responseJson.get("question").getAsString());
		response.setSkill(nextSkill);
		response.setCurrentDifficulty("easy");
		response.setCurrentSkillIndex(nextSkillIndex);
		response.setAnalysis(feedback);
		return response;
	}

	private InterviewResponse handleSimplerQuestion(List<Object> history, String currentSkill, int questionNumber,
			String feedback, String difficulty, int currentSkillIndex) {
		String newDifficulty = downgradeDifficulty(difficulty);
		String prompt = generateQuestion(newDifficulty, currentSkill, history);
		List<String> aiResponse = callGemini(prompt);
		JsonObject responseJson = parseAIResponse(aiResponse);

		InterviewResponse response = new InterviewResponse();
		response.setQuestionNumber(questionNumber);
		response.setQuestion(responseJson.get("question").getAsString());
		response.setSkill(currentSkill);
		response.setCurrentDifficulty(newDifficulty);
		response.setCurrentSkillIndex(currentSkillIndex);
		response.setAnalysis(feedback);
		return response;
	}

	private InterviewResponse handleNextQuestion(List<Object> history, String currentSkill, int questionNumber,
			String feedback, String difficulty, int currentSkillIndex) {
		String currentDifficulty = upgradeDifficulty(difficulty);
		String prompt = generateQuestion(currentDifficulty, currentSkill, history);
		List<String> aiResponse = callGemini(prompt);
		JsonObject responseJson = parseAIResponse(aiResponse);

		InterviewResponse response = new InterviewResponse();
		response.setQuestionNumber(questionNumber);
		response.setCurrentSkillQuestionNumber(currentSkillIndex + 1);
		response.setQuestion(responseJson.get("question").getAsString());
		response.setSkill(currentSkill);
		response.setCurrentDifficulty(currentDifficulty);
		response.setCurrentSkillIndex(currentSkillIndex);
		response.setAnalysis(feedback);
		return response;
	}

	private InterviewResponse completeInterview(List<Object> history, int questionNumber, String feedback) {
		InterviewResponse response = new InterviewResponse();
		response.setQuestionNumber(questionNumber);
		response.setQuestion("Interview completed");
		response.setAnalysis(feedback);
		response.setCompletionStatus(true);
		return response;
	}

	private InterviewResponse handleDefaultAction(List<Object> history, String currentSkill, int questionNumber,
			String feedback, int currentSkillIndex, String difficulty) {
		return handleNextQuestion(history, currentSkill, questionNumber, feedback, difficulty, currentSkillIndex);
	}

	private String generatePromptForEvaluation(String skill, String difficulty, List<Object> history) {
		difficulty = validateDifficulty(difficulty);
		System.out.println("applicant answer: " + history);

		return "You're an expert technical interviewer evaluating a candidate's answer for a " + difficulty
				+ " level question about " + skill + ".\n" + "Candidate answer (latest current answer from history): \""
				+ history + "\"\n\n" + "Evaluate the answer considering the candidate as a fresher:\n" 
				+ "Determine the next step:\n"
				+ "- If excellent and confident: move to next skill (action: next_skill)\n"
				+ "- If excellent but not confident: ask a harder question (action: next_question)\n"
				+ "- If good: ask a similar difficulty question (action: next_question)\n"
				+ "- If weak: ask an easier question (action: simpler_question)\n"
				+ "- If very poor or 'I don't know': move to next skill (action: next_skill)\n\n"
				+ "Provide brief constructive feedback in 2 lines.\n" + "Respond with ONLY raw JSON in this exact format:\n"
				+ "{\"action\": \"next_question|simpler_question|next_skill|end\", "
				+ "\"question\": \"next question or empty\", " + "\"feedback\": \"your feedback\"}";
	}

	private String generateQuestion(String difficulty, String skill, List<Object> history) {
		return "You are an adaptive technical interviewer. Please respond with ONLY the JSON output, no additional text or code blocks.\n\n"
				+ "Given:\n" + "- skill: " + skill + "\n" + "- history: " + history + " difficulty :" + difficulty
				+ "\n" + "Process:\r\n" + "\r\n" + "Generate a " + difficulty + " level question on the given skill"
				+ skill + "Rules: " + "Conceptual Questions or Theoretical Questions\r\n"
				+ "Which focus on understanding how and why things work in Java, without requiring code."
				+ " Strictly Return a JSON object with these fields:\r\n" + "{\r\n"
				+ "  \"question\": \"<your generated question>\",\r\n" + "  \"skill\": \"<current skill>\",\r\n}";
	}

	private JsonObject parseAIResponse(List<String> response) {
		try {
			System.out.println("response: " + response);

			String combined = String.join(" ", response).trim();

			String cleaned = combined.replaceAll("```json", "").replaceAll("```", "").trim();

			int start = cleaned.indexOf("{");
			int end = cleaned.lastIndexOf("}");

			if (start == -1 || end == -1 || end <= start) {
				throw new RuntimeException("Invalid JSON content in response");
			}

			String jsonPart = cleaned.substring(start, end + 1).trim();
			System.out.println("jsonPart: " + jsonPart);

			return JsonParser.parseString(jsonPart).getAsJsonObject();

		} catch (Exception e) {
			System.out.println("Failed to parse response:" + e);
			throw new RuntimeException("Invalid response format from Gemini");
		}
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
			System.out.println("Error calling Gemini API" + e);
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
}