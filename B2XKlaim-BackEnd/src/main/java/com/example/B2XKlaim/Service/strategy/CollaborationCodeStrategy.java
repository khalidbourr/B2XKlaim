package com.example.B2XKlaim.Service.strategy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.example.B2XKlaim.Service.CodeGenerationService.ProcessCode;
import com.example.B2XKlaim.Service.bpmnElements.BpmnElements;
import com.example.B2XKlaim.Service.bpmnElements.objects.pool.Collab;
import com.example.B2XKlaim.Service.bpmnElements.objects.pool.PL;
import com.example.B2XKlaim.Service.codeGenerator.Generator;
import com.example.B2XKlaim.Service.codeGenerator.Optimizer;

import lombok.extern.slf4j.Slf4j;

/**
 * Strategy for generating code when BPMN contains collaboration elements.
 * Handles collaboration-based code generation with participant extraction.
 */
@Component
@Slf4j
public class CollaborationCodeStrategy implements CodeGenerationStrategy {

    @Override
    public String generateCollaborationCode(Generator codeGenerator, BpmnElements mainBpmnElements) throws Exception {
        log.debug("Generating collaboration code using CollaborationCodeStrategy");
        
        List<String> fullXklaimCodeLines = codeGenerator.translateBpmnCollaboration();
        
        // Apply optimizer
        log.debug("Applying optimizer to collaboration output");
        List<String> optimizedLines = Optimizer.optimize(fullXklaimCodeLines);
        
        String collaborationCode = String.join("\n", optimizedLines);
        log.debug("Generated collaboration code ({} characters)", collaborationCode.length());
        
        return collaborationCode;
    }

    @Override
    public List<ProcessCode> generateProcesses(Generator codeGenerator, BpmnElements mainBpmnElements, 
                                              String collaborationCode) throws Exception {
        log.debug("Extracting processes from collaboration code using CollaborationCodeStrategy");
        
        // For collaboration-based generation, processes are extracted from the collaboration code
        // This is handled by the CodeFormattingService in the ResponseBuilderService
        // Return empty list as processes will be extracted later from the full collaboration code
        return new ArrayList<>();
    }

    @Override
    public Set<String> extractParticipants(BpmnElements mainBpmnElements) {
        log.debug("Extracting participants from collaboration elements");
        
        Set<String> participants = new HashSet<>();
        for (PL plElement : mainBpmnElements.getElementsByType(PL.class)) {
            String participantName = plElement.getName();
            if (participantName != null && !participantName.isEmpty()) {
                participants.add(participantName);
                log.trace("Added participant: {}", participantName);
            }
        }
        
        log.debug("Extracted {} participants from collaboration", participants.size());
        return participants;
    }

    @Override
    public boolean canHandle(BpmnElements mainBpmnElements) {
        List<Collab> collabElements = mainBpmnElements.getElementsByType(Collab.class);
        boolean canHandle = !collabElements.isEmpty();
        
        log.debug("CollaborationCodeStrategy canHandle: {} (found {} collaboration elements)", 
                 canHandle, collabElements.size());
        
        return canHandle;
    }

    @Override
    public String getStrategyName() {
        return "CollaborationCodeStrategy";
    }
}