package com.example.B2XKlaim.ServiceTest.codeGenerator;

import com.example.B2XKlaim.Service.bpmnElements.BpmnElement;
import com.example.B2XKlaim.Service.bpmnElements.BpmnElements;
import com.example.B2XKlaim.Service.bpmnElements.activities.CLA;
import com.example.B2XKlaim.Service.bpmnElements.activities.ESP;
import com.example.B2XKlaim.Service.bpmnElements.activities.ST;
import com.example.B2XKlaim.Service.bpmnElements.events.*;
import com.example.B2XKlaim.Service.bpmnElements.flows.MessageFLow;
import com.example.B2XKlaim.Service.bpmnElements.flows.SQ;
import com.example.B2XKlaim.Service.bpmnElements.gateways.AND;
import com.example.B2XKlaim.Service.bpmnElements.gateways.LP;
import com.example.B2XKlaim.Service.bpmnElements.gateways.XOR;
import com.example.B2XKlaim.Service.bpmnElements.objects.DO;
import com.example.B2XKlaim.Service.bpmnElements.objects.Field;
import com.example.B2XKlaim.Service.bpmnElements.objects.pool.Collab;
import com.example.B2XKlaim.Service.bpmnElements.objects.pool.PL;
import com.example.B2XKlaim.Service.codeGenerator.BPMNTranslator;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BPMNTranslator visit methods — each test verifies the X-Klaim
 * code output for a specific BPMN element type.
 */
public class BPMNTranslatorTest {

    // Helper: create a minimal BpmnElements container with given elements
    private BpmnElements buildElements(BpmnElement... elements) {
        BpmnElements bpmnElements = new BpmnElements();
        for (BpmnElement el : elements) {
            bpmnElements.addElement(el);
        }
        return bpmnElements;
    }

    private BPMNTranslator translatorFor(BpmnElement... elements) {
        BpmnElements bpmnElements = buildElements(elements);
        bpmnElements.analyzeInteractions();
        return new BPMNTranslator(bpmnElements);
    }

    // ── Start Events ──

    @Test
    public void test_NSE_translation() {
        NSE nse = NSE.builder().id("start1").outgoingEdge("flow1").build();
        BPMNTranslator translator = translatorFor(nse);

        String result = translator.visit(nse);

        assertEquals("out('flow1')@self\n", result);
    }

    @Test
    public void test_MSE_translation() {
        MSE mse = MSE.builder().id("start1").messageId("msg1").outgoingEdge("flow1").build();
        BPMNTranslator translator = translatorFor(mse);

        String result = translator.visit(mse);

        assertTrue(result.contains("in('msg1'"));
        assertTrue(result.contains("out('flow1')@self"));
    }

    @Test
    public void test_SSE_translation() {
        SSE sse = SSE.builder().id("start1").signalId("sig1").SignalSenderName("self").outgoingEdge("flow1").build();
        BPMNTranslator translator = translatorFor(sse);

        String result = translator.visit(sse);

        assertTrue(result.contains("read('sig1'"));
        assertTrue(result.contains("out('flow1')@self"));
    }

    @Test
    public void test_TSE_translation() throws Exception {
        TSE tse = TSE.builder().id("start1").Duration(5000L).outgoingEdge("flow1").build();
        BPMNTranslator translator = translatorFor(tse);

        String result = translator.visit(tse);

        assertTrue(result.contains("Thread.sleep(5000)"));
        assertTrue(result.contains("out('flow1')@self"));
    }

    // ── Intermediate Catch Events ──

    @Test
    public void test_MIC_translation() {
        MIC mic = MIC.builder().id("mid1").messageId("msg1").outgoingEdge("flow1").build();
        BPMNTranslator translator = translatorFor(mic);

        String result = translator.visit(mic);

        assertTrue(result.contains("in('msg1'"));
        assertTrue(result.contains("out('flow1')@self"));
    }

    @Test
    public void test_SIC_translation() {
        SIC sic = SIC.builder().id("mid1").signalId("sig1").SignalSenderName("self").outgoingEdge("flow1").build();
        BPMNTranslator translator = translatorFor(sic);

        String result = translator.visit(sic);

        assertTrue(result.contains("read('sig1'"));
        assertTrue(result.contains("out('flow1')@self"));
    }

