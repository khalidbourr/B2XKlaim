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



package com.example.B2XKlaim.Service.bpmnElements;

import com.example.B2XKlaim.Service.codeGenerator.BPMNTranslator;
import com.example.B2XKlaim.Service.codeGenerator.Visitable;
import com.example.B2XKlaim.Service.codeGenerator.Visitor;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

public abstract class BpmnElement implements Visitable{
    private String id;
    private String name;
    private String outgoingEdge;

    private String processId;
    private String processName;



    public String getProcessId() {
        return processId;
    }

    public String getProcessName() {
        return processName;
    }

    private static Map<String, Class<? extends BpmnElement>> idToClassMap = new HashMap<>();

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getOutgoingEdge() {
        return outgoingEdge;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public void setProcessName(String processName) {
        this.processName = processName;
    }


    public static void registerClass(String id, Class<? extends BpmnElement> clazz) {
        // Register the class with the given ID
        idToClassMap.put(id, clazz);
    }

    public static Class<? extends BpmnElement> getClassFromId(String id) {
        // Get the class corresponding to the given ID from the map
        return idToClassMap.get(id);
    }



}




