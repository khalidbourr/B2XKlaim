<template>
  <div id="container" class="app-container">
    <!-- Top navigation bar -->
    <div id="nav-bar" class="topnav">
      <div class="logo-container">
        <a href="index.html" class="active">
          <img src="./assets/b2xklaim.jpg" alt="B2Xklaim Web Client" class="logo">
        </a>
        <div class="title-container">
          <h1 class="app-main-title">B2XKlaim</h1>
          <h2 class="app-subtitle">an X-Klaim BPMN Translator</h2>
        </div>
      </div>
      <div class="nav-buttons">
        <a href="#" @click="newProject" class="nav-btn nav-btn-new">
          <i class="fas fa-plus"></i> New
        </a>
        <a href="#" id="ImportBPMN" @click="importBPMN" class="nav-btn">
          <i class="fa fa-upload"></i> Import
        </a>
        <a href="#" id="SaveBPMN" @click="saveBPMN" class="nav-btn">
          <i class="fa fa-save"></i> Save
        </a>
        <a href="#" id="Download" @click="exportCode" class="download-btn">
          <i class="fa fa-download"></i> Download
        </a>
      </div>
      <!-- Hidden file input for import -->
      <input type="file" id="bpmn-file-input" accept=".bpmn,.xml,.b2x" style="display: none;" @change="handleFileSelect">
    </div>

    <div id="main-content">
      <div class="process-tabs">
        <div v-for="[processId, processData] in processesArray"
             :key="processId"
             :class="{ 'tab': true, 'active': activeProcess === processId }"
             @click="switchToProcess(processId)">
          {{ processData.name }}
          <span v-if="processId !== 'main'"
                class="close-tab"
                @click.stop="removeProcess(processId)">
      ×
    </span>
        </div>
        <button class="add-process-btn" @click="addNewProcess">+ Add Process</button>
      </div>
      <!-- BPMN Editor Section -->
      <div id="canvas-container">
        <div class="panel-header">
          <h3>BPMN Editor</h3>
        </div>
        <div class="editor-panels">
          <div id="canvas" class="canvas-panel"></div>
          <div id="properties" class="properties-panel"></div>
        </div>
        <div class="center-button-container">
          <button @click="generateCode" class="primary-button">Generate X-Klaim Code</button>
        </div>
      </div>

      <!-- Generated Code Section -->
      <div v-if="showButtons" id="code-section">
        <div class="code-tabs">
          <button v-for="tab in availableTabs" :key="tab" @click="activeTab = tab"
                  :class="{ 'tab-button--active': activeTab === tab }">
            {{ tab }}
          </button>
        </div>

        <!-- Project Configuration Panel -->
        <div class="project-config">
          <h3 class="section-title">Project Configuration</h3>
          <div class="form-grid">
            <div class="form-group">
              <label for="projectName">Project Name:</label>
              <input type="text" id="projectName" v-model="projectConfig.name" placeholder="my-xklaim-project">
            </div>
            <div class="form-group">
              <label for="groupId">Group ID:</label>
              <input type="text" id="groupId" v-model="projectConfig.groupId" placeholder="com.example">
            </div>
            <div class="form-group">
              <label for="artifactId">Artifact ID:</label>
              <input type="text" id="artifactId" v-model="projectConfig.artifactId" placeholder="xklaim-bpmn-project">
            </div>
            <div class="form-group">
              <label for="version">Version:</label>
              <input type="text" id="version" v-model="projectConfig.version" placeholder="1.0-SNAPSHOT">
            </div>
          </div>
        </div>

        <!-- Collaboration Code Textarea -->
        <div v-if="collaboration && activeTab === 'collaboration'" class="code-container collaboration">
          <div class="code-header">
            <h4>Main Collaboration</h4>
            <div class="code-actions">
              <span @click="copyToClipboard('collaboration')" class="copy-icon" title="Copy to clipboard">
                <i class="fa fa-copy"></i>
              </span>
            </div>
          </div>
          <div class="textarea-wrapper">
            <textarea ref="collaboration" class="textarea code-editor" placeholder="Collaboration Code..." v-model="collaboration"></textarea>
          </div>
        </div>

        <!-- Participant's Process Code Textareas -->
        <div v-if="activeTab === 'processes'" class="code-processes">
          <div v-for="process in processes" :key="process.name" class="code-container process">
            <div class="code-header">
              <h4>{{ process.name }}</h4>
              <div class="code-actions">
                <span @click="copyToClipboard(process.name)" class="copy-icon" title="Copy to clipboard">
                  <i class="fa fa-copy"></i>
                </span>
              </div>
            </div>
            <div class="textarea-wrapper">
              <textarea :ref="process.name" class="textarea code-editor"
                        :placeholder="process.name + ' Code...'" v-model="process.code"></textarea>
            </div>
          </div>
        </div>

        <!-- Event Sub-Processes Code Textareas -->
        <div v-if="activeTab === 'event-subprocesses' && hasEventSubprocesses" class="code-processes">
          <div v-for="(codeList, espId) in eventSubProcesses" :key="espId" class="code-container process">
            <div class="code-header">
              <h4>Event Sub-Process: {{ espId }}</h4>
              <div class="code-actions">
                <span @click="copyToClipboard(espId)" class="copy-icon" title="Copy to clipboard">
                  <i class="fa fa-copy"></i>
                </span>
              </div>
            </div>
            <div class="textarea-wrapper">
              <textarea :ref="espId" class="textarea code-editor"
                        :placeholder="'Event Sub-Process Code...'" v-model="eventSubProcesses[espId][0]"></textarea>
            </div>
          </div>
        </div>
      </div>
    </div>



    <!-- Footer -->
    <footer class="footer">
      <p>&copy; 2026 B2XKlaim. All rights reserved.</p>
    </footer>
  </div>
</template>

<script>
import BpmnModeler from "camunda-bpmn-js/lib/camunda-platform/Modeler";
import "camunda-bpmn-js/dist/assets/camunda-platform-modeler.css";
import JSZip from 'jszip';
import CustomPaletteProvider from './CustomPaletteProvider.js';
import CustomReplaceMenuProvider from './CustomReplaceMenuProvider.js';
import { CustomCreateMenuProvider, CustomAppendMenuProvider } from './CustomCreateAppendProvider.js';
import CustomPropertiesProvider from './CustomPropertiesProvider.js';
import CallActivityDrilldownProvider from './CallActivityDrilldownProvider.js';

