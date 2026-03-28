package com.example.B2XKlaim.Service.builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.example.B2XKlaim.Service.CodeGenerationService.GenerationResult;
import com.example.B2XKlaim.Service.CodeGenerationService.ProcessCode;

import lombok.extern.slf4j.Slf4j;

/**
 * Builder class for constructing GenerationResult objects using the Builder Pattern.
 * Provides a fluent API for step-by-step result construction with validation.
 */
@Slf4j
public class GenerationResultBuilder {

    private String collaborationCode;
    private String fullGeneratedCode;
    private List<ProcessCode> processes;
    private Map<String, List<String>> callActivities;
    private Map<String, List<String>> scriptTasks;
    private Map<String, List<String>> eventSubProcesses;
    private Map<String, List<String>> andBranchProcs;
    private Set<String> participants;

    private GenerationResultBuilder() {
        this.processes = new ArrayList<>();
        this.callActivities = new HashMap<>();
        this.scriptTasks = new HashMap<>();
        this.eventSubProcesses = new HashMap<>();
        this.andBranchProcs = new HashMap<>();
        this.participants = new HashSet<>();
    }

    public static GenerationResultBuilder create() {
        log.debug("Creating new GenerationResultBuilder instance");
        return new GenerationResultBuilder();
    }

    public GenerationResultBuilder withCollaborationCode(String collaborationCode) {
        log.trace("Setting collaboration code ({} chars)",
                 collaborationCode != null ? collaborationCode.length() : 0);
        this.collaborationCode = collaborationCode;
        return this;
    }

    public GenerationResultBuilder withFullGeneratedCode(String fullGeneratedCode) {
        log.trace("Setting full generated code ({} chars)",
                 fullGeneratedCode != null ? fullGeneratedCode.length() : 0);
        this.fullGeneratedCode = fullGeneratedCode;
        return this;
    }

    public GenerationResultBuilder withCode(String code) {
        log.trace("Setting both collaboration and full code ({} chars)",
                 code != null ? code.length() : 0);
        this.collaborationCode = code;
        this.fullGeneratedCode = code;
        return this;
    }

    public GenerationResultBuilder addProcess(String name, String code) {
        log.trace("Adding process: {}", name);
        this.processes.add(new ProcessCode(name, code));
        return this;
    }

    public GenerationResultBuilder addProcess(ProcessCode processCode) {
        log.trace("Adding ProcessCode: {}", processCode.getName());
        this.processes.add(processCode);
        return this;
    }

    public GenerationResultBuilder withProcesses(List<ProcessCode> processes) {
        log.trace("Setting processes list with {} items", processes.size());
        this.processes = new ArrayList<>(processes);
        return this;
    }

    public GenerationResultBuilder withCallActivities(Map<String, List<String>> callActivities) {
        log.trace("Setting call activities with {} entries", callActivities.size());
        this.callActivities = new HashMap<>(callActivities);
        return this;
    }

    public GenerationResultBuilder addCallActivity(String key, List<String> activities) {
        log.trace("Adding call activity: {}", key);
        this.callActivities.put(key, new ArrayList<>(activities));
        return this;
    }

    public GenerationResultBuilder withScriptTasks(Map<String, List<String>> scriptTasks) {
        log.trace("Setting script tasks with {} entries", scriptTasks.size());
        this.scriptTasks = new HashMap<>(scriptTasks);
        return this;
    }

    public GenerationResultBuilder addScriptTask(String key, List<String> tasks) {
        log.trace("Adding script task: {}", key);
        this.scriptTasks.put(key, new ArrayList<>(tasks));
        return this;
    }

    public GenerationResultBuilder withEventSubProcesses(Map<String, List<String>> eventSubProcesses) {
        log.trace("Setting event sub processes with {} entries", eventSubProcesses.size());
        this.eventSubProcesses = new HashMap<>(eventSubProcesses);
        return this;
    }

    public GenerationResultBuilder addEventSubProcess(String key, List<String> subProcesses) {
        log.trace("Adding event sub process: {}", key);
        this.eventSubProcesses.put(key, new ArrayList<>(subProcesses));
        return this;
    }

    public GenerationResultBuilder withAndBranchProcs(Map<String, List<String>> andBranchProcs) {
        log.trace("Setting AND branch procs with {} entries", andBranchProcs.size());
        this.andBranchProcs = new HashMap<>(andBranchProcs);
        return this;
    }

    public GenerationResultBuilder withParticipants(Set<String> participants) {
        log.trace("Setting participants with {} entries", participants.size());
        this.participants = new HashSet<>(participants);
        return this;
    }

    public GenerationResultBuilder addParticipant(String participant) {
        log.trace("Adding participant: {}", participant);
        this.participants.add(participant);
        return this;
    }

    public boolean isValid() {
        boolean valid = collaborationCode != null && fullGeneratedCode != null &&
                       processes != null && callActivities != null &&
                       scriptTasks != null && eventSubProcesses != null &&
                       andBranchProcs != null && participants != null;

        if (!valid) {
            log.warn("GenerationResultBuilder validation failed - missing required fields");
        }

        return valid;
    }

    public GenerationResult build() {
        if (!isValid()) {
            throw new IllegalStateException("GenerationResultBuilder is in invalid state - missing required fields");
        }

        log.debug("Building GenerationResult with {} processes, {} participants",
                 processes.size(), participants.size());

        return new GenerationResult(collaborationCode, fullGeneratedCode, processes,
                                  callActivities, scriptTasks, eventSubProcesses, andBranchProcs, participants);
    }

    public GenerationResult buildWithDefaults() {
        String safeCollaborationCode = collaborationCode != null ? collaborationCode : "";
        String safeFullGeneratedCode = fullGeneratedCode != null ? fullGeneratedCode : safeCollaborationCode;
        List<ProcessCode> safeProcesses = processes != null ? processes : new ArrayList<>();
        Map<String, List<String>> safeCallActivities = callActivities != null ? callActivities : new HashMap<>();
        Map<String, List<String>> safeScriptTasks = scriptTasks != null ? scriptTasks : new HashMap<>();
        Map<String, List<String>> safeEventSubProcesses = eventSubProcesses != null ? eventSubProcesses : new HashMap<>();
        Map<String, List<String>> safeAndBranchProcs = andBranchProcs != null ? andBranchProcs : new HashMap<>();
        Set<String> safeParticipants = participants != null ? participants : new HashSet<>();

        log.debug("Building GenerationResult with defaults - {} processes, {} participants",
                 safeProcesses.size(), safeParticipants.size());

        return new GenerationResult(safeCollaborationCode, safeFullGeneratedCode, safeProcesses,
                                  safeCallActivities, safeScriptTasks, safeEventSubProcesses,
                                  safeAndBranchProcs, safeParticipants);
    }

    public GenerationResultBuilder reset() {
        log.trace("Resetting GenerationResultBuilder to initial state");
        this.collaborationCode = null;
        this.fullGeneratedCode = null;
        this.processes = new ArrayList<>();
        this.callActivities = new HashMap<>();
        this.scriptTasks = new HashMap<>();
        this.eventSubProcesses = new HashMap<>();
        this.andBranchProcs = new HashMap<>();
        this.participants = new HashSet<>();
        return this;
    }
}