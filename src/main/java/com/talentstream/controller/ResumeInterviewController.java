package com.talentstream.controller;

import java.io.InputStream;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.talentstream.service.ResumeInterviewService;

@RestController
@RequestMapping("/interview")
public class ResumeInterviewController {

    @Autowired
    private ResumeInterviewService resumeInterviewService;

    @PostMapping("/resumeBased")
    public ResponseEntity<?> resumeInterview(@RequestParam("file") MultipartFile file) {
        try (
            InputStream inputStream = file.getInputStream();
            PDDocument document = PDDocument.load(inputStream)
        ) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            List<String> questions = resumeInterviewService.ResumeInterview(text);

            return ResponseEntity.ok(questions);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to process PDF: " + e.getMessage());
        }
    }
}
