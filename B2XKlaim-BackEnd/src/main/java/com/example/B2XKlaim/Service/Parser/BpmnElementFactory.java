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

import com.example.B2XKlaim.Service.bpmnElements.BpmnElement;
import com.example.B2XKlaim.Service.bpmnElements.activities.CLA;
import com.example.B2XKlaim.Service.bpmnElements.activities.ESP;
import com.example.B2XKlaim.Service.bpmnElements.activities.ST;
import com.example.B2XKlaim.Service.bpmnElements.events.*;
import com.example.B2XKlaim.Service.bpmnElements.flows.*;
import com.example.B2XKlaim.Service.bpmnElements.gateways.AND;
import com.example.B2XKlaim.Service.bpmnElements.gateways.XOR;
import com.example.B2XKlaim.Service.bpmnElements.objects.pool.Collab;
import com.example.B2XKlaim.Service.bpmnElements.objects.pool.PL;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.util.*;

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
        // Check the required attributes right after fetching them
        switch (tagName) {

            case "bpmn:messageFlow":
                String msgId = element.getAttribute("id");
                String eventSourceId = element.getAttribute("sourceRef");
                String eventTargetId = element.getAttribute("targetRef");

                // Assuming you have methods to get the enclosing participant's or process's ID for an event
                String senderParticipantId = getEnclosingParticipantId(eventSourceId);
                String receiverParticipantId = getEnclosingParticipantId(eventTargetId);

                // Further, if you need names:
                String senderParticipantName = getParticipantNameById(senderParticipantId);
                String receiverParticipantName = getParticipantNameById(receiverParticipantId);

                MessageFLow messageFlow = new MessageFLow(msgId, receiverParticipantId, receiverParticipantName,  senderParticipantId, senderParticipantName, eventTargetId, eventSourceId);
                messageFlows.add(messageFlow);
                return messageFlow;


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
                        break;  // Break out of the loop once the process is found
                    }
                }

                return new PL(participantName, participantId, processIdn, processNamen);


            case "bpmn:collaboration":
                String collaborationId = element.getAttribute("id");
                List<PL> participantsForThisCollaboration = new ArrayList<>();

                // Assuming the collaboration element has children nodes representing participants
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
                            MSE mse = new MSE(name, id, outgoing, messageId, processId,processName);
                            return mse;
                        } else if ("bpmn:signalEventDefinition".equals(childTagName)) {
                            String signalId = childElement.getAttribute("signalRef");

                            String  senderEventId = findThrowingSignalEventBySignalId(signalId);
                            if (senderEventId != null) {

                                String senderSignalParticipantId = getEnclosingParticipantId(senderEventId);

                                String senderSignalParticipantName = getParticipantNameById(senderSignalParticipantId);

                                SSE sse = new SSE(name, id, outgoing, signalId,processId,processName, senderSignalParticipantName);
                                return sse;
                            }
                        } else if ("bpmn:timerEventDefinition".equals(childTagName)) {
                        }
                    }
                }
                NSE nse = new NSE(name, id, outgoing,processId,processName);
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
                            String  senderEventId = findThrowingSignalEventBySignalId(signalId);
                            if (senderEventId != null) {
                                String senderSignalParticipantId = getEnclosingParticipantId(senderEventId);
                                String senderSignalParticipantName = getParticipantNameById(senderSignalParticipantId);
                                SIC sic = new SIC(name, id, incoming, outgoing, signalId,senderSignalParticipantName);
                                return sic;
                            }
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
                            MIT mit = new MIT(name, id, incoming, outgoing, messageId,correspondingMessageFlow);
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
                if (element.hasAttribute("triggeredByEvent")) {
                    String espId = element.getAttribute("id");
                    String espName = element.getAttribute("name");
                    String processEventId = null;  // Initialize outside the if condition
                    String processEventName = null;  // Initialize outside the if condition

                    // navigate to the parent process node
                    Node parent = element.getParentNode();
                    if (parent != null && parent.getNodeType() == Node.ELEMENT_NODE) {
                        Element parentElement = (Element) parent;
                        if ("bpmn:process".equals(parentElement.getNodeName())) {
                            processEventId = parentElement.getAttribute("id");
                            processEventName = parentElement.getAttribute("name");
                        }
                    }

                    return new ESP(espName, espId, processEventId, processEventName);
                }
                break;


            case "bpmn:sequenceFlow":
                String sfId = element.getAttribute("id");
                String sfSourceRef = element.getAttribute("sourceRef");
                String sfTargetRef = element.getAttribute("targetRef");
                SQ sf = new SQ(sfId, sfSourceRef, sfTargetRef);
                return sf;


            case "bpmn:parallelGateway":
                String andSplitId = element.getAttribute("id");
                NodeList andIncomings = element.getElementsByTagName("bpmn:incoming");
                NodeList andOutgoings = element.getElementsByTagName("bpmn:outgoing");
                Integer counter = 0;
                // Check if the AND split has an incoming flow
                if (andIncomings.getLength() == 1) {
                    Map<Integer, List<String>> flowElementMap = new HashMap<>();

                    List<Element> outgoingFlows = new ArrayList<>();
                    for (int j = 0; j < andOutgoings.getLength(); j++) {
                        Element outgoingFlow = factory.getElementByFlowId(andOutgoings.item(j).getTextContent());
                        outgoingFlows.add(outgoingFlow);
                    }

                    outgoingFlows.sort(Comparator.comparing(o -> o.getAttribute("targetRef")));

                    Element andMerge = null;
                    for (Element outgoingFlow : outgoingFlows) {
                        String andNextElementId = outgoingFlow.getAttribute("targetRef");
                        Element andNextElement = factory.getElementById(andNextElementId);

                        List<String> elementList = new ArrayList<>();

                        // Loop through the elements until we reach another gateway or end event
                        while (andNextElement != null && !andNextElement.getTagName().equals("bpmn:parallelGateway")) {
                            // Check if the next element is an end event
                            if (andNextElement.getTagName().equals("bpmn:endEvent")) {
                                elementList.add(andNextElement.getAttribute("id"));
                                break;
                            }

                            // Add the ID of the next element to the list
                            elementList.add(andNextElement.getAttribute("id"));

                            // Get the outgoing flow of the next element
                            NodeList nextElementOutgoings = andNextElement.getElementsByTagName("bpmn:outgoing");
                            if (nextElementOutgoings.getLength() == 0) {
                                break;
                            }
                            Element outgoingFlow2 = factory.getElementByFlowId(nextElementOutgoings.item(0).getTextContent());

                            // Get the ID of the next element in the flow
                            andNextElementId = outgoingFlow2.getAttribute("targetRef");
                            // Get the actual element based on the ID
                            andNextElement = factory.getElementById(andNextElementId);

                            // Check if the next element is an AND merge gateway
                            if (andNextElement.getTagName().equals("bpmn:parallelGateway") && andNextElement.getElementsByTagName("bpmn:incoming").getLength() > 1) {
                                andMerge = andNextElement;
                                break;
                            }
                        }

                        // Store the list of elements associated with the outgoing flow in the flow element map
                        flowElementMap.put(counter, elementList);
                        counter=counter+1;
                    }

                    // Check if the AND split has an AND merge gateway
                    if (andMerge != null) {
                        String andMergeOutgoing = andMerge.getElementsByTagName("bpmn:outgoing").item(0).getTextContent();
                        return new AND(andSplitId, flowElementMap, andMergeOutgoing);
                    }
                }

                return null;


            case "bpmn:exclusiveGateway":
                String xorid = element.getAttribute("id");
                NodeList incomings = element.getElementsByTagName("bpmn:incoming");
                NodeList outgoings = element.getElementsByTagName("bpmn:outgoing");

                if (incomings.getLength() == 1) {
                    // XOR element is a split
                    Map<String, List<String>> conditionElementMap = new HashMap<>();
                    String caOutgoing = null;

                    List<Element> outgoingFlows = new ArrayList<>();
                    for (int j = 0; j < outgoings.getLength(); j++) {
                        Element outgoingFlow = factory.getElementByFlowId(outgoings.item(j).getTextContent());
                        outgoingFlows.add(outgoingFlow);
                    }

                    outgoingFlows.sort(Comparator.comparing(o -> o.getAttribute("targetRef")));

                    for (Element outgoingFlow : outgoingFlows) {
                        String nextElementId = outgoingFlow.getAttribute("targetRef");
                        Element nextElement = factory.getElementById(nextElementId);
                        outgoing = nextElement.getElementsByTagName("bpmn:outgoing").item(0).getTextContent();
                        List<String> elementList = new ArrayList<>();

                        // loop through elements until we reach another gateway or end event
                        Element mergeExclusiveGateway = null;
                        while (nextElement != null && !nextElement.getTagName().equals("bpmn:exclusiveGateway")) {
                            if (nextElement.getTagName().equals("bpmn:endEvent")) {
                                break;
                            }

                            if (nextElement instanceof XOR) {
                                XOR nextXOR = (XOR) nextElement;
                                Map<String, List<String>> nextMap = nextXOR.getConditionElementMap();

                                // get the first element for each condition in the next XOR element
                                for (Map.Entry<String, List<String>> entry : nextMap.entrySet()) {
                                    String firstElementId = entry.getValue().get(0);
                                    Element firstElement = factory.getElementById(firstElementId);
                                    elementList.add(firstElementId);
                                    nextElement = firstElement;
                                }
                            } else {
                                elementList.add(nextElementId);
                                NodeList nextElementOutgoings = nextElement.getElementsByTagName("bpmn:outgoing");
                                if (nextElementOutgoings.getLength() == 0) {
                                    break;
                                }

                                Element outgoingFlow2 = factory.getElementByFlowId(nextElementOutgoings.item(0).getTextContent());
                                nextElementId = outgoingFlow2.getAttribute("targetRef");
                                nextElement = factory.getElementById(nextElementId);
                            }

                            if (nextElement.getTagName().equals("bpmn:exclusiveGateway") && nextElement.getElementsByTagName("bpmn:outgoing").getLength() == 1) {
                                mergeExclusiveGateway = nextElement;
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

                    return new XOR(xorid, conditionElementMap, caOutgoing);
                }

            default:
                return null;
        }

        return null;
    }


    private  Element getElementByFlowId(String flowId) {
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
        // First, find the process that contains the event or signal/message definition
        NodeList processNodes = document.getElementsByTagName("bpmn:process");
        String enclosingProcessId = null;

        // List of BPMN tags we are interested in
        List<String> bpmnTags = Arrays.asList(
                "bpmn:startEvent", "bpmn:intermediateThrowEvent", "bpmn:endEvent",
                "bpmn:signalEventDefinition", "bpmn:messageEventDefinition");

        for (int i = 0; i < processNodes.getLength(); i++) {
            Element processElement = (Element) processNodes.item(i);

            // Loop through each BPMN tag
            for (String bpmnTag : bpmnTags) {
                NodeList bpmnNodes = processElement.getElementsByTagName(bpmnTag);
                for (int j = 0; j < bpmnNodes.getLength(); j++) {
                    Element bpmnElement = (Element) bpmnNodes.item(j);
                    if (elementId.equals(bpmnElement.getAttribute("id"))) {
                        enclosingProcessId = processElement.getAttribute("id");
                        break;
                    }
                }
                if (enclosingProcessId != null) {
                    break;
                }
            }

            if (enclosingProcessId != null) {
                break;
            }
        }

        // Now, find the participant associated with this process
        if (enclosingProcessId != null) {
            NodeList participantNodes = document.getElementsByTagName("bpmn:participant");
            for (int i = 0; i < participantNodes.getLength(); i++) {
                Element participantElement = (Element) participantNodes.item(i);
                if (enclosingProcessId.equals(participantElement.getAttribute("processRef"))) {
                    return participantElement.getAttribute("id");
                }
            }
        }

        return null;
    }
    private String getParticipantNameById(String participantId) {
        NodeList participantNodes = document.getElementsByTagName("bpmn:participant");
        for (int i = 0; i < participantNodes.getLength(); i++) {
            Element participantElement = (Element) participantNodes.item(i);
            if (participantId != null || participantId.equals(participantElement.getAttribute("id"))) {
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
        // Get all signalEventDefinition elements
        NodeList signalEventDefinitions = document.getElementsByTagName("bpmn:signalEventDefinition");

        for (int i = 0; i < signalEventDefinitions.getLength(); i++) {
            Element signalEventDefElement = (Element) signalEventDefinitions.item(i);

            // Check if the signalRef attribute matches the given signalId
            if (signalId.equals(signalEventDefElement.getAttribute("signalRef"))) {
                // If it matches, return the id of the parent event (startEvent or endEvent or any other event)
                return ((Element) signalEventDefElement.getParentNode()).getAttribute("id");
            }
        }

        return null; // Return null if not found
    }


}
