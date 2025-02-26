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





package com.example.B2XKlaim.Service.codeGenerator;

import com.example.B2XKlaim.Service.bpmnElements.BpmnElements;
import com.example.B2XKlaim.Service.bpmnElements.BpmnElement;
import com.example.B2XKlaim.Service.bpmnElements.activities.CLA;
import com.example.B2XKlaim.Service.bpmnElements.objects.pool.Collab;
import com.example.B2XKlaim.Service.bpmnElements.objects.pool.PL;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class Generator {

    private final BpmnElements processDiagram;
    private static BPMNTranslator visitor;


    public Generator(BpmnElements processDiagram) {
        this.processDiagram = processDiagram;
        this.visitor=new BPMNTranslator(processDiagram);
    }


    public List<String> translateBpmnCollaboration() throws FileNotFoundException, UnsupportedEncodingException {
        List<BpmnElement> collaborationElements = getCollaboration(processDiagram);
        List<String> translations = new ArrayList<>();


        for (BpmnElement element : collaborationElements) {
            if (element instanceof Collab) {
                String translation = visitor.visit((Collab) element);
                translations.add(translation);
            }
        }

        return translations;
    }


    public Map<String, List<String>> translateBPMNProcess() throws FileNotFoundException, UnsupportedEncodingException, InvocationTargetException, IllegalAccessException {
        Map<String, List<String>> result = new HashMap<>();

        Map<String, List<String>> eventSubprocessTranslations = new HashMap<>();

        List<BpmnElement> eventSubprocesses = getAllEventSubProcesses(processDiagram);
        for (BpmnElement subProcess : eventSubprocesses) {
            String TranslateSubProcess = translateSubprocess(subProcess);
            if (TranslateSubProcess != null) {
                eventSubprocessTranslations
                        .computeIfAbsent(subProcess.getProcessId(), k -> new ArrayList<>())
                        .add(TranslateSubProcess);
            }
        }

        List<BpmnElement> startEvents = getAllStartEvents(processDiagram);
        for (BpmnElement startEvent : startEvents) {
            if (startEvent == null) continue;

            String processId = startEvent.getProcessId();
            if (processId == null) {
                throw new IllegalArgumentException("Process ID is missing for a start event.");
            }

            List<String> translatedElements = new ArrayList<>();
            Set<BpmnElement> visitedElements = new HashSet<>();

            List<String> combinedTranslations = result.getOrDefault(processId, new ArrayList<>());
            combinedTranslations.addAll(eventSubprocessTranslations.getOrDefault(processId, Collections.emptyList()));
            String processName = startEvent.getProcessName();


            if (processId.startsWith("Activity")){
                combinedTranslations.addAll(translateElement(startEvent, translatedElements, visitedElements));
                String recursiveCall = String.format("eval(new %s())@self\n", processName);
                combinedTranslations.add(recursiveCall);            }
            else {
                combinedTranslations.addAll(translateElement(startEvent, translatedElements, visitedElements));

            }
            result.put(processName, combinedTranslations);
        }
        return result;
    }

    private String translateSubprocess(BpmnElement subProcess) throws InvocationTargetException, IllegalAccessException {
        String result = null;
        Method visitMethod = getVisitMethod(subProcess.getClass());
        if (visitMethod != null) {
            try {
                result = (String) visitMethod.invoke(visitor, subProcess);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
                throw e;
            }
        }
        return result;
    }


    public Map<String, List<String>> translateCallActivity() throws InvocationTargetException, IllegalAccessException {
        Map<String, List<String>> result = new HashMap<>();
        List<BpmnElement> callActivities = getAllCallActivity(processDiagram);

        for (BpmnElement callActivity : callActivities) {
            CLA callAct = (CLA) callActivity;
            if (callAct != null) {
                String callActivityId = callAct.getCalledProcess();
                String callActivityOutgoingFlow = callAct.getOutgoingEdge();

                String codeString = "proc " + callActivityId + "(String edge){\n\n" +
                        "      /* write your code here */ \n\n" +
                        "      out(edge)@self\n" +
                        "}";

                List<String> translations = new ArrayList<>();
                translations.add(codeString);

                result.put(callActivityId, translations);
            }
        }

        return result;
    }


    private List<String> translateElement(BpmnElement element, List<String> translatedElements, Set<BpmnElement> visitedElements) throws FileNotFoundException, UnsupportedEncodingException {
        Method visitMethod = getVisitMethod(element.getClass());
        if (visitMethod != null) {
            visitedElements.add(element);

            try {
                String result = (String) visitMethod.invoke(visitor, element);
                translatedElements.add(result);

                String outgoingEdge = element.getOutgoingEdge();
                BpmnElement nextElement = processDiagram.getNextElementById(outgoingEdge);
                if (nextElement != null && !visitedElements.contains(nextElement)) {
                    BpmnElement edge = processDiagram.getElementById(outgoingEdge);
                    translatedElements.addAll(translateElement(edge, new ArrayList<>(), visitedElements));
                    translatedElements.addAll(translateElement(nextElement, new ArrayList<>(), visitedElements));
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        return translatedElements;
    }

    private Method getVisitMethod(Class<? extends BpmnElement> elementType) {
        try {
            return visitor.getClass().getMethod("visit", elementType);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static List<BpmnElement> getAllStartEvents(BpmnElements bpmnElements) {
        List<BpmnElement> startEvents = bpmnElements.getElementsByElementType(BpmnElements.ElementType.NSE);
        startEvents.addAll(bpmnElements.getElementsByElementType(BpmnElements.ElementType.MSE));
        startEvents.addAll(bpmnElements.getElementsByElementType(BpmnElements.ElementType.SSE));
        startEvents.addAll(bpmnElements.getElementsByElementType(BpmnElements.ElementType.TSE));

        return startEvents;
    }

    private static List<BpmnElement> getPools(BpmnElements bpmnElements) {
        return bpmnElements.getElementsByElementType(BpmnElements.ElementType.PL);
    }

    private static List<BpmnElement> getCollaboration(BpmnElements bpmnElements) {
        List<BpmnElement> elements = new ArrayList<>();
        for (BpmnElement element : bpmnElements.getElementsById().values()) {
            if (element instanceof Collab) {
                elements.add(element);
            }
        }
        return elements;
    }

    private static List<BpmnElement> getParticipants(BpmnElements bpmnElements) {
        List<BpmnElement> participants = new ArrayList<>();
        for (BpmnElement element : bpmnElements.getElementsById().values()) {
            if (element instanceof PL) { // Assuming Participant is a class representing BPMN participant
                participants.add(element);
            }
        }
        return participants;
    }

    private List<BpmnElement> getAllEventSubProcesses(BpmnElements bpmnElements) {
        List<BpmnElement> eventSubprocess = bpmnElements.getElementsByElementType(BpmnElements.ElementType.ESP);
        return eventSubprocess;
    }

    private List<BpmnElement> getAllCallActivity(BpmnElements bpmnElements) {
        List<BpmnElement> CallActivity = bpmnElements.getElementsByElementType(BpmnElements.ElementType.CLA);
        return CallActivity;
    }


}
