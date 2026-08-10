package com.example.B2XKlaim.Service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test class to demonstrate improved code formatting capabilities.
 */
class CodeFormattingServiceTest {

    private CodeFormattingService codeFormattingService;

    @BeforeEach
    void setUp() {
        codeFormattingService = new CodeFormattingService();
    }

    @Test
    void testFormatProcessCodeImprovedSpacing() {
        // Given - Unformatted X-Klaim code similar to your example
        String unformattedCode = "proc MissionRobot2(Locality explorer_loc) {\n" +
                "eval(new Handler2())@self\n" +
                "in('Message_217d5nt' )@self\n" +
                "if(true){\n" +
                "out('Message_0rm7e5v')@explorer_loc\n" +
                "}else{\n" +
                "out('Message_2g93ger')@explorer_loc\n" +
                "eval(new MoveTo('Flow_10o3vbp'/* TODO: Pass other necessary args */))@self\n" +
                "in('Flow_10o3vbp')@self\n" +
                "out('Message_0cvmukp')@explorer_loc\n" +
                "}\n" +
                "eval(new Here('Flow_02uur6k'/* TODO: Pass other necessary args */))@self\n" +
                "in('Flow_02uur6k')@self\n" +
                "}";

        // When - Format the code
        String formattedCode = codeFormattingService.formatProcessCode(unformattedCode, "  ");

        // Print the formatted result for visual inspection
        System.out.println("=== ORIGINAL X-KLAIM CODE ===");
        System.out.println(unformattedCode);
        System.out.println("=== FORMATTED X-KLAIM CODE ===");
        System.out.println(formattedCode);
        System.out.println("=== END FORMATTED CODE ===");

        // Then - Verify improved formatting
        assertNotNull(formattedCode);
        assertFalse(formattedCode.isEmpty());
        
        // Check for proper indentation (basic indentation should be present)
        assertTrue(formattedCode.contains("  eval("));
        assertTrue(formattedCode.contains("  in("));
        
        // Check for proper control structure spacing
        assertTrue(formattedCode.contains("if ("));
        assertTrue(formattedCode.contains("} else"));
        
        // Check for proper nested indentation
        assertTrue(formattedCode.contains("    out("));
        
        // Check for proper line breaks
        String[] lines = formattedCode.split("\n");
        assertTrue(lines.length > 10); // Should have proper line breaks
        
        // Verify the code starts with proc declaration without indentation
        assertTrue(formattedCode.startsWith("proc MissionRobot2"));
    }

    @Test
    void testFormatProcessCodeHandlesComments() {
        // Given - Code with comments
        String codeWithComments = "proc TestProc() {\n" +
                "/* This is a comment */\n" +
                "eval(new Handler())@self\n" +
                "// Another comment\n" +
                "out('test')@self\n" +
                "}";

        // When
        String formatted = codeFormattingService.formatProcessCode(codeWithComments, "  ");

        // Then - Comments should be preserved and properly indented
        assertTrue(formatted.contains("  /* This is a comment */"));
        assertTrue(formatted.contains("  // Another comment"));
    }

    @Test
    void testFormatProcessCodeEmptyInput() {
        // Given
        String emptyCode = "";

        // When
        String formatted = codeFormattingService.formatProcessCode(emptyCode, "  ");

        // Then
        assertEquals("", formatted);
    }

    @Test
    void testFormatProcessCodeNullInput() {
        // Given
        String nullCode = null;

        // When
        String formatted = codeFormattingService.formatProcessCode(nullCode, "  ");

        // Then
        assertEquals("", formatted);
    }
}