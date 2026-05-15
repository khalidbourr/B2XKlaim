package com.example.B2XKlaim.Service.bpmnElements.activities;

import com.example.B2XKlaim.Service.bpmnElements.BpmnElement;
import com.example.B2XKlaim.Service.codeGenerator.Visitable;
import com.example.B2XKlaim.Service.codeGenerator.Visitor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class ST extends BpmnElement implements Visitable {
    private String name;
    private String id;
    private String incoming;
    private String outgoingEdge;

    @Builder.Default
    private List<String> dataInputRefs = new ArrayList<>();
    @Builder.Default
    private List<String> dataOutputRefs = new ArrayList<>();

    @Override
    public String accept(Visitor v) throws FileNotFoundException, UnsupportedEncodingException {
        return v.visit(this);
    }
}
