package com.example.B2XKlaim.Service.bpmnElements;
import com.example.B2XKlaim.Service.bpmnElements.activities.CLA;
import com.example.B2XKlaim.Service.bpmnElements.activities.ESP;
import com.example.B2XKlaim.Service.bpmnElements.activities.ST;
import com.example.B2XKlaim.Service.bpmnElements.events.*;
import com.example.B2XKlaim.Service.bpmnElements.gateways.AND;
import com.example.B2XKlaim.Service.bpmnElements.gateways.LP;
import com.example.B2XKlaim.Service.bpmnElements.gateways.XOR;
import com.example.B2XKlaim.Service.bpmnElements.objects.DO;
import com.example.B2XKlaim.Service.bpmnElements.flows.SQ;
import lombok.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Data
@AllArgsConstructor
@Builder
public class BpmnElementsInsideXorAnd {


    private List<MIC> micList = new ArrayList<>();
    private List<SIC> sicList = new ArrayList<>();
    private List<MIT> mitList = new ArrayList<>();
    private List<SIT> sitList = new ArrayList<>();
    private List<XOR> xorList = new ArrayList<>();
    private List<AND> andList = new ArrayList<>();
    private List<LP> lpList = new ArrayList<>();
    private List<CLA> claList = new ArrayList<>();
    private List<ESP> espList = new ArrayList<>();
    private List<ST> stList = new ArrayList<>();
    private List<SQ> sqList = new ArrayList<>();
    private List<DO> dataList = new ArrayList<>();
    private Map<String, BpmnElement> elementsById = new HashMap<>();

    // Constructor to initialize elementsById map
    public BpmnElementsInsideXorAnd() {
    }

    public void populateElementsById() {
        for (Field field : this.getClass().getDeclaredFields()) {
            if (List.class.isAssignableFrom(field.getType())) {
                List<? extends BpmnElement> elementList;
                try {
                    elementList = (List<? extends BpmnElement>) field.get(this);
                } catch (IllegalAccessException e) {
                    // Handle exception
                    continue;
                }
                for (BpmnElement element : elementList) {
                    elementsById.put(element.getId(), element);
                }
            }
        }
    }

    // Get element by Id
    public BpmnElement getElementById(String id) {
        populateElementsById();
        return elementsById.get(id);
    }

    // Get the Target of the sequence
    public String getTargetById(String id) {
        for (SQ sq : sqList) {
            if (sq.getId().equals(id)) {
                return sq.getTarget();
            }
        }
        return null; // if no matching SQ object is found
    }

    // From the outgoingEdge, it returns the next Element
    public BpmnElement getNextElementById(String id) {
        String targetId = getTargetById(id);
        if (targetId != null) {
            return getElementById(targetId);
        }
        return null;
    }

}
    // constructors, getters and setters