    @Test
    public void test_TCE_translation() throws Exception {
        TCE tce = TCE.builder().id("mid1").Duration(3000L).outgoingEdge("flow1").build();
        BPMNTranslator translator = translatorFor(tce);

        String result = translator.visit(tce);

        assertTrue(result.contains("Thread.sleep(3000)"));
        assertTrue(result.contains("out('flow1')@self"));
    }

    // ── Intermediate Throw Events ──

    @Test
    public void test_MIT_translation() {
        MessageFLow flow = MessageFLow.builder().receiverName("robot2").build();
        MIT mit = MIT.builder().id("throw1").messageId("msg1").outgoingEdge("flow1").messageFlow(flow).build();
        BPMNTranslator translator = translatorFor(mit);

        String result = translator.visit(mit);

        assertTrue(result.contains("out('msg1')@robot2"));
        assertTrue(result.contains("out('flow1')@self"));
    }

    @Test
    public void test_SIT_translation() {
        SIT sit = SIT.builder().id("throw1").signalId("sig1").outgoingEdge("flow1").build();
        BPMNTranslator translator = translatorFor(sit);

        String result = translator.visit(sit);

        assertTrue(result.contains("out('sig1')@self"));
        assertTrue(result.contains("in('sig1')@self"));
        assertTrue(result.contains("out('flow1')@self"));
    }

    // ── End Events ──

    @Test
    public void test_NEE_translation() {
        NEE nee = NEE.builder().id("end1").build();
        BPMNTranslator translator = translatorFor(nee);

        String result = translator.visit(nee);

        assertEquals("\n", result);
    }

    @Test
    public void test_MEE_translation() {
        MessageFLow flow = MessageFLow.builder().receiverName("robot2").build();
        MEE mee = MEE.builder().id("end1").messageId("msg1").messageFlow(flow).build();
        BPMNTranslator translator = translatorFor(mee);

        String result = translator.visit(mee);

        assertTrue(result.contains("out('msg1')@robot2"));
    }

    @Test
    public void test_SEE_translation() {
        SEE see = SEE.builder().id("end1").signalId("sig1").build();
        BPMNTranslator translator = translatorFor(see);

        String result = translator.visit(see);

        assertTrue(result.contains("out('sig1')@self"));
        assertTrue(result.contains("in('sig1')@self"));
    }

    @Test
    public void test_TEE_translation() throws Exception {
        TEE tee = TEE.builder().id("end1").build();
        BPMNTranslator translator = translatorFor(tee);

        String result = translator.visit(tee);

        assertTrue(result.contains("exit"));
    }

    // ── Sequence Flow ──

    @Test
    public void test_SQ_translation() {
        SQ sq = SQ.builder().id("flow1").source("a").target("b").build();
        BPMNTranslator translator = translatorFor(sq);

        String result = translator.visit(sq);

        assertEquals("in('flow1')@self\n\n", result);
    }

    // ── Activities ──

    @Test
    public void test_ST_translation() {
        ST st = ST.builder().name("TaskA").id("st1").outgoingEdge("flow1").build();
        BPMNTranslator translator = translatorFor(st);

        String result = translator.visit(st);

        assertTrue(result.contains("eval(new TaskA('flow1'"));
        assertTrue(result.contains("))@self"));
    }

    @Test
    public void test_ST_missing_name() {
        ST st = ST.builder().id("st1").outgoingEdge("flow1").build();
        BPMNTranslator translator = translatorFor(st);

        String result = translator.visit(st);

        assertTrue(result.contains("no name"), "Should warn about missing name");
    }

    @Test
    public void test_ST_proc_no_data() {
        ST st = ST.builder().name("Scan").id("st1").outgoingEdge("flow1").build();
        BPMNTranslator translator = translatorFor(st);

        String proc = translator.generateScriptTaskProc(st);

        assertTrue(proc.startsWith("proc Scan(String edge) {"), "Header missing");
        assertTrue(proc.contains("out(edge)@self"), "Outgoing edge emission missing");
        assertFalse(proc.contains("read("), "Should not emit reads without inputs");
        assertFalse(proc.contains("= null"), "Should not declare locals without outputs");
    }

