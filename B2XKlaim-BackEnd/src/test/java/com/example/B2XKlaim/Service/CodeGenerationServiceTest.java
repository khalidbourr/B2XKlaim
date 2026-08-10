package com.example.B2XKlaim.Service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.B2XKlaim.Service.CodeGenerationService.GenerationResult;
import com.example.B2XKlaim.Service.CodeGenerationService.ProcessCode;
import com.example.B2XKlaim.Service.bpmnElements.BpmnElements;
import com.example.B2XKlaim.Service.strategy.CollaborationCodeStrategy;
import com.example.B2XKlaim.Service.strategy.StandaloneProcessStrategy;

/**
 * Unit tests for CodeGenerationService demonstrating improved testability
 * after architectural improvements.
 */
@ExtendWith(MockitoExtension.class)
class CodeGenerationServiceTest {

    @Mock
    private CollaborationCodeStrategy collaborationCodeStrategy;

    @Mock
    private StandaloneProcessStrategy standaloneProcessStrategy;

    @InjectMocks
    private CodeGenerationService codeGenerationService;

    private Map<String, BpmnElements> testProcesses;
    private BpmnElements mainBpmnElements;

    @BeforeEach
    void setUp() {
        mainBpmnElements = mock(BpmnElements.class);
        testProcesses = new HashMap<>();
        testProcesses.put("main", mainBpmnElements);
    }

    @Test
    void testGenerateCodeWithCollaborationStrategy() throws Exception {
        // Given - Setup test data
        String expectedCollaborationCode = "collaboration { ... }";
        List<ProcessCode> expectedProcesses = Arrays.asList(
            new ProcessCode("Process1", "proc Process1 { ... }"),
            new ProcessCode("Process2", "proc Process2 { ... }")
        );
        Set<String> expectedParticipants = new HashSet<>(Arrays.asList("Participant1", "Participant2"));

        // Mock strategy behavior
        when(collaborationCodeStrategy.canHandle(mainBpmnElements)).thenReturn(true);
        when(collaborationCodeStrategy.getStrategyName()).thenReturn("CollaborationCodeStrategy");
        when(collaborationCodeStrategy.generateCollaborationCode(any(), eq(mainBpmnElements)))
            .thenReturn(expectedCollaborationCode);
        when(collaborationCodeStrategy.generateProcesses(any(), eq(mainBpmnElements), anyString()))
            .thenReturn(expectedProcesses);
        when(collaborationCodeStrategy.extractParticipants(mainBpmnElements))
            .thenReturn(expectedParticipants);

        // When
        GenerationResult result = codeGenerationService.generateCode(testProcesses);

        // Then
        assertNotNull(result);
        assertEquals(expectedCollaborationCode, result.getCollaborationCode());
        assertEquals(2, result.getProcesses().size());
        assertEquals("Process1", result.getProcesses().get(0).getName());
        assertEquals(expectedParticipants, result.getParticipants());

        // Verify strategy was used correctly
        verify(collaborationCodeStrategy).canHandle(mainBpmnElements);
        verify(collaborationCodeStrategy).generateCollaborationCode(any(), eq(mainBpmnElements));
        verify(collaborationCodeStrategy).generateProcesses(any(), eq(mainBpmnElements), anyString());
        verify(collaborationCodeStrategy).extractParticipants(mainBpmnElements);
    }

    @Test
    void testGenerateCodeWithStandaloneStrategy() throws Exception {
        // Given
        String expectedCode = "proc Main { ... }";
        List<ProcessCode> expectedProcesses = Arrays.asList(
            new ProcessCode("Main", expectedCode)
        );

        // Mock strategy behavior - collaboration strategy doesn't handle, standalone does
        when(collaborationCodeStrategy.canHandle(mainBpmnElements)).thenReturn(false);
        when(standaloneProcessStrategy.canHandle(mainBpmnElements)).thenReturn(true);
        when(standaloneProcessStrategy.getStrategyName()).thenReturn("StandaloneProcessStrategy");
        when(standaloneProcessStrategy.generateCollaborationCode(any(), eq(mainBpmnElements)))
            .thenReturn(expectedCode);
        when(standaloneProcessStrategy.generateProcesses(any(), eq(mainBpmnElements), anyString()))
            .thenReturn(expectedProcesses);
        when(standaloneProcessStrategy.extractParticipants(mainBpmnElements))
            .thenReturn(new HashSet<>());

        // When
        GenerationResult result = codeGenerationService.generateCode(testProcesses);

        // Then
        assertNotNull(result);
        assertEquals(expectedCode, result.getCollaborationCode());
        assertEquals(1, result.getProcesses().size());
        assertEquals("Main", result.getProcesses().get(0).getName());
        assertTrue(result.getParticipants().isEmpty());

        // Verify correct strategy selection
        verify(collaborationCodeStrategy).canHandle(mainBpmnElements);
        verify(standaloneProcessStrategy).canHandle(mainBpmnElements);
        verify(standaloneProcessStrategy).generateCollaborationCode(any(), eq(mainBpmnElements));
    }

    @Test
    void testGenerateCodeHandlesException() throws Exception {
        // Given - Strategy throws exception
        when(collaborationCodeStrategy.canHandle(mainBpmnElements)).thenReturn(true);
        when(collaborationCodeStrategy.getStrategyName()).thenReturn("CollaborationCodeStrategy");
        when(collaborationCodeStrategy.generateCollaborationCode(any(), eq(mainBpmnElements)))
            .thenThrow(new RuntimeException("Test error"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            codeGenerationService.generateCode(testProcesses);
        });

        assertEquals("Code generation failed", exception.getMessage());
        assertNotNull(exception.getCause());
        assertEquals("Test error", exception.getCause().getMessage());
    }

    @Test
    void testStrategySelectionFallback() throws Exception {
        // Given - No strategy can handle
        when(collaborationCodeStrategy.canHandle(mainBpmnElements)).thenReturn(false);
        when(standaloneProcessStrategy.canHandle(mainBpmnElements)).thenReturn(false);
        when(standaloneProcessStrategy.getStrategyName()).thenReturn("StandaloneProcessStrategy");
        when(standaloneProcessStrategy.generateCollaborationCode(any(), eq(mainBpmnElements)))
            .thenReturn("fallback code");
        when(standaloneProcessStrategy.generateProcesses(any(), eq(mainBpmnElements), anyString()))
            .thenReturn(new ArrayList<>());
        when(standaloneProcessStrategy.extractParticipants(mainBpmnElements))
            .thenReturn(new HashSet<>());

        // When
        GenerationResult result = codeGenerationService.generateCode(testProcesses);

        // Then - Should default to standalone strategy
        assertNotNull(result);
        assertEquals("fallback code", result.getCollaborationCode());
        verify(standaloneProcessStrategy, times(1)).canHandle(mainBpmnElements); // Only called once in selectStrategy
    }
}