package com.example.B2XKlaim.Controller;

import com.example.B2XKlaim.Service.Parser.BpmnParser;
import com.example.B2XKlaim.Service.bpmnElements.BpmnElement;
import com.example.B2XKlaim.Service.bpmnElements.BpmnElements;
import com.example.B2XKlaim.Service.bpmnElements.objects.pool.PL;
import com.example.B2XKlaim.Service.codeGenerator.Generator;
import com.example.B2XKlaim.Service.codeGenerator.Optimizer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * Controller for handling BPMN to X-Klaim translation requests.
 */
@CrossOrigin(origins = "*")
@RestController
public class TranslationController {

    /**
     * Generates X-Klaim code from BPMN XML.
     *
     * @param bpmnXml The BPMN XML content
     * @return ResponseEntity containing the generated X-Klaim code
     */
    @PostMapping("/generate-code")
    public ResponseEntity<Map<String, Object>> generateCode(@RequestBody String bpmnXml) {
        try {
            BpmnParser parser = new BpmnParser();
            BpmnElements bpmnElements = parser.parse(bpmnXml);
            Generator code = new Generator(bpmnElements);
            
            // Translate and optimize collaboration
            List<String> collaborationTranslation = code.translateBpmnCollaboration();
            Optimizer optimizer = new Optimizer();
            List<String> optimizedCollabo = optimizer.optimize(collaborationTranslation);
            
            // Translate processes
            Map<String, List<String>> processesTranslations = code.translateBPMNProcess();
            
            // Extract participants from BPMN using name instead of ID
            Set<String> participants = new HashSet<>();
            for (BpmnElement element : bpmnElements.getElementsByElementType(BpmnElements.ElementType.PL)) {
                if (element instanceof PL) {
                    // Use participant name instead of ID
                    String participantName = ((PL) element).getName();
                    if (participantName != null && !participantName.isEmpty()) {
                        participants.add(participantName);
                    }
                }
            }
            
            // Optimize each process code
            List<Map<String, String>> processesList = new ArrayList<>();
            for (Map.Entry<String, List<String>> entry : processesTranslations.entrySet()) {
                List<String> rawProcessCode = entry.getValue();
                List<String> optimizedProcessCode = optimizer.optimize(rawProcessCode);
                
                StringBuilder processCodeBuilder = new StringBuilder();
                String processName = entry.getKey();
                
                processCodeBuilder.append("proc ").append(processName).append("(){\n\n");
                for (String line : optimizedProcessCode) {
                    processCodeBuilder.append(" ").append(line).append("\n");
                }
                processCodeBuilder.append("}");
                
                Map<String, String> processEntry = new HashMap<>();
                processEntry.put("name", processName);
                processEntry.put("code", processCodeBuilder.toString());
                processEntry.put("isParticipant", String.valueOf(participants.contains(processName)));
                processesList.add(processEntry);
            }
            
            // Translate call activities
            Map<String, List<String>> callActivityTranslations = code.translateCallActivity();
            
            // Create result map
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("collaboration", String.join("\n", optimizedCollabo));
            resultMap.put("processes", processesList);
            resultMap.put("callActivities", callActivityTranslations);
            resultMap.put("participants", new ArrayList<>(participants));
            
            return new ResponseEntity<>(resultMap, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}