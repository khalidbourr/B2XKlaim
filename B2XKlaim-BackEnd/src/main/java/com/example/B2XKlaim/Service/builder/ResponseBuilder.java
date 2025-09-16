package com.example.B2XKlaim.Service.builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.example.B2XKlaim.Service.CodeGenerationService.ProcessCode;

import lombok.extern.slf4j.Slf4j;

/**
 * Builder class for constructing API responses using the Builder Pattern.
 * Provides a fluent API for step-by-step response construction.
 */
@Slf4j
public class ResponseBuilder {

    private String collaboration;
    private List<Map<String, String>> processes;
    private Map<String, List<String>> callActivities;
    private Map<String, List<String>> scriptTaskProcs;
    private Map<String, List<String>> eventSubProcesses;
    private List<String> participants;
    private String error;

    /**
     * Private constructor to enforce builder pattern usage.
     */
    private ResponseBuilder() {
        this.processes = new ArrayList<>();
        this.callActivities = new HashMap<>();
        this.scriptTaskProcs = new HashMap<>();
        this.eventSubProcesses = new HashMap<>();
        this.participants = new ArrayList<>();
    }

    /**
     * Creates a new ResponseBuilder instance.
     * 
     * @return New ResponseBuilder instance
     */
    public static ResponseBuilder create() {
        log.debug("Creating new ResponseBuilder instance");
        return new ResponseBuilder();
    }

    /**
     * Sets the collaboration code.
     * 
     * @param collaboration The collaboration code
     * @return This builder instance for method chaining
     */
    public ResponseBuilder withCollaboration(String collaboration) {
        log.trace("Setting collaboration code ({} chars)", collaboration != null ? collaboration.length() : 0);
        this.collaboration = collaboration;
        return this;
    }

    /**
     * Adds a single process to the response.
     * 
     * @param name The process name
     * @param code The process code
     * @return This builder instance for method chaining
     */
    public ResponseBuilder addProcess(String name, String code) {
        log.trace("Adding process: {}", name);
        Map<String, String> processEntry = new HashMap<>();
        processEntry.put("name", name);
        processEntry.put("code", code);
        this.processes.add(processEntry);
        return this;
    }

    /**
     * Adds multiple processes from ProcessCode objects.
     * 
     * @param processCodes List of ProcessCode objects
     * @return This builder instance for method chaining
     */
    public ResponseBuilder withProcesses(List<ProcessCode> processCodes) {
        log.trace("Adding {} ProcessCode objects", processCodes.size());
        for (ProcessCode processCode : processCodes) {
            addProcess(processCode.getName(), processCode.getCode());
        }
        return this;
    }

    /**
     * Sets the processes directly from a list of maps.
     * 
     * @param processes List of process maps
     * @return This builder instance for method chaining
     */
    public ResponseBuilder withProcessList(List<Map<String, String>> processes) {
        log.trace("Setting process list with {} processes", processes.size());
        this.processes = new ArrayList<>(processes);
        return this;
    }

    /**
     * Sets the call activities map.
     * 
     * @param callActivities Map of call activities
     * @return This builder instance for method chaining
     */
    public ResponseBuilder withCallActivities(Map<String, List<String>> callActivities) {
        log.trace("Setting call activities with {} entries", callActivities.size());
        this.callActivities = new HashMap<>(callActivities);
        return this;
    }

    /**
     * Sets the script task procedures map.
     * 
     * @param scriptTaskProcs Map of script task procedures
     * @return This builder instance for method chaining
     */
    public ResponseBuilder withScriptTaskProcs(Map<String, List<String>> scriptTaskProcs) {
        log.trace("Setting script task procs with {} entries", scriptTaskProcs.size());
        this.scriptTaskProcs = new HashMap<>(scriptTaskProcs);
        return this;
    }

    /**
     * Sets the event sub processes map.
     * 
     * @param eventSubProcesses Map of event sub processes
     * @return This builder instance for method chaining
     */
    public ResponseBuilder withEventSubProcesses(Map<String, List<String>> eventSubProcesses) {
        log.trace("Setting event sub processes with {} entries", eventSubProcesses.size());
        this.eventSubProcesses = new HashMap<>(eventSubProcesses);
        return this;
    }

    /**
     * Sets the participants from a Set.
     * 
     * @param participants Set of participant names
     * @return This builder instance for method chaining
     */
    public ResponseBuilder withParticipants(Set<String> participants) {
        log.trace("Setting participants with {} entries", participants.size());
        this.participants = new ArrayList<>(participants);
        return this;
    }

    /**
     * Sets the participants from a List.
     * 
     * @param participants List of participant names
     * @return This builder instance for method chaining
     */
    public ResponseBuilder withParticipantsList(List<String> participants) {
        log.trace("Setting participants list with {} entries", participants.size());
        this.participants = new ArrayList<>(participants);
        return this;
    }

    /**
     * Sets an error message for error responses.
     * 
     * @param error The error message
     * @return This builder instance for method chaining
     */
    public ResponseBuilder withError(String error) {
        log.trace("Setting error message: {}", error);
        this.error = error;
        return this;
    }

    /**
     * Builds and returns the final response map.
     * 
     * @return The constructed response map
     */
    public Map<String, Object> build() {
        log.debug("Building response with {} processes, {} participants", 
                 processes.size(), participants.size());

        Map<String, Object> response = new HashMap<>();

        if (error != null) {
            log.debug("Building error response");
            response.put("error", error);
            return response;
        }

        response.put("collaboration", collaboration != null ? collaboration : "");
        response.put("processes", processes);
        response.put("callActivities", callActivities);
        response.put("scriptTaskProcs", scriptTaskProcs);
        response.put("eventSubProcesses", eventSubProcesses);
        response.put("participants", participants);

        log.debug("Built complete response with all fields");
        return response;
    }

    /**
     * Builds a minimal response with only essential fields.
     * 
     * @return The minimal response map
     */
    public Map<String, Object> buildMinimal() {
        log.debug("Building minimal response with {} processes", processes.size());

        Map<String, Object> response = new HashMap<>();
        response.put("collaboration", collaboration != null ? collaboration : "");
        response.put("processes", processes);

        return response;
    }

    /**
     * Validates the current state of the builder before building.
     * 
     * @return true if the builder state is valid, false otherwise
     */
    public boolean isValid() {
        if (error != null) {
            return true; // Error responses are always valid
        }

        boolean valid = collaboration != null && processes != null && 
                       callActivities != null && scriptTaskProcs != null && 
                       eventSubProcesses != null && participants != null;

        log.trace("Builder validation result: {}", valid);
        return valid;
    }

    /**
     * Resets the builder to initial state for reuse.
     * 
     * @return This builder instance for method chaining
     */
    public ResponseBuilder reset() {
        log.trace("Resetting ResponseBuilder to initial state");
        this.collaboration = null;
        this.processes = new ArrayList<>();
        this.callActivities = new HashMap<>();
        this.scriptTaskProcs = new HashMap<>();
        this.eventSubProcesses = new HashMap<>();
        this.participants = new ArrayList<>();
        this.error = null;
        return this;
    }
}