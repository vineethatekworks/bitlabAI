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
public class InterviewService2 {

	private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=";
	@Value("${gemini.api.key}")
	private String apiKey;

	private final HttpClient httpClient = HttpClient.newHttpClient();
	private final Gson gson = new GsonBuilder().create();

	public List<Object> generateNextQuestion(InterviewRequest request) {

		String prompt = generatePrompt(request.getSkills(), request.getHistory());
		System.out.println("prompt: " + prompt);

		List<String> airesponse = callGemini(prompt);
		System.out.println("gemini: " + airesponse);

		JsonObject responseJson = parseAIResponse(airesponse);

		return null;
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

	private String generatePrompt(List<String> skills, List<Object> history) {
		return "You are an expert Java technical interviewer. Based on the following list of skills:" + skills + "\r\n"
				+ "Generate **conceptual** interview questions for each skill **separately**. Ask questions for only one skill at a time before moving to the next skill.\r\n"
				+ "⚠️ Guidelines:\r\n" + "Do not repeat topics from previously asked questions.\r\n"
				+ "Focus on real-world reasoning, design decisions, performance, OOP principles, memory, or concurrency.\r\n"
				+ "Avoid questions like “What is X?” or “Define Y.”\r\n" + "Each question must be in one line only.\r\n"
				+ " Strictly Return a JSON object with these fields:\r\n" + "{\r\n"
				+ "  \"questions\": <your generated questions>,\r\n";
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

}