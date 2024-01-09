package com.example.B2XKlaim.Service.bpmnElements.flows;

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
public class MessageFLow extends BpmnElement implements Visitable {
    private String Id;
    private String receiverId;
    private String receiverName;
    private String senderId;
    private String senderName;
    private String targetRef;
    private String sourceRef;

    @Override
    public String accept(Visitor v) throws FileNotFoundException, UnsupportedEncodingException {
        return null;
    }
}
