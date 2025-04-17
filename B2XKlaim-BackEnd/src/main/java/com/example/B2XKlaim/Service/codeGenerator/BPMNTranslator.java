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

 // Core Java Imports
 import java.io.FileNotFoundException;
 import java.io.UnsupportedEncodingException;
 import java.util.*;
 import java.util.stream.Collectors;
 
 // Lombok Imports
 import lombok.extern.slf4j.Slf4j;
 
 // Your BPMN Element Classes (Ensure all used types are imported)
 import com.example.B2XKlaim.Service.bpmnElements.*;
 import com.example.B2XKlaim.Service.bpmnElements.activities.*;
 import com.example.B2XKlaim.Service.bpmnElements.events.*;
 import com.example.B2XKlaim.Service.bpmnElements.flows.*;
 import com.example.B2XKlaim.Service.bpmnElements.gateways.*;
 import com.example.B2XKlaim.Service.bpmnElements.objects.pool.*;
 import com.example.B2XKlaim.Service.bpmnElements.objects.DO;
 


 // Core Java Imports

 import java.lang.reflect.InvocationTargetException;
 import java.lang.reflect.Method;

 
 
 /**
  * Translates BPMN elements into XKlaim code using the Visitor pattern.
  * Includes logic to traverse process flows and handle inter-participant parameters.
  */
 @Slf4j // Use Slf4j for logging
 public class BPMNTranslator implements Visitor {
 
     // --- Member Variables ---
     private final BpmnElements bpmnElements; // Reference to the parsed model container
     private final Map<String, Set<String>> requiredParticipantRefs; // Analysis results
     private final Map<String, PL> participantsMap; // Quick lookup for participants by ID
 
     // State for the current process being translated
     private String currentParticipantId = null;
     private PL currentParticipant = null;
     // Maps required Participant Name -> Generated Local Variable Name for the current process
     private Map<String, String> currentParamMap = new HashMap<>();
 
     // --- Constructor ---
 
     public BPMNTranslator(BpmnElements bpmnElements) {
         Objects.requireNonNull(bpmnElements, "BpmnElements object cannot be null");
         this.bpmnElements = bpmnElements;
         this.requiredParticipantRefs = bpmnElements.getRequiredParticipantRefs();
         this.participantsMap = bpmnElements.getParticipantsMap();
         log.info("BPMNTranslator initialized. Required Refs: {}", this.requiredParticipantRefs);
     }
 
     // --- Helper Methods ---
 
     private String getParticipantName(String participantId) {
         PL participant = participantsMap.get(participantId);
         return (participant != null) ? participant.getName() : "UNKNOWN_ID_" + participantId;
     }
 
     private String generateLocalVarName(String participantName) {
         if (participantName == null || participantName.isEmpty()) {
             return "unknown_loc";
         }
         return participantName.replaceAll("[^a-zA-Z0-9_]", "").toLowerCase() + "_loc";
     }
 
     private void resetProcessState() {
         this.currentParticipantId = null;
         this.currentParticipant = null;
         if (this.currentParamMap == null) { this.currentParamMap = new HashMap<>(); }
         this.currentParamMap.clear();
         log.trace("Process state reset.");
     }
 
      private void setupProcessState(PL participant) {
          resetProcessState();
          Objects.requireNonNull(participant, "Participant cannot be null for setupProcessState");
          currentParticipant = participant;
          currentParticipantId = participant.getId();
          log.debug("Setting up state for Participant: '{}' (ID: {})", participant.getName(), participant.getId());
 
          Set<String> neededRefs = requiredParticipantRefs.get(currentParticipantId);
          if (neededRefs != null && !neededRefs.isEmpty()) {
              log.debug("Participant '{}' requires references to IDs: {}", participant.getName(), neededRefs);
              for (String neededParticipantId : neededRefs) {
                  String participantName = getParticipantName(neededParticipantId);
                  if (!participantName.startsWith("UNKNOWN_ID")) {
                      String localVarName = generateLocalVarName(participantName);
                      currentParamMap.put(participantName, localVarName);
                      log.trace("Mapping required participant '{}' to local var '{}'", participantName, localVarName);
                  } else {
                       log.warn("Could not find name for required participant ID: {}. Cannot generate parameter.", neededParticipantId);
                  }
              }
          } else {
               log.debug("Participant '{}' requires no external references.", participant.getName());
          }
      }
 
     /**
      * Finds the appropriate "visit" method in THIS Translator instance.
      * Traverses superclasses if no exact match is found.
      */
     private Method getVisitMethod(Class<? extends BpmnElement> elementType) {
          Class<?> currentClass = elementType;
          while (currentClass != null && BpmnElement.class.isAssignableFrom(currentClass)) {
               try {
                   // Look for visit method accepting the current class type in THIS class
                   // Ensure the parameter type matches exactly (no primitive vs wrapper issues)
                   return this.getClass().getMethod("visit", currentClass);
               } catch (NoSuchMethodException e) {
                   currentClass = currentClass.getSuperclass();
                   if (currentClass == Object.class || currentClass == null) break;
               }
          }
          log.trace("No specific visit method found in BPMNTranslator for {} or its superclasses.", elementType.getName());
          // Attempt to find a more generic visit method if specific one fails
           try {
                 return this.getClass().getMethod("visit", BpmnElement.class); // Example fallback
             } catch (NoSuchMethodException e) {
                  log.warn("No visit method found for {} or its hierarchy.", elementType.getName());
                  return null;
             }
     }
 
    /**
     * Translates the body of a process starting from a given element,
     * traversing sequence flows recursively/iteratively. Delegates complex
     * control flow (gateways) to specific visit methods.
     * Applies optimization to remove adjacent out(FlowID)/in(FlowID) pairs if enabled (currently risky).
     *
     * @param startElement The element to start traversal from (usually a Start Event).
     * @return The generated XKlaim code string for the process body path.
     */
    public String translateProcessBody(BpmnElement startElement) throws FileNotFoundException, UnsupportedEncodingException {
        StringBuilder bodyCode = new StringBuilder();
        Set<BpmnElement> visitedElements = new HashSet<>();
        BpmnElement currentElement = startElement;

        while (currentElement != null && !visitedElements.contains(currentElement)) {
            visitedElements.add(currentElement);
            log.trace(">>> [Translator Traversal] Visiting Element: {} ({})", currentElement.getId(), currentElement.getClass().getSimpleName());

            Method visitMethod = getVisitMethod(currentElement.getClass());
            String elementResult = null;

            if (visitMethod != null) {
                try {
                    // 1. Visit the current element
                    elementResult = (String) visitMethod.invoke(this, currentElement);
                     if (elementResult != null) {
                        // Append element result now (Optimizer might remove previous out later)
                        bodyCode.append(elementResult);
                     } else {
                        log.warn("Visit method for {} returned null.", currentElement.getId());
                     }

                    // 2. Check Element Type for Control Flow Handling
                    boolean continueTraversal = true;
                    if (currentElement instanceof AND || currentElement instanceof XOR || currentElement instanceof LP) {
                        // Complex gateways (AND, XOR, LP) handle their own subsequent flow logic
                        // within their respective visit methods (visit(AND), visit(XOR), visit(LP)).
                        // The visit method for the gateway should generate the necessary code
                        // for branching/merging/looping and potentially trigger translation of sub-paths.
                        // Therefore, this linear traversal should stop here.
                        log.trace(">>> [Translator Traversal] Hit Gateway {}. visit method handles subsequent flow.", currentElement.getId());
                        continueTraversal = false;
                        currentElement = null; // Stop the while loop
                    }

                    // 3. If it's not a complex gateway, continue linear traversal
                    if (continueTraversal) {
                        String outgoingSequenceFlowId = currentElement.getOutgoingEdge();
                        log.trace(">>> [Translator Traversal] Outgoing Edge from {}: {}", currentElement.getId(), outgoingSequenceFlowId);

                        if (outgoingSequenceFlowId != null && !outgoingSequenceFlowId.isEmpty()) {
                            BpmnElement sequenceFlowElement = this.bpmnElements.getElementById(outgoingSequenceFlowId);
                            log.trace(">>> [Translator Traversal] Sequence Flow Element Found: {} ({})", (sequenceFlowElement!=null?sequenceFlowElement.getId():"null"), (sequenceFlowElement!=null?sequenceFlowElement.getClass().getSimpleName():"null"));

                            if (sequenceFlowElement instanceof SQ) {
                                // Visit SQ
                                Method sqVisitMethod = getVisitMethod(SQ.class);
                                String sqResult = null;
                                if (sqVisitMethod != null) {
                                    sqResult = (String) sqVisitMethod.invoke(this, sequenceFlowElement);
                                } else { log.warn("No visit method for SQ {}", outgoingSequenceFlowId); }

                                // --- Optional Optimization ---
                                // WARNING: Enabling this optimization is risky and may break logic.
                                boolean optimized = false;

                                if (!optimized && sqResult != null) {
                                    bodyCode.append(sqResult); // Append SQ code if not optimized away
                                }

                                // Find next element
                                currentElement = this.bpmnElements.getElementAfterSequenceFlow(outgoingSequenceFlowId);
                                log.trace(">>> [Translator Traversal] Next Element after Flow {}: {} ({})", outgoingSequenceFlowId, (currentElement!=null?currentElement.getId():"null"), (currentElement!=null?currentElement.getClass().getSimpleName():"null"));

                            } else {
                                log.warn("Element {} outgoing edge {} is not an SQ element or not found.", currentElement.getId(), outgoingSequenceFlowId);
                                currentElement = null; // Stop
                            }
                        } else {
                            // No outgoing edge -> End of this path
                            log.trace("Element {} has no outgoing edge. Ending traversal.", currentElement.getId());
                            currentElement = null; // Stop
                        }
                    } // End if(continueTraversal)

                } catch (Exception e) { // Catch errors during visit/reflection/traversal
                    log.error("Error during translation processing for element {}: {}",
                              (currentElement != null ? currentElement.getId() : "unknown"), e.getMessage(), e);
                    currentElement = null; // Stop traversal on error
                }
            } else { // No visit method found
                log.warn("No visit method found for element {} (Class: {}). Skipping.", currentElement.getId(), currentElement.getClass().getSimpleName());
                currentElement = null; // Stop traversal if element cannot be visited
            }
        } // End while loop

        if (currentElement != null && visitedElements.contains(currentElement)) {
             log.debug("Stopping traversal for element {} - already visited in this path (potential loop).", currentElement.getId());
        }

        log.warn(">>> translateProcessBody returning: [\n{}]", bodyCode.toString()); // Log final result
        return bodyCode.toString();
    }
 
 
     // --- Visitor Methods ---
 
    /**
     * Entry point for translating a collaboration. Generates the network definition
     * and then iterates through participants (sorted by name) to generate their
     * process definitions, including calls to Event Sub-Processes.
     */
    @Override
    public String visit(Collab collab) throws FileNotFoundException, UnsupportedEncodingException {
        StringBuilder collabCode = new StringBuilder();
        collabCode.append(String.format("net %s physical \"localhost:9999\" {\n\n", collab.getId()));

        List<PL> participants = bpmnElements.getElementsByType(PL.class);

        // Sort participants by name for consistent output order
        if (participants != null) {
             participants.sort(Comparator.comparing(PL::getName, String.CASE_INSENSITIVE_ORDER));
             log.debug("Sorted participants by name: {}", participants.stream().map(PL::getName).collect(Collectors.toList()));
        }

        // Phase 1: Generate node definitions
        log.info("Generating node definitions (sorted)...");
        for (PL participant : participants) {
             if (participant == null) continue;
            setupProcessState(participant);
            collabCode.append(participant.accept(this)); // Calls visit(PL)
        }
        resetProcessState();
        collabCode.append("}\n"); // Close net block

        // Phase 2: Generate process definitions
        log.info("Generating process definitions (sorted)...");
        for (PL participant : participants) {
             if (participant == null) continue;
            setupProcessState(participant); // Sets up currentParamMap

            // Generate proc signature
            String procParamsString = "";
            if (!currentParamMap.isEmpty()) {
                procParamsString = currentParamMap.values().stream()
                                .map(varName -> "Locality " + varName)
                                .sorted()
                                .collect(Collectors.joining(", "));
            }
            collabCode.append(String.format("\nproc %s(%s) {\n",
                    participant.getProcessName(),
                    procParamsString));

            // *** ADDED: Find and translate Event Sub-Processes for this process ***
            log.debug("Looking for Event Sub-Processes in process {}", participant.getProcessId());
            // Assumes helper method exists in BpmnElements
            List<ESP> eventSubProcesses = bpmnElements.getEventSubProcessesForProcess(participant.getProcessId());
            if (eventSubProcesses != null && !eventSubProcesses.isEmpty()) {
                 log.debug("Found {} ESPs for process {}. Generating eval calls.", eventSubProcesses.size(), participant.getProcessId());
                  for (ESP esp : eventSubProcesses) {
                       collabCode.append(esp.accept(this)); // Call visit(ESP) -> generates eval()
                  }
                  collabCode.append("\n"); // Add newline after ESP evals
            }
            // *** END ADDED ESP Handling ***

            // Generate the main process body using the traversal helper method
            List<BpmnElement> startEvents = bpmnElements.findStartEventsForProcess(participant.getProcessId());

            if (startEvents == null || startEvents.isEmpty()) {
                 log.warn("No main start event found for process '{}' (ID: {}). Process body will be empty (excluding ESPs).", participant.getProcessName(), participant.getProcessId());
                 collabCode.append("\n\t// No main start event found for this process.\n");
            } else {
                log.debug("Found {} main start event(s) for process '{}'. Translating body...", startEvents.size(), participant.getProcessName());
                for (BpmnElement startEvent : startEvents) {
                    // Call the traversal method starting from the start event
                    collabCode.append(this.translateProcessBody(startEvent)); // Append the generated body
                }
            }

            collabCode.append("}\n"); // Close proc definition
            resetProcessState();
        }

        log.warn(">>> visit(Collab) returning full code:\n{}", collabCode.toString());
        return collabCode.toString();
    }

 
     @Override
     public String visit(PL pl) throws FileNotFoundException, UnsupportedEncodingException {
         // Check and potentially recover state (keep this part)
         if (currentParticipant == null || !Objects.equals(currentParticipant.getId(), pl.getId())) {
              log.error("Visiting PL '{}' but translator state is not set up correctly! Expected state for {}.", pl.getName(), pl.getId());
              setupProcessState(pl);
         }
 
         String participantName = pl.getName();
         String processName = pl.getProcessName();
 
         // Build the inner argument string (comma-separated names, or empty)
         String argsString = ""; // Default to empty args string INSIDE parentheses
         if (!currentParamMap.isEmpty()) {
             Set<String> neededRefsIds = requiredParticipantRefs.get(pl.getId());
              if (neededRefsIds != null) {
                  List<String> neededNames = neededRefsIds.stream()
                          .map(this::getParticipantName)
                          .filter(name -> !name.startsWith("UNKNOWN_ID"))
                          .sorted()
                          .collect(Collectors.toList());
                  // Join names with ", " only if neededNames is not empty
                  if (!neededNames.isEmpty()) {
                     argsString = String.join(", ", neededNames);
                  }
              }
         }
         // Now argsString is either "" or "robot2" or "robot1, robot2", etc.
 
         // Log using the inner arguments string
         log.info(">>> visit(PL): Formatting node. ID='{}', Name='{}', Process='{}', Args='{}'",
                  pl.getId(), participantName, processName, argsString);
 
         // Check for null names before formatting
         if (participantName == null || processName == null) {
              log.error(">>> visit(PL) Error: Participant Name ('{}') or Process Name ('{}') is null for PL ID: {}",
                       participantName, processName, pl.getId());
              return "/* ERROR: Participant Name or Process Name missing for "+pl.getId()+" */\n";
         }
 
         // Updated nodeTemplate always includes parentheses for eval
         String nodeTemplate = "\tnode %s {\n" +
                 "\t\teval(new %s(%s))@self\n" + // <<< Always has (...) around the argsString placeholder
                 "\t}\n";
         // Format using the inner arguments string
         String result = String.format(nodeTemplate, participantName, processName, argsString);
 
         log.info(">>> visit(PL) Returning: [\n{}]", result);
         return result;
     }
 
     // --- Event Visitor Methods (Updated for Parameter Passing) ---
 
     @Override
     public String visit(NSE nse) {
         log.trace("Visiting NSE: {}", nse.getId());
         return String.format("out('%s')@self\n", nse.getOutgoingEdge());
     }
 
     @Override
     public String visit(MSE mse) {
         log.trace("Visiting MSE: {} (Msg: {})", mse.getId(), mse.getMessageId());
         return String.format("in('%s' %s)@self\nout('%s')@self\n",
                  mse.getMessageId(), "", mse.getOutgoingEdge());
     }
 
     @Override
     public String visit(SSE sse) {
          log.trace("Visiting SSE: {} (Sig: {})", sse.getId(), sse.getSignalId());
          String senderName = sse.getSignalSenderName();
          String targetLocation = "self";
 
          if (senderName != null && !senderName.equalsIgnoreCase("self") && currentParamMap.containsKey(senderName)) {
               targetLocation = currentParamMap.get(senderName);
          } else if (senderName != null && !senderName.equalsIgnoreCase("self")) {
               log.warn("SSE {}: Reading from sender '{}' but no Local param found. Using raw name.", sse.getId(), senderName);
               targetLocation = senderName;
          } else { targetLocation = "self"; }
 
          return String.format("read('%s' %s)@%s\nout('%s')@self\n",
                  sse.getSignalId(), "", targetLocation, sse.getOutgoingEdge());
     }
 
     @Override
     public String visit(MIC mic) {
         log.trace("Visiting MIC: {} (Msg: {})", mic.getId(), mic.getMessageId());
         return String.format("in('%s' %s)@self\nout('%s')@self\n",
                 mic.getMessageId(), "", mic.getOutgoingEdge());
     }
 
     @Override
     public String visit(SIC sic) {
          log.trace("Visiting SIC: {} (Sig: {})", sic.getId(), sic.getSignalId());
          String senderName = sic.getSignalSenderName();
          String targetLocation = "self";
 
          if (senderName != null && !senderName.equalsIgnoreCase("self") && currentParamMap.containsKey(senderName)) {
               targetLocation = currentParamMap.get(senderName);
          } else if (senderName != null && !senderName.equalsIgnoreCase("self")) {
               log.warn("SIC {}: Reading from sender '{}' but no Local param found. Using raw name.", sic.getId(), senderName);
               targetLocation = senderName;
          } else { targetLocation = "self"; }
 
         return String.format("read('%s' %s)@%s\nout('%s')@self\n",
                  sic.getSignalId(), "", targetLocation, sic.getOutgoingEdge());
     }
 
     @Override
     public String visit(MIT mit) {
         log.trace("Visiting MIT: {} (Msg: {})", mit.getId(), mit.getMessageId());
         MessageFLow flow = mit.getMessageFlow();
         String receiverName = (flow != null) ? flow.getReceiverName() : null;
         String targetLocation = "null";
 
         if (receiverName != null && currentParamMap.containsKey(receiverName)) {
              targetLocation = currentParamMap.get(receiverName);
         } else if (receiverName != null) {
              log.warn("MIT {}: Sending to receiver '{}' but no Local param found. Using raw name.", mit.getId(), receiverName);
              targetLocation = receiverName;
         } else { log.warn("MIT {}: Cannot determine receiver. Target set to 'null'.", mit.getId()); targetLocation = "null"; }
 
         return String.format("out('%s')@%s\nout('%s')@self\n",
                 mit.getMessageId(), targetLocation, mit.getOutgoingEdge());
     }
 
     @Override
     public String visit(SIT sit) {
         log.trace("Visiting SIT: {} (Sig: {})", sit.getId(), sit.getSignalId());
         return String.format("out('%s')@self\nThread.sleep(Signal_Duration)\nin('%s')@self\nout('%s')@self\n",
                 sit.getSignalId(), sit.getSignalId(), sit.getOutgoingEdge());
     }
 
     @Override
     public String visit(NEE nee) {
         log.trace("Visiting NEE: {}", nee.getId());
         return "\n";
     }
 
     @Override
     public String visit(MEE mee) {
          log.trace("Visiting MEE: {} (Msg: {})", mee.getId(), mee.getMessageId());
          MessageFLow flow = mee.getMessageFlow();
          String receiverName = (flow != null) ? flow.getReceiverName() : null;
          String targetLocation = "null";
 
          if (receiverName != null && currentParamMap.containsKey(receiverName)) {
              targetLocation = currentParamMap.get(receiverName);
          } else if (receiverName != null) {
              log.warn("MEE {}: Sending to receiver '{}' but no Local param found. Using raw name.", mee.getId(), receiverName);
              targetLocation = receiverName;
          } else { log.warn("MEE {}: Cannot determine receiver. Target set to 'null'.", mee.getId()); targetLocation = "null"; }
 
          return String.format("out('%s')@%s\n\n", mee.getMessageId(), targetLocation);
     }
 
      @Override
      public String visit(SEE see) {
          log.trace("Visiting SEE: {} (Sig: {})", see.getId(), see.getSignalId());
          return String.format("out('%s')@self\nThread.sleep(Signal_Duration)\nin('%s')@self\n\n",
                  see.getSignalId(), see.getSignalId());
      }
 
      @Override
      public String visit(TSE tse) throws FileNotFoundException, UnsupportedEncodingException {
         log.trace("Visiting TSE: {} (Duration: {})", tse.getId(), tse.getDuration());
         return String.format("Thread.sleep(%d)\nout('%s')@self\n", tse.getDuration(), tse.getOutgoingEdge());
      }
 
      @Override
      public String visit(TCE tce) throws FileNotFoundException, UnsupportedEncodingException {
         log.trace("Visiting TCE: {} (Duration: {})", tce.getId(), tce.getDuration());
          return String.format("Thread.sleep(%d)\nout('%s')@self\n", tce.getDuration(), tce.getOutgoingEdge());
      }
 
     @Override
     public String visit(TEE tee) throws FileNotFoundException, UnsupportedEncodingException {
         log.trace("Visiting TEE (Terminate End Event): {}", tee.getId());
         return "// Terminate Event\nexit @ self\n";
     }
 
     // --- Sequence Flow ---
     @Override
     public String visit(SQ sq) {
         log.trace("Visiting SQ: {}", sq.getId());
         // This generates the "in" for the completed sequence flow token
         return String.format("in('%s')@self\n\n", sq.getId());
     }
 
 
     @Override
     public String visit(CLA cla) throws FileNotFoundException, UnsupportedEncodingException {
          log.trace("Visiting CLA: {} (Calls: {})", cla.getId(), cla.getCalledProcess());
          String calledProcess = cla.getCalledProcess();
          String outgoingEdge = cla.getOutgoingEdge(); // Get the outgoing edge ID
 
         if (calledProcess == null || calledProcess.isEmpty()) {
              log.error("Call Activity {} is missing the 'calledElement' attribute.", cla.getId());
              return "// ERROR: Call Activity missing calledElement\n";
         }
         if (outgoingEdge == null || outgoingEdge.isEmpty()) {
             log.error("Call Activity {} ({}) has no outgoing edge defined. Cannot generate proper eval.", cla.getId(), calledProcess);
              // Return only eval without out, but this breaks flow
              return String.format("eval(new %s(/* ERROR: Missing outgoing edge */))@self\n", calledProcess);
         }
 
          // Generate eval, passing the outgoing edge ID as the first argument (as a string literal)
          // Add placeholder for other potential arguments
          return String.format("eval(new %s('%s'/* TODO: Pass other necessary args */))@self\n",
                               calledProcess,
                               outgoingEdge);
          // NOTE: The "out(outgoingEdge)@self" is intentionally REMOVED here.
     }
 
     @Override
     public String visit(ESP esp) {
         log.trace("Visiting ESP (as Eval): {} ({})", esp.getName(), esp.getId());
         String processToEval = esp.getName(); // Use ESP name for the proc
         if (processToEval == null || processToEval.isEmpty()) {
              log.warn("Event Sub-Process {} has no name. Cannot generate eval call.", esp.getId());
              return "// Event Sub-Process with ID " + esp.getId() + " has no name.\n";
         }
         // Generate eval for the named procedure.
         return String.format("eval(new %s())@self\n", processToEval);
     }


     @Override
     public String visit(AND and) throws FileNotFoundException, UnsupportedEncodingException {
        StringBuilder s = new StringBuilder();

        List<String> sequences = new ArrayList<>();

        for (List<String> branchElements : and.getFlowElementMap().values()) {

            for (int i = 0; i < branchElements.size(); i++) {
                String elementId = branchElements.get(i);
                BpmnElement element = bpmnElements.getElementById(elementId);
                if (element!=null) {
                    s.append(element.accept(this));

                    if (i < branchElements.size() - 1) {
                        BpmnElement sequence = bpmnElements.getElementById(element.getOutgoingEdge());
                        s.append(sequence.accept(this));
                    }
                    if (i == branchElements.size() - 1) {
                        sequences.add(element.getOutgoingEdge());
                    }
                }
            }
        }

        for (String sequenceId : sequences) {
            if (sequenceId != and.getOutgoingEdge()) {
                BpmnElement sequence = bpmnElements.getElementById(sequenceId);
                if (sequence != null) {
                    s.append(sequence.accept(this));
                }}
            }
        s.append(String.format("out('%s')@self\n", and.getOutgoingEdge()));


        return s.toString();
    }

    @Override
    public String visit(XOR xor) throws FileNotFoundException, UnsupportedEncodingException {
        StringBuilder s = new StringBuilder();

        if (xor.getConditionElementMap().size() != 2) {
            throw new IllegalArgumentException("XOR gateway must have two branches");
        }

        Iterator<Map.Entry<String, List<String>>> iterator = xor.getConditionElementMap().entrySet().iterator();

        Map.Entry<String, List<String>> trueBranch = iterator.next();
        Map.Entry<String, List<String>> falseBranch = iterator.next();

        // Condition for the true branch
        s.append(String.format("if(%s){\n  ", trueBranch.getKey()));

        // Translating elements for the true branch
        for (String elementId : trueBranch.getValue()) {
            BpmnElement element = bpmnElements.getElementById(elementId);
            s.append(element.accept(this));
            BpmnElement sequence = bpmnElements.getElementById(element.getOutgoingEdge());
            s.append(sequence.accept(this));
        }
        s.append("} else {\n  ");

        // Translating elements for the false branch
        for (String elementId : falseBranch.getValue()) {
            BpmnElement element = bpmnElements.getElementById(elementId);
            s.append(element.accept(this));
            BpmnElement sequence = bpmnElements.getElementById(element.getOutgoingEdge());
            s.append(sequence.accept(this));
        }
        s.append("}\n");

        s.append(String.format("out('%s')@self\n", xor.getOutgoingEdge()));

        return s.toString();
    }
 
 
     @Override
     public String visit(LP lp) throws FileNotFoundException, UnsupportedEncodingException {
         StringBuilder sb = new StringBuilder();
         sb.append("while(").append(lp.getCondition()).append("){\n");
         for (String elementId : lp.getFlowElementMap()) {
             BpmnElement element = bpmnElements.getElementById(elementId);
             sb.append(element.accept(this));
             BpmnElement sequence = bpmnElements.getElementById(element.getOutgoingEdge());
             sb.append(sequence.accept(this));
         }
         sb.append("}\n");
         sb.append("out('").append(lp.getOutgoingEdge()).append("')@self\n");
         return sb.toString();
     }
 
 
     @Override
     public String visit(ST st) {
         log.trace("Visiting ST (as Eval): {} ({})", st.getName(), st.getId());
 
         String processToEval = st.getName(); // Use the Script Task's name as the proc name
         String outgoingEdge = st.getOutgoingEdge(); // Get the outgoing edge ID
 
         if (processToEval == null || processToEval.isEmpty()) {
             log.warn("Script Task {} has no name. Cannot generate eval call.", st.getId());
             // If no name, cannot eval - how should flow continue? Output edge directly?
             if (outgoingEdge != null && !outgoingEdge.isEmpty()) {
                 return String.format("// Script Task with ID {} has no name defined.\nout('%s')@self\n",
                                      st.getId(), outgoingEdge);
             } else {
                  return String.format("// Script Task with ID {} has no name and no outgoing edge.\n", st.getId());
             }
         }
 
         if (outgoingEdge == null || outgoingEdge.isEmpty()) {
              log.error("Script Task {} ({}) has no outgoing edge defined. Cannot generate proper eval.", st.getId(), processToEval);
              // Return only eval without out, but this breaks flow
               return String.format("eval(new %s(/* ERROR: Missing outgoing edge */))@self\n",
                               processToEval);
         }
 
         // Generate eval, passing the outgoing edge ID as the first argument (as a string literal)
         // Add placeholder for other potential arguments
         return String.format("eval(new %s('%s'/* TODO: Pass other necessary args */))@self\n",
                              processToEval,
                              outgoingEdge);
         // NOTE: The "out(outgoingEdge)@self" is intentionally REMOVED here.
     }
 
 
     // --- Other Unsupported/Placeholder Types ---
     @Override
     public String visit(MIPL mipl) throws FileNotFoundException, UnsupportedEncodingException {
         log.warn("Visiting Multi-Instance Pool (MIPL) - Translation not implemented: {}", mipl.getId());
         return String.format("// Multi-Instance Pool %s translation not implemented\n", mipl.getId());
     }
 
     @Override
     public String visit(DO data) throws FileNotFoundException, UnsupportedEncodingException {
          log.warn("Visiting Data Object (DO) - Translation not implemented: {}", data.getId());
         return String.format("// Data Object %s translation not implemented\n", data.getId());
     }
 
 } 






