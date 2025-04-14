package com.example.B2XKlaim.Controller;

import com.example.B2XKlaim.Service.Parser.BpmnParser;
import com.example.B2XKlaim.Service.bpmnElements.BpmnElement;
import com.example.B2XKlaim.Service.bpmnElements.BpmnElements;
import com.example.B2XKlaim.Service.bpmnElements.objects.pool.Collab;
import com.example.B2XKlaim.Service.bpmnElements.objects.pool.PL;
import com.example.B2XKlaim.Service.codeGenerator.BPMNTranslator;
import com.example.B2XKlaim.Service.codeGenerator.Generator;
import com.example.B2XKlaim.Service.codeGenerator.Optimizer; // Import Optimizer

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

@CrossOrigin(origins = "*")
@RestController
@Slf4j
public class TranslationController {

    @PostMapping("/generate-code")
    public ResponseEntity<Map<String, Object>> generateCode(@RequestBody String bpmnXml) {
        Map<String, Object> resultMap = new HashMap<>();
        String collaborationCode = ""; // Default to empty
        List<Map<String, String>> processesList = new ArrayList<>(); // Final list for response
        Set<String> participants = new HashSet<>();
        Map<String, List<String>> callActivityTranslations = new HashMap<>();
        Map<String, List<String>> scriptTaskPlaceholders = new HashMap<>();

        try {
            log.info("Received request to generate code.");
            // 1. Parse
            BpmnParser parser = new BpmnParser();
            BpmnElements bpmnElements = parser.parse(bpmnXml);

            // 2. Analyze Interactions
            log.info("Analyzing interactions...");
            bpmnElements.analyzeInteractions();

            // 3. Create Generator and Optimizer
            Generator codeGenerator = new Generator(bpmnElements);
            Optimizer optimizer = new Optimizer(); // Instantiate Optimizer

            // --- Check if Collaboration Exists ---
            List<Collab> collabElements = bpmnElements.getElementsByType(Collab.class);

            if (!collabElements.isEmpty()) {
                // --- Collaboration Exists: Translate and OPTIMIZE ---
                log.info("Collaboration found. Translating full collaboration...");
                List<String> fullXklaimCodeLines = codeGenerator.translateBpmnCollaboration();

                // *** Apply Optimizer (ENABLED AS REQUESTED) ***
                log.info("Applying optimizer to collaboration output...");
                List<String> fullXklaimCodeLinesOptimized = Optimizer.optimize(fullXklaimCodeLines); // Static call
                List<String> codeToParse = fullXklaimCodeLinesOptimized; // Use OPTIMIZED code
                // *** End Apply Optimizer ***

                String fullCodeString = String.join("\n", codeToParse);
                log.debug("Full OPTIMIZED code string length (collab): {}", fullCodeString.length());

                // Extract blocks from the potentially optimized string
                collaborationCode = extractCollaborationBlock(fullCodeString);
                List<Map<String, String>> rawProcessesList = extractProcessBlocks(fullCodeString);

                // Format the extracted process code
                String indent = "  "; // Define indentation (e.g., 2 spaces)
                for(Map<String, String> rawProcess : rawProcessesList) {
                     String processName = rawProcess.get("name");
                     String rawCode = rawProcess.get("code");
                     String formattedCode = formatProcessCode(rawCode, indent); // <<< Apply Formatter

                     Map<String, String> processEntry = new HashMap<>();
                     processEntry.put("name", processName);
                     processEntry.put("code", formattedCode); // Store formatted code
                     processesList.add(processEntry);
                }

                // Extract participant names
                for (PL plElement : bpmnElements.getElementsByType(PL.class)) {
                    String participantName = plElement.getName();
                    if (participantName != null && !participantName.isEmpty()) {
                        participants.add(participantName);
                    }
                }
                log.debug("Extracted participants: {}", participants);

            } else {
                // --- No Collaboration: Translate Processes and OPTIMIZE ---
                log.info("No collaboration found. Translating defined process(es)...");
                collaborationCode = "// No collaboration defined in BPMN input.";

                Map<String, List<BpmnElement>> processStartEvents = new HashMap<>();
                List<BpmnElement> allStartEvents = bpmnElements.getAllStartEvents();
                 for (BpmnElement startEvent : allStartEvents) {
                    String processId = startEvent.getProcessId();
                     if (processId != null) {
                          processStartEvents.computeIfAbsent(processId, k -> new ArrayList<>()).add(startEvent);
                     } else { log.warn("Start Event {} has no processId!", startEvent.getId()); }
                 }

                 if (processStartEvents.isEmpty()) {
                      log.warn("No collaboration and no start events found. No processes to translate.");
                 } else {
                     BPMNTranslator translator = codeGenerator.getVisitor();

                     for (Map.Entry<String, List<BpmnElement>> entry : processStartEvents.entrySet()) {
                          String processId = entry.getKey();
                          List<BpmnElement> startEventsForProcess = entry.getValue();
                          String processName = startEventsForProcess.get(0).getProcessName();
                          if (processName == null) { 
                            processName = "Process_" + processId; 
                            log.warn("Process name not found for process ID: {}. Using default name.", processId); 
                        }                          StringBuilder processBodyCode = new StringBuilder();
                           log.debug("Translating body for process '{}' (ID: {})...", processName, processId);
                          for (BpmnElement startEvent : startEventsForProcess) {
                               processBodyCode.append(translator.translateProcessBody(startEvent));
                          }

                          // *** Apply optimizer to the generated body ***
                          log.info("Applying optimizer to process body for '{}'...", processName);
                          List<String> bodyLines = Arrays.asList(processBodyCode.toString().split("\\r?\\n"));
                          List<String> optimizedBodyLines = Optimizer.optimize(bodyLines); // Static call
                          String optimizedBodyString = String.join("\n", optimizedBodyLines);
                          // *** End Apply optimizer ***

                          // Manually format the proc definition using OPTIMIZED body
                          String rawFullProcCode = String.format("proc %s() {\n%s\n}", processName, optimizedBodyString);

                          // *** Format the proc code ***
                           String indent = "  ";
                           String formattedCode = formatProcessCode(rawFullProcCode, indent); // <<< Apply Formatter

                          Map<String, String> processEntry = new HashMap<>();
                          processEntry.put("name", processName);
                          processEntry.put("code", formattedCode); // Store formatted code
                          processesList.add(processEntry);
                     }
                 }
            }

            // --- Common parts: Get Placeholders ---
            callActivityTranslations = codeGenerator.translateCallActivity();
            scriptTaskPlaceholders = codeGenerator.translateST();
            log.debug("Generated {} call activity and {} script task placeholders.",
                      callActivityTranslations.size(), scriptTaskPlaceholders.size());

            // --- Assemble Final Result ---
            resultMap.put("collaboration", collaborationCode); // Contains formatted net block (if collab existed)
            resultMap.put("processes", processesList);         // Contains formatted proc blocks
            resultMap.put("callActivities", callActivityTranslations);
            resultMap.put("scriptTaskProcs", scriptTaskPlaceholders);
            resultMap.put("participants", new ArrayList<>(participants));

            log.info("Code generation successful (Optimizer Enabled).");
            return new ResponseEntity<>(resultMap, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error during code generation: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Translation failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // --- Helper Methods For Formatting ---
    // (Includes extractCollaborationBlock, extractProcessBlocks, formatProcessCode)

    /**
     * Extracts the 'net ... { ... }' block from the full generated code.
     */
    private String extractCollaborationBlock(String fullCode) {
        int netStart = fullCode.indexOf("net ");
        if (netStart == -1) { log.warn("Could not find 'net ' block start."); return ""; }
        int braceCount = 0;
        int netEnd = -1;
        boolean foundFirstBrace = false;
        for (int i = netStart; i < fullCode.length(); i++) {
            if (fullCode.charAt(i) == '{') { if (!foundFirstBrace) foundFirstBrace = true; braceCount++; }
            else if (fullCode.charAt(i) == '}') { braceCount--; if (foundFirstBrace && braceCount == 0) { netEnd = i; break; } }
        }
        if (netEnd != -1) { return fullCode.substring(netStart, netEnd + 1).trim(); }
        else { log.warn("Could not find matching closing brace for 'net' block."); return fullCode.substring(netStart).trim(); }
    }

    /**
     * Extracts the 'proc ... { ... }' blocks from the full generated code using Regex.
     */

    /**
     * Extracts the 'proc ... { ... }' blocks from the full generated code using Regex.
     * Uses a greedy quantifier for the body to capture nested structures correctly.
     */
    private List<Map<String, String>> extractProcessBlocks(String fullCode) {
        List<Map<String, String>> processes = new ArrayList<>();
        // Regex: Find "proc", capture name (group 1), capture optional params (group 2),
        // capture body using GREEDY quantifier (.*) between the first { and the last } (group 3)
        // Pattern.DOTALL allows '.' to match newline characters.
        Pattern pattern = Pattern.compile("proc\\s+([\\w\\d_]+)\\s*(\\([^)]*\\))?\\s*\\{(.*)\\}", Pattern.DOTALL); // <<< Changed .*? to .*
        Matcher matcher = pattern.matcher(fullCode);

        int searchStart = 0;
        // Try to start searching after the net block to avoid matching "proc" in comments there
        int netBlockEnd = fullCode.indexOf('}', fullCode.indexOf('{', fullCode.indexOf("net "))) + 1;
        if (netBlockEnd > 0) {
            searchStart = netBlockEnd;
            log.debug("Starting process block search after potential net block at index {}", searchStart);
        } else {
             log.debug("Net block end not found or net block missing, starting process search from beginning.");
        }


        while (matcher.find(searchStart)) {
            String processName = matcher.group(1); // Group 1: Process Name (e.g., "mission1")
            String fullProcBlock = matcher.group(0); // Group 0: The entire matched block "proc ... }"

            if (processName != null) {
                log.debug("Regex found proc block for: {}", processName);
                Map<String, String> processEntry = new HashMap<>();
                processEntry.put("name", processName);
                processEntry.put("code", fullProcBlock.trim()); // Store the full, trimmed proc block
                processes.add(processEntry);
                searchStart = matcher.end(); // Continue search after the end of this match
            } else {
                 // This part of the regex should always capture group 1 if it matches "proc name"
                 // If it gets here, the regex matched unexpectedly. Move search forward.
                 searchStart = matcher.start() + 1;
            }
        }

         if (processes.isEmpty()) {
            log.warn("Could not extract any 'proc' blocks using regex from the generated code. Check translator output and regex pattern.");
            log.warn("Code searched:\n---\n{}\n---", fullCode); // Log the string that was searched
         } else {
              log.debug("Extracted {} process blocks.", processes.size());
         }
        return processes;
    }

    /**
     * Applies basic indentation to a block of XKlaim proc code.
     * @param rawCode The unindented code block string (including proc Name{...})
     * @param indentString The string to use for each indentation level (e.g., "  " or "\t")
     * @return The indented code string.
     */
    private String formatProcessCode(String rawCode, String indentString) {
        if (rawCode == null || rawCode.isEmpty()) { return ""; }
        StringBuilder indentedCode = new StringBuilder();
        int currentIndentLevel = 0;
        String[] lines = rawCode.trim().split("\\r?\\n");

        for (int i = 0; i < lines.length; i++) {
            String trimmedLine = lines[i].trim();
            if (trimmedLine.isEmpty()) { // Handle potentially empty lines
                indentedCode.append("\n");
                continue;
            }

            // Decrease indent level BEFORE processing line starting with '}' or 'else'
            if (trimmedLine.equals("}") || trimmedLine.startsWith("}") || trimmedLine.startsWith("else")) {
                 if (currentIndentLevel > 0) { currentIndentLevel--; }
            }

            // Append indentation (skip for first "proc..." line)
            boolean isProcStart = i == 0 && trimmedLine.startsWith("proc ");
            if (!isProcStart) {
                 indentedCode.append(indentString.repeat(currentIndentLevel));
            }

            indentedCode.append(trimmedLine).append("\n"); // Append the trimmed line and newline

            // Increase indent level AFTER processing line ending with '{'
            if (trimmedLine.endsWith("{") && !trimmedLine.startsWith("/*") && !trimmedLine.startsWith("//")) {
                currentIndentLevel++;
            }
             // Handle immediate block close e.g. proc foo() {}
             if (trimmedLine.contains("{") && trimmedLine.endsWith("}") && !trimmedLine.startsWith("/*") && !trimmedLine.startsWith("//")) {
                  if(currentIndentLevel > 0) currentIndentLevel--; // Decrease level if opened and closed on same line
             }
        }
        return indentedCode.toString();
    }

}