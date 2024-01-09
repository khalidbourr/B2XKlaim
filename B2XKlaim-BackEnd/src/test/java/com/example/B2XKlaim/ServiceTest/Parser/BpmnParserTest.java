package com.example.B2XKlaim.ServiceTest.Parser;


import com.example.B2XKlaim.Service.Parser.BpmnElementFactory;
import com.example.B2XKlaim.Service.bpmnElements.BpmnElement;
import com.example.B2XKlaim.Service.bpmnElements.activities.CLA;
import com.example.B2XKlaim.Service.bpmnElements.activities.ST;
import com.example.B2XKlaim.Service.bpmnElements.events.*;
import com.example.B2XKlaim.Service.bpmnElements.flows.MessageFLow;
import com.example.B2XKlaim.Service.bpmnElements.flows.SQ;
import com.example.B2XKlaim.Service.bpmnElements.objects.pool.Collab;
import com.example.B2XKlaim.Service.bpmnElements.objects.pool.PL;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.StringReader;

import static com.example.B2XKlaim.utile.XmlUtiles.convertStringToXMLDocument;
import static com.example.B2XKlaim.utile.XmlUtiles.retrieveStringFromFile;
import static org.junit.jupiter.api.Assertions.*;

public class BpmnParserTest {


    @Test
    public void test_createNEE() {
        // XML snippet representing an End Event in BPMN
        String xmlSnippet = "<bpmn:endEvent id='endEvent_1' name='End Event'>" +
                "<bpmn:incoming>Flow_1</bpmn:incoming>" +
                "</bpmn:endEvent>";

        Document doc = convertStringToXMLDocument(xmlSnippet);
        Element endEventElement = (Element) doc.getFirstChild();
        BpmnElementFactory factory = new BpmnElementFactory(doc);

        // Act
        BpmnElement bpmnElement = factory.createBpmnElement(endEventElement);

        // Assert
        assertTrue(bpmnElement instanceof NEE);
        assertEquals("End Event", ((NEE) bpmnElement).getName());
        assertEquals("endEvent_1", ((NEE) bpmnElement).getId());
        // Additional assertions as needed
    }

    @Test
    public void test_createST() {
        // XML snippet representing an End Event in BPMN
        String xmlSnippet = "<bpmn:scriptTask id='stId' name='stName'>" +
                "<bpmn:incoming>stIncoming</bpmn:incoming>" +
                "<bpmn:outgoing>stOutgoingEdge</bpmn:outgoing>" +
                "</bpmn:scriptTask>";

        Document doc = convertStringToXMLDocument(xmlSnippet);
        Element endEventElement = (Element) doc.getFirstChild();
        BpmnElementFactory factory = new BpmnElementFactory(doc);

        // Act
        BpmnElement bpmnElement = factory.createBpmnElement(endEventElement);

        // Assert
        assertTrue(bpmnElement instanceof ST);
        assertEquals("stName", ((ST) bpmnElement).getName());
        assertEquals("stId", ((ST) bpmnElement).getId());
        assertEquals("stIncoming", ((ST) bpmnElement).getIncoming());
        assertEquals("stOutgoingEdge", ((ST) bpmnElement).getOutgoingEdge());
        // Additional assertions as needed
    }

    @Test
    public void test_createSQ() {
        // XML snippet representing an End Event in BPMN
        String xmlSnippet = "<bpmn:sequenceFlow id='sequenceFlow_1' sourceRef='task_1' targetRef='task_2'/>";

        Document doc = convertStringToXMLDocument(xmlSnippet);
        Element endEventElement = (Element) doc.getFirstChild();
        BpmnElementFactory factory = new BpmnElementFactory(doc);

        // Act
        BpmnElement bpmnElement = factory.createBpmnElement(endEventElement);

        // Assert
        assertTrue(bpmnElement instanceof SQ);
        assertEquals("sequenceFlow_1", ((SQ) bpmnElement).getId());
        assertEquals("task_1", ((SQ) bpmnElement).getSource());
        assertEquals("task_2", ((SQ) bpmnElement).getTarget());
    }

