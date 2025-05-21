/*
 * Copyright 2023 Khalid BOURR
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */




package com.example.B2XKlaim.Service.Parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.example.B2XKlaim.Service.bpmnElements.BpmnElement;
import com.example.B2XKlaim.Service.bpmnElements.activities.CLA;
import com.example.B2XKlaim.Service.bpmnElements.activities.ESP;
import com.example.B2XKlaim.Service.bpmnElements.activities.ST;
import com.example.B2XKlaim.Service.bpmnElements.events.MEE;
import com.example.B2XKlaim.Service.bpmnElements.events.MIC;
import com.example.B2XKlaim.Service.bpmnElements.events.MIT;
import com.example.B2XKlaim.Service.bpmnElements.events.MSE;
import com.example.B2XKlaim.Service.bpmnElements.events.NEE;
import com.example.B2XKlaim.Service.bpmnElements.events.NSE;
import com.example.B2XKlaim.Service.bpmnElements.events.SEE;
import com.example.B2XKlaim.Service.bpmnElements.events.SIC;
import com.example.B2XKlaim.Service.bpmnElements.events.SIT;
import com.example.B2XKlaim.Service.bpmnElements.events.SSE;
import com.example.B2XKlaim.Service.bpmnElements.events.TCE;
import com.example.B2XKlaim.Service.bpmnElements.events.TSE;
import com.example.B2XKlaim.Service.bpmnElements.flows.MessageFLow;
import com.example.B2XKlaim.Service.bpmnElements.flows.SQ;
import com.example.B2XKlaim.Service.bpmnElements.gateways.AND;
import com.example.B2XKlaim.Service.bpmnElements.gateways.EB;
import com.example.B2XKlaim.Service.bpmnElements.gateways.LP;
import com.example.B2XKlaim.Service.bpmnElements.gateways.XOR;
import com.example.B2XKlaim.Service.bpmnElements.objects.pool.Collab;
import com.example.B2XKlaim.Service.bpmnElements.objects.pool.PL;

public class BpmnElementFactory {
    private static Document document;
    private List<MessageFLow> messageFlows;


    public BpmnElementFactory(Document document) {
        this.document = document;
        this.messageFlows = new ArrayList<>();

    }

