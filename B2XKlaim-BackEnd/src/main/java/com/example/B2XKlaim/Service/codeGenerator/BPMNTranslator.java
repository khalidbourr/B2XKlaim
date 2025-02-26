/*
 * Copyright 2023 Khalid BOURR
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.B2XKlaim.Service.codeGenerator;

import com.example.B2XKlaim.Service.bpmnElements.BpmnElements;
import com.example.B2XKlaim.Service.bpmnElements.BpmnElement;
import com.example.B2XKlaim.Service.bpmnElements.activities.CLA;
import com.example.B2XKlaim.Service.bpmnElements.activities.ESP;
import com.example.B2XKlaim.Service.bpmnElements.activities.ST;
import com.example.B2XKlaim.Service.bpmnElements.events.*;
import com.example.B2XKlaim.Service.bpmnElements.flows.SQ;
import com.example.B2XKlaim.Service.bpmnElements.gateways.AND;
import com.example.B2XKlaim.Service.bpmnElements.gateways.LP;
import com.example.B2XKlaim.Service.bpmnElements.gateways.XOR;
import com.example.B2XKlaim.Service.bpmnElements.objects.DO;
import com.example.B2XKlaim.Service.bpmnElements.objects.pool.Collab;
import com.example.B2XKlaim.Service.bpmnElements.objects.pool.MIPL;
import com.example.B2XKlaim.Service.bpmnElements.objects.pool.PL;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class BPMNTranslator implements Visitor {

    BpmnElements bpmnElements = new BpmnElements();

    public BPMNTranslator(BpmnElements bpmnElements) {
        this.bpmnElements = bpmnElements;
    }

    public BPMNTranslator() {
    }

    @Override
    public String visit(NSE nse) {
        return String.format("out('%s')@self\n", nse.getOutgoingEdge());
    }

    @Override
    public String visit(MSE mse) {
        return String.format("in('%s')@self\nout('%s')@self\n", mse.getMessageId(), mse.getOutgoingEdge());
    }

    @Override
    public String visit(SSE sse) {
        return String.format("read('%s')@%s\nout('%s')@self\n", sse.getSignalId(), sse.getSignalSenderName(), sse.getOutgoingEdge());
    }

    @Override
    public String visit(MIC mic) {
        return String.format("in('%s')@self\nout('%s')@self\n", mic.getMessageId(), mic.getOutgoingEdge());
    }

    @Override
    public String visit(SIC sic) {
        return String.format("read('%s')@%s\nout('%s')@self\n", sic.getSignalId(), sic.getSignalSenderName(), sic.getOutgoingEdge());
    }

    @Override
    public String visit(MIT mit) {
        return String.format("out('%s')@%s\nout('%s')@self\n", mit.getMessageId(), mit.getMessageFlow().getReceiverName(),mit.getOutgoingEdge());
    }

    @Override
    public String visit(SIT sit) {
        return String.format("out('%s')@self\nThread.sleep(Signal_Duration)\nin('%s')@self\nout('%s')@self\n", sit.getSignalId(), sit.getSignalId(), sit.getOutgoingEdge());
    }

    @Override
    public String visit(NEE nee) {
        return String.format("\n");
    }

    @Override
    public String visit(MEE mee) {
        return String.format("out('%s')@%s\n\n", mee.getMessageId(), mee.getMessageFlow().getReceiverName());
    }

    @Override
    public String visit(TSE tse) throws FileNotFoundException, UnsupportedEncodingException {
        return String.format("Thread.sleep(%d)\nout('%s')@self\n", tse.getDuration(), tse.getOutgoingEdge());
    }

    @Override
    public String visit(TCE tce) throws FileNotFoundException, UnsupportedEncodingException {
        return String.format("Thread.sleep(%d)\nout('%s')@self\n", tce.getDuration(), tce.getOutgoingEdge());
    }

    @Override
    public String visit(TEE tee) throws FileNotFoundException, UnsupportedEncodingException {
            return String.format("\n");
        }

    @Override
    public String visit(SEE see) {
        return String.format("out('%s')@self\nThread.sleep(Signal_Duration)\nin('%s')@self\n \n", see.getSignalId(), see.getSignalId(), see.getOutgoingEdge());
    }



    public String visit(AND and) throws FileNotFoundException, UnsupportedEncodingException {
        StringBuilder s = new StringBuilder();

        List<String> sequences = new ArrayList<>();

        for (List<String> branchElements : and.getFlowElementMap().values()) {

            for (int i = 0; i < branchElements.size(); i++) {
                String elementId = branchElements.get(i);
                BpmnElement element = bpmnElements.getElementById(elementId);
                if (element!=null) {
                    s.append(element.accept(this));

                    if (i < branchElements.size() - 1) {
                        BpmnElement sequence = bpmnElements.getElementById(element.getOutgoingEdge());
                        s.append(sequence.accept(this));
                    }
                    if (i == branchElements.size() - 1) {
                        sequences.add(element.getOutgoingEdge());
                    }
                }
            }
        }

        for (String sequenceId : sequences) {
            if (sequenceId != and.getOutgoingEdge()) {
                BpmnElement sequence = bpmnElements.getElementById(sequenceId);
                if (sequence != null) {
                    s.append(sequence.accept(this));
                }}
            }
        s.append(String.format("out('%s')@self\n", and.getOutgoingEdge()));


        return s.toString();
    }

    @Override
    public String visit(XOR xor) throws FileNotFoundException, UnsupportedEncodingException {
        StringBuilder s = new StringBuilder();

        if (xor.getConditionElementMap().size() != 2) {
            throw new IllegalArgumentException("XOR gateway must have two branches");
        }

        Iterator<Map.Entry<String, List<String>>> iterator = xor.getConditionElementMap().entrySet().iterator();

        Map.Entry<String, List<String>> trueBranch = iterator.next();
        Map.Entry<String, List<String>> falseBranch = iterator.next();

        // Condition for the true branch
        s.append(String.format("if(%s){\n  ", trueBranch.getKey()));

        // Translating elements for the true branch
        for (String elementId : trueBranch.getValue()) {
            BpmnElement element = bpmnElements.getElementById(elementId);
            s.append(element.accept(this));
            BpmnElement sequence = bpmnElements.getElementById(element.getOutgoingEdge());
            s.append(sequence.accept(this));
        }
        s.append("} else {\n  ");

        // Translating elements for the false branch
        for (String elementId : falseBranch.getValue()) {
            BpmnElement element = bpmnElements.getElementById(elementId);
            s.append(element.accept(this));
            BpmnElement sequence = bpmnElements.getElementById(element.getOutgoingEdge());
            s.append(sequence.accept(this));
        }
        s.append("}\n");

        s.append(String.format("out('%s')@self\n", xor.getOutgoingEdge()));

        return s.toString();
    }


    @Override
    public String visit(LP lp) throws FileNotFoundException, UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder();
        sb.append("while(").append(lp.getCondition()).append("){\n");
        for (String elementId : lp.getFlowElementMap()) {
            BpmnElement element = bpmnElements.getElementById(elementId);
            sb.append(element.accept(this));
            BpmnElement sequence = bpmnElements.getElementById(element.getOutgoingEdge());
            sb.append(sequence.accept(this));
        }
        sb.append("}\n");
        sb.append("out('").append(lp.getOutgoingEdge()).append("')@self\n");
        return sb.toString();
    }


    @Override
    public String visit(ST st) {
        return String.format("// { ... initialization code ... }\nout('%s')@self\n", st.getOutgoingEdge());
    }

    public String visit(CLA cla) {
        return String.format("eval(new %s('%s'))@self\n",cla.getCalledProcess(), cla.getOutgoingEdge());
    }

    @Override
    public String visit(ESP esp) {
        return String.format("eval(new %s())@self\n",esp.getId());
    }

    public String visit(SQ sq) {
        return String.format("in('%s')@self\n\n",sq.getId());
    }

    @Override
    public String visit(PL pl) throws FileNotFoundException, UnsupportedEncodingException {
        String nodeTemplate = "\tnode %s {\n" +
                "\t\teval(new %s())@self\n" +
                "\t}\n";

        return String.format(nodeTemplate,
                pl.getName(),
                pl.getProcessName());
    }

    @Override
    public String visit(Collab collab) throws FileNotFoundException, UnsupportedEncodingException {
        StringBuilder collabCode = new StringBuilder();
        collabCode.append(String.format("net %s physical \"localhost:9999\" {\n\n", collab.getId()));

        for (PL participant : collab.getParticipants()) {
            collabCode.append(visit(participant));
        }

        collabCode.append("}\n");
        return collabCode.toString();
    }

    @Override
    public String visit(MIPL mipl) throws FileNotFoundException, UnsupportedEncodingException {
        return null;
    }

    @Override
    public String visit(DO data) throws FileNotFoundException, UnsupportedEncodingException {
        return null;
    }

}
