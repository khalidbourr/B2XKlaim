/*
 * Copyright 2023 Khalid BOURR
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

 package com.example.B2XKlaim.Service.codeGenerator;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.example.B2XKlaim.Service.bpmnElements.BpmnElement;
import com.example.B2XKlaim.Service.bpmnElements.BpmnElements;
import com.example.B2XKlaim.Service.bpmnElements.activities.CLA;
import com.example.B2XKlaim.Service.bpmnElements.activities.ESP;
import com.example.B2XKlaim.Service.bpmnElements.activities.ST;
import com.example.B2XKlaim.Service.bpmnElements.objects.pool.Collab;

import lombok.extern.slf4j.Slf4j;

/**
 * Orchestrates high-level translation tasks, using BPMNTranslator for element-specific logic.
 */
@Slf4j
public class Generator {

    private final BpmnElements processDiagram;
    private final BPMNTranslator visitor;
    private final Map<String, BpmnElements> allProcesses;

    /**
     * Constructor for Generator.
     * @param processDiagram The populated BpmnElements object
     */
    public Generator(BpmnElements processDiagram) {
        Objects.requireNonNull(processDiagram, "processDiagram cannot be null");
        this.processDiagram = processDiagram;
        this.visitor = new BPMNTranslator(processDiagram);
        this.allProcesses = null;
    }

    public Generator(Map<String, BpmnElements> allProcesses) {
        Objects.requireNonNull(allProcesses, "allProcesses cannot be null");
        this.allProcesses = allProcesses;
        this.processDiagram = allProcesses.get("main");
        Objects.requireNonNull(this.processDiagram, "Main process ('main' key) is required in allProcesses");
        this.visitor = new BPMNTranslator(processDiagram);
    }

    /**
     * Translates the entire collaboration into a list of XKlaim code lines.
     * This should be the primary method called by the controller.
     */
    public List<String> translateBpmnCollaboration() throws FileNotFoundException, UnsupportedEncodingException {
        List<BpmnElement> collaborationElements = getCollaboration(processDiagram);
        if (collaborationElements.isEmpty()) {
            log.warn("No Collaboration element found in the BPMN diagram.");
            return Collections.emptyList();
        }
        // Assuming only one collaboration element
        BpmnElement collab = collaborationElements.get(0);
        if (collab instanceof Collab) {
            // visit(Collab) generates the full net + procs structure
            String translation = visitor.visit((Collab) collab);
            if (translation == null) {
                 log.error("BPMNTranslator.visit(Collab) returned null.");
                 return Collections.emptyList();
            }
            return Arrays.asList(translation.split("\\r?\\n")); // Split into lines
        } else {
             log.error("Top-level element is not a Collab object: {}", collab.getClass().getName());
             return Collections.emptyList();
        }
    }

    /**
     * Generates placeholder/stub proc definitions for each uniquely named Script Task.
     * Called by the controller to provide stubs for the frontend.
     */
    public Map<String, List<String>> translateST() throws InvocationTargetException, IllegalAccessException {
        Map<String, List<String>> result = new HashMap<>();
        
        // Process script tasks from main diagram
        List<BpmnElement> scriptTasks = getAllScriptTasks(processDiagram);
        Set<String> processedTaskNames = new HashSet<>();

        for (BpmnElement scriptTask : scriptTasks) {
            if (!(scriptTask instanceof ST)) continue;
            ST st = (ST) scriptTask;
            String taskName = st.getName();

            if (taskName != null && !taskName.isEmpty() && !processedTaskNames.contains(taskName)) {
                // Basic placeholder proc definition
                String codeString = "proc " + taskName + "(String edge){\n\n" +
                        " /* Placeholder implementation for Script Task '" + taskName + "' */ \n" +
                        " /* Add logic representing the script task */ \n\n" +
                        " out(edge)@self\n" +
                        "}";
                //result.computeIfAbsent(taskName, k -> new ArrayList<>()).add(codeString);
                result.put(taskName, Collections.singletonList(codeString));
                processedTaskNames.add(taskName);
            } else if (taskName == null || taskName.isEmpty()) {
                 log.warn("Script Task with ID {} has no name. Cannot generate placeholder proc.", st.getId());
            }
        }
        
        // Process script tasks from other processes (call activities)
        if (allProcesses != null) {
            for (Map.Entry<String, BpmnElements> entry : allProcesses.entrySet()) {
                if (!entry.getKey().equals("main")) {
                    BpmnElements process = entry.getValue();
                    List<BpmnElement> processScriptTasks = getAllScriptTasks(process);
                    
                    for (BpmnElement scriptTask : processScriptTasks) {
                        if (!(scriptTask instanceof ST)) continue;
                        ST st = (ST) scriptTask;
                        String taskName = st.getName();
                        
                        if (taskName != null && !taskName.isEmpty() && !processedTaskNames.contains(taskName)) {
                            String codeString = "proc " + taskName + "(String edge){\n\n" +
                                    " /* Placeholder implementation for Script Task '" + taskName + "' */ \n" +
                                    " /* From process: " + entry.getKey() + " */ \n" +
                                    " /* Add logic representing the script task */ \n\n" +
                                    " out(edge)@self\n" +
                                    "}";
                            result.put(taskName, Collections.singletonList(codeString));
                            processedTaskNames.add(taskName);
                        }
                    }
                }
            }
        }
        
        return result;
    }