    @Test
    public void test_ST_proc_with_inputs() {
        DO sensor = DO.builder()
                .id("sensorDoRef").name("sensor")
                .fields(Arrays.asList(
                        Field.builder().name("reading").type("Double").build(),
                        Field.builder().name("ts").type("Long").build()))
                .build();
        ST st = ST.builder()
                .name("Analyze").id("st1").outgoingEdge("flow1")
                .dataInputRefs(new ArrayList<>(Arrays.asList("sensorDoRef")))
                .build();
        BPMNTranslator translator = translatorFor(st, sensor);

        String proc = translator.generateScriptTaskProc(st);

        assertTrue(proc.contains("read('sensor', var Double sensor_reading, var Long sensor_ts)@self"),
                "Input read missing");
        assertFalse(proc.contains("dummy_"), "No output dummies expected when no outputs");
        assertTrue(proc.contains("out(edge)@self"));
    }

    @Test
    public void test_ST_proc_with_outputs() {
        DO scanResult = DO.builder()
                .id("scanDoRef").name("scan")
                .fields(Arrays.asList(
                        Field.builder().name("count").type("Integer").build(),
                        Field.builder().name("label").type("String").build()))
                .build();
        ST st = ST.builder()
                .name("Detect").id("st1").outgoingEdge("flow1")
                .dataOutputRefs(new ArrayList<>(Arrays.asList("scanDoRef")))
                .build();
        BPMNTranslator translator = translatorFor(st, scanResult);

        String proc = translator.generateScriptTaskProc(st);

        assertTrue(proc.contains("var Integer scan_count = null"), "Output local init missing");
        assertTrue(proc.contains("var String scan_label = null"), "Output local init missing");
        assertTrue(proc.contains("in('scan', var Integer dummy_count, var String dummy_label)@self"),
                "Dummy consumption missing");
        assertTrue(proc.contains("out('scan', scan_count, scan_label)@self"),
                "Updated output emission missing");

        // Ordering: null-init must precede sigma placeholder, which must precede in/out update.
        int initIdx = proc.indexOf("var Integer scan_count = null");
        int sigmaIdx = proc.indexOf("Script body");
        int inIdx = proc.indexOf("in('scan'");
        assertTrue(initIdx < sigmaIdx && sigmaIdx < inIdx,
                "Order must be: null-init → script body → in/out update");
    }

    @Test
    public void test_ST_proc_with_inputs_and_outputs() {
        DO sensor = DO.builder()
                .id("sensorDoRef").name("sensor")
                .fields(Arrays.asList(Field.builder().name("v").type("Double").build()))
                .build();
        DO result = DO.builder()
                .id("resultDoRef").name("result")
                .fields(Arrays.asList(Field.builder().name("ok").type("Boolean").build()))
                .build();
        ST st = ST.builder()
                .name("Process").id("st1").outgoingEdge("flow1")
                .dataInputRefs(new ArrayList<>(Arrays.asList("sensorDoRef")))
                .dataOutputRefs(new ArrayList<>(Arrays.asList("resultDoRef")))
                .build();
        BPMNTranslator translator = translatorFor(st, sensor, result);

        String proc = translator.generateScriptTaskProc(st);

        int readIdx = proc.indexOf("read('sensor'");
        int initIdx = proc.indexOf("var Boolean result_ok = null");
        int sigmaIdx = proc.indexOf("Script body");
        int inIdx = proc.indexOf("in('result'");
        int outIdx = proc.indexOf("out('result'");
        int edgeOutIdx = proc.indexOf("out(edge)@self");
        assertTrue(readIdx >= 0 && initIdx > readIdx && sigmaIdx > initIdx
                        && inIdx > sigmaIdx && outIdx > inIdx && edgeOutIdx > outIdx,
                "Order must be: read inputs → null-init outputs → script body → in/out update → out(edge)");
    }

    @Test
    public void test_CLA_translation() throws Exception {
        CLA cla = CLA.builder().name("call1").id("cla1").calledProcess("SubProcess").outgoingEdge("flow1").build();
        BPMNTranslator translator = translatorFor(cla);

        String result = translator.visit(cla);

        assertTrue(result.contains("eval(new SubProcess('flow1'"));
        assertTrue(result.contains("))@self"));
    }

