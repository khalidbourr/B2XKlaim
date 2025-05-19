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

// Element Imports
import com.example.B2XKlaim.Service.bpmnElements.*;
import com.example.B2XKlaim.Service.bpmnElements.activities.CLA;
import com.example.B2XKlaim.Service.bpmnElements.activities.ESP;
import com.example.B2XKlaim.Service.bpmnElements.activities.ST;
import com.example.B2XKlaim.Service.bpmnElements.objects.pool.Collab;

// Util Imports
import lombok.extern.slf4j.Slf4j;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Orchestrates high-level translation tasks, using BPMNTranslator for element-specific logic.
 */
@Slf4j
public class Generator {

    private final BpmnElements processDiagram;
    private final BPMNTranslator visitor;

    /**
     * Constructor for Generator.
     * @param processDiagram The populated BpmnElements object
     */
    public Generator(BpmnElements processDiagram) {
        Objects.requireNonNull(processDiagram, "processDiagram cannot be null");
        this.processDiagram = processDiagram;
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
                result.computeIfAbsent(taskName, k -> new ArrayList<>()).add(codeString);
                processedTaskNames.add(taskName);
            } else if (taskName == null || taskName.isEmpty()) {
                 log.warn("Script Task with ID {} has no name. Cannot generate placeholder proc.", st.getId());
            }
        }
        return result;
    }


    /**
     * Generates placeholder code for BPMN Call Activities.
     * Called by the controller to provide stubs for the frontend.
     */
    public Map<String, List<String>> translateCallActivity() throws InvocationTargetException, IllegalAccessException {
        Map<String, List<String>> result = new HashMap<>();
        List<BpmnElement> callActivities = getAllCallActivity(processDiagram);
        Set<String> processedCalledElements = new HashSet<>(); // Keep track of processed IDs

        for (BpmnElement callActivity : callActivities) {
             if (!(callActivity instanceof CLA)) continue;
            CLA callAct = (CLA) callActivity;
            String calledProcessId = callAct.getCalledProcess(); // Process Name/ID to call

            // Only generate placeholder if this calledProcessId hasn't been processed yet
            if (calledProcessId != null && !processedCalledElements.contains(calledProcessId)) {
                String codeString = "proc " + calledProcessId + "(String edge){\n\n" +
                        " /* Placeholder implementation for Call Activity: " + calledProcessId + " */ \n" +
                        " /* Add logic representing the called process */ \n\n" +
                        " out(edge)@self\n" +
                        "}";
                // computeIfAbsent is still fine, it will create the list once
                result.computeIfAbsent(calledProcessId, k -> new ArrayList<>()).add(codeString);
                processedCalledElements.add(calledProcessId); // Mark this ID as processed
                log.debug("Generated placeholder for Call Activity target: {}", calledProcessId);
            } else if (calledProcessId != null) {
                 log.trace("Placeholder for Call Activity target '{}' already generated, skipping duplicate.", calledProcessId);
            }
        }
        return result;
    }

    

    // --- Helper methods to get specific element types (Corrected for type safety) ---

    // Removed getAllStartEvents as it wasn't used directly by the simplified Generator

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