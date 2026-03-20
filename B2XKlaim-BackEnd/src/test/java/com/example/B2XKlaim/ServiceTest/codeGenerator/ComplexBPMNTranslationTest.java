package com.example.B2XKlaim.ServiceTest.codeGenerator;

import com.example.B2XKlaim.Service.Parser.BpmnParser;
import com.example.B2XKlaim.Service.bpmnElements.BpmnElements;
import com.example.B2XKlaim.Service.codeGenerator.BPMNTranslator;
import com.example.B2XKlaim.Service.bpmnElements.objects.pool.Collab;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration test: parses a complex BPMN XML file and verifies
 * the full X-Klaim translation output.
 *
 * The test BPMN (ComplexCollaboration.bpmn) contains:
 * - 2 participants (Explorer, Worker) with 4 message flows
 * - Call activities (Explore, WorkAlone, WorkTogether, MoveTo)
 * - Event-based gateway with 3 branches (message catch, message catch, timer)
 * - XOR gateways (diverging and merging)
 * - Script tasks (sd, Here, wdf, xd, Check)
 * - Timer intermediate catch event (30 sec)
 * - Message intermediate throw/catch events
 * - Terminate end event
 * - Event sub-process "Battery_check" with timer start, XOR, signal end
 * - Event sub-process "Handler2" with signal start, script task, normal end
 */
public class ComplexBPMNTranslationTest {

    private String translatedCode;

    @BeforeEach
    public void setUp() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream("ComplexCollaboration.bpmn");
        assertNotNull(is, "Test resource ComplexCollaboration.bpmn not found");
        String xml = new String(is.readAllBytes(), StandardCharsets.UTF_8);

        BpmnParser parser = new BpmnParser();
        BpmnElements parsedElements = parser.parse(xml);
        parsedElements.analyzeInteractions();

        BPMNTranslator translator = new BPMNTranslator(parsedElements);
        Collab collab = parsedElements.getElementsByType(Collab.class).get(0);
        translatedCode = translator.visit(collab);

        assertNotNull(translatedCode, "Translation should not return null");
        assertFalse(translatedCode.isEmpty(), "Translation should not be empty");
    }

    // ── Network Structure ──

    @Test
    public void test_network_definition() {
        assertTrue(translatedCode.contains("net Collaboration_17a6cz8 physical"),
                "Should contain network definition with collaboration ID");
    }

    @Test
    public void test_explorer_node() {
        assertTrue(translatedCode.contains("node Explorer"),
                "Should contain Explorer node");
        assertTrue(translatedCode.contains("eval(new MissionRobot1(Worker))@self"),
                "Explorer should instantiate MissionRobot1 with Worker as argument");
    }

    @Test
    public void test_worker_node() {
        assertTrue(translatedCode.contains("node Worker"),
                "Should contain Worker node");
        assertTrue(translatedCode.contains("eval(new MissionRobot2(Explorer))@self"),
                "Worker should instantiate MissionRobot2 with Explorer as argument");
    }

    // ── Process Definitions with Locality Parameters ──

    @Test
    public void test_explorer_process_with_locality() {
        assertTrue(translatedCode.contains("proc MissionRobot1(Locality worker_loc)"),
                "MissionRobot1 should have Worker locality parameter");
    }

    @Test
    public void test_worker_process_with_locality() {
        assertTrue(translatedCode.contains("proc MissionRobot2(Locality explorer_loc)"),
                "MissionRobot2 should have Explorer locality parameter");
    }

    // ── Explorer: Call Activities ──

    @Test
    public void test_explore_call_activity() {
        assertTrue(translatedCode.contains("eval(new Explore("),
                "Explorer should call Explore sub-process");
    }

    @Test
    public void test_work_alone_call_activity() {
        assertTrue(translatedCode.contains("eval(new WorkAlone("),
                "Explorer should call WorkAlone sub-process");
    }

    @Test
    public void test_work_together_call_activity() {
        assertTrue(translatedCode.contains("eval(new WorkTogether("),
                "Explorer should call WorkTogether sub-process");
    }

    // ── Explorer: Message Sending ──

    @Test
    public void test_explorer_sends_position() {
        assertTrue(translatedCode.contains("out('Message_217d5nt')@worker_loc"),
                "Explorer should send position message to Worker using locality variable");
    }

    // ── Explorer: Event-Based Gateway (polling structure) ──

    @Test
    public void test_event_based_gateway_polling_structure() {
        assertTrue(translatedCode.contains("while (!eventOccurred)"),
                "Event-based gateway should generate polling loop");
    }

    @Test
    public void test_event_based_gateway_timer_branch() {
        assertTrue(translatedCode.contains("System.currentTimeMillis() - currentTime > 30"),
                "Timer branch should check 30ms elapsed");
    }

    @Test
    public void test_event_based_gateway_join_message_branch() {
        assertTrue(translatedCode.contains("in('Message_2g93ger')@self within pollTimeOut"),
                "Join message branch should poll for Message_2g93ger");
    }

    @Test
    public void test_event_based_gateway_not_join_message_branch() {
        assertTrue(translatedCode.contains("in('Message_0rm7e5v')@self within pollTimeOut"),
                "NotJoin message branch should poll for Message_0rm7e5v");
    }

    @Test
    public void test_event_based_gateway_arrival_in_join_path() {
        // After receiving join message, Explorer waits for arrival notification
        assertTrue(translatedCode.contains("in('Message_0cvmukp'"),
                "Join path should wait for arrival notification (Message_0cvmukp)");
    }

    // ── Explorer: Script Task and End ──

    @Test
    public void test_script_task_sd() {
        assertTrue(translatedCode.contains("eval(new sd("),
                "Explorer should eval script task 'sd'");
    }

    // ── Worker: Message Start Event ──

    @Test
    public void test_worker_receives_position() {
        assertTrue(translatedCode.contains("in('Message_217d5nt'"),
                "Worker should receive position message");
    }

    // ── Event Sub-Process: Battery_check ──

    @Test
    public void test_battery_check_esp_eval() {
        assertTrue(translatedCode.contains("eval(new Battery_check())@self"),
                "Explorer should eval Battery_check event sub-process");
    }

    // ── Event Sub-Process: Handler2 ──

    @Test
    public void test_handler2_esp_eval() {
        assertTrue(translatedCode.contains("eval(new Handler2())@self"),
                "Worker should eval Handler2 event sub-process");
    }

    // ── Code Quality Checks ──

    @Test
    public void test_no_error_markers() {
        assertFalse(translatedCode.contains("ERROR"),
                "Generated code should not contain ERROR markers");
    }

    @Test
    public void test_no_null_localities() {
        assertFalse(translatedCode.contains("@null"),
                "Generated code should not have @null locality references");
    }

    @Test
    public void test_balanced_braces() {
        long open = translatedCode.chars().filter(c -> c == '{').count();
        long close = translatedCode.chars().filter(c -> c == '}').count();
        assertEquals(open, close, "All braces should be balanced");
    }
}