export default {
  name: "App",
  data() {
    return {
      bpmnModeler: null,
      activeTab: 'collaboration',
      showButtons: false,
      collaboration: '',
      processes: [],
      callActivities: {},
      scriptTaskProcs: {},
      eventSubProcesses: {},
      allTabs: ['collaboration', 'processes', 'event-subprocesses'],
      projectConfig: {
        name: 'xklaim-bpmn-project',
        groupId: 'com.example',
        artifactId: 'xklaim-bpmn-project',
        version: '1.0-SNAPSHOT'
      },
      bpmnProcesses: new Map([
        ['main', { xml: '', name: 'Main Process' }]
      ]),
      activeProcess: 'main'
    };
  },

  computed: {
    hasEventSubprocesses() {
      return Object.keys(this.eventSubProcesses).length > 0;
    },
    availableTabs() {
      // Filter tabs to only show event-subprocesses when they exist
      return this.allTabs.filter(tab => {
        if (tab === 'event-subprocesses') {
          return this.hasEventSubprocesses;
        }
        return true;
      });
    },
    processesArray() {
      return Array.from(this.bpmnProcesses.entries());
    }
  },

  async mounted() {
    this.bpmnModeler = new BpmnModeler({
      container: "#canvas",
      propertiesPanel: {
        parent: "#properties",
      },
      additionalModules: [
        {
          __init__: ['paletteProvider', 'customPropertiesProvider', 'callActivityDrilldownProvider'],
          paletteProvider: ['type', CustomPaletteProvider],
          replaceMenuProvider: ['type', CustomReplaceMenuProvider],
          createMenuProvider: ['type', CustomCreateMenuProvider],
          appendMenuProvider: ['type', CustomAppendMenuProvider],
          customPropertiesProvider: ['type', CustomPropertiesProvider],
          callActivityDrilldownProvider: ['type', CallActivityDrilldownProvider]
        },
      ]
    });

    // Restore previous session from localStorage, or load empty diagram
    await this.restoreFromLocalStorage();

    const eventBus = this.bpmnModeler.get('eventBus');

    // Listen for drill-down into Call Activities
    eventBus.on('callActivity.drilldown', (event) => {
      this.openCallActivityProcess(event.element);
    });

    // Auto-sync participant name to its process and auto-save to localStorage
    eventBus.on('commandStack.changed', () => {
      const elementRegistry = this.bpmnModeler.get('elementRegistry');

      elementRegistry.filter(el => el.type === 'bpmn:Participant').forEach(participant => {
        const bo = participant.businessObject;
        const processRef = bo.processRef;
        if (bo.name && processRef) {
          const expectedName = bo.name + 'Behavior';
          // Set process name if missing, or still has the default "Main" name
          if (!processRef.name || processRef.name === 'Main') {
            processRef.name = expectedName;
          }
        }
      });

      // Auto-save after every change
      this.saveToLocalStorage();
    });
  },
  methods: {
    // Default empty diagram for new projects
    getDefaultDiagram() {
      return '<?xml version="1.0" encoding="UTF-8"?>\n' +
        '<bpmn:definitions xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" id="Definitions_1mpw3ap" targetNamespace="http://bpmn.io/schema/bpmn" exporter="bpmn-js (https://demo.bpmn.io)" exporterVersion="14.0.0">\n' +
        '  <bpmn:process id="Process_0d01xqv" name="Main" isExecutable="false">\n' +
        '    <bpmn:startEvent id="StartEvent_08r9k21" />\n' +
        '  </bpmn:process>\n' +
        '  <bpmndi:BPMNDiagram id="BPMNDiagram_1">\n' +
        '    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="Process_0d01xqv">\n' +
        '      <bpmndi:BPMNShape id="_BPMNShape_StartEvent_2" bpmnElement="StartEvent_08r9k21">\n' +
        '        <dc:Bounds x="152" y="82" width="36" height="36" />\n' +
        '      </bpmndi:BPMNShape>\n' +
        '    </bpmndi:BPMNPlane>\n' +
        '  </bpmndi:BPMNDiagram>\n' +
        '</bpmn:definitions>';
    },

    // Save current session to localStorage
    async saveToLocalStorage() {
      try {
        if (this.bpmnModeler) {
          const currentXML = await this.bpmnModeler.saveXML({ format: true });
          this.bpmnProcesses.get(this.activeProcess).xml = currentXML.xml;
        }
        const data = { activeProcess: this.activeProcess, processes: {} };
        for (const [key, val] of this.bpmnProcesses.entries()) {
          data.processes[key] = { xml: val.xml, name: val.name };
        }
        localStorage.setItem('b2xklaim_project', JSON.stringify(data));
      } catch (err) {
        console.error('Auto-save failed:', err);
      }
    },

    // Restore session from localStorage, or load default empty diagram
    async restoreFromLocalStorage() {
      try {
        const saved = localStorage.getItem('b2xklaim_project');
        if (saved) {
          const data = JSON.parse(saved);
          this.bpmnProcesses = new Map();
          for (const [key, val] of Object.entries(data.processes)) {
            this.bpmnProcesses.set(key, { xml: val.xml, name: val.name });
          }
          const activeKey = data.activeProcess || 'main';
          const processData = this.bpmnProcesses.get(activeKey);
          if (processData && processData.xml) {
            await this.bpmnModeler.importXML(processData.xml);
            this.activeProcess = activeKey;
            this.bpmnModeler.get('canvas').zoom('fit-viewport');
            this.$forceUpdate();
            return;
          }
        }
      } catch (err) {
        console.error('Failed to restore from localStorage:', err);
      }

      // Fallback: load default empty diagram
      await this.bpmnModeler.importXML(this.getDefaultDiagram());
      this.bpmnModeler.get('canvas').zoom('fit-viewport');
    },

    // Clear everything and start a fresh project
    async newProject() {
      if (!confirm('Start a new project? All unsaved work will be lost.')) return;

      localStorage.removeItem('b2xklaim_project');
      this.bpmnProcesses = new Map([['main', { xml: '', name: 'Main Process' }]]);
      this.activeProcess = 'main';
      this.showButtons = false;
      this.collaboration = '';
      this.processes = [];
      this.callActivities = {};
      this.scriptTaskProcs = {};
      this.eventSubProcesses = {};

      await this.bpmnModeler.importXML(this.getDefaultDiagram());
      this.bpmnModeler.get('canvas').zoom('fit-viewport');
      this.$forceUpdate();
    },

    // Open or create a sub-process for a Call Activity element
    async openCallActivityProcess(element) {
      const bo = element.businessObject;
      const processKey = `callActivity_${bo.id}`;

      // If this Call Activity already has a linked process tab, switch to it
      if (this.bpmnProcesses.has(processKey)) {
        await this.switchToProcess(processKey);
        return;
      }

      // Prompt for a meaningful name if the Call Activity has no name yet
      let activityName = bo.name;
      if (!activityName || !activityName.trim()) {
        activityName = prompt('Enter a name for this called process:');
        if (!activityName || !activityName.trim()) {
          return; // User cancelled
        }
        activityName = activityName.trim();
        // Set the name on the Call Activity element
        const modeling = this.bpmnModeler.get('modeling');
        modeling.updateProperties(element, { name: activityName });
      }

      const sanitizedName = activityName.replace(/[^a-zA-Z0-9_]/g, '_');

      // Check if a process with this name already exists (reuse — same name = same process)
      let existingKey = null;
      for (const [key, data] of this.bpmnProcesses.entries()) {
        if (data.name === activityName) {
          existingKey = key;
          break;
        }
      }

      if (existingKey) {
        // Reuse existing process — just link and navigate
        const modeling = this.bpmnModeler.get('modeling');
        modeling.updateProperties(element, { calledElement: sanitizedName });
        await this.switchToProcess(existingKey);
        return;
      }

      // Create a new process tab for this Call Activity
      const newProcessXML = this.createEmptyBPMNProcess(activityName);

      this.bpmnProcesses.set(processKey, {
        xml: newProcessXML,
        name: activityName
      });

      // Set calledElement directly to the name (readable in generated X-Klaim code)
      const modeling = this.bpmnModeler.get('modeling');
      modeling.updateProperties(element, {
        calledElement: sanitizedName
      });

      // Switch to the new process tab
      await this.switchToProcess(processKey);
      this.$forceUpdate();
    },

    importBPMN() {
      document.getElementById('bpmn-file-input').click();
    },

    handleFileSelect(event) {
      const file = event.target.files[0];
      if (!file) return;

      const reader = new FileReader();
      reader.onload = async (e) => {
        const content = e.target.result;

        try {
          // Try loading as a B2XKlaim project file (.b2x)
          const projectData = JSON.parse(content);
          if (projectData.b2xklaimVersion && projectData.processes) {
            await this.loadProject(projectData);
            return;
          }
        } catch (_) {
          // Not JSON — treat as single BPMN XML file
        }

        // Load as a single BPMN XML into the main tab
        try {
          this.bpmnProcesses = new Map([['main', { xml: content, name: 'Main Process' }]]);
          this.activeProcess = 'main';
          const { warnings } = await this.bpmnModeler.importXML(content);
          if (warnings.length) {
            console.warn('Warnings while importing BPMN:', warnings);
          }
          this.bpmnModeler.get('canvas').zoom('fit-viewport');
          this.$forceUpdate();
        } catch (err) {
          console.error('Error importing BPMN diagram:', err);
          alert('Error importing BPMN diagram: ' + err.message);
        }
      };

      reader.onerror = (e) => {
        console.error('Error reading file:', e);
        alert('Error reading file: ' + e.target.error);
      };

      reader.readAsText(file);
      // Reset so the same file can be imported again
      event.target.value = '';
    },

    // Load a B2XKlaim project file with all process tabs
    async loadProject(projectData) {
      this.bpmnProcesses = new Map();
      for (const [key, data] of Object.entries(projectData.processes)) {
        this.bpmnProcesses.set(key, { xml: data.xml, name: data.name });
      }

      const activeKey = projectData.activeProcess || 'main';
      const processData = this.bpmnProcesses.get(activeKey);
      if (processData) {
        await this.bpmnModeler.importXML(processData.xml);
        this.activeProcess = activeKey;
        this.bpmnModeler.get('canvas').zoom('fit-viewport');
      }
      this.$forceUpdate();
    },

    async saveBPMN() {
      try {
        // Save the currently active process XML first
        if (this.bpmnModeler) {
          const currentXML = await this.bpmnModeler.saveXML({ format: true });
          this.bpmnProcesses.get(this.activeProcess).xml = currentXML.xml;
        }

        // Build project file containing all process tabs
        const projectData = {
          b2xklaimVersion: 1,
          activeProcess: this.activeProcess,
          processes: {}
        };
        for (const [key, data] of this.bpmnProcesses.entries()) {
          projectData.processes[key] = { xml: data.xml, name: data.name };
        }

        const blob = new Blob([JSON.stringify(projectData, null, 2)], { type: 'application/json' });
        const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
        const filename = `b2xklaim-project-${timestamp}.b2x`;
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = filename;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(url);
      } catch (err) {
        console.error('Error saving project:', err);
        alert('Error saving project: ' + err.message);
      }
    },

    async addNewProcess() {
      const processName = prompt('Enter process name:');
      if (!processName || !processName.trim()) {
        return; // User cancelled or entered empty name
      }

      const trimmedName = processName.trim();

      if (this.bpmnProcesses.has(trimmedName)) {
        alert(`Process "${trimmedName}" already exists!`);
        return;
      }

      try {
        const newProcessXML = this.createEmptyBPMNProcess(trimmedName);
        this.bpmnProcesses.set(trimmedName, {
          xml: newProcessXML,
          name: trimmedName
        });

        await this.switchToProcess(trimmedName);

        this.$forceUpdate();
      } catch (error) {
        console.error('Error creating new process:', error);
        alert(`Error creating process "${trimmedName}": ${error.message}`);

        if (this.bpmnProcesses.has(trimmedName)) {
          this.bpmnProcesses.delete(trimmedName);
        }
      }
    },

    async removeProcess(processId) {
      if (processId === 'main') {
        alert('Cannot remove the main process!');
        return;
      }

      if (confirm(`Are you sure you want to remove the process "${this.bpmnProcesses.get(processId).name}"?`)) {
        this.bpmnProcesses.delete(processId);
        if (this.activeProcess === processId) {
          await this.switchToProcess('main');
        }

        this.$forceUpdate();
      }
    },

    async switchToProcess(processName) {
      try {
        if (this.bpmnModeler) {
          const currentXML = await this.bpmnModeler.saveXML({ format: true });
          this.bpmnProcesses.get(this.activeProcess).xml = currentXML.xml;
        }

        // Load new process
        const processData = this.bpmnProcesses.get(processName);
        if (processData) {
          const result = await this.bpmnModeler.importXML(processData.xml);

          if (result.warnings && result.warnings.length > 0) {
            console.warn('Warnings while switching process:', result.warnings);
          }

          this.activeProcess = processName;
          this.bpmnModeler.get('canvas').zoom('fit-viewport');
          this.saveToLocalStorage();
        } else {
          throw new Error(`Process data not found for: ${processName}`);
        }
      } catch (error) {
        console.error('Error switching process:', error);
        alert(`Error switching to process "${processName}": ${error.message}`);
      }
    },

    createEmptyBPMNProcess(processName) {
      const safe = processName.replace(/[^a-zA-Z0-9_]/g, '_');
      const startEventId = `StartEvent_${safe}`;
      const flowId = `Flow_${safe}`;

      return `<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
                  xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  id="Definitions_${safe}"
                  targetNamespace="http://bpmn.io/schema/bpmn"
                  exporter="bpmn-js (https://demo.bpmn.io)"
                  exporterVersion="14.0.0">
  <bpmn:process id="${safe}" name="${processName}" isExecutable="false">
    <bpmn:startEvent id="${startEventId}" name="start">
      <bpmn:outgoing>${flowId}</bpmn:outgoing>
    </bpmn:startEvent>
  </bpmn:process>

  <bpmndi:BPMNDiagram id="BPMNDiagram_${safe}">
    <bpmndi:BPMNPlane id="BPMNPlane_${safe}" bpmnElement="${safe}">
      <bpmndi:BPMNShape id="Shape_${startEventId}" bpmnElement="${startEventId}">
        <dc:Bounds x="152" y="102" width="36" height="36" />
        <bpmndi:BPMNLabel>
          <dc:Bounds x="157" y="145" width="25" height="14" />
        </bpmndi:BPMNLabel>
      </bpmndi:BPMNShape>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</bpmn:definitions>`;
    },



    // Validate all process tabs before sending to backend
    validateDiagrams() {
      const errors = [];
      const ns = 'http://www.omg.org/spec/BPMN/20100524/MODEL';
      const parser = new DOMParser();

      // Helper to get a readable label for an element
      const label = (el) => el.getAttribute('name') || el.getAttribute('id') || 'unnamed';

      // Helper to check if an element has a child event definition of a given type
      const hasEventDef = (el, defType) => el.getElementsByTagNameNS(ns, defType).length > 0;

      for (const [tabKey, processData] of this.bpmnProcesses.entries()) {
        const tabName = processData.name || tabKey;
        if (!processData.xml) {
          errors.push(`Tab "${tabName}" has no BPMN content`);
          continue;
        }

        const doc = parser.parseFromString(processData.xml, 'text/xml');

        // --- Participants ---
        const participants = doc.getElementsByTagNameNS(ns, 'participant');
        for (let i = 0; i < participants.length; i++) {
          if (!participants[i].getAttribute('name')) {
            errors.push(`A participant in "${tabName}" has no name`);
          }
        }

        // --- Processes ---
        const processes = doc.getElementsByTagNameNS(ns, 'process');
        for (let i = 0; i < processes.length; i++) {
          if (!processes[i].getAttribute('name')) {
            errors.push(`Process "${processes[i].getAttribute('id')}" in "${tabName}" has no name`);
          }
        }

        // --- Call Activities ---
        const callActivities = doc.getElementsByTagNameNS(ns, 'callActivity');
        for (let i = 0; i < callActivities.length; i++) {
          const el = callActivities[i];
          if (!el.getAttribute('name')) {
            errors.push(`Call Activity "${label(el)}" in "${tabName}" has no name`);
          }
          if (!el.getAttribute('calledElement')) {
            errors.push(`Call Activity "${label(el)}" in "${tabName}" has no called element — use the drill-down button to link a process`);
          }
        }

        // --- Script Tasks ---
        const scriptTasks = doc.getElementsByTagNameNS(ns, 'scriptTask');
        for (let i = 0; i < scriptTasks.length; i++) {
          if (!scriptTasks[i].getAttribute('name')) {
            errors.push(`A Script Task in "${tabName}" has no name`);
          }
        }

        // --- Start Events ---
        const startEvents = doc.getElementsByTagNameNS(ns, 'startEvent');
        for (let i = 0; i < startEvents.length; i++) {
          const el = startEvents[i];
          if (!el.getAttribute('name')) {
            errors.push(`Start Event "${el.getAttribute('id')}" in "${tabName}" has no name`);
          }
          // Message start events must have a messageRef
          if (hasEventDef(el, 'messageEventDefinition')) {
            const def = el.getElementsByTagNameNS(ns, 'messageEventDefinition')[0];
            if (!def.getAttribute('messageRef')) {
              errors.push(`Message Start Event "${label(el)}" in "${tabName}" has no message reference`);
            }
          }
          // Signal start events must have a signalRef
          if (hasEventDef(el, 'signalEventDefinition')) {
            const def = el.getElementsByTagNameNS(ns, 'signalEventDefinition')[0];
            if (!def.getAttribute('signalRef')) {
              errors.push(`Signal Start Event "${label(el)}" in "${tabName}" has no signal reference`);
            }
          }
          // Timer start events must have a timer definition
          if (hasEventDef(el, 'timerEventDefinition')) {
            const def = el.getElementsByTagNameNS(ns, 'timerEventDefinition')[0];
            const hasDuration = def.getElementsByTagNameNS(ns, 'timeDuration').length > 0;
            const hasDate = def.getElementsByTagNameNS(ns, 'timeDate').length > 0;
            const hasCycle = def.getElementsByTagNameNS(ns, 'timeCycle').length > 0;
            if (!hasDuration && !hasDate && !hasCycle) {
              errors.push(`Timer Start Event "${label(el)}" in "${tabName}" has no timer definition (duration, date, or cycle)`);
            }
          }
        }

        // --- End Events ---
        const endEvents = doc.getElementsByTagNameNS(ns, 'endEvent');
        for (let i = 0; i < endEvents.length; i++) {
          const el = endEvents[i];
          if (!el.getAttribute('name')) {
            errors.push(`End Event "${el.getAttribute('id')}" in "${tabName}" has no name`);
          }
          if (hasEventDef(el, 'messageEventDefinition')) {
            const def = el.getElementsByTagNameNS(ns, 'messageEventDefinition')[0];
            if (!def.getAttribute('messageRef')) {
              errors.push(`Message End Event "${label(el)}" in "${tabName}" has no message reference`);
            }
          }
          if (hasEventDef(el, 'signalEventDefinition')) {
            const def = el.getElementsByTagNameNS(ns, 'signalEventDefinition')[0];
            if (!def.getAttribute('signalRef')) {
              errors.push(`Signal End Event "${label(el)}" in "${tabName}" has no signal reference`);
            }
          }
        }

        // --- Intermediate Catch Events ---
        const catchEvents = doc.getElementsByTagNameNS(ns, 'intermediateCatchEvent');
        for (let i = 0; i < catchEvents.length; i++) {
          const el = catchEvents[i];
          if (!el.getAttribute('name')) {
            errors.push(`Intermediate Catch Event "${el.getAttribute('id')}" in "${tabName}" has no name`);
          }
          if (hasEventDef(el, 'messageEventDefinition')) {
            const def = el.getElementsByTagNameNS(ns, 'messageEventDefinition')[0];
            if (!def.getAttribute('messageRef')) {
              errors.push(`Message Intermediate Catch Event "${label(el)}" in "${tabName}" has no message reference`);
            }
          }
          if (hasEventDef(el, 'signalEventDefinition')) {
            const def = el.getElementsByTagNameNS(ns, 'signalEventDefinition')[0];
            if (!def.getAttribute('signalRef')) {
              errors.push(`Signal Intermediate Catch Event "${label(el)}" in "${tabName}" has no signal reference`);
            }
          }
          if (hasEventDef(el, 'timerEventDefinition')) {
            const def = el.getElementsByTagNameNS(ns, 'timerEventDefinition')[0];
            const hasDuration = def.getElementsByTagNameNS(ns, 'timeDuration').length > 0;
            const hasDate = def.getElementsByTagNameNS(ns, 'timeDate').length > 0;
            const hasCycle = def.getElementsByTagNameNS(ns, 'timeCycle').length > 0;
            if (!hasDuration && !hasDate && !hasCycle) {
              errors.push(`Timer Intermediate Catch Event "${label(el)}" in "${tabName}" has no timer definition`);
            }
          }
        }

        // --- Intermediate Throw Events ---
        const throwEvents = doc.getElementsByTagNameNS(ns, 'intermediateThrowEvent');
        for (let i = 0; i < throwEvents.length; i++) {
          const el = throwEvents[i];
          if (!el.getAttribute('name')) {
            errors.push(`Intermediate Throw Event "${el.getAttribute('id')}" in "${tabName}" has no name`);
          }
          if (hasEventDef(el, 'messageEventDefinition')) {
            const def = el.getElementsByTagNameNS(ns, 'messageEventDefinition')[0];
            if (!def.getAttribute('messageRef')) {
              errors.push(`Message Intermediate Throw Event "${label(el)}" in "${tabName}" has no message reference`);
            }
          }
          if (hasEventDef(el, 'signalEventDefinition')) {
            const def = el.getElementsByTagNameNS(ns, 'signalEventDefinition')[0];
            if (!def.getAttribute('signalRef')) {
              errors.push(`Signal Intermediate Throw Event "${label(el)}" in "${tabName}" has no signal reference`);
            }
          }
        }

        // --- Sequence Flows from XOR Gateways must have conditions ---
        const exclusiveGateways = doc.getElementsByTagNameNS(ns, 'exclusiveGateway');
        for (let i = 0; i < exclusiveGateways.length; i++) {
          const gw = exclusiveGateways[i];
          const outgoing = gw.getElementsByTagNameNS(ns, 'outgoing');
          // Only validate diverging gateways (more than 1 outgoing flow)
          if (outgoing.length > 1) {
            const defaultFlow = gw.getAttribute('default');
            for (let j = 0; j < outgoing.length; j++) {
              const flowId = outgoing[j].textContent.trim();
              if (flowId === defaultFlow) continue; // default flow doesn't need a condition
              // Find the sequence flow element
              const flows = doc.getElementsByTagNameNS(ns, 'sequenceFlow');
              for (let k = 0; k < flows.length; k++) {
                if (flows[k].getAttribute('id') === flowId) {
                  const conditionExpr = flows[k].getElementsByTagNameNS(ns, 'conditionExpression');
                  if (conditionExpr.length === 0 || !conditionExpr[0].textContent.trim()) {
                    errors.push(`Sequence flow "${flowId}" from XOR gateway "${label(gw)}" in "${tabName}" has no condition`);
                  }
                }
              }
            }
          }
        }
      }

      return errors;
    },

    async generateCode() {
      try {
        // Save current active process first
        if (this.bpmnModeler) {
          const currentXML = await this.bpmnModeler.saveXML({ format: true });
          this.bpmnProcesses.get(this.activeProcess).xml = currentXML.xml;
        }

        // Validate all diagrams before sending to backend
        const errors = this.validateDiagrams();
        if (errors.length > 0) {
          alert('Please fix the following issues:\n\n' + errors.map(e => '• ' + e).join('\n'));
          return;
        }

        this.showButtons = true;

        // Collect all processes — use the sanitized name as key for Call Activity
        // sub-processes so the backend can match them to calledElement references
        const allProcesses = {};
        for (let [processId, processData] of this.bpmnProcesses.entries()) {
          if (processId.startsWith('callActivity_')) {
            const key = processData.name.replace(/[^a-zA-Z0-9_]/g, '_');
            allProcesses[key] = processData.xml;
          } else {
            allProcesses[processId] = processData.xml;
          }
        }

        const response = await fetch("http://localhost:8081/generate-code", {
          method: "POST",
          headers: {
            "Content-Type": "application/json"
          },
          body: JSON.stringify({ processes: allProcesses })
        });

        if (!response.ok) {
          const errorMessage = await response.text();
          alert(errorMessage);
          return;
        }

        const data = await response.json();

        this.collaboration = data.collaboration || '';
        this.processes = data.processes || [];
        this.callActivities = data.callActivities || {};
        this.scriptTaskProcs = data.scriptTaskProcs || {};
        this.eventSubProcesses = data.eventSubProcesses || {};

        // Select first available tab
        this.activeTab = this.availableTabs[0];

      } catch (err) {
        console.error("Failed to generate code:", err);
        alert("Failed to generate code. Please check the console for details.");
      }
    },

    async exportCode() {
      const timestamp = Math.random().toString(36).substr(2, 8);
      const uniqueName = `${this.projectConfig.name}-${timestamp}`;
      this.projectConfig.groupId = uniqueName;
      this.projectConfig.name = uniqueName;
      this.projectConfig.artifactId = uniqueName;

      if (!this.collaboration && !this.processes.length && !Object.keys(this.callActivities).length && !Object.keys(this.scriptTaskProcs).length) {
        alert("No code has been generated to download.");
        return;
      }

      const zip = new JSZip();
      const projectName = this.projectConfig.name;
      const srcMainJavaXklaimPath = `src/main/java/xklaim/`;

      zip.file(`pom.xml`, this.generatePomXml());
      zip.file(`README.md`, this.generateReadme());
      zip.file(`.gitignore`, this.generateGitIgnore());
      zip.file(`.project`, this.generateProject());
      zip.file(`.classpath`, this.generateClassPath());

      // Track which elements need to be imported where
      const elementLocations = new Map(); // element name -> package path
      const processImports = new Map(); // process name -> Set of imports needed
      
      // --- First pass: Determine locations of all elements ---
      
      // Process main activities (script tasks and ESPs that belong to main processes)
      this.processes.forEach(process => {
        processImports.set(process.name, new Set());
        elementLocations.set(process.name, `xklaim.processes.${process.name}`);
      });
      
      // Track call activities
      Object.keys(this.callActivities).forEach(activityName => {
        elementLocations.set(activityName, `xklaim.activities.${activityName}`);
      });
      
      // Track script tasks - first determine their parent
      const scriptTaskParents = new Map();
      Object.keys(this.scriptTaskProcs).forEach(taskName => {
        let parentProcess = null;
        let belongsToCallActivity = false;
        
        // Check if it belongs to a call activity
        Object.keys(this.callActivities).forEach(activityName => {
          const activityCode = this.callActivities[activityName].join('\n');
          if (activityCode.includes(`${taskName}(`)) {
            parentProcess = activityName;
            belongsToCallActivity = true;
          }
        });
        
        // If not in call activity, check main processes
        if (!parentProcess) {
          this.processes.forEach(process => {
            if (process.code.includes(`${taskName}(`)) {
              parentProcess = process.name;
            }
          });
        }
        
        scriptTaskParents.set(taskName, { parent: parentProcess || 'main', isCallActivity: belongsToCallActivity });
        elementLocations.set(taskName, `xklaim.tasks.${taskName}`);
      });
      
      // Track event sub-processes
      const espParents = new Map();
      Object.keys(this.eventSubProcesses).forEach(espId => {
        let parentProcess = null;
        let belongsToCallActivity = false;
        
        // Check if it belongs to a call activity
        Object.keys(this.callActivities).forEach(activityName => {
          const activityCode = this.callActivities[activityName].join('\n');
          if (activityCode.includes(espId)) {
            parentProcess = activityName;
            belongsToCallActivity = true;
          }
        });
        
        // If not in call activity, check main processes
        if (!parentProcess) {
          this.processes.forEach(process => {
            if (process.code.includes(espId)) {
              parentProcess = process.name;
            }
          });
        }
        
        espParents.set(espId, { parent: parentProcess || 'main', isCallActivity: belongsToCallActivity });
        elementLocations.set(espId, `xklaim.tasks.${espId}`);
      });

      // --- Second pass: Analyze dependencies and build import lists ---
      
      // For each process, find what it references
      this.processes.forEach(process => {
        const imports = processImports.get(process.name);
        
        // Check for call activities
        Object.keys(this.callActivities).forEach(activityName => {
          if (process.code.includes(`${activityName}(`)) {
            imports.add(`import ${elementLocations.get(activityName)}`);
          }
        });
        
        // Check for script tasks
        Object.keys(this.scriptTaskProcs).forEach(taskName => {
          if (process.code.includes(`${taskName}(`)) {
            imports.add(`import ${elementLocations.get(taskName)}`);
          }
        });
        
        // Check for event sub-processes
        Object.keys(this.eventSubProcesses).forEach(espId => {
          if (process.code.includes(espId)) {
            imports.add(`import ${elementLocations.get(espId)}`);
          }
        });
      });
      
      // For each call activity, find what it references
      const callActivityImports = new Map();
      Object.keys(this.callActivities).forEach(activityName => {
        const imports = new Set();
        const activityCode = this.callActivities[activityName].join('\n');
        
        // Check for script tasks
        Object.keys(this.scriptTaskProcs).forEach(taskName => {
          if (activityCode.includes(`${taskName}(`)) {
            imports.add(`import ${elementLocations.get(taskName)}`);
          }
        });
        
        // Check for event sub-processes
        Object.keys(this.eventSubProcesses).forEach(espId => {
          if (activityCode.includes(espId)) {
            imports.add(`import ${elementLocations.get(espId)}`);
          }
        });
        
        // Check for other call activities
        Object.keys(this.callActivities).forEach(otherActivity => {
          if (otherActivity !== activityName && activityCode.includes(`${otherActivity}(`)) {
            imports.add(`import ${elementLocations.get(otherActivity)}`);
          }
        });
        
        callActivityImports.set(activityName, imports);
      });

      // --- Collaboration File ---
      if (this.collaboration) {
        const collabImports = new Set();
        
        // Import all processes
        this.processes.forEach(process => {
          collabImports.add(`import ${elementLocations.get(process.name)}`);
        });
        
        const collaborationWithPackage = `package xklaim\n\n${Array.from(collabImports).join('\n')}\n\n${this.collaboration}`;
        zip.file(`${srcMainJavaXklaimPath}Collaboration.xklaim`, collaborationWithPackage);
      }

      // --- Process Files ---
      this.processes.forEach(process => {
        const processName = process.name;
        const packageDeclaration = `package xklaim.processes\n\n`;
        
        const baseImports = ['import klava.Locality'];
        const specificImports = Array.from(processImports.get(processName) || []);
        
        const allImports = [...baseImports, ...specificImports].join('\n');
        const processWithPackage = packageDeclaration + allImports + "\n\n" + process.code;
        const processFilePath = `${srcMainJavaXklaimPath}processes/${processName}.xklaim`;
        zip.file(processFilePath, processWithPackage);
      });

      // --- Call Activity Files ---
      Object.keys(this.callActivities).forEach(activityName => {
        const activityCode = this.callActivities[activityName].join('\n');
        const packageDeclaration = `package xklaim.activities\n\n`;
        
        const baseImports = ['import klava.Locality'];
        const specificImports = Array.from(callActivityImports.get(activityName) || []);
        
        const allImports = [...baseImports, ...specificImports].join('\n');
        const activityWithPackage = packageDeclaration + allImports + "\n\n" + activityCode;
        const activityFilePath = `${srcMainJavaXklaimPath}activities/${activityName}.xklaim`;
        zip.file(activityFilePath, activityWithPackage);
      });

      // --- Script Task Files ---
      Object.keys(this.scriptTaskProcs).forEach(taskName => {
        const taskData = this.scriptTaskProcs[taskName];
        const taskCode = Array.isArray(taskData) ? taskData.join('\n') : 
                         (typeof taskData === 'object' ? (taskData.code || '') : taskData);
        
        const packageDeclaration = `package xklaim.tasks\n\n`;
        const imports = `import klava.Locality\n`;
        
        const taskWithPackage = packageDeclaration + imports + "\n" + taskCode;
        const taskFilePath = `${srcMainJavaXklaimPath}tasks/${taskName}.xklaim`;
        zip.file(taskFilePath, taskWithPackage);
      });

      // --- Event Sub-Process Files ---
      Object.keys(this.eventSubProcesses).forEach(espId => {
        const espData = this.eventSubProcesses[espId];
        const espCode = Array.isArray(espData) ? espData.join('\n') : 
                        (typeof espData === 'object' ? (espData.code || '') : espData);
        
        const packageDeclaration = `package xklaim.tasks\n\n`;
        const imports = `import klava.Locality\n`;
        
        const espWithPackage = packageDeclaration + imports + "\n" + espCode;
        const espFilePath = `${srcMainJavaXklaimPath}tasks/${espId}.xklaim`;
        zip.file(espFilePath, espWithPackage);
      });

      // --- Generate zip ---
      try {
        const content = await zip.generateAsync({type: "blob"});
        const link = document.createElement("a");
        link.href = URL.createObjectURL(content);
        link.download = `${projectName}.zip`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(link.href);
      } catch (err) {
        console.error("Error generating zip file:", err);
        alert("Error generating zip file. Please try again.");
      }
    },

    generatePomXml() {
      return `<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" 
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <groupId>${this.projectConfig.groupId}</groupId>
    <artifactId>${this.projectConfig.artifactId}</artifactId>
    <version>${this.projectConfig.version}</version>
    
    <properties>
      <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
      <maven.compiler.source>21</maven.compiler.source>
      <maven.compiler.target>21</maven.compiler.target>
    </properties>

    <repositories>
        <repository>
            <id>jitpack.io</id>
            <url>https://jitpack.io</url>
        </repository>
    </repositories>

    <dependencies>
      <dependency>
        <groupId>io.github.lorenzobettini.klaim</groupId>
        <artifactId>xklaim.runtime</artifactId>
        <version>2.5.0</version>
      </dependency>
      <dependency>
          <groupId>org.codeberg.kbourr</groupId>
          <artifactId>common-ros-msgs</artifactId>
          <version>v1.0.0</version> 
      </dependency>
      <dependency>
        <groupId>us.ihmc</groupId>
        <artifactId>jros2</artifactId>
        <version>1.0.1</version>
      </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
              <groupId>org.codehaus.mojo</groupId>
              <artifactId>build-helper-maven-plugin</artifactId>
              <version>3.0.0</version>
              <executions>
                <execution>
                  <id>add-src-gen-source</id>
                  <goals>
                    <goal>add-source</goal>
                  </goals>
                  <configuration>
                    <sources>
                      <!-- Where Xklaim generates Java files -->
                      <source>src-gen/</source>
                    </sources>
                  </configuration>
                </execution>
              </executions>
            </plugin>
            
            <!-- Assembly plugin to create executable jar with dependencies -->
            <plugin>
                <artifactId>maven-assembly-plugin</artifactId>
                <version>3.3.0</version>
                <configuration>
                    <archive>
                        <manifest>
                            <mainClass>${this.projectConfig.groupId}.Main</mainClass>
                        </manifest>
                    </archive>
                    <descriptorRefs>
                        <descriptorRef>jar-with-dependencies</descriptorRef>
                    </descriptorRefs>
                </configuration>
                <executions>
                    <execution>
                        <id>make-assembly</id>
                        <phase>package</phase>
                        <goals>
                            <goal>single</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>`;
    },


    generateProject() {
      return `<?xml version="1.0" encoding="UTF-8"?>
<projectDescription>
	<name>${this.projectConfig.groupId}</name>
	<comment></comment>
	<projects>
	</projects>
	<buildSpec>
		<buildCommand>
			<name>org.eclipse.xtext.ui.shared.xtextBuilder</name>
			<arguments>
			</arguments>
		</buildCommand>
		<buildCommand>
			<name>org.eclipse.jdt.core.javabuilder</name>
			<arguments>
			</arguments>
		</buildCommand>
		<buildCommand>
			<name>org.eclipse.m2e.core.maven2Builder</name>
			<arguments>
			</arguments>
		</buildCommand>
	</buildSpec>
	<natures>
		<nature>org.eclipse.jdt.core.javanature</nature>
		<nature>org.eclipse.m2e.core.maven2Nature</nature>
		<nature>org.eclipse.xtext.ui.shared.xtextNature</nature>
	</natures>
	<filteredResources>
		<filter>
			<id>0</id>
			<name></name>
			<type>30</type>
			<matcher>
				<id>org.eclipse.core.resources.regexFilterMatcher</id>
				<arguments>node_modules|.git|__CREATED_BY_JAVA_LANGUAGE_SERVER__</arguments>
			</matcher>
		</filter>
	</filteredResources>
</projectDescription>`;
    },


    generateClassPath() {
      return `<?xml version="1.0" encoding="UTF-8"?>
<classpath>
	<classpathentry kind="src" output="target/classes" path="src/main/java">
		<attributes>
			<attribute name="optional" value="true"/>
			<attribute name="maven.pomderived" value="true"/>
		</attributes>
	</classpathentry>
	<classpathentry kind="src" output="target/classes" path="src-gen">
		<attributes>
			<attribute name="optional" value="true"/>
			<attribute name="maven.pomderived" value="true"/>
		</attributes>
	</classpathentry>
	<classpathentry kind="con" path="org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-21">
		<attributes>
			<attribute name="maven.pomderived" value="true"/>
		</attributes>
	</classpathentry>
	<classpathentry kind="con" path="org.eclipse.m2e.MAVEN2_CLASSPATH_CONTAINER">
		<attributes>
			<attribute name="maven.pomderived" value="true"/>
		</attributes>
	</classpathentry>
	<classpathentry excluding="**" kind="src" output="target/classes" path="src/main/resources">
		<attributes>
			<attribute name="maven.pomderived" value="true"/>
			<attribute name="optional" value="true"/>
		</attributes>
	</classpathentry>
	<classpathentry kind="output" path="target/classes"/>
</classpath>`;
    },


    generateReadme() {
      return `# ${this.projectConfig.name}

This XKlaim project was automatically generated using the B2XKlaim tool.

## Project Structure

- \`src/main/java/xklaim/\`: Contains the XKlaim source files
  - \`Collaboration.xklaim\`: Main collaboration coordination code (if collaboration exists)
  - \`processes/\`: Contains all participant process implementations
  - \`activities/\`: Contains all call activity implementations
  - \`tasks/\`: Contains all script tasks and event sub-processes
- \`src-gen/\`: Generated Java code (populated when building)

## Package Organization

The project follows a clean package structure:
- \`xklaim\`: Root package containing the collaboration
- \`xklaim.processes\`: All main process implementations
- \`xklaim.activities\`: All call activities
- \`xklaim.tasks\`: All script tasks and event sub-processes

This structure ensures proper separation of concerns and makes imports straightforward.

## Building the Project

To build the project:

\`\`\`
mvn clean package
\`\`\`

## Running the Application

\`\`\`
java -jar target/${this.projectConfig.artifactId}-${this.projectConfig.version}-jar-with-dependencies.jar
\`\`\`

## Generated from BPMN

This code was generated from a BPMN model using B2XKlaim.
`;
    },

    generateGitIgnore() {
      return `# Maven
target/
pom.xml.tag
pom.xml.releaseBackup
pom.xml.versionsBackup
pom.xml.next
release.properties
dependency-reduced-pom.xml
buildNumber.properties
.mvn/timing.properties

# Eclipse
.classpath
.project
.settings/
bin/

# IntelliJ
.idea/
*.iml
*.iws
*.ipr

# NetBeans
nbproject/
nb-configuration.xml

# Generated code
src-gen/

# Logs
*.log

# OS specific
.DS_Store
Thumbs.db
`;
    },

    copyToClipboard(refName) {
      const element = this.$refs[refName];
      if (element) {
        element.select();
        document.execCommand('copy');
        alert('Code copied to clipboard!');
      }
    }
  }
};
</script>

