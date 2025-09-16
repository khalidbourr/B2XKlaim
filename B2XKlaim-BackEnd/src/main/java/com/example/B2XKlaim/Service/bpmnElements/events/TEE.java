package com.example.B2XKlaim.Service.bpmnElements.events;

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
public class TEE extends BpmnElement implements Visitable{
    private String name;
    private String id;

    @Override
    public String accept(Visitor v) throws FileNotFoundException, UnsupportedEncodingException {
        return v.visit(this);
    }

}