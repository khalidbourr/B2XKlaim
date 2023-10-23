package com.example.B2XKlaim.Service.bpmnElements.gateways;
import com.example.B2XKlaim.Service.bpmnElements.flows.Edge;
import com.example.B2XKlaim.Service.bpmnElements.BpmnElement;
import com.example.B2XKlaim.Service.codeGenerator.Visitable;
import com.example.B2XKlaim.Service.codeGenerator.Visitor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@Builder
public class AND extends BpmnElement implements Visitable {
    private String id;
    private Map<Integer, List<String>> flowElementMap = new HashMap<>();
    private String outgoingEdge;

    public AND(String id, Map<Integer, List<String>> flowElementMap, String outgoingEdge) {
        this.id = id;
        this.flowElementMap = flowElementMap;
        this.outgoingEdge = outgoingEdge;
    }

    public String getId() {
        return id;
    }

    public Map<Integer, List<String>> getFlowElementMap() {
        return flowElementMap;
    }

    public String getOutgoingEdge() {
        return outgoingEdge;
    }

    @Override
    public String accept(Visitor v) throws FileNotFoundException, UnsupportedEncodingException {
        return v.visit(this);
    }
}

