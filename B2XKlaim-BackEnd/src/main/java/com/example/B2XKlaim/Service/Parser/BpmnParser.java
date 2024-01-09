/*
 * Copyright 2023 Khalid BOURR
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */







package com.example.B2XKlaim.Service.Parser;

import com.example.B2XKlaim.Service.bpmnElements.BpmnElement;
import com.example.B2XKlaim.Service.bpmnElements.BpmnElements;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

public class BpmnParser {

    private Document document;

    public BpmnParser(Document document) {
        this.document = document;
    }
    public BpmnParser() {
    }
    public BpmnElements parse(String xmlContent) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        // Convert string to InputSource
        InputSource is = new InputSource(new StringReader(xmlContent));

        // Parse the XML content directly
        this.document = builder.parse(is);
        this.document = removeBpmnDiagram(document); // If needed

        BpmnElements bpmnElements = new BpmnElements();
        BpmnElementFactory bpmnElementFactory = new BpmnElementFactory(document);

        // 2. Handle generic elements
        NodeList elementNodes = document.getElementsByTagName("*");
        for (int i = 0; i < elementNodes.getLength(); i++) {
            Element element = (Element) elementNodes.item(i);
            BpmnElement bpmnElement = bpmnElementFactory.createBpmnElement(element);
            if (bpmnElement != null) {
                bpmnElements.addElement(bpmnElement);
            }
        }

        // 3. Check for <bpmn:collaboration>
        NodeList collaborationNodes = document.getElementsByTagName("bpmn:collaboration");
        if (collaborationNodes != null && collaborationNodes.getLength() > 0) {
            for (int c = 0; c < collaborationNodes.getLength(); c++) {
                Element collaboration = (Element) collaborationNodes.item(c);
                BpmnElement collaborationElement = bpmnElementFactory.createBpmnElement(collaboration);
                if (collaborationElement != null) {
                    bpmnElements.addElement(collaborationElement);
                }
                NodeList participantNodes = collaboration.getElementsByTagName("bpmn:participant");
                for (int p = 0; p < participantNodes.getLength(); p++) {
                    Element participant = (Element) participantNodes.item(p);
                    BpmnElement participantElement = bpmnElementFactory.createBpmnElement(participant);
                    if (participantElement != null) {
                        bpmnElements.addElement(participantElement);
                    }
                    String processId = participant.getAttribute("processRef");
                    Element process = document.getElementById(processId);
                    if (process != null) {
                        BpmnElement processElement = bpmnElementFactory.createBpmnElement(process);
                        if (processElement != null) {
                            bpmnElements.addElement(processElement);                        }
                    }
                }
            }
        }

        return bpmnElements;
    }

    public BpmnElements parseWithPath(String filePath) throws Exception {
        // Create a new document builder factory
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        // Parse the XML file and get the root element
        this.document = builder.parse(filePath);
        this.document = removeBpmnDiagram(document); // Add this line
        Element root = this.document.getDocumentElement();
        Element process = (Element) document.getElementsByTagName("bpmn:process").item(0);
        BpmnElements bpmnElements = new BpmnElements();
        BpmnElementFactory bpmnElementFactory = new BpmnElementFactory(document);
        NodeList elementNodes = document.getElementsByTagName("*");
        for (int i = 0; i < elementNodes.getLength(); i++) {
            Element element = (Element) elementNodes.item(i);
            BpmnElement bpmnElement = bpmnElementFactory.createBpmnElement(element);
            if (bpmnElement != null) {
                bpmnElements.addElement(bpmnElement);
            }
        }

        return bpmnElements;
    }

    public Document removeBpmnDiagram(Document document) {
        NodeList bpmnDiagrams = document.getElementsByTagName("bpmndi:BPMNDiagram");

        if (bpmnDiagrams.getLength() > 0) {
            Node bpmnDiagram = bpmnDiagrams.item(0);
            Node parent = bpmnDiagram.getParentNode();
            parent.removeChild(bpmnDiagram);
        }

        return document;
    }
}
