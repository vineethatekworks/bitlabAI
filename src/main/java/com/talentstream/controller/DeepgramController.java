package com.talentstream.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class DeepgramController {

	@Value("${deepgram.api.key}")
	private String deepgramApiKey;
	private static final String DEEPGRAM_URL = "https://api.deepgram.com/v1/listen";

	private RestTemplate restTemplate = new RestTemplate();

	@PostMapping(value = "/transcribe", consumes = MediaType.ALL_VALUE)
	public String transcribeRawAudio(@RequestBody byte[] audioBytes) {

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
		headers.set("Authorization", "Token " + deepgramApiKey);

		HttpEntity<byte[]> entity = new HttpEntity<>(audioBytes, headers);

		ResponseEntity<String> response = restTemplate.postForEntity(DEEPGRAM_URL, entity, String.class);

		return response.getBody();
	}
}