<style>
/* Color Variables */
:root {
  --primary-color: #388285;
  --secondary-color: #387c85;
  --accent-color: #75a2a8;
  --background-color: #f8f9fa;
  --panel-color: #ffffff;
  --border-color: #e0e0e0;
  --text-color: #333333;
  --code-bg-color: #f5f7f9;
  --header-bg-color: #CEE1DF;
}

/* Global Styles */
body {
  font-family: 'Roboto', Arial, sans-serif;
  margin: 0;
  padding: 0;
  color: var(--text-color);
  background-color: var(--background-color);
  font-size: 14px;
}

.app-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

#main-content {
  padding: 10px;
  flex: 1;
}

h1, h2, h3, h4 {
  margin: 0;
  color: var(--secondary-color);
}

h1 {
  font-size: 20px;
}

h2 {
  font-size: 16px;
}

h3 {
  font-size: 14px;
}

h4 {
  font-size: 13px;
}

.section-title {
  margin-bottom: 8px;
  border-bottom: 1px solid var(--accent-color);
  padding-bottom: 3px;
}

/* Navigation Bar */
.topnav {
  background-color: var(--header-bg-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 10px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 0;
  z-index: 100;
  height: 40px;
}

.logo-container {
  display: flex;
  align-items: center;
}

.logo {
  height: 32px;
  margin-right: 10px;
}

.title-container {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  position: sticky;
}

.app-main-title {
  font-size: 1.0rem;
  font-weight: 1000;
  color: #388285;
  margin: 0;
  line-height: 0.5;
  letter-spacing: -1px;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.2);
}

