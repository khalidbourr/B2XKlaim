package com.example.B2XKlaim.Service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.B2XKlaim.Service.Parser.BpmnParser;
import com.example.B2XKlaim.Service.bpmnElements.BpmnElements;

import lombok.extern.slf4j.Slf4j;

/**
 * Service responsible for parsing BPMN XML content into BpmnElements objects.
 * Handles both single and multi-process parsing scenarios.
 */
@Service
@Slf4j
public class BpmnParsingService {

    /**
     * Parses multiple BPMN processes from a map of process IDs to XML content.
     * 
     * @param processes Map of process ID to XML content
     * @return Map of process ID to parsed BpmnElements
     * @throws RuntimeException if parsing fails for any process
     */
    public Map<String, BpmnElements> parseMultipleProcesses(Map<String, Object> processes) {
        log.info("Parsing {} BPMN processes", processes.size());
        
        Map<String, BpmnElements> parsedProcesses = new HashMap<>();
        BpmnParser parser = new BpmnParser();

        for (Map.Entry<String, Object> entry : processes.entrySet()) {
            String processId = entry.getKey();
            String xml = (String) entry.getValue();
            
            try {
                log.debug("Parsing process: {}", processId);
                BpmnElements elements = parser.parse(xml);
                elements.analyzeInteractions();
                parsedProcesses.put(processId, elements);
                log.debug("Successfully parsed process: {}", processId);
            } catch (Exception e) {
                log.error("Error parsing process '{}': {}", processId, e.getMessage(), e);
                throw new RuntimeException("Failed to parse process: " + processId, e);
            }
        }

        log.info("Successfully parsed all {} processes", parsedProcesses.size());
        return parsedProcesses;
    }

    /**
     * Parses a single BPMN XML content.
     * 
     * @param xml The BPMN XML content
     * @return Parsed BpmnElements object
     * @throws RuntimeException if parsing fails
     */
    public BpmnElements parseSingleProcess(String xml) {
        log.info("Parsing single BPMN process");
        
        try {
            BpmnParser parser = new BpmnParser();
            BpmnElements elements = parser.parse(xml);
            elements.analyzeInteractions();
            log.info("Successfully parsed single process");
            return elements;
        } catch (Exception e) {
            log.error("Error parsing single process: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to parse BPMN process", e);
        }
    }
}