    @Test
    public void test_createCLA() {
        // XML snippet representing an End Event in BPMN
        String xmlSnippet = "<bpmn:callActivity id=\"Activity_07tpo1r\" name=\"activity\" calledElement=\"activity_1\">\n" +
                "      <bpmn:incoming>Flow_1iyldqj</bpmn:incoming>\n" +
                "      <bpmn:outgoing>Flow_0m06jbw</bpmn:outgoing>\n" +
                "    </bpmn:callActivity>";

        Document doc = convertStringToXMLDocument(xmlSnippet);
        Element endEventElement = (Element) doc.getFirstChild();
        BpmnElementFactory factory = new BpmnElementFactory(doc);

        // Act
        BpmnElement bpmnElement = factory.createBpmnElement(endEventElement);

        // Assert
        assertTrue(bpmnElement instanceof CLA);
        assertEquals("Activity_07tpo1r", ((CLA) bpmnElement).getId());
        assertEquals("activity", ((CLA) bpmnElement).getName());
        assertEquals("activity_1", ((CLA) bpmnElement).getCalledProcess());
        assertEquals("Flow_0m06jbw", ((CLA) bpmnElement).getOutgoingEdge());

    }

    @Test
    public void test_createCollab() {
        // XML snippet representing an End Event in BPMN
        String xmlSnippet = "  <bpmn:collaboration id=\"Collaboration_0l301ir\">\n" +
                "    <bpmn:participant id=\"Participant_0i30hvd\" name=\"participant1\" processRef=\"Process_0d01xqv\" />\n" +
                "    <bpmn:participant id=\"Participant_0ksydy8\" name=\"participant2\" processRef=\"Process_0h2wusu\" />\n" +
                "  </bpmn:collaboration>";

        Document doc = convertStringToXMLDocument(xmlSnippet);
        Element endEventElement = (Element) doc.getFirstChild();
        BpmnElementFactory factory = new BpmnElementFactory(doc);

        // Act
        BpmnElement bpmnElement = factory.createBpmnElement(endEventElement);

        // Assert
        assertTrue(bpmnElement instanceof Collab);
        assertEquals("Collaboration_0l301ir", ((Collab) bpmnElement).getId());
        assertEquals(2, ((Collab) bpmnElement).getParticipants().size());
    }

    @Test
    public void test_createParticipant() {
        // XML snippet representing an End Event in BPMN
        String xmlSnippet = "    <bpmn:participant id=\"Participant_0i30hvd\" name=\"participant1\" processRef=\"Process_0d01xqv\" />\n";

        Document doc = convertStringToXMLDocument(xmlSnippet);
        Element endEventElement = (Element) doc.getFirstChild();
        BpmnElementFactory factory = new BpmnElementFactory(doc);

        // Act
        BpmnElement bpmnElement = factory.createBpmnElement(endEventElement);

        // Assert
        assertTrue(bpmnElement instanceof PL);
        assertEquals("participant1", ((PL) bpmnElement).getName());
        assertEquals("Participant_0i30hvd", ((PL) bpmnElement).getId());
        assertEquals("Process_0d01xqv", ((PL) bpmnElement).getProcessId());
    }