    @Test
    public void test_CLA_missing_calledElement() throws Exception {
        CLA cla = CLA.builder().name("call1").id("cla1").outgoingEdge("flow1").build();
        BPMNTranslator translator = translatorFor(cla);

        String result = translator.visit(cla);

        assertTrue(result.contains("ERROR"), "Should warn about missing calledElement");
    }

    // ── ESP (Event Sub-Process) ──

    @Test
    public void test_ESP_translation() throws Exception {
        // Create internal start event and end event for the ESP
        NSE internalStart = NSE.builder().id("espStart").outgoingEdge("espFlow1").build();
        SQ espFlow = SQ.builder().id("espFlow1").source("espStart").target("espEnd").build();
        NEE internalEnd = NEE.builder().id("espEnd").build();

        ESP esp = ESP.builder()
                .name("ErrorHandler")
                .id("esp1")
                .internalElements(new ArrayList<>(Arrays.asList(internalStart, espFlow, internalEnd)))
                .build();

        // Add all elements to the container so the translator can follow flows
        BpmnElements elements = buildElements(esp, internalStart, espFlow, internalEnd);
        elements.analyzeInteractions();
        BPMNTranslator translator = new BPMNTranslator(elements);

        String result = translator.visit(esp);

        assertTrue(result.contains("proc ErrorHandler()"));
        assertTrue(result.contains("out('espFlow1')@self"));
        assertTrue(result.contains("eval(new ErrorHandler())@self"),
                "ESP body must end with recursive eval to re-arm itself");
        assertFalse(result.contains("while"), "ESP must not use a while loop");
    }

    @Test
    public void test_ESP_with_message_start_and_payload() throws Exception {
        DO alarm = DO.builder()
                .id("alarmDoRef").name("alarm")
                .fields(Arrays.asList(
                        Field.builder().name("level").type("Integer").build(),
                        Field.builder().name("source").type("String").build()))
                .build();

        MSE trigger = MSE.builder()
                .name("AlarmReceived").id("espStart")
                .messageId("alarm_msg")
                .outgoingEdge("espFlow1")
                .targetDataRef("alarmDoRef")
                .payload(List.of(
                        Field.builder().name("level").type("Integer").build(),
                        Field.builder().name("source").type("String").build()))
                .build();
        SQ espFlow = SQ.builder().id("espFlow1").source("espStart").target("espEnd").build();
        NEE internalEnd = NEE.builder().id("espEnd").build();

        ESP esp = ESP.builder()
                .name("AlarmHandler")
                .id("esp1")
                .internalElements(new ArrayList<>(Arrays.asList(trigger, espFlow, internalEnd)))
                .build();

        BpmnElements elements = buildElements(esp, trigger, espFlow, internalEnd, alarm);
        elements.analyzeInteractions();
        BPMNTranslator translator = new BPMNTranslator(elements);

        String result = translator.visit(esp);

        assertTrue(result.contains("proc AlarmHandler()"), "ESP proc header missing");
        assertTrue(result.contains("in('alarm_msg', var Integer level, var String source)@self"),
                "Catch with payload binding missing");
        assertTrue(result.contains("in('alarm', var Integer dummy_level, var String dummy_source)@self"),
                "Dummy DO consumption missing");
        assertTrue(result.contains("out('alarm', level, source)@self"),
                "DO refresh missing");
        assertTrue(result.contains("out('espFlow1')@self"), "Outgoing edge emission missing");
        assertTrue(result.contains("eval(new AlarmHandler())@self"),
                "ESP body must end with recursive eval to re-arm itself");
    }

