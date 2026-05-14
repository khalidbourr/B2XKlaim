package com.example.B2XKlaim.Service.bpmnElements.objects;

import com.example.B2XKlaim.Service.bpmnElements.BpmnElement;
import com.example.B2XKlaim.Service.codeGenerator.Visitor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
public class DO extends BpmnElement {

    private String dataObjectRef;

    @Builder.Default
    private List<Field> fields = new ArrayList<>();

    @Override
    public String accept(Visitor v) throws FileNotFoundException, UnsupportedEncodingException {
        return v.visit(this);
    }
}