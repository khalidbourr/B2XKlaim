package com.example.B2XKlaim.Service.builder;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.example.B2XKlaim.Service.CodeGenerationService.ProcessCode;

/**
 * Unit tests for ResponseBuilder demonstrating improved test data creation
 * with the Builder Pattern.
 */
class ResponseBuilderTest {

    @Test
    void testBuildCompleteResponse() {
        // Given - Using builder pattern makes test data creation much cleaner
        Map<String, List<String>> callActivities = new HashMap<>();
        callActivities.put("activity1", Arrays.asList("task1", "task2"));
        
        Map<String, List<String>> scriptTasks = new HashMap<>();
        scriptTasks.put("script1", Arrays.asList("console.log('test')"));

        // When - Fluent API makes the test setup very readable
        Map<String, Object> response = ResponseBuilder.create()
                .withCollaboration("collaboration { ... }")
                .addProcess("Process1", "proc Process1 { ... }")
                .addProcess("Process2", "proc Process2 { ... }")
                .withCallActivities(callActivities)
                .withScriptTaskProcs(scriptTasks)
                .withEventSubProcesses(new HashMap<>())
                .withParticipants(new HashSet<>(Arrays.asList("P1", "P2")))
                .build();

        // Then
        assertNotNull(response);
        assertEquals("collaboration { ... }", response.get("collaboration"));
        
        @SuppressWarnings("unchecked")
        List<Map<String, String>> processes = (List<Map<String, String>>) response.get("processes");
        assertEquals(2, processes.size());
        assertEquals("Process1", processes.get(0).get("name"));
        assertEquals("proc Process1 { ... }", processes.get(0).get("code"));
        
        assertEquals(callActivities, response.get("callActivities"));
        assertEquals(scriptTasks, response.get("scriptTaskProcs"));
        
        @SuppressWarnings("unchecked")
        List<String> participants = (List<String>) response.get("participants");
        assertEquals(2, participants.size());
    }

    @Test
    void testBuildErrorResponse() {
        // Given & When
        Map<String, Object> response = ResponseBuilder.create()
                .withError("Translation failed: Invalid BPMN")
                .build();

        // Then
        assertNotNull(response);
        assertEquals("Translation failed: Invalid BPMN", response.get("error"));
        assertFalse(response.containsKey("collaboration"));
        assertFalse(response.containsKey("processes"));
    }

    @Test
    void testBuildMinimalResponse() {
        // Given & When
        Map<String, Object> response = ResponseBuilder.create()
                .withCollaboration("minimal collaboration")
                .addProcess("Main", "proc Main { ... }")
                .buildMinimal();

        // Then
        assertNotNull(response);
        assertEquals("minimal collaboration", response.get("collaboration"));
        assertEquals(1, ((List<?>) response.get("processes")).size());
        assertFalse(response.containsKey("callActivities"));
        assertFalse(response.containsKey("scriptTaskProcs"));
    }

    @Test
    void testBuilderWithProcessCodeObjects() {
        // Given - Can also use ProcessCode objects directly
        List<ProcessCode> processCodes = Arrays.asList(
            new ProcessCode("P1", "code1"),
            new ProcessCode("P2", "code2")
        );

        // When
        Map<String, Object> response = ResponseBuilder.create()
                .withCollaboration("test")
                .withProcesses(processCodes)
                .build();

        // Then
        @SuppressWarnings("unchecked")
        List<Map<String, String>> processes = (List<Map<String, String>>) response.get("processes");
        assertEquals(2, processes.size());
        assertEquals("P1", processes.get(0).get("name"));
        assertEquals("code1", processes.get(0).get("code"));
    }

    @Test
    void testBuilderValidation() {
        // Given
        ResponseBuilder builder = ResponseBuilder.create()
                .withCollaboration("test");

        // When & Then
        assertTrue(builder.isValid()); // All fields have defaults
        
        // Error responses are always valid
        ResponseBuilder errorBuilder = ResponseBuilder.create()
                .withError("error");
        assertTrue(errorBuilder.isValid());
    }

    @Test
    void testBuilderReset() {
        // Given
        ResponseBuilder builder = ResponseBuilder.create()
                .withCollaboration("test")
                .addProcess("P1", "code1");

        // When
        builder.reset();
        Map<String, Object> response = builder
                .withCollaboration("new")
                .build();

        // Then
        assertEquals("new", response.get("collaboration"));
        assertEquals(0, ((List<?>) response.get("processes")).size());
    }
}