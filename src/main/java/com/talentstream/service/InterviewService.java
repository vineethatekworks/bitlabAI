package com.talentstream.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
	@Value("${gemini.api.key}")
	private String apiKey;

	private final HttpClient httpClient = HttpClient.newHttpClient();
	private final Gson gson = new GsonBuilder().create();
	public InterviewResponse generateNextQuestion(InterviewRequest request) {
	    // Step 1: Generate the prompt based on skills, history, and current answer
	    String prompt = generatePrompt(request.getSkills(), request.getHistory(), request.getCurrentAnswer());
	    System.out.println("prompt: " + prompt);

	    // Step 2: Call Gemini and get the response
	    List<String> airesponse = callGemini(prompt);
	    System.out.println("gemini: " + airesponse);

	    // Step 3: Parse Gemini's response JSON (you already have a method for this)
	    JsonObject responseJson = parseAIResponse(airesponse);

	    // Step 4: Extract fields from JsonObject
	    String questionNumber = responseJson.get("questionNumber").getAsString();
	    String question = responseJson.get("question").getAsString();
	    String analysis = responseJson.has("analysis") ? responseJson.get("analysis").getAsString() : "";
	    boolean completionStatus = responseJson.get("completionStatus").getAsBoolean();
	    String overallFeedback = responseJson.has("overallFeedback") ? responseJson.get("overallFeedback").getAsString() : "";

	    // Step 5: Create and return InterviewResponse
	    InterviewResponse response = new InterviewResponse();
	    response.setQuestionNumber(questionNumber);
	    response.setQuestion(question);
	    response.setAnalysis(analysis);
	    response.setCompletionStatus(completionStatus);
	    response.setOverallFeedback(overallFeedback);

	    return response;
	}

	private JsonObject parseAIResponse(List<String> response) {
	    try {
	        System.out.println("response: " + response);

	        String combined = String.join(" ", response).trim();

	        String cleaned = combined.replaceAll("```json", "")
	                                 .replaceAll("```", "")
	                                 .trim();

	        int start = cleaned.indexOf("{");
	        int end = cleaned.lastIndexOf("}");

	        if (start == -1 || end == -1 || end <= start) {
	            throw new RuntimeException("Invalid JSON content in response");
	        }

	        String jsonPart = cleaned.substring(start, end + 1).trim();
	        System.out.println("jsonPart: " + jsonPart);

	        return JsonParser.parseString(jsonPart).getAsJsonObject();

	    } catch (Exception e) {
	        System.out.println("Failed to parse response:"+e);
	        throw new RuntimeException("Invalid response format from Gemini");
	    }
	}



	private String generatePrompt(List<String> skills, List<Object> history, String currentAnswer) {
		return "You are Vineetha , An AI inetreview but you have to think like a human and follow up the below rules:"
	            + "First Question:\r\n"
				+ "If history and current_answer are null, pick the first skill from the skills list:"+skills+" and ask a beginner-level question.\r\n"
				+ "Follow-Up Based on History:"+history
				+ "If history exists, reference it to:\r\n"
				+ "Avoid repeated questions and repeated topic in a skill.\r\n"
				+ "Avoid revisiting already tested skills (unless further probing is needed).\r\n"
				+ "Evaluate Current Answer: "+currentAnswer
				+ "If answer is irrelevant/\"I don’t know or null\"/low-confidence:\r\n"
				+ "Move to the next skill in the list.\r\n"
				+ "If answer is good/high-confidence:\r\n"
				+ "Move to a different skill (unless deeper assessment is needed).\r\n"
				+ "If answer is relevant but shallow/unclear:\r\n"
				+ "Ask a follow-up question on the same skill (deeper or alternative angle).\r\n"
				+ "Skill Progression:\r\n"
				+ "Never loop back to a skill once confidently answered.\r\n"
				+ "Ensure all skills are covered unless the candidate consistently struggles."
				+ "strictly Return only 1 JSON object dont give extra information rather than JSON:\r\n"
				+ "{\r\n"
				+ "  \"questionNumber\": \"<Next question number>\",\r\n"
				+ "  \"question\": \"<Next practical conceptual question>\",\r\n"
				+ "  \"analysis\": \"<Evaluation of the latest answer>\",\r\n"
				+ "  \"completionStatus\": <true | false>,\r\n"
				+ "  \"overallFeedback\": \"<Summary if done, else empty>\"\r\n"
				+ "}`";
		}



	private List<String> callGemini(String prompt)   {
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
			System.out.println("Error calling Gemini API"+ e);
			throw new RuntimeException("Error calling Gemini API: " + e.getMessage());
		}
	}

	
}