        /**
         * Generates standalone process definitions for each Event Sub-Process.
         * Called by the controller to provide ESPs for the frontend.
         */
        public Map<String, List<String>> translateEventSubProcesses() throws FileNotFoundException, UnsupportedEncodingException {
            Map<String, List<String>> result = new HashMap<>();
            Set<String> processedESPNames = new HashSet<>();
            
            // Process ESPs from main diagram
            List<BpmnElement> eventSubProcesses = getAllEventSubProcesses(processDiagram);
            
            for (BpmnElement eventSubProcess : eventSubProcesses) {
                if (!(eventSubProcess instanceof ESP)) continue;
                ESP esp = (ESP) eventSubProcess;
                
                // Use name if available, otherwise ID
                String espName = esp.getName() != null && !esp.getName().isEmpty() ? 
                                esp.getName() : esp.getId();
                
                if (processedESPNames.contains(espName)) continue;
                processedESPNames.add(espName);
                
                // Generate the raw ESP process definition
                String rawEspCode = visitor.visit(esp);
                
                log.info("Applying optimizer to event sub-process '{}'...", espName);
                List<String> espCodeLines = Arrays.asList(rawEspCode.split("\\r?\\n"));
                List<String> optimizedEspLines = Optimizer.optimize(espCodeLines);
                String optimizedEspCode = String.join("\n", optimizedEspLines);
                
                // Add the optimized code to the result map
                //result.computeIfAbsent(espName, k -> new ArrayList<>()).add(optimizedEspCode);
                result.put(espName, Collections.singletonList(optimizedEspCode));
                
                log.debug("Generated optimized Event Sub-Process: {} ({})", espName, esp.getId());
            }
            
            // Process ESPs from other processes (call activities)
            if (allProcesses != null) {
                for (Map.Entry<String, BpmnElements> entry : allProcesses.entrySet()) {
                    if (!entry.getKey().equals("main")) {
                        BpmnElements process = entry.getValue();
                        List<BpmnElement> processESPs = getAllEventSubProcesses(process);
                        
                        for (BpmnElement eventSubProcess : processESPs) {
                            if (!(eventSubProcess instanceof ESP)) continue;
                            ESP esp = (ESP) eventSubProcess;
                            
                            String espName = esp.getName() != null && !esp.getName().isEmpty() ? 
                                            esp.getName() : esp.getId();
                            
                            if (processedESPNames.contains(espName)) continue;
                            processedESPNames.add(espName);
                            
                            // Create a visitor for this specific process
                            BPMNTranslator processVisitor = new BPMNTranslator(process);
                            String rawEspCode = processVisitor.visit(esp);
                            
                            log.info("Applying optimizer to event sub-process '{}' from process '{}'...", espName, entry.getKey());
                            List<String> espCodeLines = Arrays.asList(rawEspCode.split("\\r?\\n"));
                            List<String> optimizedEspLines = Optimizer.optimize(espCodeLines);
                            String optimizedEspCode = String.join("\n", optimizedEspLines);
                            
                            result.put(espName, Collections.singletonList(optimizedEspCode));
                            
                            log.debug("Generated optimized Event Sub-Process: {} ({}) from process: {}", espName, esp.getId(), entry.getKey());
                        }
                    }
                }
            }
            
            return result;
        }


