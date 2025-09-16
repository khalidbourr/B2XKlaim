package com.example.B2XKlaim.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.B2XKlaim.Service.CodeGenerationService.GenerationResult;
import com.example.B2XKlaim.Service.CodeGenerationService.ProcessCode;
import com.example.B2XKlaim.Service.builder.ResponseBuilder;

import lombok.extern.slf4j.Slf4j;

/**
 * Service responsible for building structured responses for the REST API.
 * Handles formatting and organizing code generation results into API response format.
 */
@Service
@Slf4j
public class ResponseBuilderService {

    @Autowired
    private CodeFormattingService codeFormattingService;

    /**
     * Builds a complete API response from generation results using Builder Pattern.
     * 
     * @param generationResult The code generation results
     * @param fullGeneratedCode The complete generated code string (for extraction) - can be null
     * @return Structured response map ready for API return
     */
    public Map<String, Object> buildResponse(GenerationResult generationResult, String fullGeneratedCode) {
        log.debug("Building API response from generation results using ResponseBuilder");

        // Handle collaboration code
        String collaborationCode = generationResult.getCollaborationCode();
        if (fullGeneratedCode != null && !fullGeneratedCode.isEmpty()) {
            collaborationCode = codeFormattingService.extractCollaborationBlock(fullGeneratedCode);
        }

        // Handle processes
        List<Map<String, String>> processList = buildProcessList(generationResult, fullGeneratedCode);

        // Build response using ResponseBuilder
        Map<String, Object> response = ResponseBuilder.create()
                .withCollaboration(collaborationCode)
                .withProcessList(processList)
                .withCallActivities(generationResult.getCallActivities())
                .withScriptTaskProcs(generationResult.getScriptTasks())
                .withEventSubProcesses(generationResult.getEventSubProcesses())
                .withParticipants(generationResult.getParticipants())
                .build();

        log.debug("Built API response with {} processes and {} participants", 
                 processList.size(), generationResult.getParticipants().size());

        return response;
    }

    /**
     * Builds error response for API failures using ResponseBuilder.
     * 
     * @param errorMessage The error message
     * @return Error response map
     */
    public Map<String, Object> buildErrorResponse(String errorMessage) {
        log.debug("Building error response using ResponseBuilder: {}", errorMessage);
        
        return ResponseBuilder.create()
                .withError("Translation failed: " + errorMessage)
                .build();
    }

    /**
     * Builds process list from generation results, handling both collaboration and standalone cases.
     */
    private List<Map<String, String>> buildProcessList(GenerationResult generationResult, String fullGeneratedCode) {
        List<Map<String, String>> processList = new ArrayList<>();

        if (fullGeneratedCode != null && !fullGeneratedCode.isEmpty() && 
            !generationResult.getCollaborationCode().contains("No collaboration defined")) {
            
            // Extract processes from collaboration code
            List<Map<String, String>> rawProcesses = codeFormattingService.extractProcessBlocks(fullGeneratedCode);
            processList = codeFormattingService.formatProcessList(rawProcesses, "  ");
            
            log.debug("Extracted and formatted {} processes from collaboration", processList.size());
            
        } else {
            // Use standalone processes
            for (ProcessCode processCode : generationResult.getProcesses()) {
                String formattedCode = codeFormattingService.formatProcessCode(processCode.getCode(), "  ");
                
                Map<String, String> processEntry = new HashMap<>();
                processEntry.put("name", processCode.getName());
                processEntry.put("code", formattedCode);
                processList.add(processEntry);
            }
            
            log.debug("Formatted {} standalone processes", processList.size());
        }

        return processList;
    }

    /**
     * Builds a minimal success response with just essential fields using ResponseBuilder.
     * Useful for simplified API responses or testing.
     */
    public Map<String, Object> buildMinimalResponse(String collaborationCode, List<ProcessCode> processes) {
        log.debug("Building minimal response using ResponseBuilder");
        
        return ResponseBuilder.create()
                .withCollaboration(collaborationCode)
                .withProcesses(processes)
                .buildMinimal();
    }

    /**
     * Validates that a response contains all required fields.
     * 
     * @param response The response to validate
     * @return true if valid, false otherwise
     */
    public boolean validateResponse(Map<String, Object> response) {
        String[] requiredFields = {"collaboration", "processes", "callActivities", 
                                 "scriptTaskProcs", "eventSubProcesses", "participants"};
        
        for (String field : requiredFields) {
            if (!response.containsKey(field)) {
                log.warn("Response missing required field: {}", field);
                return false;
            }
        }
        
        return true;
    }
}