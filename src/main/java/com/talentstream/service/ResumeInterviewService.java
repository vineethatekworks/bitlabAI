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

@Service
public class ResumeInterviewService {

	private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=";

	@Value("${gemini.api.key}")
	private String apiKey;

	private final HttpClient httpClient = HttpClient.newHttpClient();
	private final Gson gson = new GsonBuilder().create();

	public List<String> ResumeInterview(String resumeText) {
		String prompt = buildPrompt(resumeText);
		System.out.println("prompt :" + prompt);
		List<String> responseLines = callGemini(prompt);
		JsonObject jsonOutput = parseAIResponse(responseLines);

		if (jsonOutput.has("questions")) {
			JsonArray questions = jsonOutput.getAsJsonArray("questions");
			return questionsAsList(questions);
		} else {
			throw new RuntimeException("Response does not contain 'questions' field");
		}
	}

	private List<String> questionsAsList(JsonArray jsonArray) {
		List<String> questions = new java.util.ArrayList<>();
		for (int i = 0; i < jsonArray.size(); i++) {
			questions.add(jsonArray.get(i).getAsString());
		}
		return questions;
	}

	private String buildPrompt(String resumeText) {
		return "You are an expert technical interviewer interviewing a **fresher** for a **software development role**. The candidate has submitted the following resume:\n\n"
				+ resumeText + "\n\nBased on the resume, ask **10 highly relevant interview questions** ONLY focusing on:\n"
				+ "- Technologies and programming languages (e.g., Java, SQL, HTML/CSS, etc.)\n"
				+ "- Projects (highlighting system design, tools used, real-world application only for checking knowledge)\n"
				+ "- Tools and platforms mentioned (e.g., Eclipse, Arduino, databases, etc.)\n"
				+ "- Certifications, career goals, and learning interests\n\n"
				+ "**Avoid generic course-related questions.** Focus only on what is mentioned in the resume, especially things related to software \n\n"
				+ "Respond in the following JSON format:\n" + "```json\n" + "{\n" + "  \"questions\": [\n"
				+ "    \"Question 1\",\n" + "    \"Question 2\",\n" + "    ...,\n" + "    \"Question 10\"\n" + "  ]\n"
				+ "}\n" + "```";
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
}
