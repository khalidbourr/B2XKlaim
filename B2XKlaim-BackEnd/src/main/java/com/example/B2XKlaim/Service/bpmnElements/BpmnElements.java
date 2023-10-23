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
import com.example.B2XKlaim.Service.bpmnElements.flows.SQ;
import lombok.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Data
@AllArgsConstructor
@Builder
public class BpmnElements {

    public enum ElementType {
        NSE, MSE, SSE, MIC, SIC, MIT, SIT, NEE, MEE, SEE, XOR, AND, LP, CLA, ESP, ST, SQ, PL, MIPL, DO, COLLAB
    }

    private Map<String, BpmnElement> elementsById = new HashMap<>();

    public void addElement(BpmnElement element) {
        elementsById.put(element.getId(), element);
    }

    public BpmnElements() {
    }

    public BpmnElement getElementById(String id) {
        return elementsById.get(id);
    }

    public String getTargetById(String id) {
        if (id == null) {
            return null;
        }
        return elementsById.values().stream()
                .filter(element -> element instanceof SQ && id.equals(element.getId()))
                .map(element -> ((SQ) element).getTarget())
                .findFirst()
                .orElse(null);
    }

    public BpmnElement getNextElementById(String id) {
        String targetId = getTargetById(id);
        return targetId != null ? getElementById(targetId) : null;
    }

    public <T extends BpmnElement> List<T> getElementsByType(Class<T> elementType) {
        return elementsById.values().stream()
                .filter(elementType::isInstance)
                .map(elementType::cast)
                .collect(Collectors.toList());
    }

    public List<BpmnElement> getElementsByElementType(ElementType elementType) {
        return elementsById.values().stream()
                .filter(element -> elementType.name().equals(element.getClass().getSimpleName()))
                .collect(Collectors.toList());
    }

    public Stream<BpmnElement> elementStream() {
        return elementsById.values().stream();
    }
}

