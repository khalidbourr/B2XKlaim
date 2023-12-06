package com.example.B2XKlaim.ServiceTest.Parser;

import com.example.B2XKlaim.Service.Parser.BpmnElementFactory;
import com.example.B2XKlaim.Service.Parser.BpmnParser;
import com.example.B2XKlaim.Service.bpmnElements.BpmnElement;
import com.example.B2XKlaim.Service.bpmnElements.BpmnElements;
import com.example.B2XKlaim.Service.bpmnElements.events.NSE;
import com.example.B2XKlaim.Service.bpmnElements.flows.MessageFLow;
import com.example.B2XKlaim.Service.bpmnElements.objects.pool.PL;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

public class BpmnParserTest {


}