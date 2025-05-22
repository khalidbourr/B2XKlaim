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

 package com.example.B2XKlaim.Service.bpmnElements; 

 // Core Imports
 import java.util.*;
 import java.util.stream.Collectors;
 import java.util.stream.Stream;
 
 // Lombok Imports
 import lombok.*;
 import lombok.extern.slf4j.Slf4j;

import com.example.B2XKlaim.Service.bpmnElements.activities.ESP;
import com.example.B2XKlaim.Service.bpmnElements.events.MSE;
import com.example.B2XKlaim.Service.bpmnElements.events.NSE;
import com.example.B2XKlaim.Service.bpmnElements.events.SIC;
 import com.example.B2XKlaim.Service.bpmnElements.events.SSE;
import com.example.B2XKlaim.Service.bpmnElements.events.TSE;
import com.example.B2XKlaim.Service.bpmnElements.flows.MessageFLow;
 import com.example.B2XKlaim.Service.bpmnElements.flows.SQ;
 import com.example.B2XKlaim.Service.bpmnElements.objects.pool.PL;
 
 /**
  * Container for BPMN elements parsed from a model.
  * Includes logic to analyze inter-participant interactions for XKlaim generation.
  */
 @Data 
 @Builder
 @Slf4j 
 public class BpmnElements {
 
     /**
      * Enum representing the different types of BPMN elements, primarily for filtering.
      * This was needed by your TranslationController.
      */
     public enum ElementType {
         NSE, MSE, SSE, MIC, SIC, MIT, SIT, NEE, MEE, SEE, TSE, TCE, TEE,
         XOR, AND, LP, EB,
         CLA, ESP, ST,
         SQ,
         PL, MIPL, 
         DO, 
         COLLAB, 
         MessageFLow 
         
     }
 
     @Builder.Default // Initialize map if using builder
     private Map<String, BpmnElement> elementsById = new HashMap<>();
 
     /**
      * Stores the results of interaction analysis.
      * Key: Participant ID that needs references.
      * Value: Set of Participant IDs it needs references to.
      * Populated by analyzeInteractions(). Marked transient for builder/serialization if needed.
      */
     @Getter // Lombok getter
     private transient Map<String, Set<String>> requiredParticipantRefs = new HashMap<>();
 
  
     /**
      * Default constructor. Initializes internal maps.
      */
     public BpmnElements() {
         this.elementsById = new HashMap<>();
         this.requiredParticipantRefs = new HashMap<>();
     }
 

     
     /**
      * Constructor used by Lombok's @Builder.
      * @param elementsById Map of elements parsed.
      */
     @Builder // Annotation needed here if using @Builder on class
     public BpmnElements(Map<String, BpmnElement> elementsById) {
         this.elementsById = (elementsById != null) ? elementsById : new HashMap<>();
         // requiredParticipantRefs will be populated by analyzeInteractions() after construction
         this.requiredParticipantRefs = new HashMap<>();
     }
 
    /**
     * Constructor that accepts both elements and connections.
     * @param elementsById Map of elements parsed.
     * @param connections Map of participant references.
     */
    public BpmnElements(Map<String, BpmnElement> elementsById, Map<String, Set<String>> connections) {
        this.elementsById = (elementsById != null) ? elementsById : new HashMap<>();
        this.requiredParticipantRefs = (connections != null) ? connections : new HashMap<>();
    }

     // --- Core Methods ---
 
     /**
      * Adds a parsed BPMN element to the container.
      * @param element The BpmnElement to add.
      */
     public void addElement(BpmnElement element) {
         if (element != null && element.getId() != null) {
             elementsById.put(element.getId(), element);
         } else {
             log.warn("Attempted to add a null element or element with null ID.");
         }
     }
 
     /**
      * Retrieves a BPMN element by its ID.
      * @param id The ID of the element.
      * @return The BpmnElement, or null if not found.
      */
     public BpmnElement getElementById(String id) {
         return elementsById.get(id);
     }
 
     /**
      * Gets the target element ID for a given Sequence Flow (SQ) ID.
      * Uses the 'target' field from your SQ class.
      * @param sequenceFlowId The ID of the SQ element.
      * @return The ID of the target element, or null if not found or input is not an SQ.
      */
     public String getTargetIdForSequenceFlow(String sequenceFlowId) {
         if (sequenceFlowId == null) {
             return null;
         }
         BpmnElement element = elementsById.get(sequenceFlowId);
         if (element instanceof SQ) {
             // Use direct field access or getter from SQ class
             return ((SQ) element).getTarget(); // Accessing the 'target' field
         }
         log.warn("getTargetIdForSequenceFlow called with ID '{}' which is not an SQ element.", sequenceFlowId);
         return null;
     }
 
     /**
      * Gets the element immediately following a given Sequence Flow (SQ).
      * @param sequenceFlowId The ID of the SQ element.
      * @return The target BpmnElement, or null if not found.
      */
     public BpmnElement getElementAfterSequenceFlow(String sequenceFlowId) {
         String targetId = getTargetIdForSequenceFlow(sequenceFlowId);
         return targetId != null ? getElementById(targetId) : null;
     }
 
     /**
      * Retrieves all elements of a specific type using Class object.
      * @param elementType The Class of the desired element type.
      * @param <T> The type of the element.
      * @return A List of elements of the specified type.
      */
     public <T extends BpmnElement> List<T> getElementsByType(Class<T> elementType) {
         return elementsById.values().stream()
                 .filter(Objects::nonNull) // Add null check for safety
                 .filter(elementType::isInstance)
                 .map(elementType::cast)
                 .collect(Collectors.toList());
     }
 
     /**
      * Retrieves all elements matching a specific ElementType enum constant.
      * Compares enum name against the element's class simple name.
      * @param elementType The ElementType enum constant.
      * @return A List of matching BpmnElement objects.
      */
     public List<BpmnElement> getElementsByElementType(ElementType elementType) {
         if (elementType == null) return Collections.emptyList();
         return elementsById.values().stream()
                 .filter(Objects::nonNull)
                 .filter(element -> elementType.name().equals(element.getClass().getSimpleName()))
                 .collect(Collectors.toList());
     }
 
     /**
      * Provides a stream of all contained BPMN elements.
      * @return A Stream<BpmnElement>.
      */
     public Stream<BpmnElement> elementStream() {
         return elementsById.values().stream();
     }
 
     // --- Interaction Analysis ---
 
     /**
      * Analyzes message flows and signal catches to determine which participants
      * need location references to other participants for XKlaim generation.
      * This method should be called AFTER all BPMN elements have been parsed
      * and added via addElement().
      */
     public void analyzeInteractions() {
         // Ensure requiredParticipantRefs is initialized
         if (this.requiredParticipantRefs == null) {
              this.requiredParticipantRefs = new HashMap<>();
         }
         this.requiredParticipantRefs.clear(); // Reset analysis results
         log.info("Starting interaction analysis...");
 
         List<MessageFLow> messageFlows = getElementsByType(MessageFLow.class);
         // Get all participants using the map for efficient lookup later
         Map<String, PL> participantsMap = getParticipantsMap(); // Use the helper method
 
         // 1. Analyze Message Flows
         // For out(msg)@receiverLoc pattern, the sender needs the receiver's location.
         log.debug("Analyzing {} message flows...", messageFlows.size());
         for (MessageFLow flow : messageFlows) {
              if (flow == null) continue; 
             String senderId = flow.getSenderId();
             String receiverId = flow.getReceiverId();
 
             // Check if it's an inter-participant flow
             if (senderId != null && receiverId != null && !senderId.equals(receiverId)) {
                 // The sender participant needs a reference to the receiver participant
                 requiredParticipantRefs.computeIfAbsent(senderId, k -> new HashSet<>()).add(receiverId);
                 log.info("Interaction Analysis: Participant '{}' (ID: {}) needs reference to '{}' (ID: {}) (for MessageFlow {})",
                         getParticipantName(senderId, participantsMap), senderId,
                         getParticipantName(receiverId, participantsMap), receiverId, flow.getId());
             } else {
                 log.debug("Skipping MessageFlow {}: sender/receiver same or null (Sender ID: {}, Receiver ID: {})", flow.getId(), senderId, receiverId);
             }
         }
 
         // 2. Analyze Signal Catch Events (SSE, SIC)
         // For read(signal)@senderLoc pattern, the catcher needs the sender's location.
         log.debug("Analyzing Signal Catch Events...");
         // Combine filtering for SSE and SIC
         List<BpmnElement> signalCatchEvents = elementsById.values().stream()
                 .filter(el -> el instanceof SSE || el instanceof SIC)
                 .collect(Collectors.toList());
 
         for (BpmnElement element : signalCatchEvents) {
              if (element == null) continue; // Safety check
             String catcherElementId = element.getId();
             // *** CRITICAL DEPENDENCY: getParticipantIdForElement ***
             // Pass the participantsMap for efficiency if the helper uses it
             String catcherParticipantId = getParticipantIdForElement(catcherElementId, participantsMap);
 
             if (catcherParticipantId == null) {
                 log.warn("Could not determine participant for Signal Catch Event: {}. Skipping analysis for this event.", catcherElementId);
                 continue; // Skip if we don't know who is catching
             }
 
             String signalId = "";
             String senderParticipantName = null; // Name provided by factory's analysis
 
             // Use specific getters assuming they exist in SSE/SIC classes
             try {
                  if (element instanceof SSE) {
                      senderParticipantName = ((SSE) element).getSignalSenderName();
                      signalId = ((SSE) element).getSignalId();
                  } else { // SIC
                      senderParticipantName = ((SIC) element).getSignalSenderName();
                      signalId = ((SIC) element).getSignalId();
                  }
             } catch (Exception e) {
                  log.error("Error getting signal info from catch event {} ({}): {}", catcherElementId, element.getClass().getSimpleName(), e.getMessage());
                  continue; // Skip if we can't get needed info
             }
 
 
             PL catcherParticipant = participantsMap.get(catcherParticipantId);
             String catcherParticipantName = (catcherParticipant != null) ? catcherParticipant.getName() : "UNKNOWN";
 
             // Check if the sender is known, not "self", and actually different from the catcher
             if (senderParticipantName != null && !senderParticipantName.equalsIgnoreCase("self") && !senderParticipantName.equals(catcherParticipantName)) {
 
                 // Find the participant ID for the sender name using the list derived from the map
                 String senderParticipantId = findParticipantIdByName(senderParticipantName, new ArrayList<>(participantsMap.values()));
 
                 if (senderParticipantId != null) {
                     // The catcher participant needs a reference to the sender participant
                     requiredParticipantRefs.computeIfAbsent(catcherParticipantId, k -> new HashSet<>()).add(senderParticipantId);
                     log.info("Interaction Analysis: Participant '{}' (ID: {}) needs reference to '{}' (ID: {}) (for Signal '{}' catch by {})",
                             catcherParticipantName, catcherParticipantId,
                             senderParticipantName, senderParticipantId,
                             signalId, catcherElementId);
                 } else {
                     log.warn("Could not find participant ID for sender name '{}' referenced by Signal Catch Event {}. Parameter passing may fail.", senderParticipantName, catcherElementId);
                 }
             } else {
                 log.debug("Skipping Signal Catch {}: Sender is self, null, or same as catcher (Sender Name: {}, Catcher Name: {})", catcherElementId, senderParticipantName, catcherParticipantName);
             }
         }
         log.info("Interaction Analysis Complete. Required Refs: {}", requiredParticipantRefs);
     }
 
     // --- Helper Methods ---
 
     /**
      * Retrieves the participant name for a given participant ID using a pre-built map.
      * @param participantId The ID of the participant.
      * @param participantsMap A map of participant ID to PL object.
      * @return The participant name, or "UNKNOWN" if not found.
      */
     private String getParticipantName(String participantId, Map<String, PL> participantsMap) {
         PL p = participantsMap.get(participantId);
         // Use getName() generated by Lombok
         return (p != null) ? p.getName() : "UNKNOWN";
     }
 
     /**
      * Finds the ID of a participant given its name from a list.
      * @param name The name of the participant.
      * @param participantsList List of all PL participants.
      * @return The participant ID, or null if not found.
      */
     private String findParticipantIdByName(String name, List<PL> participantsList) {
         if (name == null) return null;
         return participantsList.stream()
                 .filter(Objects::nonNull)
                 .filter(p -> name.equals(p.getName())) 
                 .map(PL::getId)                      
                 .findFirst()
                 .orElse(null);
     }
 
     private String getParticipantIdForElement(String elementId, Map<String, PL> participantsMap) {
         BpmnElement element = getElementById(elementId);
         if (element == null) {
             log.warn("getParticipantIdForElement: Element not found for ID: {}", elementId);
             return null;
         }
 
         // --- Logic to get processId from the element via Reflection ---
         String processId = null;
         try {
             // Attempt to find and invoke a public getProcessId() method
             java.lang.reflect.Method method = element.getClass().getMethod("getProcessId");
             Object result = method.invoke(element);
             if (result instanceof String) {
                 processId = (String) result;
                 log.trace("Retrieved processId '{}' via reflection for element {}", processId, elementId);
             } else {
                  log.warn("getParticipantIdForElement: getProcessId() for element {} ({}) did not return a String.", elementId, element.getClass().getSimpleName());
             }
         } catch (NoSuchMethodException e) {
             // This is the most likely error if the method isn't defined
             log.warn("getParticipantIdForElement: Element {} ({}) does not have a public getProcessId() method. Cannot link to participant.", elementId, element.getClass().getSimpleName());
             return null; // Cannot determine without process ID method
         } catch (IllegalAccessException e) {
              log.error("getParticipantIdForElement: Cannot access getProcessId method for element {} ({}): {}", elementId, element.getClass().getSimpleName(), e.getMessage());
              return null; // Access denied
         } catch (java.lang.reflect.InvocationTargetException e) {
              log.error("getParticipantIdForElement: Exception thrown by getProcessId method for element {} ({}): {}", elementId, element.getClass().getSimpleName(), e.getTargetException());
              return null; // Method threw an exception
         } catch (Exception e) {
             // Catch any other unexpected exceptions during reflection
             log.error("getParticipantIdForElement: Unexpected error invoking getProcessId for element {} ({}): {}", elementId, element.getClass().getSimpleName(), e.getMessage(), e);
             return null;
         }
         // --- End Logic to get processId ---
 
 
         // Check if processId was successfully retrieved
         if (processId == null) {
             // Specific warning already logged within the catch blocks if applicable
             return null;
         }
 
         // Find the participant whose processRef matches the element's processId
         for (PL participant : participantsMap.values()) {
             // Use getProcessId() which Lombok generates from the ProcessId field
             if (processId.equals(participant.getProcessId())) {
                 return participant.getId(); // Found the participant
             }
         }
 
         log.warn("getParticipantIdForElement: Found process ID '{}' for element '{}', but no participant references this process in the provided map.", processId, elementId);
         return null; // No participant found for this process ID
     }
 
     /**
      * Finds all start events associated with a specific process ID.
      * Relies on start event elements having a working getProcessId() method.
      *
      * @param processId The ID of the process (<bpmn:process id="...">) to find start events for.
      * @return A List of BpmnElement objects representing the start events found for that process,
      * or an empty list if none are found or the processId is null.
      */
     public List<BpmnElement> findStartEventsForProcess(String processId) {
         if (processId == null || processId.isEmpty()) {
             log.warn("findStartEventsForProcess called with null or empty processId.");
             return Collections.emptyList(); // Return empty list if no processId provided
         }
 
         List<BpmnElement> startEvents = new ArrayList<>();
 
         // Iterate through all elements to find potential start events
         for (BpmnElement element : elementsById.values()) {
             if (element == null) continue; // Safety check
 
             // Check if the element is one of the known start event types
             boolean isStartEvent = element instanceof com.example.B2XKlaim.Service.bpmnElements.events.NSE ||
                                    element instanceof com.example.B2XKlaim.Service.bpmnElements.events.MSE ||
                                    element instanceof com.example.B2XKlaim.Service.bpmnElements.events.SSE ||
                                    element instanceof com.example.B2XKlaim.Service.bpmnElements.events.TSE;
                                    // Add other start types if applicable
 
             if (isStartEvent) {
                 // --- Logic to get processId from the element ---
                 String elementProcessId = null;
                 try {
                     java.lang.reflect.Method method = element.getClass().getMethod("getProcessId");
                     Object result = method.invoke(element);
                     if (result instanceof String) {
                         elementProcessId = (String) result;
                     }
                 } catch (NoSuchMethodException e) {
                     log.trace("findStartEventsForProcess: Start event element {} ({}) does not have getProcessId(). Skipping.", element.getId(), element.getClass().getSimpleName());
                     continue; // Skip if method doesn't exist
                 } catch (Exception e) {
                     log.error("findStartEventsForProcess: Error invoking getProcessId for start event element {} ({}): {}", element.getId(), element.getClass().getSimpleName(), e.getMessage());
                     continue; // Skip on error
                 }
                 // --- End Logic to get processId ---
 
                 // Check if the element's processId matches the requested one
                 if (processId.equals(elementProcessId)) {
                     startEvents.add(element);
                     log.debug("Found start event {} for process {}", element.getId(), processId);
                 }
             }
         }
 
         if (startEvents.isEmpty()) {
             log.warn("No start events found for process ID: {}", processId);
         }
         return startEvents;
     }
 
     /**
      * Provides access to the map of all participants (ID -> PL object).
      * Useful for the BPMNTranslator. Filters out nulls.
      * @return A map containing all parsed PL elements with non-null IDs.
      */
     public Map<String, PL> getParticipantsMap() {
         return getElementsByType(PL.class).stream()
                 .filter(Objects::nonNull)
                 .filter(p -> p.getId() != null) // Ensure ID is not null for map key
                 .collect(Collectors.toMap(PL::getId, p -> p, (p1, p2) -> {
                      log.warn("Duplicate participant ID found: {}. Keeping first encountered.", p1.getId());
                      return p1; // Handle duplicates
                  }));
     }

    public BpmnElement getNextElementById(String elementId) {
        if (elementId == null) {
            return null;
        }
        BpmnElement currentElement = getElementById(elementId);
        if (currentElement == null) {
            log.warn("getNextElementById: Element not found for ID: {}", elementId);
            return null;
        }

        // --- Logic to get the single outgoing sequence flow ID ---
        // This requires element classes to have a method like getOutgoingEdge()
        String outgoingSequenceFlowId = null;
        try {
            // Try common method names used in your event/task/gateway classes
            java.lang.reflect.Method method = null;
            try {
                 method = currentElement.getClass().getMethod("getOutgoingEdge");
            } catch (NoSuchMethodException nsme) {
                try {
                     // Maybe it's called getOutgoing()? Check your classes.
                     method = currentElement.getClass().getMethod("getOutgoing");
                 } catch (NoSuchMethodException nsme2) {
                      log.warn("getNextElementById: Element {} ({}) does not have a recognized method (e.g., getOutgoingEdge) to find its outgoing sequence flow.", elementId, currentElement.getClass().getSimpleName());
                      return null; // Cannot proceed without outgoing edge
                 }
            }

            Object result = method.invoke(currentElement);
            if (result instanceof String) {
                outgoingSequenceFlowId = (String) result;
            } else if (result instanceof List && !((List<?>) result).isEmpty() && ((List<?>) result).get(0) instanceof String) {
                 // Handle cases where it might return a List<String> of outgoing edges (e.g., gateways)
                 // For this simple method, assume we take the first one if it's a list.
                 // Complex gateway logic should likely be handled differently in the Generator/Translator.
                 outgoingSequenceFlowId = (String) ((List<?>) result).get(0);
                 if (((List<?>) result).size() > 1) {
                    log.warn("getNextElementById: Element {} has multiple outgoing edges. Following the first one: {}", elementId, outgoingSequenceFlowId);
                 }
            } else {
                log.warn("getNextElementById: Could not determine outgoing sequence flow ID for element {} ({}). Method result was: {}", elementId, currentElement.getClass().getSimpleName(), result);
                return null;
            }
        } catch (Exception e) {
            log.error("getNextElementById: Error invoking outgoing edge method for element {} ({}): {}", elementId, currentElement.getClass().getSimpleName(), e.getMessage());
            return null;
        }
        // --- End Logic ---


        if (outgoingSequenceFlowId == null || outgoingSequenceFlowId.isEmpty()) {
            log.trace("getNextElementById: Element {} ({}) has no outgoing sequence flow ID defined.", elementId, currentElement.getClass().getSimpleName());
            return null; // Element might be an end event or improperly defined
        }

        // Use the existing helper to get the element after the sequence flow
        return getElementAfterSequenceFlow(outgoingSequenceFlowId);
    }

    /**
     * Retrieves all elements that are considered start events (NSE, MSE, SSE, TSE).
     * @return A List of BpmnElement objects representing the start events.
     */
    public List<BpmnElement> getAllStartEvents() {
        List<BpmnElement> startEvents = new ArrayList<>();
        // Use 'this' to call getElementsByType from within the same class
        startEvents.addAll(this.getElementsByType(NSE.class));
        startEvents.addAll(this.getElementsByType(MSE.class));
        startEvents.addAll(this.getElementsByType(SSE.class));
        startEvents.addAll(this.getElementsByType(TSE.class));
        // Ensure imports for NSE, MSE, SSE, TSE are present at the top of BpmnElements.java
        log.debug("Found {} total start events.", startEvents.size());
        return startEvents;
    }


    /**
     * Finds all Event Sub-Process (ESP) elements associated with a specific parent process ID.
     * Relies on ESP elements having their parent processId populated correctly by the factory.
     * @param parentProcessId The ID of the parent <bpmn:process>.
     * @return A List of ESP elements belonging to the parent process.
     */
    public List<ESP> getEventSubProcessesForProcess(String parentProcessId) {
        if (parentProcessId == null || parentProcessId.isEmpty()) {
            return Collections.emptyList();
        }
        return this.getElementsByType(ESP.class) // Use existing generic method
                .stream()
                .filter(esp -> parentProcessId.equals(esp.getProcessId())) // Assumes ESP has getProcessId()
                .collect(Collectors.toList());
    }
 
 } // End of BpmnElements class
