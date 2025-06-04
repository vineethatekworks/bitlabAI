package com.talentstream.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.talentstream.dto.InterviewRequest;
import com.talentstream.dto.InterviewResponse;
import com.talentstream.service.InterviewService2;

@RestController
@RequestMapping("/api/interview")
public class InterviewController {

    @Autowired
    private InterviewService2 interviewService;

    /**
     * Generates the next interview question in the thread based on applicantId,
     * skills, history, and current answer.
     */
    @PostMapping("/next-question")
    public ResponseEntity<?> getNextQuestion(@RequestBody InterviewRequest request) {
        try {
        	System.out.println("request"+request);
            InterviewResponse response = interviewService.generateNextQuestion(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // You can also log the exception here if needed
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
