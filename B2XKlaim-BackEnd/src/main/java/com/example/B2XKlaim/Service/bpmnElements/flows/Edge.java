package com.example.B2XKlaim.Service.bpmnElements.flows;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Edge {
    private String id;
    private String source;
    private String target;

}
