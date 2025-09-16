package com.example.B2XKlaim.Service.bpmnElements.gateways;

import com.example.B2XKlaim.Service.bpmnElements.BpmnElement;
import com.example.B2XKlaim.Service.bpmnElements.flows.Edge;
import com.example.B2XKlaim.Service.bpmnElements.messages.Message;
import com.example.B2XKlaim.Service.codeGenerator.Visitable;
import com.example.B2XKlaim.Service.codeGenerator.Visitor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class LP extends BpmnElement implements Visitable {
    private String id;
    private String condition; // Loop condition
    @Builder.Default
    private List<String> flowElementMap = new ArrayList<>(); // Elements inside the loop
    private String outgoingEdge; // Edge for loop exit



    @Override
    public String accept(Visitor v) throws  FileNotFoundException, UnsupportedEncodingException {
        return v.visit(this);
    }

}