    @Test
    public void test_ESP_eval_lives_inside_node_block() throws Exception {
        NSE espStart = NSE.builder().id("espStart").outgoingEdge("espFlow").build();
        SQ espFlow = SQ.builder().id("espFlow").source("espStart").target("espEnd").build();
        NEE espEnd = NEE.builder().id("espEnd").build();
        ESP esp = ESP.builder()
                .name("Recovery").id("esp1")
                .processId("process1")
                .internalElements(new ArrayList<>(Arrays.asList(espStart, espFlow, espEnd)))
                .build();

        PL pool = PL.builder()
                .id("pool1").name("Robot1")
                .ProcessId("process1").ProcessName("RobotMain")
                .build();

        BpmnElements elements = buildElements(pool, esp, espStart, espFlow, espEnd);
        elements.analyzeInteractions();
        BPMNTranslator translator = new BPMNTranslator(elements);

        String result = translator.visit(pool);

        assertTrue(result.contains("node Robot1"), "Node header missing");
        assertTrue(result.contains("eval(new RobotMain())@self"), "Main proc eval missing");
        assertTrue(result.contains("eval(new Recovery())@self"), "ESP eval missing from node block");

        int nodeOpenIdx = result.indexOf("node Robot1");
        int nodeCloseIdx = result.indexOf("\t}", nodeOpenIdx);
        int espEvalIdx = result.indexOf("eval(new Recovery())@self");
        int mainEvalIdx = result.indexOf("eval(new RobotMain())@self");
        assertTrue(nodeOpenIdx < espEvalIdx && espEvalIdx < nodeCloseIdx,
                "ESP eval must sit inside the node {} block");
        assertTrue(espEvalIdx < mainEvalIdx,
                "ESP eval must come before the main proc eval");
    }

    // ── Gateways ──

    @Test
    public void test_AND_gateway_translation() throws Exception {
        // Two parallel branches: branch1 has ST "TaskA", branch2 has ST "TaskB"
        ST taskA = ST.builder().name("TaskA").id("taskA").outgoingEdge("flowA").build();
        SQ flowA = SQ.builder().id("flowA").source("taskA").target("merge").build();
        ST taskB = ST.builder().name("TaskB").id("taskB").outgoingEdge("flowB").build();
        SQ flowB = SQ.builder().id("flowB").source("taskB").target("merge").build();

        Map<Integer, List<String>> flowMap = new LinkedHashMap<>();
        flowMap.put(0, Arrays.asList("taskA"));
        flowMap.put(1, Arrays.asList("taskB"));

        AND and = AND.builder().id("and1").outgoingEdge("flowOut").flowElementMap(flowMap).build();

        BpmnElements elements = buildElements(and, taskA, flowA, taskB, flowB);
        elements.analyzeInteractions();
        BPMNTranslator translator = new BPMNTranslator(elements);

        String result = translator.visit(and);

        // Should contain eval calls for branch procs, in() waits, and outgoing flow
        assertTrue(result.contains("eval(new AND_and1_Branch0())@self"));
        assertTrue(result.contains("eval(new AND_and1_Branch1())@self"));
        assertTrue(result.contains("in('flowA')@self"));
        assertTrue(result.contains("in('flowB')@self"));
        assertTrue(result.contains("out('flowOut')@self"));
    }

    @Test
    public void test_AND_branch_does_not_consume_its_terminal_edge() throws Exception {
        // Invariant: each AND branch proc must NOT end with in('<lastEdge>')@self.
        ST taskA = ST.builder().name("TaskA").id("taskA").outgoingEdge("flowA").build();
        SQ flowA = SQ.builder().id("flowA").source("taskA").target("merge").build();
        ST taskB = ST.builder().name("TaskB").id("taskB").outgoingEdge("flowB").build();
        SQ flowB = SQ.builder().id("flowB").source("taskB").target("merge").build();

        Map<Integer, List<String>> flowMap = new LinkedHashMap<>();
        flowMap.put(0, Arrays.asList("taskA"));
        flowMap.put(1, Arrays.asList("taskB"));

        AND and = AND.builder().id("and1").outgoingEdge("flowOut").flowElementMap(flowMap).build();

        BpmnElements elements = buildElements(and, taskA, flowA, taskB, flowB);
        elements.analyzeInteractions();
        BPMNTranslator translator = new BPMNTranslator(elements);

        translator.visit(and);

        List<String> branchProcs = translator.getAuxiliaryProcs();
        assertEquals(2, branchProcs.size(), "Expected one auxiliary proc per AND branch");

        for (String procDef : branchProcs) {
            assertFalse(procDef.contains("in('flowA')@self"),
                "Branch proc must not consume its terminal edge 'flowA': " + procDef);
            assertFalse(procDef.contains("in('flowB')@self"),
                "Branch proc must not consume its terminal edge 'flowB': " + procDef);
        }
    }

