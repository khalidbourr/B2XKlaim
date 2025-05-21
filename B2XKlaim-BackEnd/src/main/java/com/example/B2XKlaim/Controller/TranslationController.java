package com.example.B2XKlaim.Controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher; // Import Optimizer
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.B2XKlaim.Service.Parser.BpmnParser;
import com.example.B2XKlaim.Service.bpmnElements.BpmnElement;
import com.example.B2XKlaim.Service.bpmnElements.BpmnElements;
import com.example.B2XKlaim.Service.bpmnElements.objects.pool.Collab;
import com.example.B2XKlaim.Service.bpmnElements.objects.pool.PL;
import com.example.B2XKlaim.Service.codeGenerator.BPMNTranslator;
import com.example.B2XKlaim.Service.codeGenerator.Generator;
import com.example.B2XKlaim.Service.codeGenerator.Optimizer;

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
        Map<String, List<String>> eventSubProcesses = new HashMap<>();

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

                          String rawFullProcCode = String.format("proc %s() {\n%s\n}", processName, optimizedBodyString);

                          // *** Format the proc code ***
                           String indent = "  ";
                           String formattedCode = formatProcessCode(rawFullProcCode, indent); 

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
            eventSubProcesses = codeGenerator.translateEventSubProcesses();


            // --- Assemble Final Result ---
            resultMap.put("collaboration", collaborationCode); 
            resultMap.put("processes", processesList);        
            resultMap.put("callActivities", callActivityTranslations);
            resultMap.put("scriptTaskProcs", scriptTaskPlaceholders);
            resultMap.put("eventSubProcesses", eventSubProcesses); 
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
        if (fullCode == null || fullCode.isEmpty()) {
            return processes;
        }

        // Regex to find ONLY the start of the proc definition up to the opening brace '{'
        // Captures Name (group 1) and optional Parameters (group 2)
        Pattern procStartPattern = Pattern.compile("proc\\s+([\\w\\d_]+)\\s*(\\([^)]*\\))?\\s*\\{");
        Matcher matcher = procStartPattern.matcher(fullCode);

        int searchStart = 0;
        // Optimization: Try to start searching after the net block
        int netBlockEnd = fullCode.indexOf('}', fullCode.indexOf('{', fullCode.indexOf("net "))) + 1;
        if (netBlockEnd > 0) {
            searchStart = netBlockEnd;
            log.debug("Starting process block search after potential net block at index {}", searchStart);
        } else {
             log.debug("Net block end not found or net block missing, starting process search from beginning.");
        }

        while (matcher.find(searchStart)) {
            String processName = matcher.group(1);
            int procStartIndex = matcher.start();       // Index where "proc" starts
            int openingBraceIndex = matcher.end() - 1;  // Index of the opening '{'
            int bodyStartIndex = matcher.end();         // Index immediately after '{'

            log.debug("Found potential proc start for '{}' at index {}", processName, procStartIndex);

            int braceCount = 1; // Start count at 1 for the opening brace we just matched
            int closingBraceIndex = -1;

            // Start searching for the matching closing brace
            for (int i = bodyStartIndex; i < fullCode.length(); i++) {
                char currentChar = fullCode.charAt(i);
                if (currentChar == '{') {
                    braceCount++;
                } else if (currentChar == '}') {
                    braceCount--;
                }

                // Check if we found the matching closing brace
                if (braceCount == 0) {
                    closingBraceIndex = i;
                    log.trace("Found matching closing brace for '{}' at index {}", processName, closingBraceIndex);
                    break; // Exit the inner character loop
                }
            }

            if (processName != null && closingBraceIndex != -1) {
                // Extract the full block from the start of "proc" to the closing brace
                String fullProcBlock = fullCode.substring(procStartIndex, closingBraceIndex + 1);

                log.debug("Extracted proc block for: {}", processName);
                Map<String, String> processEntry = new HashMap<>();
                processEntry.put("name", processName);
                processEntry.put("code", fullProcBlock.trim()); // Store the full, trimmed proc block
                processes.add(processEntry);

                // Update searchStart to look for the *next* proc *after* this one ends
                searchStart = closingBraceIndex + 1;

            } else {
                // Could not find matching closing brace - log error and stop searching here
                // to avoid potential infinite loops if code is malformed.
                log.error("Could not find matching closing brace for proc '{}' starting at index {}. Stopping extraction.", processName, procStartIndex);
                log.warn("Code searched was:\n---\n{}\n---", fullCode.substring(searchStart)); // Log remaining code
                break; // Exit the while loop
            }
        } // End while matcher.find

         if (processes.isEmpty() && !fullCode.trim().startsWith("net")) {
            // Added check: If code doesn't start with 'net' maybe it's just procs? Log if still empty.
             log.warn("Could not extract any 'proc' blocks. Check translator output and parsing logic.");
             log.warn("Code searched:\n---\n{}\n---", fullCode);
         } else if (!processes.isEmpty()){
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