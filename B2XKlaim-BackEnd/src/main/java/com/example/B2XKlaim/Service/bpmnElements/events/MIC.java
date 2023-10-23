package com.example.B2XKlaim.Service.bpmnElements.events;

import com.example.B2XKlaim.Service.bpmnElements.BpmnElement;
import com.example.B2XKlaim.Service.codeGenerator.Visitable;
import com.example.B2XKlaim.Service.codeGenerator.Visitor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MIC extends BpmnElement implements Visitable {
    private String name;
    private String id;
    private String incomingEdge;
    private String outgoingEdge;
    private String messageId;

    @Override
    public String accept(Visitor v) throws FileNotFoundException, UnsupportedEncodingException {
        return v.visit(this);
    }
    // Constructor

}
