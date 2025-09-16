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
    private Set<String> participants;

    /**
     * Private constructor to enforce builder pattern usage.
     */
    private GenerationResultBuilder() {
        this.processes = new ArrayList<>();
        this.callActivities = new HashMap<>();
        this.scriptTasks = new HashMap<>();
        this.eventSubProcesses = new HashMap<>();
        this.participants = new HashSet<>();
    }

    /**
     * Creates a new GenerationResultBuilder instance.
     * 
     * @return New GenerationResultBuilder instance
     */
    public static GenerationResultBuilder create() {
        log.debug("Creating new GenerationResultBuilder instance");
        return new GenerationResultBuilder();
    }

    /**
     * Sets the collaboration code.
     * 
     * @param collaborationCode The collaboration code
     * @return This builder instance for method chaining
     */
    public GenerationResultBuilder withCollaborationCode(String collaborationCode) {
        log.trace("Setting collaboration code ({} chars)", 
                 collaborationCode != null ? collaborationCode.length() : 0);
        this.collaborationCode = collaborationCode;
        return this;
    }

    /**
     * Sets the full generated code (for backward compatibility).
     * 
     * @param fullGeneratedCode The full generated code
     * @return This builder instance for method chaining
     */
    public GenerationResultBuilder withFullGeneratedCode(String fullGeneratedCode) {
        log.trace("Setting full generated code ({} chars)", 
                 fullGeneratedCode != null ? fullGeneratedCode.length() : 0);
        this.fullGeneratedCode = fullGeneratedCode;
        return this;
    }

    /**
     * Sets both collaboration and full generated code to the same value.
     * Useful when they should be identical.
     * 
     * @param code The code to use for both fields
     * @return This builder instance for method chaining
     */
    public GenerationResultBuilder withCode(String code) {
        log.trace("Setting both collaboration and full code ({} chars)", 
                 code != null ? code.length() : 0);
        this.collaborationCode = code;
        this.fullGeneratedCode = code;
        return this;
    }

    /**
     * Adds a single process to the result.
     * 
     * @param name The process name
     * @param code The process code
     * @return This builder instance for method chaining
     */
    public GenerationResultBuilder addProcess(String name, String code) {
        log.trace("Adding process: {}", name);
        this.processes.add(new ProcessCode(name, code));
        return this;
    }

    /**
     * Adds a ProcessCode object to the result.
     * 
     * @param processCode The ProcessCode object
     * @return This builder instance for method chaining
     */
    public GenerationResultBuilder addProcess(ProcessCode processCode) {
        log.trace("Adding ProcessCode: {}", processCode.getName());
        this.processes.add(processCode);
        return this;
    }

    /**
     * Sets the processes list.
     * 
     * @param processes List of ProcessCode objects
     * @return This builder instance for method chaining
     */
    public GenerationResultBuilder withProcesses(List<ProcessCode> processes) {
        log.trace("Setting processes list with {} items", processes.size());
        this.processes = new ArrayList<>(processes);
        return this;
    }

    /**
     * Sets the call activities map.
     * 
     * @param callActivities Map of call activities
     * @return This builder instance for method chaining
     */
    public GenerationResultBuilder withCallActivities(Map<String, List<String>> callActivities) {
        log.trace("Setting call activities with {} entries", callActivities.size());
        this.callActivities = new HashMap<>(callActivities);
        return this;
    }

    /**
     * Adds a single call activity entry.
     * 
     * @param key The call activity key
     * @param activities List of activities
     * @return This builder instance for method chaining
     */
    public GenerationResultBuilder addCallActivity(String key, List<String> activities) {
        log.trace("Adding call activity: {}", key);
        this.callActivities.put(key, new ArrayList<>(activities));
        return this;
    }

    /**
     * Sets the script tasks map.
     * 
     * @param scriptTasks Map of script tasks
     * @return This builder instance for method chaining
     */
    public GenerationResultBuilder withScriptTasks(Map<String, List<String>> scriptTasks) {
        log.trace("Setting script tasks with {} entries", scriptTasks.size());
        this.scriptTasks = new HashMap<>(scriptTasks);
        return this;
    }

    /**
     * Adds a single script task entry.
     * 
     * @param key The script task key
     * @param tasks List of tasks
     * @return This builder instance for method chaining
     */
    public GenerationResultBuilder addScriptTask(String key, List<String> tasks) {
        log.trace("Adding script task: {}", key);
        this.scriptTasks.put(key, new ArrayList<>(tasks));
        return this;
    }

    /**
     * Sets the event sub processes map.
     * 
     * @param eventSubProcesses Map of event sub processes
     * @return This builder instance for method chaining
     */
    public GenerationResultBuilder withEventSubProcesses(Map<String, List<String>> eventSubProcesses) {
        log.trace("Setting event sub processes with {} entries", eventSubProcesses.size());
        this.eventSubProcesses = new HashMap<>(eventSubProcesses);
        return this;
    }

    /**
     * Adds a single event sub process entry.
     * 
     * @param key The event sub process key
     * @param subProcesses List of sub processes
     * @return This builder instance for method chaining
     */
    public GenerationResultBuilder addEventSubProcess(String key, List<String> subProcesses) {
        log.trace("Adding event sub process: {}", key);
        this.eventSubProcesses.put(key, new ArrayList<>(subProcesses));
        return this;
    }

    /**
     * Sets the participants set.
     * 
     * @param participants Set of participant names
     * @return This builder instance for method chaining
     */
    public GenerationResultBuilder withParticipants(Set<String> participants) {
        log.trace("Setting participants with {} entries", participants.size());
        this.participants = new HashSet<>(participants);
        return this;
    }

    /**
     * Adds a single participant.
     * 
     * @param participant The participant name
     * @return This builder instance for method chaining
     */
    public GenerationResultBuilder addParticipant(String participant) {
        log.trace("Adding participant: {}", participant);
        this.participants.add(participant);
        return this;
    }

    /**
     * Validates the current state of the builder before building.
     * 
     * @return true if the builder state is valid, false otherwise
     */
    public boolean isValid() {
        boolean valid = collaborationCode != null && fullGeneratedCode != null && 
                       processes != null && callActivities != null && 
                       scriptTasks != null && eventSubProcesses != null && 
                       participants != null;

        if (!valid) {
            log.warn("GenerationResultBuilder validation failed - missing required fields");
        }

        return valid;
    }

    /**
     * Builds and returns the final GenerationResult object.
     * 
     * @return The constructed GenerationResult
     * @throws IllegalStateException if the builder state is invalid
     */
    public GenerationResult build() {
        if (!isValid()) {
            throw new IllegalStateException("GenerationResultBuilder is in invalid state - missing required fields");
        }

        log.debug("Building GenerationResult with {} processes, {} participants", 
                 processes.size(), participants.size());

        return new GenerationResult(collaborationCode, fullGeneratedCode, processes, 
                                  callActivities, scriptTasks, eventSubProcesses, participants);
    }

    /**
     * Builds a GenerationResult with default values for missing fields.
     * This method will not throw exceptions but will use sensible defaults.
     * 
     * @return The constructed GenerationResult with defaults applied
     */
    public GenerationResult buildWithDefaults() {
        String safeCollaborationCode = collaborationCode != null ? collaborationCode : "";
        String safeFullGeneratedCode = fullGeneratedCode != null ? fullGeneratedCode : safeCollaborationCode;
        List<ProcessCode> safeProcesses = processes != null ? processes : new ArrayList<>();
        Map<String, List<String>> safeCallActivities = callActivities != null ? callActivities : new HashMap<>();
        Map<String, List<String>> safeScriptTasks = scriptTasks != null ? scriptTasks : new HashMap<>();
        Map<String, List<String>> safeEventSubProcesses = eventSubProcesses != null ? eventSubProcesses : new HashMap<>();
        Set<String> safeParticipants = participants != null ? participants : new HashSet<>();

        log.debug("Building GenerationResult with defaults - {} processes, {} participants", 
                 safeProcesses.size(), safeParticipants.size());

        return new GenerationResult(safeCollaborationCode, safeFullGeneratedCode, safeProcesses, 
                                  safeCallActivities, safeScriptTasks, safeEventSubProcesses, safeParticipants);
    }

    /**
     * Resets the builder to initial state for reuse.
     * 
     * @return This builder instance for method chaining
     */
    public GenerationResultBuilder reset() {
        log.trace("Resetting GenerationResultBuilder to initial state");
        this.collaborationCode = null;
        this.fullGeneratedCode = null;
        this.processes = new ArrayList<>();
        this.callActivities = new HashMap<>();
        this.scriptTasks = new HashMap<>();
        this.eventSubProcesses = new HashMap<>();
        this.participants = new HashSet<>();
        return this;
    }
}