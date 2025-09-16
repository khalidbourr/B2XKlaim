package com.example.B2XKlaim.Service.strategy;

import java.util.List;
import java.util.Set;

import com.example.B2XKlaim.Service.CodeGenerationService.ProcessCode;
import com.example.B2XKlaim.Service.bpmnElements.BpmnElements;
import com.example.B2XKlaim.Service.codeGenerator.Generator;

/**
 * Strategy interface for different code generation approaches.
 * Enables flexible handling of collaboration-based vs standalone process generation.
 */
public interface CodeGenerationStrategy {

    /**
     * Generates collaboration code using the specific strategy.
     * 
     * @param codeGenerator The BPMN code generator
     * @param mainBpmnElements The main BPMN elements to process
     * @return The generated collaboration code
     * @throws Exception if code generation fails
     */
    String generateCollaborationCode(Generator codeGenerator, BpmnElements mainBpmnElements) throws Exception;

    /**
     * Generates process codes using the specific strategy.
     * 
     * @param codeGenerator The BPMN code generator
     * @param mainBpmnElements The main BPMN elements to process
     * @param collaborationCode The collaboration code (may be null for standalone)
     * @return List of generated process codes
     * @throws Exception if code generation fails
     */
    List<ProcessCode> generateProcesses(Generator codeGenerator, BpmnElements mainBpmnElements, 
                                       String collaborationCode) throws Exception;

    /**
     * Extracts participants from BPMN elements using the specific strategy.
     * 
     * @param mainBpmnElements The main BPMN elements to process
     * @return Set of participant names
     */
    Set<String> extractParticipants(BpmnElements mainBpmnElements);

    /**
     * Determines if this strategy can handle the given BPMN elements.
     * 
     * @param mainBpmnElements The BPMN elements to check
     * @return true if this strategy can handle the elements, false otherwise
     */
    boolean canHandle(BpmnElements mainBpmnElements);

    /**
     * Returns a descriptive name for this strategy.
     * 
     * @return Strategy name
     */
    String getStrategyName();
}