.app-subtitle {
  font-size: 0.8rem;
  font-weight: 300;
  color: #265557;
  margin: 5px 0 0;
  text-transform: lowercase;
  font-style: italic;
}

/* Navigation buttons container */
.nav-buttons {
  display: flex;
  gap: 8px;
  align-items: center;
}

/* Styling for nav buttons */
.nav-btn {
  background-color: var(--accent-color);
  color: white;
  padding: 6px 12px;
  border-radius: 10px;
  text-decoration: none;
  font-weight: bold;
  transition: background-color 0.3s, transform 0.2s;
  border: none;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}

.nav-btn:hover {
  background-color: var(--secondary-color);
  transform: translateY(-1px);
}

.nav-btn-new {
  background-color: #e07050;
}

.nav-btn-new:hover {
  background-color: #c0503a;
}

/* Download button */
.download-btn {
  background-color: #4a9e6e;
  color: white;
  padding: 6px 12px;
  border-radius: 10px;
  text-decoration: none;
  font-weight: bold;
  transition: background-color 0.3s, transform 0.2s;
  border: none;
  display: flex;
  align-items: baseline;
  gap: 6px;
  font-size: 13px;
}

.download-btn:hover {
  background-color: #3b8559;
  transform: translateY(-1px);
}

/* BPMN Editor Section */
#canvas-container {
  background-color: var(--panel-color);
  border-radius: 0 6px 6px 6px;
  box-shadow: 0 1px 5px rgba(0, 0, 0, 0.05);
  margin-bottom: 15px;
  overflow: hidden;
}

