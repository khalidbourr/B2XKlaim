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

public interface Visitor {

    public String visit(NSE nse) throws FileNotFoundException, UnsupportedEncodingException;
    public String visit(MSE mse) throws FileNotFoundException, UnsupportedEncodingException;
    public String visit(SSE sse) throws FileNotFoundException, UnsupportedEncodingException;
    public String visit(MIC mic) throws FileNotFoundException, UnsupportedEncodingException;
    public String visit(SIC sic) throws FileNotFoundException, UnsupportedEncodingException;
    public String visit(MIT mit) throws FileNotFoundException, UnsupportedEncodingException;
    public String visit(SIT sit) throws FileNotFoundException, UnsupportedEncodingException;
    public String visit(NEE nee) throws FileNotFoundException, UnsupportedEncodingException;
    public String visit(MEE nee) throws FileNotFoundException, UnsupportedEncodingException;
    public String visit(SEE nee) throws FileNotFoundException, UnsupportedEncodingException;
    public String visit(XOR xor) throws FileNotFoundException, UnsupportedEncodingException;
    public String visit(AND and) throws FileNotFoundException, UnsupportedEncodingException;
    public String visit(LP lp) throws FileNotFoundException, UnsupportedEncodingException;
    public String visit(CLA cla) throws FileNotFoundException, UnsupportedEncodingException;
    public String visit(ESP esp) throws FileNotFoundException, UnsupportedEncodingException;
    public String visit(ST st) throws FileNotFoundException, UnsupportedEncodingException;
    public String visit(SQ sq) throws FileNotFoundException, UnsupportedEncodingException;
    public String visit(PL pl) throws FileNotFoundException, UnsupportedEncodingException;
    public String visit(MIPL mipl) throws FileNotFoundException, UnsupportedEncodingException;

    public String visit(Collab collab) throws FileNotFoundException, UnsupportedEncodingException;
    public String visit(DO data) throws FileNotFoundException, UnsupportedEncodingException;

}
