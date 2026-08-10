package com.example.B2XKlaim.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.B2XKlaim.Service.bpmnElements.BpmnElements;
import com.example.B2XKlaim.Service.builder.GenerationResultBuilder;
import com.example.B2XKlaim.Service.codeGenerator.Generator;
import com.example.B2XKlaim.Service.strategy.CodeGenerationStrategy;
import com.example.B2XKlaim.Service.strategy.CollaborationCodeStrategy;
import com.example.B2XKlaim.Service.strategy.StandaloneProcessStrategy;

import lombok.extern.slf4j.Slf4j;

/**
 * Service responsible for generating XKlaim code from parsed BPMN elements.
 * Handles collaboration translation, process generation, and optimization.
 */
@Service
@Slf4j
public class CodeGenerationService {

    @Autowired
    private CollaborationCodeStrategy collaborationCodeStrategy;

    @Autowired
    private StandaloneProcessStrategy standaloneProcessStrategy;

    /**
     * Result object containing all generated code components.
     */
    public static class GenerationResult {
        private final String collaborationCode;
        private final String fullGeneratedCode;
        private final List<ProcessCode> processes;
        private final Map<String, List<String>> callActivities;
        private final Map<String, List<String>> scriptTasks;
        private final Map<String, List<String>> eventSubProcesses;
        private final Map<String, List<String>> andBranchProcs;
        private final Set<String> participants;

        public GenerationResult(String collaborationCode, String fullGeneratedCode, List<ProcessCode> processes,
                              Map<String, List<String>> callActivities, Map<String, List<String>> scriptTasks,
                              Map<String, List<String>> eventSubProcesses, Map<String, List<String>> andBranchProcs,
                              Set<String> participants) {
            this.collaborationCode = collaborationCode;
            this.fullGeneratedCode = fullGeneratedCode;
            this.processes = processes;
            this.callActivities = callActivities;
            this.scriptTasks = scriptTasks;
            this.eventSubProcesses = eventSubProcesses;
            this.andBranchProcs = andBranchProcs;
            this.participants = participants;
        }

        public String getCollaborationCode() { return collaborationCode; }
        public String getFullGeneratedCode() { return fullGeneratedCode; }
        public List<ProcessCode> getProcesses() { return processes; }
        public Map<String, List<String>> getCallActivities() { return callActivities; }
        public Map<String, List<String>> getScriptTasks() { return scriptTasks; }
        public Map<String, List<String>> getEventSubProcesses() { return eventSubProcesses; }
        public Map<String, List<String>> getAndBranchProcs() { return andBranchProcs; }
        public Set<String> getParticipants() { return participants; }
    }

    /**
     * Represents a generated process with name and code.
     */
    public static class ProcessCode {
        private final String name;
        private final String code;

        public ProcessCode(String name, String code) {
            this.name = name;
            this.code = code;
        }

        public String getName() { return name; }
        public String getCode() { return code; }
    }

    /**
     * Generates XKlaim code from parsed BPMN processes using strategy pattern.
     * 
     * @param parsedProcesses Map of process ID to BpmnElements
     * @return GenerationResult containing all generated code components
     */
    public GenerationResult generateCode(Map<String, BpmnElements> parsedProcesses) {
        log.info("Starting code generation for {} processes", parsedProcesses.size());

        Generator codeGenerator = new Generator(parsedProcesses);
        BpmnElements mainBpmnElements = parsedProcesses.get("main");

        try {
            // Select appropriate strategy
            CodeGenerationStrategy strategy = selectStrategy(mainBpmnElements);
            log.info("Using strategy: {}", strategy.getStrategyName());

            // Generate code using strategy
            String collaborationCode = strategy.generateCollaborationCode(codeGenerator, mainBpmnElements);
            List<ProcessCode> processes = strategy.generateProcesses(codeGenerator, mainBpmnElements, collaborationCode);
            Set<String> participants = strategy.extractParticipants(mainBpmnElements);

            // Generate supporting elements
            Map<String, List<String>> callActivities = codeGenerator.translateCallActivity();
            Map<String, List<String>> scriptTasks = codeGenerator.translateST();
            Map<String, List<String>> eventSubProcesses = codeGenerator.translateEventSubProcesses();
            Map<String, List<String>> andBranchProcs = codeGenerator.translateAndBranchProcs();

            log.info("Code generation completed successfully using {}", strategy.getStrategyName());
            
            // Build result using GenerationResultBuilder
            return GenerationResultBuilder.create()
                    .withCode(collaborationCode)
                    .withProcesses(processes)
                    .withCallActivities(callActivities)
                    .withScriptTasks(scriptTasks)
                    .withEventSubProcesses(eventSubProcesses)
                    .withAndBranchProcs(andBranchProcs)
                    .withParticipants(participants)
                    .build();

        } catch (Exception e) {
            log.error("Error during code generation: {}", e.getMessage(), e);
            throw new RuntimeException("Code generation failed", e);
        }
    }

    /**
     * Selects the appropriate code generation strategy based on BPMN elements.
     * 
     * @param mainBpmnElements The main BPMN elements to analyze
     * @return The selected strategy
     */
    private CodeGenerationStrategy selectStrategy(BpmnElements mainBpmnElements) {
        if (collaborationCodeStrategy.canHandle(mainBpmnElements)) {
            log.debug("Selected CollaborationCodeStrategy");
            return collaborationCodeStrategy;
        } else if (standaloneProcessStrategy.canHandle(mainBpmnElements)) {
            log.debug("Selected StandaloneProcessStrategy");
            return standaloneProcessStrategy;
        } else {
            log.warn("No strategy can handle the BPMN elements, defaulting to StandaloneProcessStrategy");
            return standaloneProcessStrategy;
        }
    }

}