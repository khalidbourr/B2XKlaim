package com.example.B2XKlaim.utile;

import ch.qos.logback.core.util.FileUtil;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class XmlUtiles {

    public static Document convertStringToXMLDocument(String xmlString) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(xmlString));
            return builder.parse(is);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String retrieveStringFromFile(String filePath) {
        // Use ClassLoader to get the resource as an InputStream
        try (InputStream is = FileUtil.class.getClassLoader().getResourceAsStream(filePath);
             InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(isr)) {

            // Read the InputStream and collect it into a single String
            return reader.lines().collect(Collectors.joining(System.lineSeparator()));
        } catch (Exception e) {
            e.printStackTrace();
            return null; // Return null or handle the exception as needed
        }
    }
}
