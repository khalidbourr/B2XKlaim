package com.example.B2XKlaim.ServiceTest.codeGenerator;

import com.example.B2XKlaim.Service.bpmnElements.activities.CLA;
import com.example.B2XKlaim.Service.bpmnElements.activities.ESP;
import com.example.B2XKlaim.Service.bpmnElements.activities.ST;
import com.example.B2XKlaim.Service.bpmnElements.events.*;
import com.example.B2XKlaim.Service.bpmnElements.flows.MessageFLow;
import com.example.B2XKlaim.Service.bpmnElements.flows.SQ;
import com.example.B2XKlaim.Service.bpmnElements.objects.pool.Collab;
import com.example.B2XKlaim.Service.bpmnElements.objects.pool.PL;
import com.example.B2XKlaim.Service.codeGenerator.BPMNTranslator;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import static org.junit.Assert.*;


public class BPMNTranslatorTest {
    @Test
    public void test_correct_translation_of_NSE() {
        // Create a new instance of BPMNTranslator
        BPMNTranslator translator = new BPMNTranslator();

        // Create a new NSE object
        NSE nse = new NSE();
        nse.setOutgoingEdge("outgoing_edge");

        // Call the visit method on the NSE object and assert the result
        String result = translator.visit(nse);
        assertEquals("out(outgoing_edge)@self\n", result);
    }

    @Test
    public void test_correct_translation_of_MSE() {
        // Create a new instance of BPMNTranslator
        BPMNTranslator translator = new BPMNTranslator();

        // Create a new MSE object
        MSE mse = new MSE();
        mse.setMessageId("message_id");
        mse.setOutgoingEdge("outgoing_edge");

        // Call the visit method on the MSE object and assert the result
        String result = translator.visit(mse);
        assertEquals("in(message_id)@self\nout(outgoing_edge)@self\n", result);
    }

    @Test
    public void test_correct_translation_of_SSE() {
        // Create a new instance of BPMNTranslator
        BPMNTranslator translator = new BPMNTranslator();

        // Create a new SSE object
        SSE sse = new SSE();
        sse.setSignalId("signal_id");
        sse.setSignalSenderName("signal_sender_name");
        sse.setOutgoingEdge("outgoing_edge");

        // Call the visit method on the SSE object and assert the result
        String result = translator.visit(sse);
        assertEquals("read(signal_id)@signal_sender_name\nout(outgoing_edge)@self\n", result);
    }

    @Test
    public void test_correct_translation_of_MIC() {
        // Create a new instance of BPMNTranslator
        BPMNTranslator translator = new BPMNTranslator();

        // Create a new MIC object
        MIC mic = new MIC();
        mic.setMessageId("message_id");
        mic.setOutgoingEdge("outgoing_edge");

        // Call the visit method on the MIC object and assert the result
        String result = translator.visit(mic);
        assertEquals("in(message_id)@self\nout(outgoing_edge)@self\n", result);
    }

    @Test
    public void test_correct_translation_of_SIC() {
        // Create a new instance of BPMNTranslator
        BPMNTranslator translator = new BPMNTranslator();

        // Create a new SIC object
        SIC sic = new SIC();
        sic.setSignalId("signal_id");
        sic.setSignalSenderName("signal_sender_name");
        sic.setOutgoingEdge("outgoing_edge");

        // Call the visit method on the SIC object and assert the result
        String result = translator.visit(sic);
        assertEquals("read(signal_id)@signal_sender_name\nout(outgoing_edge)@self\n", result);
    }

    @Test
    public void test_correct_translation_of_MIT() {
        MIT mit = new MIT();
        mit.setMessageId("messageId");
        MessageFLow messageFlow = new MessageFLow();
        messageFlow.setReceiverName("receiverName");
        mit.setMessageFlow(messageFlow);
        mit.setOutgoingEdge("outgoingEdge");

        BPMNTranslator translator = new BPMNTranslator();
        String result = translator.visit(mit);

        String expected = "out(messageId)@receiverName\nout(outgoingEdge)@self\n";

        assertEquals(expected, result);
    }

