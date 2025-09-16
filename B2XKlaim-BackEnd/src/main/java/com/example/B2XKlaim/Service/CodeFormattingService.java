package com.example.B2XKlaim.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Service responsible for extracting and formatting XKlaim code blocks.
 * Handles collaboration extraction, process block parsing, and code indentation.
 */
@Service
@Slf4j
public class CodeFormattingService {

    /**
     * Extracts the 'net ... { ... }' block from the full generated code.
     * 
     * @param fullCode The complete generated XKlaim code
     * @return The extracted collaboration block
     */
    public String extractCollaborationBlock(String fullCode) {
        log.debug("Extracting collaboration block from generated code");
        
        int netStart = fullCode.indexOf("net ");
        if (netStart == -1) {
            log.warn("Could not find 'net ' block start");
            return "";
        }
        
        int braceCount = 0;
        int netEnd = -1;
        boolean foundFirstBrace = false;
        
        for (int i = netStart; i < fullCode.length(); i++) {
            if (fullCode.charAt(i) == '{') {
                if (!foundFirstBrace) foundFirstBrace = true;
                braceCount++;
            } else if (fullCode.charAt(i) == '}') {
                braceCount--;
                if (foundFirstBrace && braceCount == 0) {
                    netEnd = i;
                    break;
                }
            }
        }
        
        if (netEnd != -1) {
            String collaborationBlock = fullCode.substring(netStart, netEnd + 1).trim();
            log.debug("Successfully extracted collaboration block ({} characters)", collaborationBlock.length());
            return collaborationBlock;
        } else {
            log.warn("Could not find matching closing brace for 'net' block");
            return fullCode.substring(netStart).trim();
        }
    }

    /**
     * Extracts the 'proc ... { ... }' blocks from the full generated code.
     * 
     * @param fullCode The complete generated XKlaim code
     * @return List of process entries with name and code
     */
    public List<Map<String, String>> extractProcessBlocks(String fullCode) {
        log.debug("Extracting process blocks from generated code");
        
        List<Map<String, String>> processes = new ArrayList<>();
        if (fullCode == null || fullCode.isEmpty()) {
            return processes;
        }

        // Regex to find proc definitions
        Pattern procStartPattern = Pattern.compile("proc\\s+([\\w\\d_]+)\\s*(\\([^)]*\\))?\\s*\\{");
        Matcher matcher = procStartPattern.matcher(fullCode);

        int searchStart = 0;
        // Start searching after the net block if it exists
        int netBlockEnd = findNetBlockEnd(fullCode);
        if (netBlockEnd > 0) {
            searchStart = netBlockEnd;
            log.debug("Starting process block search after net block at index {}", searchStart);
        }

        while (matcher.find(searchStart)) {
            String processName = matcher.group(1);
            int procStartIndex = matcher.start();
            int openingBraceIndex = matcher.end() - 1;
            int bodyStartIndex = matcher.end();

            log.trace("Found proc '{}' at index {}", processName, procStartIndex);

            int closingBraceIndex = findMatchingClosingBrace(fullCode, bodyStartIndex);

            if (processName != null && closingBraceIndex != -1) {
                String fullProcBlock = fullCode.substring(procStartIndex, closingBraceIndex + 1);
                
                Map<String, String> processEntry = new HashMap<>();
                processEntry.put("name", processName);
                processEntry.put("code", fullProcBlock.trim());
                processes.add(processEntry);

                searchStart = closingBraceIndex + 1;
                log.trace("Extracted proc block for: {}", processName);
            } else {
                log.error("Could not find matching closing brace for proc '{}' at index {}", 
                         processName, procStartIndex);
                break;
            }
        }

        log.debug("Extracted {} process blocks", processes.size());
        return processes;
    }

    /**
     * Applies proper indentation and spacing to XKlaim proc code.
     * 
     * @param rawCode The unindented code block
     * @param indentString The indentation string (e.g., "  ")
     * @return The formatted code with proper indentation and spacing
     */
    public String formatProcessCode(String rawCode, String indentString) {
        if (rawCode == null || rawCode.isEmpty()) {
            return "";
        }

        log.trace("Formatting process code ({} characters)", rawCode.length());
        
        // First, clean and normalize the code
        String cleanedCode = cleanAndNormalizeCode(rawCode);
        
        StringBuilder indentedCode = new StringBuilder();
        int currentIndentLevel = 0;
        String[] lines = cleanedCode.trim().split("\\r?\\n");

        for (int i = 0; i < lines.length; i++) {
            String trimmedLine = lines[i].trim();
            
            if (trimmedLine.isEmpty()) {
                indentedCode.append("\n");
                continue;
            }

            // Decrease indent level before processing closing braces and else statements
            if (trimmedLine.equals("}") || trimmedLine.startsWith("} else")) {
                if (currentIndentLevel > 0) {
                    currentIndentLevel--;
                }
            }

            // Apply indentation (skip for first "proc..." line)
            boolean isProcStart = i == 0 && trimmedLine.startsWith("proc ");
            if (!isProcStart) {
                indentedCode.append(indentString.repeat(currentIndentLevel));
            }

            // Format the line content for better readability
            String formattedLine = formatLineContent(trimmedLine);
            indentedCode.append(formattedLine);

            // Add proper line breaks
            if (shouldAddExtraLineBreak(formattedLine, i, lines)) {
                indentedCode.append("\n\n");
            } else {
                indentedCode.append("\n");
            }

            // Increase indent level after processing opening braces
            if (formattedLine.endsWith("{") && !formattedLine.startsWith("/*") && !formattedLine.startsWith("//")) {
                currentIndentLevel++;
            }
            
            // Handle immediate block close (e.g., proc foo() {})
            if (formattedLine.contains("{") && formattedLine.endsWith("}") && 
                !formattedLine.startsWith("/*") && !formattedLine.startsWith("//")) {
                if (currentIndentLevel > 0) {
                    currentIndentLevel--;
                }
            }
        }

        return indentedCode.toString().trim() + "\n";
    }