    @Test
    public void test_XOR_gateway_translation() throws Exception {
        // True branch with condition, false branch is default
        ST taskTrue = ST.builder().name("TrueTask").id("taskTrue").outgoingEdge("flowTrue").build();
        SQ flowTrue = SQ.builder().id("flowTrue").source("taskTrue").target("merge").build();
        ST taskFalse = ST.builder().name("FalseTask").id("taskFalse").outgoingEdge("flowFalse").build();
        SQ flowFalse = SQ.builder().id("flowFalse").source("taskFalse").target("merge").build();

        Map<String, List<String>> conditionMap = new LinkedHashMap<>();
        conditionMap.put("x > 10", Arrays.asList("taskTrue"));
        conditionMap.put("", Arrays.asList("taskFalse")); // default branch

        XOR xor = XOR.builder().id("xor1").outgoingEdge("flowOut").conditionElementMap(conditionMap).build();

        BpmnElements elements = buildElements(xor, taskTrue, flowTrue, taskFalse, flowFalse);
        elements.analyzeInteractions();
        BPMNTranslator translator = new BPMNTranslator(elements);

        String result = translator.visit(xor);

        assertTrue(result.contains("if(x > 10)"));
        assertTrue(result.contains("eval(new TrueTask("));
        assertTrue(result.contains("} else {"));
        assertTrue(result.contains("eval(new FalseTask("));
        assertTrue(result.contains("out('flowOut')@self"));
    }

    @Test
    public void test_LP_loop_translation() throws Exception {
        ST taskInLoop = ST.builder().name("LoopTask").id("loopTask").outgoingEdge("loopFlow").build();
        SQ loopFlow = SQ.builder().id("loopFlow").source("loopTask").target("loopTask").build();

        LP lp = LP.builder()
                .id("lp1")
                .condition("count < 5")
                .outgoingEdge("flowOut")
                .flowElementMap(Arrays.asList("loopTask"))
                .build();

        BpmnElements elements = buildElements(lp, taskInLoop, loopFlow);
        elements.analyzeInteractions();
        BPMNTranslator translator = new BPMNTranslator(elements);

        String result = translator.visit(lp);

        assertTrue(result.contains("while(count < 5)"));
        assertTrue(result.contains("eval(new LoopTask("));
        assertTrue(result.contains("out('flowOut')@self"));
    }

    // ── Process Traversal (Integration) ──

    @Test
    public void test_simple_process_traversal() throws Exception {
        // Start → Flow1 → ScriptTask → Flow2 → End
        NSE start = NSE.builder().id("start").name("start").outgoingEdge("flow1").ProcessId("proc1").build();
        SQ flow1 = SQ.builder().id("flow1").source("start").target("task1").build();
        ST task1 = ST.builder().name("MyTask").id("task1").outgoingEdge("flow2").build();
        SQ flow2 = SQ.builder().id("flow2").source("task1").target("end").build();
        NEE end = NEE.builder().id("end").name("end").incomingEdge("flow2").build();

        BpmnElements elements = buildElements(start, flow1, task1, flow2, end);
        elements.analyzeInteractions();
        BPMNTranslator translator = new BPMNTranslator(elements);

        String result = translator.translateProcessBody(start);

        // Should contain: out(flow1), in(flow1), eval(MyTask), in(flow2), end
        assertTrue(result.contains("out('flow1')@self"), "Should output start event flow token");
        assertTrue(result.contains("in('flow1')@self"), "Should consume flow1 token");
        assertTrue(result.contains("eval(new MyTask('flow2'"), "Should eval the script task");
        assertTrue(result.contains("in('flow2')@self"), "Should consume flow2 token");
    }

