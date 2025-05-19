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

 package com.example.B2XKlaim.Service.bpmnElements.gateways;

 import com.example.B2XKlaim.Service.bpmnElements.BpmnElement;
import com.example.B2XKlaim.Service.codeGenerator.Visitable;
import com.example.B2XKlaim.Service.codeGenerator.Visitor;
 import lombok.Data;
 
 import java.io.FileNotFoundException;
 import java.io.UnsupportedEncodingException;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 /**
  * Represents an Event-Based Gateway in BPMN.
  * This gateway routes the process flow based on which event occurs first.
  */
 @Data
 public class EB extends BpmnElement implements Visitable {
     private String id;
     private String outgoingEdge;
     private Map<String, List<String>> eventPathMap;
 
     /**
      * Constructor for an Event-Based Gateway
      * 
      * @param id The ID of the event-based gateway
      * @param eventPathMap Map of event paths where keys are event references and values are the elements in each path
      * @param outgoingEdge The outgoing edge from the closing gateway
      */
     public EB(String id, Map<String, List<String>> eventPathMap, String outgoingEdge) {
         this.id = id;
         this.eventPathMap = eventPathMap;
         this.outgoingEdge = outgoingEdge;
     }
 
     @Override
     public String accept(Visitor v) throws FileNotFoundException, UnsupportedEncodingException {
         return v.visit(this);
     }
 }