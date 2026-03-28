package com.example.B2XKlaim.ServiceTest.codeGenerator;

import com.example.B2XKlaim.Service.BpmnParsingService;
import com.example.B2XKlaim.Service.CodeGenerationService;
import com.example.B2XKlaim.Service.CodeGenerationService.GenerationResult;
import com.example.B2XKlaim.Service.CodeFormattingService;
import com.example.B2XKlaim.Service.ResponseBuilderService;
import com.example.B2XKlaim.Service.bpmnElements.BpmnElements;
import com.example.B2XKlaim.Service.strategy.CollaborationCodeStrategy;
import com.example.B2XKlaim.Service.strategy.StandaloneProcessStrategy;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exact-match integration tests for Event-Based Gateway BPMN diagrams.
 * Compares each generated xklaim file against the verified expected output
 * (the same files the user gets in the downloaded zip).
 */
public class EBGatewayTranslationTest {

    private static final String BPMN_DIR = "eb_gateway_patterns/";
    private static final String EXPECTED_DIR = "eb_gateway_patterns/expected/";

    @TestFactory
    Stream<DynamicTest> testAllEBGatewayPatterns() throws Exception {
        List<DynamicTest> tests = new ArrayList<>();

        String[] diagrams = {
            "EB_SingleMessage_Timer",
            "EB_TwoMessages",
            "EB_TwoMessages_Timer",
            "EB_Message_Timer_WithXOR",
            "EB_TwoMsg_Timer_WithAND",
            "EB_L3_Message_Timer_XOR_AND",
            "EB_L4_Msg_Timer_XOR_AND_LP"
        };

        for (String diagram : diagrams) {
            Map<String, Object> actual = generateViaFullPipeline(BPMN_DIR + diagram + ".bpmn");
            Map<String, String> expectedFiles = loadExpectedFiles(EXPECTED_DIR + diagram + "/");

            for (Map.Entry<String, String> entry : expectedFiles.entrySet()) {
                String relativePath = entry.getKey();
                String expectedContent = entry.getValue();

                tests.add(DynamicTest.dynamicTest(
                    diagram + " — " + relativePath,
                    () -> {
                        String actualContent = findActualFile(actual, relativePath);
                        assertNotNull(actualContent,
                            diagram + ": missing generated file " + relativePath);
                        assertEquals(expectedContent, actualContent,
                            diagram + ": mismatch in " + relativePath);
                    }
                ));
            }
        }
        return tests.stream();
    }

    // ── Pipeline helpers (identical to GatewayCombinationTranslationTest) ──

    private Map<String, Object> generateViaFullPipeline(String bpmnResource) throws Exception {
        String xml = loadResource(bpmnResource);

        BpmnParsingService parsingService = new BpmnParsingService();
        Map<String, Object> inputProcesses = new HashMap<>();
        inputProcesses.put("main", xml);
        Map<String, BpmnElements> parsed = parsingService.parseMultipleProcesses(inputProcesses);

        CodeGenerationService codeGenService = new CodeGenerationService();
        var collabField = CodeGenerationService.class.getDeclaredField("collaborationCodeStrategy");
        collabField.setAccessible(true);
        collabField.set(codeGenService, new CollaborationCodeStrategy());
        var standaloneField = CodeGenerationService.class.getDeclaredField("standaloneProcessStrategy");
        standaloneField.setAccessible(true);
        standaloneField.set(codeGenService, new StandaloneProcessStrategy());

        GenerationResult result = codeGenService.generateCode(parsed);

        CodeFormattingService formattingService = new CodeFormattingService();
        ResponseBuilderService responseBuilder = new ResponseBuilderService();
        var fmtField = ResponseBuilderService.class.getDeclaredField("codeFormattingService");
        fmtField.setAccessible(true);
        fmtField.set(responseBuilder, formattingService);

        return responseBuilder.buildResponse(result, result.getFullGeneratedCode());
    }