.panel-header {
  background-color: var(--header-bg-color);
  padding: 8px 12px;
  border-bottom: 1px solid var(--border-color);
}

.editor-panels {
  display: flex;
  height: 600px;
}

.canvas-panel {
  flex: 5;
  border-right: 1px solid var(--border-color);
}

.properties-panel {
  flex: 1;
  padding: 8px;
  overflow-y: auto;
  max-width: 240px;
}



.center-button-container {
  padding: 8px;
  text-align: center;
  border-top: 1px solid var(--border-color);
}

.primary-button {
  background-color: var(--primary-color);
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 3px;
  font-size: 14px;
  cursor: pointer;
  transition: background-color 0.3s, transform 0.2s;
}

.primary-button:hover {
  background-color: var(--secondary-color);
  transform: translateY(-1px);
}

/* Generated Code Section */
#code-section {
  background-color: var(--panel-color);
  border-radius: 6px;
  box-shadow: 0 1px 5px rgba(0, 0, 0, 0.05);
  padding: 12px;
}

.code-tabs {
  display: flex;
  margin-bottom: 10px;
  border-bottom: 1px solid var(--border-color);
}

.code-tabs button {
  background-color: transparent;
  border: none;
  padding: 6px 12px;
  cursor: pointer;
  font-size: 13px;
  color: var(--text-color);
  margin-right: 3px;
  border-bottom: 2px solid transparent;
  transition: all 0.3s;
}

