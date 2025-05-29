package com.talentstream.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.talentstream.dto.InterviewRequest;
import com.talentstream.dto.InterviewResponse;
import com.talentstream.dto.QuestionHistory;

@Service
public class InterviewService {

    private static final Logger logger = LoggerFactory.getLogger(InterviewService.class);
    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Gson gson = new Gson();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${gemini.api.key}")
	private String apiKey;
    
    public InterviewResponse generateNextQuestion(InterviewRequest request) {
        // 1. Build prompt using applicant's data
        String prompt = buildPrompt(request);
        System.out.println("prompt :"+prompt);

        // 2. Call Gemini AI to generate response
        String aiResponse = call(prompt);
        System.out.println("airesponse :"+aiResponse);

        // 3. Parse and return InterviewResponse
        return parseAIResponse(aiResponse, request.getHistory().size() + 1);
    }

    private String buildPrompt(InterviewRequest request) {
        StringBuilder historyBuilder = new StringBuilder();
        if (request.getHistory() != null) {
            for (QuestionHistory q : request.getHistory()) {
                historyBuilder.append("- Q")
                        .append(q.getQuestionNumber())
                        .append(": ")
                        .append(q.getQuestion())
                        .append("\n");
            }
        }

        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("Generate a technical interview question for a candidate.\n");
        promptBuilder.append("Skills: ").append(String.join(", ", request.getSkills())).append("\n");
        promptBuilder.append("Previous Questions:\n").append(historyBuilder.toString());
        promptBuilder.append("Candidate's Last Answer: ")
                .append(request.getCurrentAnswer() == null ? "None" : request.getCurrentAnswer())
                .append("\n\n");
        promptBuilder.append("Respond in JSON format with fields: {\"question\": \"string\", \"feedback\": \"string\"}");

        return promptBuilder.toString();
    }


    private InterviewResponse parseAIResponse(String jsonResponse, int questionNumber) {
        try {
            JsonNode node = objectMapper.readTree(jsonResponse);
            String question = node.get("question").asText();
            String feedback = node.get("feedback").asText();

            return new InterviewResponse(
                    questionNumber,
                    question,
                    false,
                    feedback
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse AI response: " + e.getMessage(), e);
        }
    }

    private String call(String prompt) {
        try {
            Map<String, Object> content = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", prompt)
                            ))
                    )
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GEMINI_URL + apiKey))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(content)))
                    .build();

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

            // Return raw JSON response (1st line expected to be JSON string)
            return Arrays.stream(text.split("\n"))
                    .map(String::trim)
                    .filter(line -> !line.isBlank())
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No usable content in Gemini response"));

        } catch (Exception e) {
            logger.error("Error calling Gemini API", e);
            throw new RuntimeException("Error calling Gemini API: " + e.getMessage());
        }
    }
}
