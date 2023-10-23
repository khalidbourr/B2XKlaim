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




package com.example.B2XKlaim.Controller;

import com.example.B2XKlaim.Service.Parser.BpmnParser;
import com.example.B2XKlaim.Service.bpmnElements.BpmnElements;
import com.example.B2XKlaim.Service.codeGenerator.Generator;
import com.example.B2XKlaim.Service.codeGenerator.Optimizer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@CrossOrigin(origins = "*")
@RestController
public class TranslationController {

    @PostMapping("/generate-code")
    public ResponseEntity<Map<String, Object>> generateCode(@RequestBody String bpmnXml) {
        try {
            BpmnParser parser = new BpmnParser();
            BpmnElements bpmnElements = parser.parse(bpmnXml);
            Generator code = new Generator(bpmnElements);
            // Translate and optimize collaboration
            List<String> collaborationTranslation = code.translateBpmnCollaboration();
            Optimizer optimizer = new Optimizer();
            List<String> optimizedCollabo = optimizer.optimize(collaborationTranslation);

            // Translate processes
            Map<String, List<String>> processesTranslations = code.translateBPMNProcess();

            // Optimize each process code
            List<Map<String, String>> processesList = new ArrayList<>();
            for (Map.Entry<String, List<String>> entry : processesTranslations.entrySet()) {
                List<String> rawProcessCode = entry.getValue();
                List<String> optimizedProcessCode = optimizer.optimize(rawProcessCode);
                Map<String, String> processEntry = new HashMap<>();
                processEntry.put("name", entry.getKey());
                processEntry.put("code", String.join("\n", optimizedProcessCode));
                processesList.add(processEntry);
            }

            // Aggregate results into resultMap
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("collaboration", String.join("\n", optimizedCollabo));
            resultMap.put("processes", processesList);


            // Assuming you want to return the resultMap as the response body
            return new ResponseEntity<>(resultMap, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}