    @Test
    public void test_correct_translation_of_SIT() {
        // Create a SIT object with sample values
        SIT sit = new SIT();
        sit.setSignalId("signal1");
        sit.setOutgoingEdge("edge1");

        // Create a BPMNTranslator object
        BPMNTranslator translator = new BPMNTranslator();

        // Call the visit method and get the translated string
        String translatedString = translator.visit(sit);

        // Assert that the translated string is correct
        assertEquals("out(signal1)@self\nThread.sleep(Signal_Duration)\nin(signal1)@self\nout(edge1)@self\n", translatedString);
    }

    @Test
    public void test_correct_translation_of_NEE() {
        BPMNTranslator translator = new BPMNTranslator();

        NEE nee = new NEE();

        String result = translator.visit(nee);
        assertEquals("Stop()\n", result);
    }

    @Test
    public void test_correct_translation_of_MEE() {
        BPMNTranslator translator = new BPMNTranslator();

        MEE mee = MEE.builder()
                .messageId("message_id")
                .messageFlow(MessageFLow.builder()
                        .receiverName("receiver_name")
                        .build())
                .build();

        String result = translator.visit(mee);
        assertEquals("out(message_id)@receiver_name\nStop()\n", result);
    }

    @Test
    public void test_correct_translation_of_SEE() {
        BPMNTranslator translator = new BPMNTranslator();

        SSE sse = new SSE();
        sse.setSignalId("signal_id");
        sse.setSignalSenderName("signal_sender_name");
        sse.setOutgoingEdge("outgoing_edge");

        String result = translator.visit(sse);
        assertEquals("read(signal_id)@signal_sender_name\nout(outgoing_edge)@self\n", result);

    }

/* To add AND and XOR test here */

    @Test
    public void test_correct_translation_of_ST() {
        ST st = new ST("Task1", "st1", "incoming", "outgoing");
        BPMNTranslator translator = new BPMNTranslator();
        String result = translator.visit(st);
        String expected = "{ ... initialization code ... }\nout(outgoing)@self\n";
        assertEquals(expected, result);
    }

    @Test
    public void test_correct_translation_of_CLA() {
        CLA cla = new CLA("name", "id", "incomingEdge", "outgoingEdge", "calledProcess");
        BPMNTranslator translator = new BPMNTranslator();
        String result = translator.visit(cla);
        assertEquals("eval(new calledProcess(outgoingEdge))@self\n", result);
    }

    @Test
    public void test_correct_translation_of_ESP() {
        // Create an instance of BPMNTranslator
        BPMNTranslator translator = new BPMNTranslator();

        // Create an instance of ESP
        ESP esp = ESP.builder()
                .name("ESP")
                .id("esp1")
                .ProcessId("process1")
                .ProcessName("Process 1")
                .build();

        String translatedCode = translator.visit(esp);
        assertEquals("eval(new esp1())@self\n", translatedCode);
    }

    @Test
    public void test_correct_translation_of_SQ() {
        BPMNTranslator translator = new BPMNTranslator();
        SQ sq = new SQ("sq1", "source", "target");
        String result = translator.visit(sq);
        assertEquals("in(sq1)@self\n\n", result);
    }

    @Test
    public void test_correct_translation_of_PL() throws FileNotFoundException, UnsupportedEncodingException {
        BPMNTranslator translator = new BPMNTranslator();

        PL pl = new PL();
        pl.setName("Participant");
        pl.setProcessId("Process");

        String result = translator.visit(pl);
        assertEquals("\tnode Participant {\n\t\teval(new Process())@self\n\t}\n", result);
    }

    @Test
    public void test_correct_translation_of_Collab() throws FileNotFoundException, UnsupportedEncodingException {
        // Arrange
        Collab collab = Collab.builder()
                .id("collab1")
                .participants(Arrays.asList(
                        PL.builder().name("participant1").id("p1").ProcessId("process1").ProcessName("Process 1").build(),
                        PL.builder().name("participant2").id("p2").ProcessId("process2").ProcessName("Process 2").build()
                ))
                .build();
        BPMNTranslator translator = new BPMNTranslator();

        // Act
        String klaimCode = translator.visit(collab);

        // Assert
        String expectedKlaimCode = "net collab1 physical \"localhost:9999\" {\n" +
                "\tnode participant1 {\n" +
                "\t\teval(new process1())@self\n" +
                "\t}\n" +
                "\tnode participant2 {\n" +
                "\t\teval(new process2())@self\n" +
                "\t}\n" +
                "}\n";
        assertEquals(expectedKlaimCode, klaimCode);
    }




    }