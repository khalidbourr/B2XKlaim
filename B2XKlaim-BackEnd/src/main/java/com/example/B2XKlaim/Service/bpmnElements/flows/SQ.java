package com.example.B2XKlaim.Service.bpmnElements.flows;

import com.example.B2XKlaim.Service.bpmnElements.BpmnElement;
import com.example.B2XKlaim.Service.codeGenerator.Visitable;
import com.example.B2XKlaim.Service.codeGenerator.Visitor;
import lombok.*;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class SQ extends BpmnElement implements Visitable {
    private String id;
    private String source;
    private String target;


    @Override
    public String accept(Visitor v) throws FileNotFoundException, UnsupportedEncodingException {
        return v.visit(this);
    }
}
