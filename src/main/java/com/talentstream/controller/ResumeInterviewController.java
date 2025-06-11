package com.talentstream.controller;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@RestController
@RequestMapping("/pdf")
public class PdfReaderController {

	@PostMapping("/read")
	public ResponseEntity<String> readPdf(@RequestParam("file") MultipartFile file) {
	    try (
	        InputStream inputStream = file.getInputStream();
	        PDDocument document = PDDocument.load(inputStream)
	    ) {
	        PDFTextStripper stripper = new PDFTextStripper();
	        String text = stripper.getText(document);
	        return ResponseEntity.ok(text);
	    } catch (Exception e) {
	        e.printStackTrace();
	        return ResponseEntity.status(500).body("Failed to read PDF: " + e.getMessage());
	    }
	}
}