    public BpmnElement createBpmnElement(Element element) {
        BpmnElementFactory factory = new BpmnElementFactory(document);
        String tagName = element.getTagName();
        String id = element.getAttribute("id");
        String name = element.getAttribute("name");
        String incoming = null;
        String outgoing = null;
        switch (tagName) {

            case "bpmn:messageFlow":
                String msgId = element.getAttribute("id");
                String eventSourceId = element.getAttribute("sourceRef");
                String eventTargetId = element.getAttribute("targetRef");

                System.err.println("--- Processing MessageFlow: " + msgId + " ---"); // DEBUG LOGGING
                System.err.println("    Source Element ID: " + eventSourceId);        // DEBUG LOGGING
                System.err.println("    Target Element ID: " + eventTargetId);        // DEBUG LOGGING

                // Call helpers - Log results immediately
                String senderParticipantId = getEnclosingParticipantId(eventSourceId);
                System.err.println("    Lookup Sender Participant ID for " + eventSourceId + ": " + senderParticipantId); // DEBUG LOGGING

                String receiverParticipantId = getEnclosingParticipantId(eventTargetId);
                 System.err.println("    Lookup Receiver Participant ID for " + eventTargetId + ": " + receiverParticipantId); // DEBUG LOGGING


                // Dependent calls - Log results immediately
                String senderParticipantName = getParticipantNameById(senderParticipantId);
                System.err.println("    Lookup Sender Name for " + senderParticipantId + ": " + senderParticipantName); // DEBUG LOGGING

                String receiverParticipantName = getParticipantNameById(receiverParticipantId);
                 System.err.println("    Lookup Receiver Name for " + receiverParticipantId + ": " + receiverParticipantName); // DEBUG LOGGING


                // Create the MessageFLow object
                // Ensure constructor matches the MessageFLow class definition
                MessageFLow messageFlow = MessageFLow.builder()
                        .id(msgId) // Use builder if available and preferable
                        .receiverId(receiverParticipantId)
                        .receiverName(receiverParticipantName)
                        .senderId(senderParticipantId)
                        .senderName(senderParticipantName)
                        .targetRef(eventTargetId)
                        .sourceRef(eventSourceId)
                        .build();

                 // Alternatively, use the constructor directly if builder isn't setup/preferred
                 // MessageFLow messageFlow = new MessageFLow(msgId, receiverParticipantId, receiverParticipantName, senderParticipantId, senderParticipantName, eventTargetId, eventSourceId);


                // Check if messageFlow object itself is null (shouldn't be)
                if (messageFlow == null) {
                     System.err.println("    ERROR: Failed to create MessageFLow object for " + msgId);
                     return null; // Or handle error appropriately
                }
                // Ensure messageFlows list exists (should be initialized in factory constructor)
                 if (this.messageFlows == null) {
                     System.err.println("    ERROR: messageFlows list is null in factory instance!");
                     this.messageFlows = new ArrayList<>();
                 }

                messageFlows.add(messageFlow); // Add to factory's instance list
                 System.err.println("    Successfully created and added MessageFLow: " + messageFlow); // DEBUG LOGGING
                 System.err.println("--- End Processing MessageFlow: " + msgId + " ---"); // DEBUG LOGGING
                return messageFlow; // Return the created object



            case "bpmn:participant":
                String participantName = element.getAttribute("name");
                String participantId = element.getAttribute("id");
                String processIdn = element.getAttribute("processRef");

                NodeList processNodes = document.getElementsByTagName("bpmn:process");
                String processNamen = null;

                for (int j = 0; j < processNodes.getLength(); j++) {
                    Element processElement = (Element) processNodes.item(j);
                    if (processIdn.equals(processElement.getAttribute("id"))) {
                        processNamen = processElement.getAttribute("name");
                        break;
                    }
                }

                return new PL(participantName, participantId, processIdn, processNamen);


            case "bpmn:collaboration":
                String collaborationId = element.getAttribute("id");
                List<PL> participantsForThisCollaboration = new ArrayList<>();

                NodeList participantNodes = element.getElementsByTagName("bpmn:participant");
                for (int i = 0; i < participantNodes.getLength(); i++) {
                    Element participantElement = (Element) participantNodes.item(i);
                    PL participant = createParticipant(participantElement);
                    participantsForThisCollaboration.add(participant);
                }

                return new Collab(collaborationId, participantsForThisCollaboration);


            case "bpmn:startEvent":
                outgoing = element.getElementsByTagName("bpmn:outgoing").item(0).getTextContent();
                Element parentProcessElement = (Element) element.getParentNode();
                String processId = parentProcessElement.getAttribute("id");
                String processName = parentProcessElement.getAttribute("name");
                if (outgoing == null || outgoing.isEmpty()) {
                    throw new IllegalArgumentException("Outgoing edge is required.");
                }
                if (name == null || name.isEmpty()) {
                    throw new IllegalArgumentException("Name is required.");
                }
                NodeList startEventChildNodes = element.getChildNodes();
                for (int i = 0; i < startEventChildNodes.getLength(); i++) {
                    Node childNode = startEventChildNodes.item(i);
                    if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element childElement = (Element) childNode;
                        String childTagName = childElement.getTagName();

                        if ("bpmn:messageEventDefinition".equals(childTagName)) {
                            String messageId = childElement.getAttribute("messageRef");
                            MSE mse = new MSE(name, id, outgoing, messageId, processId, processName);
                            return mse;
                        } else if ("bpmn:signalEventDefinition".equals(childTagName)) {
                            String signalId = childElement.getAttribute("signalRef");
                            String senderEventId = findThrowingSignalEventBySignalId(signalId);
                            String senderSignalParticipantName = "self"; // Default value
                            
                            if (senderEventId != null) {
                                String senderSignalParticipantId = getEnclosingParticipantId(senderEventId);
                                senderSignalParticipantName = Optional.ofNullable(getParticipantNameById(senderSignalParticipantId))
                                    .orElse("self");
                            }
                            
                            // Always create an SSE, even if no throwing event was found
                            SSE sse = new SSE(name, id, outgoing, signalId, processId, processName, senderSignalParticipantName);
                            return sse;
                        } else if ("bpmn:timerEventDefinition".equals(childTagName)) {
                            NodeList timeDurationNodes = childElement.getElementsByTagName("bpmn:timeDuration");
                            String timeDuration = null;
                            if (timeDurationNodes.getLength() > 0) {
                                timeDuration = timeDurationNodes.item(0).getTextContent();
                            }

                            Long duration;
                            if (timeDuration != null && !timeDuration.isEmpty()) {
                                try {
                                    duration = Long.parseLong(timeDuration);
                                } catch (NumberFormatException e) {
                                    throw new IllegalArgumentException("Invalid time duration format for " + id);
                                }
                            } else {
                                throw new IllegalArgumentException("Duration is required for " + id);
                            }

                            TSE tse = new TSE(name, id, duration, outgoing, processId, processName);
                            return tse;

                        }
                    }
                }
                NSE nse = new NSE(name, id, outgoing, processId, processName);
                return nse;


            case "bpmn:intermediateCatchEvent":
                incoming = element.getElementsByTagName("bpmn:incoming").item(0).getTextContent();
                outgoing = element.getElementsByTagName("bpmn:outgoing").item(0).getTextContent();
                if (outgoing == null || outgoing.isEmpty()) {
                    throw new IllegalArgumentException("Outgoing edge is required.");
                }
                if (name == null || name.isEmpty()) {
                    throw new IllegalArgumentException("Name is required.");
                }

                NodeList catchEventChildNodes = element.getChildNodes();
                for (int i = 0; i < catchEventChildNodes.getLength(); i++) {
                    Node childNode = catchEventChildNodes.item(i);
                    if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element childElement = (Element) childNode;
                        String childTagName = childElement.getTagName();

                        if ("bpmn:messageEventDefinition".equals(childTagName)) {
                            String messageId = childElement.getAttribute("messageRef");
                            MIC mic = new MIC(name, id, incoming, outgoing, messageId);
                            return mic;
                        } else if ("bpmn:signalEventDefinition".equals(childTagName)) {
                            String signalId = childElement.getAttribute("signalRef");
                            String senderEventId = findThrowingSignalEventBySignalId(signalId);
                            if (senderEventId != null) {
                                String senderSignalParticipantId = getEnclosingParticipantId(senderEventId);
                                String senderSignalParticipantName = Optional.ofNullable(getParticipantNameById(senderSignalParticipantId))
                                        .orElse("self");
                                System.err.println(senderEventId);        
                                SIC sic = new SIC(name, id, incoming, outgoing, signalId, senderSignalParticipantName);
                                return sic;
                            }
                        } else if ("bpmn:timerEventDefinition".equals(childTagName)) {
                            NodeList timeDurationNodes = childElement.getElementsByTagName("bpmn:timeDuration");
                            String timeDuration = null;
                            if (timeDurationNodes.getLength() > 0) {
                                timeDuration = timeDurationNodes.item(0).getTextContent();
                            }

                            Long duration;
                            if (timeDuration != null && !timeDuration.isEmpty()) {
                                try {
                                    duration = Long.parseLong(timeDuration);
                                } catch (NumberFormatException e) {
                                    throw new IllegalArgumentException("Invalid time duration format for " + id);
                                }
                            } else {
                                throw new IllegalArgumentException("Duration is required for " + id);
                            }

                            TCE tce = new TCE(name, id, duration, outgoing);
                            return tce;

                        }
                    }
                }
                break;


