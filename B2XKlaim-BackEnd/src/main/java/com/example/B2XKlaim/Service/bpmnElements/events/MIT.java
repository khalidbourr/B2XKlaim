package com.example.B2XKlaim.Service.bpmnElements.events;

import com.example.B2XKlaim.Service.bpmnElements.BpmnElement;
import com.example.B2XKlaim.Service.bpmnElements.flows.MessageFLow;
import com.example.B2XKlaim.Service.bpmnElements.objects.Field;
import com.example.B2XKlaim.Service.codeGenerator.Visitable;
import com.example.B2XKlaim.Service.codeGenerator.Visitor;
import lombok.*;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class MIT extends BpmnElement implements Visitable {
    private String name;
    private String id;
    private String incomingEdge;
    private String outgoingEdge;
    private String messageId;
    private MessageFLow messageFlow;
    @Builder.Default
    private List<String> sourceDataRefs = new ArrayList<>();
    @Builder.Default
    private List<Field> payload = new ArrayList<>();

    @Override
    public String accept(Visitor v) throws FileNotFoundException, UnsupportedEncodingException {
        return v.visit(this);
    }

}
