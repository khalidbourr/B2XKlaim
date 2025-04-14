package com.example.B2XKlaim.Service.bpmnElements.flows;


import com.example.B2XKlaim.Service.bpmnElements.BpmnElement;
import com.example.B2XKlaim.Service.codeGenerator.Visitable;
import com.example.B2XKlaim.Service.codeGenerator.Visitor;
import lombok.AllArgsConstructor;
// import lombok.Builder; // Not needed if SuperBuilder is used
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder; // Correctly imported and used




@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder // Correctly used
@NoArgsConstructor
@AllArgsConstructor
public class MessageFLow extends BpmnElement implements Visitable {
    // 'id', 'name', 'outgoingEdge', 'processId', 'processName' are inherited

    private String receiverId;
    private String receiverName;
    private String senderId;
    private String senderName;
    private String targetRef;
    private String sourceRef;

    @Override
    public String accept(Visitor v) throws java.io.FileNotFoundException, java.io.UnsupportedEncodingException {
        // MessageFlow itself is usually not visited for direct code generation.
        return null;
    }
}