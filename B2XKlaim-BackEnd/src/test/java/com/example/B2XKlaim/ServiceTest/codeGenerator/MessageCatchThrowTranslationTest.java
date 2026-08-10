package com.example.B2XKlaim.ServiceTest.codeGenerator;

import com.example.B2XKlaim.Service.Parser.BpmnParser;
import com.example.B2XKlaim.Service.bpmnElements.BpmnElements;
import com.example.B2XKlaim.Service.codeGenerator.BPMNTranslator;
import com.example.B2XKlaim.Service.bpmnElements.objects.pool.Collab;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end check for message catch/throw with Extension Properties payloads.
 * Robot1 reads data object 'pose', sends it via Message_PoseUpdate to Robot2;
 * Robot2 receives it and writes it into data object 'cmd'.
 */
public class MessageCatchThrowTranslationTest {

    @Test
    public void test_catch_uses_panel_payload_verbatim() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream("MessageCatchThrow.bpmn");
        assertNotNull(is, "Test resource MessageCatchThrow.bpmn not found");
        String xml = new String(is.readAllBytes(), StandardCharsets.UTF_8);

        BpmnParser parser = new BpmnParser();
        BpmnElements parsedElements = parser.parse(xml);
        parsedElements.analyzeInteractions();

        BPMNTranslator translator = new BPMNTranslator(parsedElements);
        Collab collab = parsedElements.getElementsByType(Collab.class).get(0);
        String code = translator.visit(collab);

        // Throw side: read the source data object, then out the selected fields
        assertTrue(code.contains(
                "read('pose', var Double x, var Double y)@self"),
                "Throwing side must read the connected source data object");
        assertTrue(code.contains(
                "out('Message_PoseUpdate', x, y)@robot2_loc"),
                "Throwing side must send the selected payload fields to the receiver");

        // Catch side: receive into the panel-declared names, drop existing tuple,
        // write received values into the connected target data object
        assertTrue(code.contains(
                "in('Message_PoseUpdate', var Double x, var Double y)@self"),
                "Catch must bind the message payload using the panel names/types");
        assertTrue(code.contains(
                "in('cmd', var Double dummy_x, var Double dummy_y)@self"),
                "Catch must consume the target data object tuple into dummy vars");
        assertTrue(code.contains(
                "out('cmd', x, y)@self"),
                "Catch must write the received values into the target data object");
        assertTrue(code.contains("out('Flow_3')@self"),
                "Catch must continue along its outgoing edge");
    }
}