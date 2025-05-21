package com.example.B2XKlaim.Service.bpmnElements.activities;
import com.example.B2XKlaim.Service.bpmnElements.BpmnElement;
import com.example.B2XKlaim.Service.codeGenerator.Visitable;
import com.example.B2XKlaim.Service.codeGenerator.Visitor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ESP extends BpmnElement implements Visitable {
    private String name;
    private String id;
    private String processId;
    private String processName;
    
    @Builder.Default
    private List<BpmnElement> internalElements = new ArrayList<>();
    
    public void addInternalElement(BpmnElement element) {
        internalElements.add(element);
    }
    
    @Override
    public String accept(Visitor v) throws FileNotFoundException, UnsupportedEncodingException {
        return v.visit(this);
    }
}