            case "bpmn:intermediateThrowEvent":
                incoming = element.getElementsByTagName("bpmn:incoming").item(0).getTextContent();
                outgoing = element.getElementsByTagName("bpmn:outgoing").item(0).getTextContent();
                if (outgoing == null || outgoing.isEmpty()) {
                    throw new IllegalArgumentException("Outgoing edge is required.");
                }
                if (name == null || name.isEmpty()) {
                    throw new IllegalArgumentException("Name is required.");
                }
                NodeList throwEventChildNodes = element.getChildNodes();
                for (int i = 0; i < throwEventChildNodes.getLength(); i++) {
                    Node childNode = throwEventChildNodes.item(i);
                    if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element childElement = (Element) childNode;
                        String childTagName = childElement.getTagName();

                        if ("bpmn:messageEventDefinition".equals(childTagName)) {
                            String messageId = childElement.getAttribute("messageRef");
                            MessageFLow correspondingMessageFlow = findMessageFlowBySourceRef(id);
                            if (correspondingMessageFlow == null) {
                                throw new IllegalArgumentException("Message flow is required.");
                            }
                            MIT mit = new MIT(name, id, incoming, outgoing, messageId, correspondingMessageFlow);
                            return mit;
                        } else if ("bpmn:signalEventDefinition".equals(childTagName)) {
                            String signalId = childElement.getAttribute("signalRef");
                            SIT sit = new SIT(name, id, incoming, outgoing, signalId);
                            return sit;
                        }
                    }
                }
                break;


            case "bpmn:endEvent":
                incoming = element.getElementsByTagName("bpmn:incoming").item(0).getTextContent();
                NodeList endEventChildNodes = element.getChildNodes();
                if (name == null || name.isEmpty()) {
                    throw new IllegalArgumentException("Name is required.");
                }
                for (int i = 0; i < endEventChildNodes.getLength(); i++) {
                    Node childNode = endEventChildNodes.item(i);
                    if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element childElement = (Element) childNode;
                        String childTagName = childElement.getTagName();

                        if ("bpmn:messageEventDefinition".equals(childTagName)) {
                            String messageId = childElement.getAttribute("messageRef");
                            MessageFLow correspondingMessageFlow = findMessageFlowBySourceRef(id);
                            if (correspondingMessageFlow == null) {
                                throw new IllegalArgumentException("Message flow is required.");
                            }
                            MEE mee = new MEE(name, id, incoming, messageId, correspondingMessageFlow);
                            return mee;
                        } else if ("bpmn:signalEventDefinition".equals(childTagName)) {
                            String signalId = childElement.getAttribute("signalRef");
                            SEE see = new SEE(name, id, incoming, signalId);
                            return see;
                        }
                    }
                }
                NEE nee = new NEE(name, id, incoming);
                return nee;


            case "bpmn:callActivity":
                String calledElementValue = element.getAttribute("calledElement");
                incoming = element.getElementsByTagName("bpmn:incoming").item(0).getTextContent();
                outgoing = element.getElementsByTagName("bpmn:outgoing").item(0).getTextContent();
                if (outgoing == null || outgoing.isEmpty()) {
                    throw new IllegalArgumentException("Outgoing edge is required.");
                }
                if (name == null || name.isEmpty()) {
                    throw new IllegalArgumentException("Name is required.");
                }
                if (calledElementValue == null || calledElementValue.isEmpty()) {
                    throw new IllegalArgumentException("CalledElement is required.");
                }
                CLA cla = new CLA(name, id, incoming, outgoing, calledElementValue);
                return cla;


            case "bpmn:scriptTask":
                id = element.getAttribute("id");
                name = element.getAttribute("name");
                incoming = element.getElementsByTagName("bpmn:incoming").item(0).getTextContent();
                outgoing = element.getElementsByTagName("bpmn:outgoing").item(0).getTextContent();
                if (outgoing == null || outgoing.isEmpty()) {
                    throw new IllegalArgumentException("Outgoing edge is required.");
                }
                if (name == null || name.isEmpty()) {
                    throw new IllegalArgumentException("Name is required.");
                }
                return new ST(name, id, incoming, outgoing);


                case "bpmn:subProcess":
                String processespId = null;    
                String processespName = null; 
                if (element.hasAttribute("triggeredByEvent")) {
                    String espId = element.getAttribute("id");
                    String espName = element.getAttribute("name");

            
                    // Get parent process
                    Node parent = element.getParentNode();
                    if (parent != null && parent.getNodeType() == Node.ELEMENT_NODE) {
                        Element parentElement = (Element) parent;
                        if ("bpmn:process".equals(parentElement.getNodeName())) {
                            processespId = parentElement.getAttribute("id");
                            processespName = parentElement.getAttribute("name");
                        }
                    }
            
                    // Create the ESP with correct process context
                    ESP esp = ESP.builder()
                            .id(espId)
                            .name(espName)
                            .processId(processespId)
                            .processName(processespName)
                            .build();
            
                    // Now parse and collect internal BPMN elements
                    NodeList children = element.getChildNodes();
                    for (int i = 0; i < children.getLength(); i++) {
                        Node childNode = children.item(i);
                        if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element childElement = (Element) childNode;
            
                            // Convert childElement to BpmnElement
                            BpmnElement internalElement = parseBpmnElement(childElement); // Implement this method
            
                            if (internalElement != null) {
                                esp.addInternalElement(internalElement);
                            }
                        }
                    }
            
                    return esp;
                }
                break;


            case "bpmn:sequenceFlow":
                String sfId = element.getAttribute("id");
                String sfSourceRef = element.getAttribute("sourceRef");
                String sfTargetRef = element.getAttribute("targetRef");
                SQ sf = new SQ(sfId, sfSourceRef, sfTargetRef);
                return sf;



            case "bpmn:eventBasedGateway":
                String ebId = element.getAttribute("id");
                return processEventBasedGateway(element, ebId);

            case "bpmn:parallelGateway":
                String andSplitId = element.getAttribute("id");
                NodeList andIncomings = element.getElementsByTagName("bpmn:incoming");
                NodeList andOutgoings = element.getElementsByTagName("bpmn:outgoing");
                Integer counter = 0;
                // Check if the AND split has an incoming flow
                if (andIncomings.getLength() == 1) {
                    AND and = processANDGateway(andOutgoings, andSplitId);
                    return and;
                }

                return null;


            case "bpmn:exclusiveGateway":
                String xorid = element.getAttribute("id");
                NodeList incomings = element.getElementsByTagName("bpmn:incoming");
                NodeList outgoings = element.getElementsByTagName("bpmn:outgoing");

                // This passage is to avoid the clash that may occur between loop and xor.

                 String firstIncomingFlow = incomings.item(0).getTextContent();
                 Element ElementFromFirstIncomingFlow= getElementByFlowId(firstIncomingFlow);
                 Element sourceElement = getElementById(ElementFromFirstIncomingFlow.getAttribute("sourceRef"));

                if (isXORSplit(element) && !isXORMerge(sourceElement)) {
                    XOR xor = processXORGateway(outgoings, xorid);
                    return xor;
                }
                else  {
                    LP xor = processLoop(outgoings, xorid);
                    return xor;
                }


            default:
                return null;
        }

        return null;
    }


    private Element getElementByFlowId(String flowId) {
        // Get all the sequenceFlow elements in the document
        NodeList sequenceFlows = document.getElementsByTagName("bpmn:sequenceFlow");

        // Loop through the sequenceFlow elements to find the one with the matching id
        Element matchingFlow = null;
        for (int i = 0; i < sequenceFlows.getLength(); i++) {
            Element flow = (Element) sequenceFlows.item(i);
            if (flow.getAttribute("id").equals(flowId)) {
                matchingFlow = flow;
                break;
            }
        }

        return matchingFlow;
    }

    private Element getElementById(String id) {
        NodeList allElements = document.getElementsByTagName("*");

        for (int i = 0; i < allElements.getLength(); i++) {
            Element element = (Element) allElements.item(i);
            if (element.getAttribute("id").equals(id)) {
                return element;
            }
        }

        System.out.println("Element with id " + id + " not found.");
        return null;
    }

    private Element getNextElementByFlowId(String flowId) {
        Element sequenceFlow = getElementByFlowId(flowId);
        if (sequenceFlow == null) {
            return null;
        }

        String targetElementId = sequenceFlow.getAttribute("targetRef");
        return getElementById(targetElementId);
    }

    private String getConditionExpression(Element flow) {
        Node conditionNode = flow.getElementsByTagName("bpmn:conditionExpression").item(0);
        return conditionNode != null ? conditionNode.getTextContent() : "";
    }

    private String collectElementsInsideLoop(Element startingFlow, List<String> elementsList, String xorMergeId) {
        Element currentElement = getNextElement(startingFlow);
        while (!currentElement.getAttribute("id").equals(xorMergeId)) {
            elementsList.add(currentElement.getAttribute("id"));
            currentElement = getNextElement(currentElement);
        }
        return startingFlow.getAttribute("id");
    }

    private Element getNextElement(Element currentElement) {
        NodeList outgoings = currentElement.getElementsByTagName("bpmn:outgoing");
        if (outgoings.getLength() > 0) {
            String nextFlowId = outgoings.item(0).getTextContent();
            Element nextFlow = getElementByFlowId(nextFlowId);
            return getElementById(nextFlow.getAttribute("targetRef"));
        }
        return null;
    }

    private PL createParticipant(Element participantElement) {
        String participantName = participantElement.getAttribute("name");
        String participantId = participantElement.getAttribute("id");
        String processId = participantElement.getAttribute("processRef");

        NodeList processNodes = document.getElementsByTagName("bpmn:process");
        String processName = null;

        for (int j = 0; j < processNodes.getLength(); j++) {
            Element processElement = (Element) processNodes.item(j);
            if (processId.equals(processElement.getAttribute("id"))) {
                processName = processElement.getAttribute("name");
                break;  // Break out of the loop once the process is found
            }
        }

        return new PL(participantName, participantId, processId, processName);
    }

    private String getEnclosingParticipantId(String elementId) {
        if (elementId == null || document == null) {
            return null;
        }
    
        XPath xpath = XPathFactory.newInstance().newXPath();
        String enclosingProcessId = null;
    
        try {
            // 1. Find the element with the specific ID anywhere in the document
            // Uses XPath expression "//*[@id='elementIdValue']"
            String expression = "//*[@id='" + elementId + "']";
            Node elementNode = (Node) xpath.evaluate(expression, document, XPathConstants.NODE);
    
            if (elementNode == null) {
                System.err.println("getEnclosingParticipantId: Could not find element with ID: " + elementId);
                return null;
            }
    
            // 2. Traverse upwards from the found node to find the parent <bpmn:process>
            Node parent = elementNode.getParentNode();
            while (parent != null) {
                // Check if the node is an Element node and its tag name is bpmn:process
                if (parent.getNodeType() == Node.ELEMENT_NODE && "bpmn:process".equals(parent.getNodeName())) {
                     // 3. Get the ID of the process element
                    enclosingProcessId = ((Element) parent).getAttribute("id");
                    break; // Found the process
                }
                parent = parent.getParentNode(); // Move up
            }
    
            if (enclosingProcessId == null) {
                 System.err.println("getEnclosingParticipantId: Could not find enclosing <bpmn:process> for element ID: " + elementId);
                 return null;
            }
    
        } catch (XPathExpressionException e) {
            System.err.println("getEnclosingParticipantId: XPath error finding element ID " + elementId + ": " + e.getMessage());
            return null;
        }
    
        // 4. Find the participant associated with this process ID (using original logic)
        if (enclosingProcessId != null) {
            NodeList participantNodes = document.getElementsByTagName("bpmn:participant");
            for (int i = 0; i < participantNodes.getLength(); i++) {
                Element participantElement = (Element) participantNodes.item(i);
                if (enclosingProcessId.equals(participantElement.getAttribute("processRef"))) {
                    return participantElement.getAttribute("id"); // Return participant ID
                }
            }
             System.err.println("getEnclosingParticipantId: Found process ID '" + enclosingProcessId + "' but no participant references it.");
        }
    
        return null; // Participant not found for the process
    }

    private String getParticipantNameById(String participantId) {
        NodeList participantNodes = document.getElementsByTagName("bpmn:participant");
        for (int i = 0; i < participantNodes.getLength(); i++) {
            Element participantElement = (Element) participantNodes.item(i);
            if (participantId != null && participantId.equals(participantElement.getAttribute("id"))) {
                return participantElement.getAttribute("name");
            }
        }
        return null;
    }


    public MessageFLow findMessageFlowBySourceRef(String sourceRefId) {
        for (MessageFLow messageFlow : messageFlows) {
            if (messageFlow.getSourceRef().equals(sourceRefId)) {
                return messageFlow;
            }
        }
        return null;
    }


    private String findThrowingSignalEventBySignalId(String signalId) {
        if (signalId == null || document == null) {
            return null;
        }
    
        // Define BPMN tag names that represent throwing events
        List<String> throwingEventTags = Arrays.asList(
                "bpmn:intermediateThrowEvent",
                "bpmn:endEvent"
                // Add "bpmn:startEvent" ONLY if a start event can *throw* a signal in your context (uncommon for signalRef)
        );
    
        NodeList signalEventDefinitions = document.getElementsByTagName("bpmn:signalEventDefinition");
        for (int i = 0; i < signalEventDefinitions.getLength(); i++) {
            Node node = signalEventDefinitions.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element signalEventDefElement = (Element) node;
    
                // Check if the signalRef matches
                if (signalId.equals(signalEventDefElement.getAttribute("signalRef"))) {
                    Node parentNode = signalEventDefElement.getParentNode();
    
                    // Check if the parent is an element and if its tag name is a throwing type
                    if (parentNode != null && parentNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element parentElement = (Element) parentNode;
                        String parentTagName = parentElement.getTagName(); // e.g., "bpmn:intermediateCatchEvent", "bpmn:endEvent"
    
                        if (throwingEventTags.contains(parentTagName)) {
                            // Found a match inside a throwing event type! Return its ID.
                            return parentElement.getAttribute("id");
                        }
                        // If parentTagName is not in throwingEventTags (e.g., it's a catch event), continue searching
                    }
                }
            }
        }
    
        System.err.println("findThrowingSignalEventBySignalId: Could not find a THROWING event for signalRef: " + signalId);
        return null; // No throwing event found for this signal ID
    }


    // Helper method to find the loop's outgoing flow with a condition
    private Map<String, Element> findLoopOutgoingFlows(Element xorSplit) {
        Map<String, Element> flows = new HashMap<>();

        NodeList splitOutgoings = xorSplit.getElementsByTagName("bpmn:outgoing");
        for (int j = 0; j < splitOutgoings.getLength(); j++) {
            Element outgoingFlow = getElementByFlowId(splitOutgoings.item(j).getTextContent());
            NodeList conditionExpressions = outgoingFlow.getElementsByTagName("bpmn:conditionExpression");

            // Check for condition expression
            if (conditionExpressions.getLength() > 0 && !conditionExpressions.item(0).getTextContent().isEmpty()) {
                // Flow with condition
                flows.put("withCondition", outgoingFlow);
            } else {
                // Flow without condition
                flows.put("withoutCondition", outgoingFlow);
            }
        }

        return flows;
    }


    private boolean isLoopGateway(Element gatewayElement) {
        NodeList incomings = gatewayElement.getElementsByTagName("bpmn:incoming");
        NodeList outgoings = gatewayElement.getElementsByTagName("bpmn:outgoing");

        // Collect all target references of incoming flows
        Set<String> incomingTargets = new HashSet<>();
        for (int i = 0; i < incomings.getLength(); i++) {
            String incomingFlowId = incomings.item(i).getTextContent();
            Element incomingFlowElement = getElementByFlowId(incomingFlowId);
            String targetRef = incomingFlowElement.getAttribute("targetRef");
            incomingTargets.add(targetRef);
        }

        // Check if any source reference of outgoing flows is in incoming targets
        for (int j = 0; j < outgoings.getLength(); j++) {
            String outgoingFlowId = outgoings.item(j).getTextContent();
            Element outgoingFlowElement = getElementByFlowId(outgoingFlowId);
            String sourceRef = outgoingFlowElement.getAttribute("sourceRef");
            if (incomingTargets.contains(sourceRef)) {
                return true; // Loop detected
            }
        }
        return false;
    }


    private XOR processXORGateway(NodeList outgoings, String xorId) {
        Map<String, List<String>> conditionElementMap = new HashMap<>();
        String outgoing = null;
        String caOutgoing = null;

        List<Element> outgoingFlows = new ArrayList<>();
        for (int j = 0; j < outgoings.getLength(); j++) {
            Element outgoingFlow = getElementByFlowId(outgoings.item(j).getTextContent());
            outgoingFlows.add(outgoingFlow);
        }

        outgoingFlows.sort(Comparator.comparing(o -> o.getAttribute("targetRef")));

        for (Element outgoingFlow : outgoingFlows) {
            String nextElementId = outgoingFlow.getAttribute("targetRef");
            Element nextElement = getElementById(nextElementId);
            List<String> elementList = new ArrayList<>();

            // loop through elements until we reach another gateway or end event
            Element mergeExclusiveGateway = null;
            while (nextElement != null) {
                if (isXORMerge(nextElement)) {
                    mergeExclusiveGateway = nextElement;
                    break;
                } else if (nextElement.getTagName().equals("bpmn:endEvent")) {
                    break;
                } else if (isXORSplit(nextElement)) {
                    elementList.add(nextElement.getAttribute("id"));
                    NodeList nestedOutgoings = nextElement.getElementsByTagName("bpmn:outgoing");
                    XOR nestedXOR = processXORGateway(nestedOutgoings, nextElement.getAttribute("id"));
                    Element mergeGateway = getElementById(nestedXOR.getOutgoingEdge());
                    nextElementId = mergeGateway.getAttribute("targetRef");
                    nextElement = getElementById(nextElementId);
                } else if (isANDSplit(nextElement)) {
                    elementList.add(nextElement.getAttribute("id"));
                    NodeList nestedOutgoings = nextElement.getElementsByTagName("bpmn:outgoing");
                    AND nestedAND = processANDGateway(nestedOutgoings, nextElement.getAttribute("id"));
                    Element mergeGateway = getElementById(nestedAND.getOutgoingEdge());
                    nextElementId = mergeGateway.getAttribute("targetRef");
                    nextElement = getElementById(nextElementId);
                }
                else {
                    elementList.add(nextElementId);
                    NodeList nextElementOutgoings = nextElement.getElementsByTagName("bpmn:outgoing");
                    if (nextElementOutgoings.getLength() == 0) {
                        break;
                    }

                    Element outgoingFlow2 = getElementByFlowId(nextElementOutgoings.item(0).getTextContent());
                    nextElementId = outgoingFlow2.getAttribute("targetRef");
                    nextElement = getElementById(nextElementId);
                }
            }

            if (mergeExclusiveGateway != null) {
                caOutgoing = mergeExclusiveGateway.getElementsByTagName("bpmn:outgoing").item(0).getTextContent();
            }

            Element conditionExpression = (Element) outgoingFlow.getElementsByTagName("bpmn:conditionExpression").item(0);
            if (conditionExpression.getTextContent() == null || conditionExpression.getTextContent().isEmpty()) {
                throw new IllegalArgumentException("Conditions are required.");
            }
            if (conditionExpression != null) {
                String condition = conditionExpression.getTextContent();
                List<String> existingElements = conditionElementMap.getOrDefault(condition, new ArrayList<>());
                existingElements.addAll(elementList);
                conditionElementMap.put(condition, existingElements);
            }

            if (outgoings.getLength() == 1) {
                caOutgoing = outgoing;
            }
        }

        return new XOR(xorId, conditionElementMap, caOutgoing);
    }

    private boolean isXORGateway(Element element) {
        // Logic to determine if the element is an XOR gateway
        return element.getTagName().equals("bpmn:exclusiveGateway");
    }

    private String extractCondition(Element flowElement) {
        // Extract the condition from a flow element
        Element conditionElement = (Element) flowElement.getElementsByTagName("bpmn:conditionExpression").item(0);
        return conditionElement != null ? conditionElement.getTextContent() : "";
    }

    private boolean isXORSplit(Element element) {
        // Check if the element is an XOR gateway
        if (!isXORGateway(element)) {
            return false;
        }

        // A split XOR gateway typically has one incoming flow and multiple outgoing flows
        NodeList incomings = element.getElementsByTagName("bpmn:incoming");
        NodeList outgoings = element.getElementsByTagName("bpmn:outgoing");

        return incomings.getLength() == 1 && outgoings.getLength() > 1;
    }

    private boolean isXORMerge(Element element) {
        // Check if the element is an XOR gateway
        if (!isXORGateway(element)) {
            return false;
        }

        // A merge XOR gateway typically has multiple incoming flows and one outgoing flow
        NodeList incomings = element.getElementsByTagName("bpmn:incoming");
        NodeList outgoings = element.getElementsByTagName("bpmn:outgoing");

        return incomings.getLength() > 1 && outgoings.getLength() <= 1;
    }

    private boolean isANDGateway(Element element) {
        // Logic to determine if the element is an AND gateway
        return element.getTagName().equals("bpmn:parallelGateway");
    }

    private boolean isANDSplit(Element element) {
        // Check if the element is an AND gateway
        if (!isANDGateway(element)) {
            return false;
        }

        // An AND split gateway typically has one incoming flow and multiple outgoing flows
        NodeList incomings = element.getElementsByTagName("bpmn:incoming");
        NodeList outgoings = element.getElementsByTagName("bpmn:outgoing");

        return incomings.getLength() == 1 && outgoings.getLength() > 1;
    }

    private boolean isANDMerge(Element element) {
        // Check if the element is an AND gateway
        if (!isANDGateway(element)) {
            return false;
        }

        // An AND merge gateway typically has multiple incoming flows and one outgoing flow
        NodeList incomings = element.getElementsByTagName("bpmn:incoming");
        NodeList outgoings = element.getElementsByTagName("bpmn:outgoing");

        return incomings.getLength() > 1 && outgoings.getLength() <= 1;
    }

    private AND processANDGateway(NodeList outgoings, String andId) {
        Map<Integer, List<String>> flowElementMap = new HashMap<>();
        String outgoing = null;
        String caOutgoing = null;
        Integer counter = 0;

        List<Element> outgoingFlows = new ArrayList<>();
        for (int j = 0; j < outgoings.getLength(); j++) {
            Element outgoingFlow = getElementByFlowId(outgoings.item(j).getTextContent());
            outgoingFlows.add(outgoingFlow);
        }

        outgoingFlows.sort(Comparator.comparing(o -> o.getAttribute("targetRef")));

        for (Element outgoingFlow : outgoingFlows) {
            String nextElementId = outgoingFlow.getAttribute("targetRef");
            Element nextElement = getElementById(nextElementId);
            List<String> elementList = new ArrayList<>();

            // loop through elements until we reach another gateway or end event
            Element mergeExclusiveGateway = null;
            while (nextElement != null) {
                if (isANDMerge(nextElement)) {
                    mergeExclusiveGateway = nextElement;
                    break;
                } else if (nextElement.getTagName().equals("bpmn:endEvent")) {
                    break;
                } else if (isANDSplit(nextElement)) {
                    elementList.add(nextElement.getAttribute("id"));
                    NodeList nestedOutgoings = nextElement.getElementsByTagName("bpmn:outgoing");
                    AND nestedAND = processANDGateway(nestedOutgoings, nextElement.getAttribute("id"));
                    Element mergeGateway = getElementById(nestedAND.getOutgoingEdge());
                    nextElementId = mergeGateway.getAttribute("targetRef");
                    nextElement = getElementById(nextElementId);
                } else if (isXORSplit(nextElement)) {
                    elementList.add(nextElement.getAttribute("id"));
                    NodeList nestedOutgoings = nextElement.getElementsByTagName("bpmn:outgoing");
                    XOR nestedXOR = processXORGateway(nestedOutgoings, nextElement.getAttribute("id"));
                    Element mergeGateway = getElementById(nestedXOR.getOutgoingEdge());
                    nextElementId = mergeGateway.getAttribute("targetRef");
                    nextElement = getElementById(nextElementId);
                }else {
                    elementList.add(nextElementId);
                    NodeList nextElementOutgoings = nextElement.getElementsByTagName("bpmn:outgoing");
                    if (nextElementOutgoings.getLength() == 0) {
                        break;
                    }

                    Element outgoingFlow2 = getElementByFlowId(nextElementOutgoings.item(0).getTextContent());
                    nextElementId = outgoingFlow2.getAttribute("targetRef");
                    nextElement = getElementById(nextElementId);
                }
            }

            if (mergeExclusiveGateway != null) {
                caOutgoing = mergeExclusiveGateway.getElementsByTagName("bpmn:outgoing").item(0).getTextContent();
            }

            // Store the list of elements associated with the outgoing flow in the flow element map
            flowElementMap.put(counter, elementList);
            counter = counter + 1;

            if (outgoings.getLength() == 1) {
                caOutgoing = outgoing;
            }
        }

        return new AND(andId, flowElementMap, caOutgoing);
    }

    private LP processLoop(NodeList outgoings, String loopID){
        String loopCondition = null;
        List<String> elementsInsideLoop = new ArrayList<>();
        String loopExitEdge = null;

        Element xorMerge = getElementById(loopID);
        String outgoingMerge = xorMerge.getElementsByTagName("bpmn:outgoing").item(0).getTextContent();
        Element xorSplit = getNextElementByFlowId(outgoingMerge);
        Map<String, Element> loopOutgoingFlow = findLoopOutgoingFlows(xorSplit);

        if (loopOutgoingFlow.containsKey("withCondition")) {
            loopCondition = getConditionExpression(loopOutgoingFlow.get("withCondition"));

            // Collect elements inside the loop, starting from the target of the loopOutgoingFlow
            String targetRef = loopOutgoingFlow.get("withCondition").getAttribute("targetRef");
            Element currentElement = getElementById(targetRef);

            while (!currentElement.getAttribute("id").equals(loopID)) {
                elementsInsideLoop.add(currentElement.getAttribute("id"));
                currentElement = getNextElement(currentElement);
            }

            loopExitEdge = loopOutgoingFlow.containsKey("withoutCondition")
                    ? loopOutgoingFlow.get("withoutCondition").getAttribute("id")
                    : null;
        }

        if (loopCondition != null && !elementsInsideLoop.isEmpty() && loopExitEdge != null) {
            // Update elementsInLoop set after confirming a valid loop structure
            List<String> elementsInLoop = new ArrayList<>(elementsInsideLoop);
            return new LP(loopID, loopCondition, elementsInLoop, loopExitEdge);
        }
        return null;
    }

    private boolean isPartOfLoop(Element gateway, Set<String> visited) {
        String gatewayId = gateway.getAttribute("id");
        if (visited.contains(gatewayId)) {
            return true;
        }

        visited.add(gatewayId);

        NodeList outgoings = gateway.getElementsByTagName("bpmn:outgoing");
        for (int i = 0; i < outgoings.getLength(); i++) {
            Node outgoing = outgoings.item(i);
            if (outgoing != null) {
                Element targetElement = getElementByFlowId(outgoing.getTextContent());
                if (targetElement != null && isPartOfLoop(targetElement, visited)) {
                    return true;
                }
            }
        }

        return false;
    }

    /*
    * Method to process an event-based gateway.
    */
    private EB processEventBasedGateway(Element element, String gatewayId) {
        NodeList outgoings = element.getElementsByTagName("bpmn:outgoing");
        Map<String, List<String>> eventPathMap = new HashMap<>();
        String gatewayOutgoing = null;
        Element mergeGateway = null;
        
        System.out.println("Processing Event-Based Gateway with ID: " + gatewayId);
    
        // Store all path endpoints to find common merge point later
        List<Element> pathEndElements = new ArrayList<>();
    
        // For each outgoing edge
        for (int i = 0; i < outgoings.getLength(); i++) {
            String outgoingId = outgoings.item(i).getTextContent();
            Element outgoingFlow = getElementByFlowId(outgoingId);
            if (outgoingFlow == null) {
                System.out.println("Warning: Could not find flow with ID: " + outgoingId);
                continue;
            }
    
            // Get the target event
            String eventId = outgoingFlow.getAttribute("targetRef");
            Element eventElement = getElementById(eventId);
            if (eventElement == null) {
                System.out.println("Warning: Could not find event with ID: " + eventId);
                continue;
            }
    
            // Collect the elements on this path
            List<String> pathElements = new ArrayList<>();
            pathElements.add(eventId); // Add the catch event itself
            
            System.out.println("Processing path from event: " + eventId + " (" + eventElement.getTagName() + ")");
            
            // Follow the path from this event until we reach a merge point or end event
            Element currentElement = eventElement;
            Element lastElement = null;
            
            while (currentElement != null) {
                // Get outgoing flow from the current element
                NodeList currentOutgoings = currentElement.getElementsByTagName("bpmn:outgoing");
                if (currentOutgoings.getLength() == 0) {
                    System.out.println("Path ends at element: " + currentElement.getAttribute("id"));
                    pathEndElements.add(currentElement);
                    break;
                }
                
                Element currentOutgoingFlow = getElementByFlowId(currentOutgoings.item(0).getTextContent());
                if (currentOutgoingFlow == null) {
                    System.out.println("Could not find flow from element: " + currentElement.getAttribute("id"));
                    break;
                }
                
                String nextElementId = currentOutgoingFlow.getAttribute("targetRef");
                Element nextElement = getElementById(nextElementId);
                
                // If we've reached an exclusive gateway, check if it's a merge point
                if (nextElement != null && isXORMerge(nextElement)) {
                    System.out.println("Found potential XOR merge at: " + nextElement.getAttribute("id"));
                    
                    // Store it as a path end element
                    pathEndElements.add(nextElement);
                    
                    // If we haven't set a merge gateway yet, use this one
                    if (mergeGateway == null) {
                        mergeGateway = nextElement;
                        
                        // Get the outgoing edge of the merge gateway
                        NodeList mergeOutgoings = mergeGateway.getElementsByTagName("bpmn:outgoing");
                        if (mergeOutgoings.getLength() > 0) {
                            gatewayOutgoing = mergeOutgoings.item(0).getTextContent();
                            System.out.println("Set merge gateway outgoing edge: " + gatewayOutgoing);
                        }
                    }
                    break;
                }
                
                // Otherwise, add this element to the path and continue
                if (nextElement != null) {
                    pathElements.add(nextElementId);
                    lastElement = currentElement;
                    currentElement = nextElement;
                } else {
                    System.out.println("Could not find next element from: " + currentElement.getAttribute("id"));
                    break;
                }
            }
            
            // Determine the event type for this path
            String eventType = determineEventType(eventElement);
            System.out.println("Event type for " + eventId + ": " + eventType);
            
            // Store the path with a unique key if there are multiple of the same type
            String pathKey = eventType;
            int counter = 1;
            while (eventPathMap.containsKey(pathKey)) {
                pathKey = eventType + "_" + counter++;
            }
            
            eventPathMap.put(pathKey, pathElements);
        }
        
        // Verify that all paths converge to the same XOR merge
        if (pathEndElements.size() > 1) {
            Map<String, Integer> endElementCounts = new HashMap<>();
            String mostCommonId = null;
            int maxCount = 0;
            
            for (Element endElement : pathEndElements) {
                String id = endElement.getAttribute("id");
                int count = endElementCounts.getOrDefault(id, 0) + 1;
                endElementCounts.put(id, count);
                
                if (count > maxCount) {
                    maxCount = count;
                    mostCommonId = id;
                }
            }
            
            // If we found a common merge point and it's an XOR gateway
            if (mostCommonId != null && maxCount > 1) {
                Element commonMerge = getElementById(mostCommonId);
                if (commonMerge != null && isXORMerge(commonMerge)) {
                    mergeGateway = commonMerge;
                    
                    // Get the outgoing edge of the merge gateway
                    NodeList mergeOutgoings = mergeGateway.getElementsByTagName("bpmn:outgoing");
                    if (mergeOutgoings.getLength() > 0) {
                        gatewayOutgoing = mergeOutgoings.item(0).getTextContent();
                        System.out.println("Confirmed common XOR merge: " + mostCommonId + " with outgoing: " + gatewayOutgoing);
                    }
                }
            }
        }
        
        // If no merge gateway was found, log a warning
        if (mergeGateway == null) {
            System.out.println("Warning: No XOR merge found for Event-Based Gateway " + gatewayId);
        }
        
        return new EB(gatewayId, eventPathMap, gatewayOutgoing);
    }
    
    /**
     * Helper method to determine the type of event.
     */
    private String determineEventType(Element eventElement) {
        // Check for child elements that define the event type
        NodeList childNodes = eventElement.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node childNode = childNodes.item(i);
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                Element childElement = (Element) childNode;
                String childTagName = childElement.getTagName();
                
                if ("bpmn:messageEventDefinition".equals(childTagName)) {
                    return "message";
                } else if ("bpmn:signalEventDefinition".equals(childTagName)) {
                    return "signal";
                } else if ("bpmn:timerEventDefinition".equals(childTagName)) {
                    return "timer";
                }
            }
        }
        
        // Default case if no specific event type is found
        return "default";
    }
    /**
     * Parse BPMN elements inside a sub-process
     * This method recursively processes child elements of a sub-process
     *
     * @param childElement The XML element to parse
     * @return A BpmnElement representation of the child element
     */
    private BpmnElement parseBpmnElement(Element childElement) {
        // Reuse the existing createBpmnElement method for consistent parsing
        BpmnElement element = createBpmnElement(childElement);
        
        // For debugging
        if (element != null) {
            System.err.println("Successfully parsed sub-process element: " + childElement.getTagName() + 
                            " (ID: " + childElement.getAttribute("id") + ")");
        } else {
            System.err.println("Failed to parse sub-process element: " + childElement.getTagName() + 
                            " (ID: " + childElement.getAttribute("id") + ")");
        }
        
        return element;
    }

}