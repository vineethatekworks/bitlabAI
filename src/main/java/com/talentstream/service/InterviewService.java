//package com.talentstream.service;
//
//import java.net.URI;
//import java.net.http.HttpClient;
//import java.net.http.HttpRequest;
//import java.net.http.HttpResponse;
//import java.util.Arrays;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//
//import com.google.gson.Gson;
//import com.google.gson.GsonBuilder;
//import com.google.gson.JsonArray;
//import com.google.gson.JsonObject;
//import com.google.gson.JsonParser;
//import com.talentstream.dto.InterviewRequest;
//import com.talentstream.dto.InterviewResponse;
//
//@Service
//public class InterviewService {
//
//	private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=";
//	@Value("${gemini.api.key}")
//	private String apiKey;
//
//	private final HttpClient httpClient = HttpClient.newHttpClient();
//	private final Gson gson = new GsonBuilder().create();
//
//	public InterviewResponse generateNextQuestion(InterviewRequest request) {
//		// Step 1: Generate the prompt based on skills, history, and current answer
//		String prompt = generatePrompt(request.getSkills(), request.getHistory());
//		System.out.println("prompt: " + prompt);
//
//		// Step 2: Call Gemini and get the response
//		List<String> airesponse = callGemini(prompt);
//		System.out.println("gemini: " + airesponse);
//
//		// Step 3: Parse Gemini's response JSON (you already have a method for this)
//		JsonObject responseJson = parseAIResponse(airesponse);
//
//		// Step 4: Extract fields from JsonObject
//		String globalQuestionNumber = responseJson.get("globalQuestionNumber").getAsString();
//		System.out.println("questionNum");
//		String currentSkillQuestionNumber = responseJson.get("currentSkillQuestionNumber").getAsString();
//		System.out.println("questionNum");
//		String question = responseJson.get("question").getAsString();
//		System.out.println("question");
//		String analysis = responseJson.has("analysis") ? responseJson.get("analysis").getAsString() : "";
//		System.out.println("Analysis");
//		boolean completionStatus = responseJson.get("completionStatus").getAsBoolean();
//		System.out.println("completionStatus");
//		String Skill = responseJson.get("skill").getAsString();
//		System.out.println("Skill");
//		String overallFeedback = responseJson.has("overallFeedback") ? responseJson.get("overallFeedback").getAsString()
//				: "";
//
//		// Step 5: Create and return InterviewResponse
//		InterviewResponse response = new InterviewResponse();
//		response.setQuestionNumber(globalQuestionNumber);
//		response.setCurrentSkillQuestionNumber(currentSkillQuestionNumber);
//		response.setQuestion(question);
//		response.setAnalysis(analysis);
//		response.setCompletionStatus(completionStatus);
//		response.setSkill(Skill);
//		response.setOverallFeedback(overallFeedback);
//
//		return response;
//	}
//
//	private JsonObject parseAIResponse(List<String> response) {
//		try {
//			System.out.println("response: " + response);
//
//			String combined = String.join(" ", response).trim();
//
//			String cleaned = combined.replaceAll("```json", "").replaceAll("```", "").trim();
//
//			int start = cleaned.indexOf("{");
//			int end = cleaned.lastIndexOf("}");
//
//			if (start == -1 || end == -1 || end <= start) {
//				throw new RuntimeException("Invalid JSON content in response");
//			}
//
//			String jsonPart = cleaned.substring(start, end + 1).trim();
//			System.out.println("jsonPart: " + jsonPart);
//
//			return JsonParser.parseString(jsonPart).getAsJsonObject();
//
//		} catch (Exception e) {
//			System.out.println("Failed to parse response:" + e);
//			throw new RuntimeException("Invalid response format from Gemini");
//		}
//	}
//
//	private String generatePrompt(List<String> skills, List<Object> history) {
//		return "You are an AI interviewer for freshers. You're assessing conceptual understanding across multiple programming skills: ${skill}.\r\n"
//				+ " \r\n"
//				+ "Your task:\r\n"
//				+ "1. Use the full interview history below.\r\n"
//				+ "   - Never repeat previously asked questions.\r\n"
//				+ "   - Ask only **one question at a time**.\r\n"
//				+ "2. Evaluate the **latest answer**: \"${inputValue}\".\r\n"
//				+ "3. Ask only **one-line conceptual questions** (not syntax or factual).\r\n"
//				+ "   - Must test core reasoning, design choices, pitfalls, or best practices.\r\n"
//				+ "   - Should provoke thought and explanation.\r\n"
//				+ "4. Rules per skill:\r\n"
//				+ "   - Start from the first skill in the list.\r\n"
//				+ "   - Ask up to **4 conceptual questions** for a skill.\r\n"
//				+ "   - If **2 answers** are irrelevant, blank, or \"I don't know\", **switch to next skill**.\r\n"
//				+ "   - dont ask the question which is related to the previous one\r\n"
//				+ "   - Never ask questions again on a completed or skipped skill.\r\n"
//				+ "5. Global rule:\r\n"
//				+ "   - If total **irrelevant/blank/“I don’t know” answers** across all skills >= 4, **end the test**.\r\n"
//				+ "6. If the answer is valid and relevant, ask a **deeper conceptual** follow-up.\r\n"
//				+ "7. Once all skills are either tested or skipped, **end the test and give final feedback**.\r\n"
//				+ " \r\n"
//				+ "You must return ONLY a **valid JSON object** with this structure:\r\n"
//				+ " \r\n"
//				+ "{\r\n"
//				+ "  \"questionNumber\": \"<Next question number>\",\r\n"
//				+ "  \"question\": \"<Next conceptual question>\",\r\n"
//				+ "  \"analysis\": \"<Short evaluation of the latest answer>\",\r\n"
//				+ "  \"completionStatus\": <true | false>,\r\n"
//				+ "  \"overallFeedback\": \"<Summary if completed, else leave empty>\"\r\n";
//	}
//
//	private List<String> callGemini(String prompt) {
//		try {
//			Map<String, Object> content = Map.of("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))));
//
//			HttpRequest request = HttpRequest.newBuilder().uri(URI.create(GEMINI_URL + apiKey))
//					.header("Content-Type", "application/json")
//					.POST(HttpRequest.BodyPublishers.ofString(gson.toJson(content))).build();
//
//			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
//
//			if (response.statusCode() != 200) {
//				throw new RuntimeException("Gemini API returned status: " + response.statusCode());
//			}
//
//			JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
//
//			if (json.has("error")) {
//				throw new RuntimeException("Gemini API error: " + json.get("error").toString());
//			}
//
//			JsonArray candidates = json.getAsJsonArray("candidates");
//			if (candidates == null || candidates.isEmpty()) {
//				throw new RuntimeException("No candidates in Gemini response");
//			}
//
//			JsonObject cont = candidates.get(0).getAsJsonObject().getAsJsonObject("content");
//			JsonArray parts = cont.getAsJsonArray("parts");
//			if (parts == null || parts.isEmpty()) {
//				throw new RuntimeException("No parts in Gemini response content");
//			}
//
//			String text = parts.get(0).getAsJsonObject().get("text").getAsString();
//
//			return Arrays.stream(text.split("\n")).map(String::trim).filter(line -> !line.isBlank())
//					.collect(Collectors.toList());
//
//		} catch (Exception e) {
//			System.out.println("Error calling Gemini API" + e);
//			throw new RuntimeException("Error calling Gemini API: " + e.getMessage());
//		}
//	}
//
//}