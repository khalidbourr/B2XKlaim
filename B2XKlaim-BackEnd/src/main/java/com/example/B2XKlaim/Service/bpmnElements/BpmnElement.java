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

 import com.example.B2XKlaim.Service.codeGenerator.Visitable;
 import com.example.B2XKlaim.Service.codeGenerator.Visitor;
 
 import lombok.AccessLevel; 
 import lombok.AllArgsConstructor;
 import lombok.Data; 
 import lombok.NoArgsConstructor;
 import lombok.experimental.SuperBuilder; 
 
 import java.io.FileNotFoundException;
 import java.io.UnsupportedEncodingException; 
 import java.util.HashMap;
 import java.util.Map;
 
 @Data 
 @SuperBuilder 
 @NoArgsConstructor 
 @AllArgsConstructor(access = AccessLevel.PROTECTED) 
 public abstract class BpmnElement implements Visitable{

    protected String id;         
     protected String name;
     protected String outgoingEdge; 
     protected String processId;    
     protected String processName; 
 

 
     // Static map logic remains unchanged
     private static Map<String, Class<? extends BpmnElement>> idToClassMap = new HashMap<>();
 
     public static void registerClass(String id, Class<? extends BpmnElement> clazz) {
         idToClassMap.put(id, clazz);
     }
 
     public static Class<? extends BpmnElement> getClassFromId(String id) {
         return idToClassMap.get(id);
     }
 
     // Abstract accept method must be implemented by concrete subclasses
     @Override
     public abstract String accept(Visitor v) throws FileNotFoundException, UnsupportedEncodingException;
 
 }