.code-tabs button:hover {
  color: var(--primary-color);
}

.tab-button--active {
  color: var(--primary-color) !important;
  border-bottom-color: var(--primary-color) !important;
  font-weight: bold;
}

/* Project Config */
.project-config {
  background-color: var(--code-bg-color);
  padding: 12px;
  border-radius: 4px;
  margin-bottom: 15px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 10px;
}

.form-group {
  margin-bottom: 8px;
}

.form-group label {
  display: block;
  margin-bottom: 3px;
  font-weight: 500;
  font-size: 12px;
}

.form-group input[type="text"] {
  width: 100%;
  padding: 6px 8px;
  border: 1px solid var(--border-color);
  border-radius: 3px;
  font-size: 12px;
  transition: border-color 0.3s;
  height: 28px;
}

.form-group input[type="text"]:focus {
  border-color: var(--primary-color);
  outline: none;
}

.form-group input[type="checkbox"] {
  margin-right: 6px;
}

/* Code Containers */
.code-container {
  background-color: var(--code-bg-color);
  border-radius: 4px;
  margin-bottom: 15px;
  overflow: hidden;
}

.code-header {
  background-color: var(--header-bg-color);
  padding: 6px 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid var(--border-color);
  height: 30px;
}

.code-actions {
  display: flex;
  gap: 6px;
}