    /*@Test
    public void test_createMessageFlow() {
        // XML snippet representing an End Event in BPMN
        Document doc = convertStringToXMLDocument(retrieveStringFromFile("MessageFlow"));
        Element endEventElement = (Element) doc.getFirstChild();
        BpmnElementFactory factory = new BpmnElementFactory(doc);

        // Act
        BpmnElement bpmnElement = factory.createBpmnElement(endEventElement);

        // Assert
        //assertTrue(bpmnElement instanceof MessageFLow);
        assertEquals("Participant_03kgg88", ((MessageFLow) bpmnElement).getReceiverId());
        assertEquals("Robot2", ((MessageFLow) bpmnElement).getReceiverName());
        assertEquals("Participant_0vo1kge", ((MessageFLow) bpmnElement).getSenderId());
        assertEquals("Robot1", ((MessageFLow) bpmnElement).getSenderName());
        assertEquals("Event_15ue4r5", ((MessageFLow) bpmnElement).getTargetRef());
        assertEquals("Event_1px66ux", ((MessageFLow) bpmnElement).getSourceRef());
    }

    @Test
    public void test_createSIT() {
        // XML snippet representing an End Event in BPMN
        String xmlSnippet = "<bpmn:endEvent id='endEvent_1' name='End Event'>" +
                "<bpmn:incoming>Flow_1</bpmn:incoming>" +
                "</bpmn:endEvent>";

        Document doc = convertStringToXMLDocument(xmlSnippet);
        Element endEventElement = (Element) doc.getFirstChild();
        BpmnElementFactory factory = new BpmnElementFactory(doc);

        // Act
        BpmnElement bpmnElement = factory.createBpmnElement(endEventElement);

        // Assert
        assertTrue(bpmnElement instanceof SIT);
        assertEquals("name", ((SIT) bpmnElement).getName());
        assertEquals("id", ((SIT) bpmnElement).getId());
        assertEquals("incomingEdge", ((SIT) bpmnElement).getIncomingEdge());
        assertEquals("outgoingEdge", ((SIT) bpmnElement).getOutgoingEdge());
        assertEquals("signalId", ((SIT) bpmnElement).getSignalId());
    }

    @Test
    public void test_createTCE() {
        // XML snippet representing an End Event in BPMN
        String xmlSnippet = "<bpmn:endEvent id='endEvent_1' name='End Event'>" +
                "<bpmn:incoming>Flow_1</bpmn:incoming>" +
                "</bpmn:endEvent>";

        Document doc = convertStringToXMLDocument(xmlSnippet);
        Element endEventElement = (Element) doc.getFirstChild();
        BpmnElementFactory factory = new BpmnElementFactory(doc);

        // Act
        BpmnElement bpmnElement = factory.createBpmnElement(endEventElement);


        // Assert
        assertTrue(bpmnElement instanceof TCE);
        assertEquals("name", ((TCE) bpmnElement).getName());
        assertEquals("id", ((TCE) bpmnElement).getId());
        assertEquals(1000L, ((TCE) bpmnElement).getDuration());
        assertEquals("outgoingEdge", ((TCE) bpmnElement).getOutgoingEdge());
    }

    @Test
    public void test_createSEE() {
        // XML snippet representing an End Event in BPMN
        String xmlSnippet = "<bpmn:endEvent id='endEvent_1' name='End Event'>" +
                "<bpmn:incoming>Flow_1</bpmn:incoming>" +
                "</bpmn:endEvent>";

        Document doc = convertStringToXMLDocument(xmlSnippet);
        Element endEventElement = (Element) doc.getFirstChild();
        BpmnElementFactory factory = new BpmnElementFactory(doc);

        // Act
        BpmnElement bpmnElement = factory.createBpmnElement(endEventElement);

        // Assert
        assertTrue(bpmnElement instanceof SEE);
        assertEquals("name", ((SEE) bpmnElement).getName());
        assertEquals("id", ((SEE) bpmnElement).getId());
        assertEquals("incoming", ((SEE) bpmnElement).getIncomingEdge());
        assertEquals("signalId", ((SEE) bpmnElement).getSignalId());
    }

    @Test
    public void test_createMEE() {
        // XML snippet representing an End Event in BPMN
        String xmlSnippet = "<bpmn:endEvent id='endEvent_1' name='End Event'>" +
                "<bpmn:incoming>Flow_1</bpmn:incoming>" +
                "</bpmn:endEvent>";

        Document doc = convertStringToXMLDocument(xmlSnippet);
        Element endEventElement = (Element) doc.getFirstChild();
        BpmnElementFactory factory = new BpmnElementFactory(doc);

        // Act
        BpmnElement bpmnElement = factory.createBpmnElement(endEventElement);

        // Assert
        assertTrue(bpmnElement instanceof MEE);
        assertEquals("name", ((MEE) bpmnElement).getName());
        assertEquals("id", ((MEE) bpmnElement).getId());
        assertEquals("incoming", ((MEE) bpmnElement).getIncomingEdge());
        assertEquals("messageId", ((MEE) bpmnElement).getMessageId());
        assertNotNull(((MEE) bpmnElement).getMessageFlow());
    }

    @Test
    public void test_createNSE() {
        // XML snippet representing an End Event in BPMN
        String xmlSnippet = "<bpmn:endEvent id='endEvent_1' name='End Event'>" +
                "<bpmn:incoming>Flow_1</bpmn:incoming>" +
                "</bpmn:endEvent>";

        Document doc = convertStringToXMLDocument(xmlSnippet);
        Element endEventElement = (Element) doc.getFirstChild();
        BpmnElementFactory factory = new BpmnElementFactory(doc);

        // Act
        BpmnElement bpmnElement = factory.createBpmnElement(endEventElement);

        // Assert
        assertTrue(bpmnElement instanceof NSE);
        assertEquals("name", ((NSE) bpmnElement).getName());
        assertEquals("id", ((NSE) bpmnElement).getId());
        assertEquals("outgoing", ((NSE) bpmnElement).getOutgoingEdge());
        assertEquals("ProcessId", ((NSE) bpmnElement).getProcessId());
        assertEquals("ProcessName", ((NSE) bpmnElement).getProcessName());
    }

*/
}