    @Test
    public void test_collaboration_with_message_flow() throws Exception {
        // Two participants exchanging a message
        PL robot1 = PL.builder().name("robot1").id("p1").ProcessId("proc1").ProcessName("Robot1Behavior").build();
        PL robot2 = PL.builder().name("robot2").id("p2").ProcessId("proc2").ProcessName("Robot2Behavior").build();
        Collab collab = new Collab("collab1", Arrays.asList(robot1, robot2));

        // Robot1: start → flow1 → messageEndEvent
        NSE start1 = NSE.builder().id("start1").name("start").outgoingEdge("f1").ProcessId("proc1").ProcessName("Robot1Behavior").build();
        SQ f1 = SQ.builder().id("f1").source("start1").target("mee1").build();

        MessageFLow msgFlow = MessageFLow.builder()
                .id("msgFlow1")
                .senderId("p1").senderName("robot1")
                .receiverId("p2").receiverName("robot2")
                .sourceRef("mee1").targetRef("mse2")
                .build();

        MEE mee1 = MEE.builder().id("mee1").name("end").messageId("msg1").messageFlow(msgFlow).build();

        // Robot2: messageStartEvent → flow2 → end
        MSE mse2 = MSE.builder().id("mse2").name("start").messageId("msg1").outgoingEdge("f2").ProcessId("proc2").ProcessName("Robot2Behavior").build();
        SQ f2 = SQ.builder().id("f2").source("mse2").target("end2").build();
        NEE end2 = NEE.builder().id("end2").name("end").incomingEdge("f2").build();

        BpmnElements elements = buildElements(collab, robot1, robot2, start1, f1, mee1, mse2, f2, end2, msgFlow);
        elements.analyzeInteractions();
        BPMNTranslator translator = new BPMNTranslator(elements);

        String result = translator.visit(collab);

        // Verify collaboration structure
        assertTrue(result.contains("net collab1 physical"), "Should contain network definition");
        assertTrue(result.contains("node robot1"), "Should contain robot1 node");
        assertTrue(result.contains("node robot2"), "Should contain robot2 node");
        assertTrue(result.contains("proc Robot1Behavior"), "Should contain Robot1 process");
        assertTrue(result.contains("proc Robot2Behavior"), "Should contain Robot2 process");
        // Verify message passing
        assertTrue(result.contains("out('msg1')@"), "Robot1 should send message");
        assertTrue(result.contains("in('msg1'"), "Robot2 should receive message");
    }

    // ── Data Objects ──

    @Test
    public void test_DO_translation_typed_defaults() throws Exception {
        DO data = DO.builder()
                .id("DataObjectReference_1")
                .name("position")
                .processId("proc1")
                .fields(List.of(
                        Field.builder().name("x").type("Double").build(),
                        Field.builder().name("y").type("Double").build()))
                .build();
        BPMNTranslator translator = translatorFor(data);

        String result = translator.visit(data);

        assertEquals("out(\"position\", 0.0, 0.0)@self\n", result);
    }

    @Test
    public void test_DO_translation_mixed_types() throws Exception {
        DO data = DO.builder()
                .id("DataObjectReference_1")
                .name("state")
                .processId("proc1")
                .fields(List.of(
                        Field.builder().name("counter").type("Integer").build(),
                        Field.builder().name("active").type("Boolean").build(),
                        Field.builder().name("label").type("String").build(),
                        Field.builder().name("payload").type("CustomType").build()))
                .build();
        BPMNTranslator translator = translatorFor(data);

        String result = translator.visit(data);

        assertEquals("out(\"state\", 0, false, \"\", null)@self\n", result);
    }

    @Test
    public void test_DO_translation_data_input_initial_values() throws Exception {
        DO data = DO.builder()
                .id("DataObjectReference_1")
                .name("target")
                .processId("proc1")
                .fields(List.of(
                        Field.builder().name("x").type("Double").initialValue("30.0").build(),
                        Field.builder().name("y").type("Double").initialValue("12.0").build(),
                        Field.builder().name("label").type("String").initialValue("home").build()))
                .build();
        BPMNTranslator translator = translatorFor(data);

        String result = translator.visit(data);

        assertEquals("out(\"target\", 30.0, 12.0, \"home\")@self\n", result);
    }

    @Test
    public void test_MIC_translation_with_target_data_object() throws Exception {
        DO target = DO.builder()
                .id("DataObjectReference_pose")
                .name("pose")
                .processId("proc1")
                .fields(List.of(
                        Field.builder().name("x").type("Double").build(),
                        Field.builder().name("y").type("Double").build()))
                .build();
        MIC mic = MIC.builder()
                .id("mic1").messageId("PoseUpdate").outgoingEdge("flow1")
                .targetDataRef("DataObjectReference_pose")
                .payload(List.of(
                        Field.builder().name("x").type("Double").build(),
                        Field.builder().name("y").type("Double").build()))
                .build();
        BPMNTranslator translator = translatorFor(target, mic);

        String result = translator.visit(mic);

        String expected =
                "in('PoseUpdate', var Double x, var Double y)@self\n" +
                "in('pose', var Double dummy_x, var Double dummy_y)@self\n" +
                "out('pose', x, y)@self\n" +
                "out('flow1')@self\n";
        assertEquals(expected, result);
    }