.copy-icon {
  cursor: pointer;
  padding: 3px;
  border-radius: 3px;
  transition: background-color 0.3s;
  font-size: 12px;
}

.copy-icon:hover {
  background-color: rgba(0, 0, 0, 0.1);
}

.textarea-wrapper {
  position: relative;
}

.code-editor {
  width: 100%;
  min-height: 150px;
  padding: 10px;
  border: none;
  font-family: 'Fira Code', monospace;
  font-size: 12px;
  line-height: 1.4;
  resize: vertical;
  background-color: var(--code-bg-color);
  color: var(--text-color);
}

.code-editor:focus {
  outline: none;
}

.collaboration {
  border-left: 2px solid var(--primary-color);
}

.process {
  border-left: 2px solid var(--accent-color);
}

.code-processes {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* Footer */
.footer {
  background-color: var(--header-bg-color);
  text-align: center;
  padding: 8px;
  margin-top: 15px;
  border-top: 1px solid var(--border-color);
  font-size: 12px;
}


.close-tab {
  margin-left: 8px;
  color: #999;
  font-weight: bold;
  cursor: pointer;
  border-radius: 50%;
  width: 16px;
  height: 16px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  transition: all 0.3s;
}

.close-tab:hover {
  background-color: rgba(255, 0, 0, 0.1);
  color: red;
}


/* Process Tabs Styling */
.process-tabs {
  display: flex;
  align-items: center;
  background-color: var(--background-color);
  border-bottom: 1px solid var(--border-color);
  padding: 0;
  margin-bottom: 0;
  gap: 2px;
}

.tab {
  background-color: var(--panel-color);
  border: 1px solid var(--border-color);
  border-bottom: none;
  padding: 8px 16px;
  cursor: pointer;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: var(--text-color);
  border-radius: 4px 4px 0 0;
  transition: background-color 0.3s;
}

.tab:hover {
  background-color: var(--code-bg-color);
}

.tab.active {
  background-color: var(--header-bg-color);
  color: var(--primary-color);
  font-weight: 600;
}

.add-process-btn {
  background-color: transparent;
  border: 1px dashed var(--border-color);
  color: var(--accent-color);
  padding: 6px 12px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  margin-left: 8px;
  transition: all 0.3s;
}

.add-process-btn:hover {
  background-color: var(--accent-color);
  color: white;
  border-color: var(--accent-color);
}

/* Responsive Design */
@media (max-width: 768px) {
  .editor-panels {
    flex-direction: column;
    height: auto;
  }

  .canvas-panel {
    height: 450px; /* Maintain larger height on mobile */
  }

  .properties-panel {
    height: 300px;
    max-width: none;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}

/* Override for BPMN.js styles - keep these at original size or larger */
:deep(.djs-palette) {
  height: 500px;
  overflow-y: auto;
}

/* Make diagram elements more visible */
:deep(.djs-element) {
  font-size: 12px !important;
}

:deep(.djs-overlay) {
  font-size: 12px !important;
}

:deep(.djs-container) {
  font-size: 12px !important;
}

</style>