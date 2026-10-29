package com.example.B2XKlaim.ServiceTest.codeGenerator;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Compiles a generated project with the real X-Klaim compiler (xtext-maven-plugin).
 * Skipped when mvn is not on the PATH. Only diagrams whose conditions are backed
 * by data objects are compiled - the others model conditions that cannot compile.
 */
public class GeneratedCodeCompilesTest {

    private static final String[] COMPILE_DIAGRAMS = {
        "gateway_combinations/expected/DataConditions",
    };

    @Test
    public void generated_code_compiles_with_the_xklaim_compiler() throws Exception {
        Assumptions.assumeTrue(mavenAvailable(), "mvn not on PATH - skipping compile check");

        Path project = Files.createTempDirectory("b2xklaim-compile-check");
        URL pom = getClass().getClassLoader().getResource("compile-check/pom.xml");
        assertNotNull(pom, "missing test resource compile-check/pom.xml");
        Files.copy(Paths.get(pom.getPath()), project.resolve("pom.xml"),
                StandardCopyOption.REPLACE_EXISTING);
        for (String diagram : COMPILE_DIAGRAMS) {
            copyGoldenTree(diagram, project);
        }

        Path log = project.resolve("build.log");
        Process process = new ProcessBuilder("mvn", "-B", "package")
                .directory(project.toFile())
                .redirectOutput(log.toFile())
                .redirectErrorStream(true)
                .start();
        boolean finished = process.waitFor(6, TimeUnit.MINUTES);

        String output = Files.exists(log) ? Files.readString(log) : "";
        if (!finished) {
            process.destroyForcibly();
            fail("nested mvn did not finish within 6 minutes; project kept at " + project);
        }
        assertEquals(0, process.exitValue(),
                "generated project does not compile - kept at " + project + "\n" + output);

        Path classes = project.resolve("target/classes");
        try (var walk = Files.walk(classes)) {
            assertTrue(walk.anyMatch(p -> p.getFileName().toString().equals("DataConditions.class")),
                    "no compiled process class in target/classes - xtext or javac did not run\n" + output);
        }
    }

    private static boolean mavenAvailable() {
        try {
            return new ProcessBuilder("mvn", "-v").redirectErrorStream(true).start().waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private void copyGoldenTree(String goldenPath, Path target) throws IOException {
        URL url = getClass().getClassLoader().getResource(goldenPath);
        assertNotNull(url, "missing golden " + goldenPath);
        Path base = Paths.get(url.getPath());
        try (var walk = Files.walk(base)) {
            walk.filter(Files::isRegularFile).forEach(p -> {
                try {
                    Path destination = target.resolve(base.relativize(p).toString());
                    Files.createDirectories(destination.getParent());
                    Files.copy(p, destination, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }
}
