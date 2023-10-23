package com.example.B2XKlaim.Service.bpmnElements.objects.pool;

import com.example.B2XKlaim.Service.bpmnElements.BpmnElement;
import com.example.B2XKlaim.Service.codeGenerator.Visitable;
import com.example.B2XKlaim.Service.codeGenerator.Visitor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class Collab extends BpmnElement implements Visitable {
    private String id;
    private List<PL> participants; // To store the list of participants

    public Collab(String id, List<PL> participants) {
        this.id = id;
        this.participants = participants;
    }

    public Collab(String id) {
        this.id = id;
        this.participants = new ArrayList<>();
    }

    public void addParticipant(PL participant) {
        this.participants.add(participant);
    }

    public String getId() {
        return id;
    }

    public List<PL> getParticipants() {
        return participants;
    }

    @Override
    public String accept(Visitor v) throws FileNotFoundException, UnsupportedEncodingException {
        return null;
    }
}