    /**
     * Formats a list of raw process blocks with proper indentation.
     * 
     * @param rawProcesses List of process maps with name and code
     * @param indent The indentation string
     * @return List of formatted process maps
     */
    public List<Map<String, String>> formatProcessList(List<Map<String, String>> rawProcesses, String indent) {
        log.debug("Formatting {} process blocks", rawProcesses.size());
        
        List<Map<String, String>> formattedProcesses = new ArrayList<>();
        
        for (Map<String, String> rawProcess : rawProcesses) {
            String processName = rawProcess.get("name");
            String rawCode = rawProcess.get("code");
            String formattedCode = formatProcessCode(rawCode, indent);

            Map<String, String> processEntry = new HashMap<>();
            processEntry.put("name", processName);
            processEntry.put("code", formattedCode);
            formattedProcesses.add(processEntry);
        }

        return formattedProcesses;
    }

    private int findNetBlockEnd(String fullCode) {
        int netIndex = fullCode.indexOf("net ");
        if (netIndex == -1) return 0;
        
        int openBrace = fullCode.indexOf('{', netIndex);
        if (openBrace == -1) return 0;
        
        return findMatchingClosingBrace(fullCode, openBrace + 1) + 1;
    }

    private int findMatchingClosingBrace(String code, int startIndex) {
        int braceCount = 1;
        
        for (int i = startIndex; i < code.length(); i++) {
            char currentChar = code.charAt(i);
            if (currentChar == '{') {
                braceCount++;
            } else if (currentChar == '}') {
                braceCount--;
                if (braceCount == 0) {
                    return i;
                }
            }
        }
        
        return -1; // No matching brace found
    }

    /**
     * Cleans and normalizes the code by removing extra whitespace and fixing common formatting issues.
     */
    private String cleanAndNormalizeCode(String rawCode) {
        if (rawCode == null || rawCode.isEmpty()) {
            return "";
        }

        // Normalize line endings and remove excessive whitespace
        String cleaned = rawCode
            .replaceAll("\\r\\n", "\n")  // Normalize line endings
            .replaceAll("\\r", "\n")     // Normalize line endings
            .replaceAll("\\n\\s*\\n\\s*\\n+", "\n\n") // Reduce multiple empty lines to double
            .replaceAll("\\s+$", "")     // Remove trailing whitespace from each line
            .trim();

        return cleaned;
    }

    /**
     * Formats individual line content for better readability.
     */
    private String formatLineContent(String line) {
        if (line == null || line.isEmpty()) {
            return line;
        }

        String formatted = line;

        // Add spaces around control structures
        formatted = formatted.replaceAll("if\\s*\\(", "if (");
        formatted = formatted.replaceAll("else\\s*\\{", "else {");
        formatted = formatted.replaceAll("\\}\\s*else\\s*\\{", "} else {");

        // Add spaces around operators but preserve existing spacing in comments
        if (!formatted.trim().startsWith("/*") && !formatted.trim().startsWith("//")) {
            // Add space after commas if not already present
            formatted = formatted.replaceAll(",(?!\\s)", ", ");
            
            // Fix spacing around @ symbols in X-Klaim specific syntax
            formatted = formatted.replaceAll("\\)\\s*@\\s*", ")@");
            
            // Ensure proper spacing in eval statements
            formatted = formatted.replaceAll("eval\\s*\\(", "eval(");
            formatted = formatted.replaceAll("new\\s+", "new ");
            
            // Clean up excessive spaces but preserve intentional indentation
            formatted = formatted.replaceAll("\\s{2,}", " ").trim();
        }

        return formatted;
    }

    /**
     * Determines if an extra line break should be added after the current line.
     */
    private boolean shouldAddExtraLineBreak(String currentLine, int currentIndex, String[] allLines) {
        if (currentLine == null || currentLine.isEmpty()) {
            return false;
        }

        // Add extra line break after proc declaration
        if (currentLine.matches("proc\\s+\\w+.*\\{")) {
            return true;
        }

        // Add extra line break before else statements
        if (currentIndex < allLines.length - 1) {
            String nextLine = allLines[currentIndex + 1].trim();
            if (nextLine.startsWith("else") || nextLine.equals("}")) {
                return false; // Don't add extra space before else or closing brace
            }
        }

        // Add extra line break after closing braces (except the final one)
        if (currentLine.equals("}") && currentIndex < allLines.length - 1) {
            String nextLine = allLines[currentIndex + 1].trim();
            // Don't add extra space if next line is another closing brace or else
            if (!nextLine.equals("}") && !nextLine.startsWith("else")) {
                return true;
            }
        }

        return false;
    }
}