    /**
     * Generates placeholder code for BPMN Call Activities.
     * Called by the controller to provide stubs for the frontend.
     */
    public Map<String, List<String>> translateCallActivity() throws FileNotFoundException, UnsupportedEncodingException, InvocationTargetException, IllegalAccessException {
        Map<String, List<String>> result = new HashMap<>();
        Map<String, List<String>> subActivities = new HashMap<>();
        Set<String> processedCallActivities = new HashSet<>();

        for (BpmnElement callActivity : getAllCallActivity(processDiagram)) {
            if (!(callActivity instanceof CLA)) continue;
            CLA cla = (CLA) callActivity;
            String calledProcessId = cla.getCalledProcess();
            if (processedCallActivities.contains(calledProcessId)) continue;
            processedCallActivities.add(calledProcessId);

            if (allProcesses != null && allProcesses.containsKey(calledProcessId)) {
                // AUTO-TRANSLATE the referenced process
                BpmnElements calledProcess = allProcesses.get(calledProcessId);
                BPMNTranslator translator = new BPMNTranslator(calledProcess);

                List<BpmnElement> startEvents = calledProcess.getAllStartEvents();
                StringBuilder processBody = new StringBuilder();
                for (BpmnElement startEvent : startEvents) {
                    String body = translator.translateProcessBody(startEvent);
                    processBody.append(body);
                }

                // Apply optimizer to the generated body
                List<String> bodyLines = Arrays.asList(processBody.toString().split("\\r?\\n"));
                List<String> optimizedBodyLines = Optimizer.optimize(bodyLines);
                String optimizedBody = String.join("\n", optimizedBodyLines);

                String codeString = "import klava.Locality\n\n" +
                        "proc " + calledProcessId + "(String edge) {\n\n" +
                        optimizedBody + "\n\n" +
                        "  out(edge)@self\n" +
                        "}";

                //result.computeIfAbsent(calledProcessId, k -> new ArrayList<>()).add(codeString);
                if (!result.containsKey(calledProcessId)) {
                    result.put(calledProcessId, Collections.singletonList(codeString));
                }

                // Note: Script tasks and ESPs from called processes are already handled
                // by translateST() and translateEventSubProcesses() when they process
                // all processes. We don't need to collect them here to avoid duplicates.

            } else {
                String placeholder = "import klava.Locality\n\n" +
                        "proc " + calledProcessId + "(String edge) {\n\n" +
                        "  /* TODO: Process '" + calledProcessId + "' not found */\n\n" +
                        "  out(edge)@self\n" +
                        "}";
                if (!result.containsKey(calledProcessId)) {
                    result.put(calledProcessId, Collections.singletonList(placeholder));
                }
            }
        }

        // Only return call activities, not their sub-elements
        return result;
    }
    

    private static List<BpmnElement> getCollaboration(BpmnElements bpmnElements) {
        List<Collab> collaborationElements = bpmnElements.getElementsByType(Collab.class);
        return new ArrayList<>(collaborationElements);
    }

    private List<BpmnElement> getAllEventSubProcesses(BpmnElements bpmnElements) {
        List<ESP> eventSubProcessList = bpmnElements.getElementsByType(ESP.class);
        return new ArrayList<>(eventSubProcessList);
    }

    private List<BpmnElement> getAllCallActivity(BpmnElements bpmnElements) {
        List<CLA> callActivityList = bpmnElements.getElementsByType(CLA.class);
        return new ArrayList<>(callActivityList);
    }

     private List<BpmnElement> getAllScriptTasks(BpmnElements bpmnElements) {
        List<ST> scriptTaskList = bpmnElements.getElementsByType(ST.class);
        return new ArrayList<>(scriptTaskList);
    }

    public BPMNTranslator getVisitor() {
        return visitor;
    }

}
