package com.example.B2XKlaim.Controller;

import java.util.Map;

import lombok.EqualsAndHashCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.B2XKlaim.Service.BpmnParsingService;
import com.example.B2XKlaim.Service.CodeGenerationService;
import com.example.B2XKlaim.Service.CodeGenerationService.GenerationResult;
import com.example.B2XKlaim.Service.ResponseBuilderService;
import com.example.B2XKlaim.Service.bpmnElements.BpmnElements;

import lombok.extern.slf4j.Slf4j;

@CrossOrigin(origins = "*")
@RestController
@Slf4j
@EqualsAndHashCode(callSuper = false)
public class TranslationController {

    @Autowired
    private BpmnParsingService bpmnParsingService;

    @Autowired
    private CodeGenerationService codeGenerationService;

    @Autowired
    private ResponseBuilderService responseBuilderService;

    @PostMapping("/generate-code")
    public ResponseEntity<Map<String, Object>> generateCode(@RequestBody Map<String, Object> request) {
        try {
            log.info("Received request to generate code");

            // Validate request
            if (!request.containsKey("processes")) {
                throw new IllegalArgumentException("Missing 'processes' in request");
            }

            // Parse BPMN processes
            Map<String, Object> processes = (Map<String, Object>) request.get("processes");
            Map<String, BpmnElements> parsedProcesses = bpmnParsingService.parseMultipleProcesses(processes);

            // Generate code
            GenerationResult generationResult = codeGenerationService.generateCode(parsedProcesses);

            // Build response using the full generated code from the result
            Map<String, Object> response = responseBuilderService.buildResponse(
                generationResult, generationResult.getFullGeneratedCode());

            log.info("Code generation successful");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error during code generation: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = responseBuilderService.buildErrorResponse(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}