    @Test
    public void test_MSE_translation_with_target_data_object() throws Exception {
        DO target = DO.builder()
                .id("DataObjectReference_cmd")
                .name("command")
                .processId("proc1")
                .fields(List.of(
                        Field.builder().name("action").type("String").build()))
                .build();
        MSE mse = MSE.builder()
                .id("mse1").messageId("Start").outgoingEdge("flow1")
                .targetDataRef("DataObjectReference_cmd")
                .payload(List.of(
                        Field.builder().name("action").type("String").build()))
                .build();
        BPMNTranslator translator = translatorFor(target, mse);

        String result = translator.visit(mse);

        String expected =
                "in('Start', var String action)@self\n" +
                "in('command', var String dummy_action)@self\n" +
                "out('command', action)@self\n" +
                "out('flow1')@self\n";
        assertEquals(expected, result);
    }

    @Test
    public void test_MIT_translation_with_source_data_object() throws Exception {
        DO source = DO.builder()
                .id("DataObjectReference_pose")
                .name("pose")
                .processId("proc1")
                .fields(List.of(
                        Field.builder().name("x").type("Double").build(),
                        Field.builder().name("y").type("Double").build()))
                .build();
        MessageFLow flow = MessageFLow.builder().id("mf1").senderId("p1").receiverName("Server").build();
        MIT mit = MIT.builder()
                .id("mit1").messageId("ReportPose").outgoingEdge("flow1")
                .messageFlow(flow)
                .sourceDataRefs(List.of("DataObjectReference_pose"))
                .payload(List.of(
                        Field.builder().name("pose_x").build(),
                        Field.builder().name("pose_y").build()))
                .build();
        BPMNTranslator translator = translatorFor(source, mit);

        String result = translator.visit(mit);

        String expected =
                "read('pose', var Double pose_x, var Double pose_y)@self\n" +
                "out('ReportPose', pose_x, pose_y)@Server\n" +
                "out('flow1')@self\n";
        assertEquals(expected, result);
    }

    @Test
    public void test_MIT_translation_no_payload_falls_back() throws Exception {
        MessageFLow flow = MessageFLow.builder().id("mf1").senderId("p1").receiverName("Server").build();
        MIT mit = MIT.builder()
                .id("mit1").messageId("Ping").outgoingEdge("flow1")
                .messageFlow(flow)
                .build();
        BPMNTranslator translator = translatorFor(mit);

        String result = translator.visit(mit);

        assertEquals("out('Ping')@Server\nout('flow1')@self\n", result);
    }

    @Test
    public void test_MIC_translation_no_target_falls_back() throws Exception {
        MIC mic = MIC.builder()
                .id("mic1").messageId("Ping").outgoingEdge("flow1")
                .build();
        BPMNTranslator translator = translatorFor(mic);

        String result = translator.visit(mic);

        assertEquals("in('Ping')@self\nout('flow1')@self\n", result);
    }

    @Test
    public void test_PL_translation_includes_data_objects() throws Exception {
        PL pool = new PL("Robot", "pool1", "proc1", "RobotBehavior");
        DO data = DO.builder()
                .id("DataObjectReference_1")
                .name("position")
                .processId("proc1")
                .fields(List.of(
                        Field.builder().name("x").type("Double").build(),
                        Field.builder().name("y").type("Double").build()))
                .build();
        Collab collab = new Collab("collab1", List.of(pool));
        BpmnElements elements = buildElements(collab, pool, data);
        elements.analyzeInteractions();
        BPMNTranslator translator = new BPMNTranslator(elements);

        String result = translator.visit(pool);

        assertTrue(result.contains("out(\"position\", 0.0, 0.0)@self"),
                "Pool node body should contain data object emission: " + result);
        assertTrue(result.contains("eval(new RobotBehavior"),
                "Pool node should still contain eval of process: " + result);
        // Data object line should come before eval
        int doIdx = result.indexOf("out(\"position\"");
        int evalIdx = result.indexOf("eval(new RobotBehavior");
        assertTrue(doIdx < evalIdx, "Data object emission should precede eval");
    }
}
