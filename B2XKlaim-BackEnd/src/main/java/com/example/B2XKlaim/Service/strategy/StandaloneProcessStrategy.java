package com.example.B2XKlaim.Service.strategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.example.B2XKlaim.Service.CodeGenerationService.ProcessCode;
import com.example.B2XKlaim.Service.bpmnElements.BpmnElement;
import com.example.B2XKlaim.Service.bpmnElements.BpmnElements;
import com.example.B2XKlaim.Service.bpmnElements.objects.pool.Collab;
import com.example.B2XKlaim.Service.codeGenerator.BPMNTranslator;
import com.example.B2XKlaim.Service.codeGenerator.Generator;
import com.example.B2XKlaim.Service.codeGenerator.Optimizer;

import lombok.extern.slf4j.Slf4j;

/**
 * Strategy for generating code when BPMN contains only standalone processes.
 * Handles process-only code generation without collaboration elements.
 */
@Component
@Slf4j
public class StandaloneProcessStrategy implements CodeGenerationStrategy {

    @Override
    public String generateCollaborationCode(Generator codeGenerator, BpmnElements mainBpmnElements) throws Exception {
        log.debug("Generating collaboration code using StandaloneProcessStrategy (no collaboration)");
        
        // For standalone processes, there's no collaboration
        return "// No collaboration defined in BPMN input.";
    }

    @Override
    public List<ProcessCode> generateProcesses(Generator codeGenerator, BpmnElements mainBpmnElements, 
                                              String collaborationCode) throws Exception {
        log.debug("Generating standalone processes using StandaloneProcessStrategy");
        
        List<ProcessCode> processes = new ArrayList<>();
        
        Map<String, List<BpmnElement>> processStartEvents = new HashMap<>();
        List<BpmnElement> allStartEvents = mainBpmnElements.getAllStartEvents();
        
        log.debug("Found {} start events for standalone process generation", allStartEvents.size());
        
        for (BpmnElement startEvent : allStartEvents) {
            String processId = startEvent.getProcessId();
            if (processId != null) {
                processStartEvents.computeIfAbsent(processId, k -> new ArrayList<>()).add(startEvent);
                log.trace("Added start event for process: {}", processId);
            }
        }

        if (!processStartEvents.isEmpty()) {
            BPMNTranslator translator = codeGenerator.getVisitor();

            for (Map.Entry<String, List<BpmnElement>> entry : processStartEvents.entrySet()) {
                String processId = entry.getKey();
                List<BpmnElement> startEventsForProcess = entry.getValue();
                String processName = startEventsForProcess.get(0).getProcessName();
                
                if (processName == null) {
                    processName = "Process_" + processId;
                }

                log.debug("Generating code for process: {} (ID: {})", processName, processId);

                StringBuilder processBodyCode = new StringBuilder();
                for (BpmnElement startEvent : startEventsForProcess) {
                    String bodyPart = translator.translateProcessBody(startEvent);
                    processBodyCode.append(bodyPart);
                    log.trace("Added body part for start event in process: {}", processName);
                }

                // Apply optimizer
                List<String> bodyLines = List.of(processBodyCode.toString().split("\\r?\\n"));
                List<String> optimizedBodyLines = Optimizer.optimize(bodyLines);
                String optimizedBodyString = String.join("\n", optimizedBodyLines);

                String fullProcCode = String.format("proc %s() {\n%s\n}", processName, optimizedBodyString);
                processes.add(new ProcessCode(processName, fullProcCode));
                
                log.debug("Generated code for process: {} ({} lines)", processName, optimizedBodyLines.size());
            }
        }

        log.debug("Generated {} standalone processes", processes.size());
        return processes;
    }

    @Override
    public Set<String> extractParticipants(BpmnElements mainBpmnElements) {
        log.debug("Extracting participants from standalone processes (no collaboration participants expected)");
        
        // For standalone processes, there are typically no collaboration participants
        // Return empty set
        return new HashSet<>();
    }

    @Override
    public boolean canHandle(BpmnElements mainBpmnElements) {
        List<Collab> collabElements = mainBpmnElements.getElementsByType(Collab.class);
        boolean canHandle = collabElements.isEmpty();
        
        log.debug("StandaloneProcessStrategy canHandle: {} (found {} collaboration elements)", 
                 canHandle, collabElements.size());
        
        return canHandle;
    }

    @Override
    public String getStrategyName() {
        return "StandaloneProcessStrategy";
    }
}