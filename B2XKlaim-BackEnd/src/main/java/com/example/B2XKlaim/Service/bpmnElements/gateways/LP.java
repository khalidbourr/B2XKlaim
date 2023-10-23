package com.example.B2XKlaim.Service.bpmnElements.gateways;

import com.example.B2XKlaim.Service.bpmnElements.flows.Edge;
import com.example.B2XKlaim.Service.bpmnElements.messages.Message;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LP {
    private String name;
    private Edge outgoingEdge;
    private Message message;
    // Constructor

}
