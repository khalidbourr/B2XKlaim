package com.example.B2XKlaim.Service.bpmnElements.objects.pool;

import com.example.B2XKlaim.Service.bpmnElements.BpmnElement;
import com.example.B2XKlaim.Service.bpmnElements.flows.Edge;
import com.example.B2XKlaim.Service.bpmnElements.messages.Message;
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
public class PL extends BpmnElement implements Visitable {
    private String name;
    private String id;
    private String ProcessId;
    private String ProcessName;

    @Override
    public String accept(Visitor v) throws FileNotFoundException, UnsupportedEncodingException {
        return v.visit(this);
    }
}