    @SuppressWarnings("unchecked")
    private String findActualFile(Map<String, Object> response, String relativePath) {
        String collab = (String) response.getOrDefault("collaboration", "");
        List<Map<String, String>> processes = (List<Map<String, String>>) response.getOrDefault("processes", List.of());
        Map<String, Object> callActivities = (Map<String, Object>) response.getOrDefault("callActivities", Map.of());
        Map<String, Object> scriptTasks = (Map<String, Object>) response.getOrDefault("scriptTaskProcs", Map.of());
        Map<String, Object> esps = (Map<String, Object>) response.getOrDefault("eventSubProcesses", Map.of());
        Map<String, Object> branches = (Map<String, Object>) response.getOrDefault("andBranchProcs", Map.of());

        String path = relativePath.replace("src/main/java/xklaim/", "");

        if (path.equals("Collaboration.xklaim")) {
            List<String> imports = new ArrayList<>();
            for (var p : processes) imports.add("import xklaim.processes." + p.get("name"));
            for (String bn : branches.keySet())
                if (collab.contains(bn + "(")) imports.add("import xklaim.branches." + bn);
            return "package xklaim\n\n" + String.join("\n", imports) + "\n\n" + collab;
        }

        if (path.startsWith("processes/")) {
            String name = path.replace("processes/", "").replace(".xklaim", "");
            for (var p : processes) {
                if (p.get("name").equals(name)) {
                    String code = p.get("code");
                    List<String> imp = new ArrayList<>(List.of("import klava.Locality"));
                    for (String an : callActivities.keySet()) if (code.contains(an + "(")) imp.add("import xklaim.activities." + an);
                    for (String tn : scriptTasks.keySet()) if (code.contains(tn + "(")) imp.add("import xklaim.tasks." + tn);
                    for (String bn : branches.keySet()) if (code.contains(bn + "(")) imp.add("import xklaim.branches." + bn);
                    for (String en : esps.keySet()) if (code.contains(en)) imp.add("import xklaim.tasks." + en);
                    return "package xklaim.processes\n\n" + String.join("\n", imp) + "\n\n" + code;
                }
            }
        }

        if (path.startsWith("branches/")) {
            String name = path.replace("branches/", "").replace(".xklaim", "");
            Object data = branches.get(name);
            if (data != null) {
                String code = data instanceof List ? String.join("\n", (List<String>) data) : data.toString();
                return "package xklaim.branches\n\nimport klava.Locality\n\n" + code;
            }
        }

        if (path.startsWith("tasks/")) {
            String name = path.replace("tasks/", "").replace(".xklaim", "");
            Object data = scriptTasks.get(name);
            if (data == null) data = esps.get(name);
            if (data != null) {
                String code = data instanceof List ? String.join("\n", (List<String>) data) : data.toString();
                return "package xklaim.tasks\n\nimport klava.Locality\n\n" + code;
            }
        }

        if (path.startsWith("activities/")) {
            String name = path.replace("activities/", "").replace(".xklaim", "");
            Object data = callActivities.get(name);
            if (data != null) {
                String code = data instanceof List ? String.join("\n", (List<String>) data) : data.toString();
                return "package xklaim.activities\n\nimport klava.Locality\n\n" + code;
            }
        }

        return null;
    }

    private Map<String, String> loadExpectedFiles(String resourceDir) throws IOException {
        Map<String, String> files = new TreeMap<>();
        java.net.URL url = getClass().getClassLoader().getResource(resourceDir);
        if (url == null) return files;
        Path base = Paths.get(url.getPath());
        try (var walk = Files.walk(base)) {
            walk.filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".xklaim"))
                .forEach(p -> {
                    try {
                        String rel = base.relativize(p).toString();
                        files.put(rel, Files.readString(p));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
        }
        return files;
    }

    private String loadResource(String name) throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream(name);
        assertNotNull(is, "Resource not found: " + name);
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
}
