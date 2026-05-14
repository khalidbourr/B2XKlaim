package com.example.B2XKlaim.Service.bpmnElements.objects;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Field {
    private String name;
    private String type;
    private String initialValue;
}