<template>
  <div id="container" class="app-container">
    <!-- Top navigation bar -->
    <div id="nav-bar" class="topnav">
      <div class="logo-container">
        <a href="index.html">
          <img src="./assets/b2xklaim.jpg" alt="B2Xklaim" class="logo">
        </a>
        <div class="title-container">
          <h1 class="app-main-title">B2XKlaim</h1>
          <span class="app-subtitle">BPMN to X-Klaim Translator</span>
        </div>
      </div>
      <div class="nav-buttons">
        <a href="#" @click="newProject" class="nav-btn nav-btn-ghost" title="New project">
          <i class="fas fa-plus"></i> New
        </a>
        <span class="nav-divider"></span>
        <a href="#" @click="importBPMN" class="nav-btn nav-btn-ghost" title="Import BPMN or project file">
          <i class="fas fa-folder-open"></i> Import
        </a>
        <a href="#" @click="saveBPMN" class="nav-btn nav-btn-ghost" title="Save project file">
          <i class="fas fa-save"></i> Save
        </a>
        <span class="nav-divider"></span>
        <a href="#" @click="generateCode" class="nav-btn nav-btn-accent" title="Generate X-Klaim code">
          <i class="fas fa-code"></i> Generate
        </a>
        <a href="#" @click="exportCode" class="nav-btn nav-btn-primary" title="Download project as ZIP">
          <i class="fas fa-download"></i> Download
        </a>
      </div>
      <input type="file" id="bpmn-file-input" accept=".bpmn,.xml,.b2x" style="display: none;" @change="handleFileSelect">
    </div>

    <div id="main-content">
      <!-- Process Tabs -->
      <div class="process-tabs">
        <div v-for="[processId, processData] in processesArray"
             :key="processId"
             :class="{ 'process-tab': true, 'active': activeProcess === processId }"
             @click="switchToProcess(processId)">
          <i class="fas fa-project-diagram" style="font-size: 10px; opacity: 0.6;"></i>
          {{ processData.name }}
          <span v-if="processId !== 'main'"
                class="close-tab"
                @click.stop="removeProcess(processId)"
                title="Close tab">&times;</span>
        </div>
        <button class="add-process-btn" @click="addNewProcess" title="Add new process tab">
          <i class="fas fa-plus"></i>
        </button>
      </div>

      <!-- BPMN Editor -->
      <div id="canvas-container">
        <div class="editor-panels">
          <div id="canvas" class="canvas-panel"></div>
          <div id="properties" class="properties-panel"></div>
        </div>
      </div>

      <!-- Generated Code Section -->
      <div v-if="showButtons" id="code-section">
        <div class="code-section-header">
          <h3><i class="fas fa-code"></i> Generated X-Klaim Code</h3>
          <div class="code-tabs">
            <button v-for="tab in availableTabs" :key="tab" @click="activeTab = tab"
                    :class="{ 'code-tab': true, 'code-tab--active': activeTab === tab }">
              {{ tab }}
            </button>
          </div>
        </div>

        <!-- Project Configuration -->
        <div class="project-config">
          <div class="config-header" @click="showConfig = !showConfig">
            <span><i class="fas fa-cog"></i> Project Configuration</span>
            <i :class="showConfig ? 'fas fa-chevron-up' : 'fas fa-chevron-down'"></i>
          </div>
          <div v-if="showConfig" class="config-body">
            <div class="form-grid">
              <div class="form-group">
                <label for="projectName">Project Name</label>
                <input type="text" id="projectName" v-model="projectConfig.name" placeholder="my-xklaim-project">
              </div>
              <div class="form-group">
                <label for="groupId">Group ID</label>
                <input type="text" id="groupId" v-model="projectConfig.groupId" placeholder="com.example">
              </div>
              <div class="form-group">
                <label for="artifactId">Artifact ID</label>
                <input type="text" id="artifactId" v-model="projectConfig.artifactId" placeholder="xklaim-bpmn-project">
              </div>
              <div class="form-group">
                <label for="version">Version</label>
                <input type="text" id="version" v-model="projectConfig.version" placeholder="1.0-SNAPSHOT">
              </div>
            </div>
          </div>
        </div>

        <!-- Collaboration Code -->
        <div v-if="collaboration && activeTab === 'collaboration'" class="code-card">
          <div class="code-card-header">
            <span class="code-card-title"><i class="fas fa-network-wired"></i> Main Collaboration</span>
            <button @click="copyToClipboard('collaboration')" class="copy-btn" title="Copy to clipboard">
              <i class="fas fa-copy"></i> Copy
            </button>
          </div>
          <textarea ref="collaboration" class="code-editor" placeholder="Collaboration Code..." v-model="collaboration"></textarea>
        </div>

        <!-- Process Code -->
        <div v-if="activeTab === 'processes'" class="code-list">
          <div v-for="process in processes" :key="process.name" class="code-card">
            <div class="code-card-header">
              <span class="code-card-title"><i class="fas fa-cogs"></i> {{ process.name }}</span>
              <button @click="copyToClipboard(process.name)" class="copy-btn" title="Copy to clipboard">
                <i class="fas fa-copy"></i> Copy
              </button>
            </div>
            <textarea :ref="process.name" class="code-editor"
                      :placeholder="process.name + ' Code...'" v-model="process.code"></textarea>
          </div>
        </div>

        <!-- Event Sub-Processes Code -->
        <div v-if="activeTab === 'event-subprocesses' && hasEventSubprocesses" class="code-list">
          <div v-for="(codeList, espId) in eventSubProcesses" :key="espId" class="code-card">
            <div class="code-card-header">
              <span class="code-card-title"><i class="fas fa-bolt"></i> {{ espId }}</span>
              <button @click="copyToClipboard(espId)" class="copy-btn" title="Copy to clipboard">
                <i class="fas fa-copy"></i> Copy
              </button>
            </div>
            <textarea :ref="espId" class="code-editor"
                      :placeholder="'Event Sub-Process Code...'" v-model="eventSubProcesses[espId][0]"></textarea>
          </div>
        </div>

        <!-- AND Branch Procs Code -->
        <div v-if="activeTab === 'and-branches' && hasAndBranchProcs" class="code-list">
          <div v-for="(codeList, branchName) in andBranchProcs" :key="branchName" class="code-card">
            <div class="code-card-header">
              <span class="code-card-title"><i class="fas fa-code-branch"></i> {{ branchName }}</span>
              <button @click="copyToClipboard(branchName)" class="copy-btn" title="Copy to clipboard">
                <i class="fas fa-copy"></i> Copy
              </button>
            </div>
            <textarea :ref="branchName" class="code-editor"
                      :placeholder="'AND Branch Code...'" v-model="andBranchProcs[branchName][0]"></textarea>
          </div>
        </div>
      </div>
    </div>
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

// Store modeler outside Vue's reactivity system to prevent infinite recursion
// when bpmn-js internally traverses model elements (e.g. auto-resize)
let _bpmnModeler = null;

export default {
  name: "App",
  data() {
    return {
      activeTab: 'collaboration',
      showButtons: false,
      collaboration: '',
      processes: [],
      callActivities: {},
      scriptTaskProcs: {},
      eventSubProcesses: {},
      andBranchProcs: {},
      allTabs: ['collaboration', 'processes', 'event-subprocesses', 'and-branches'],
      showConfig: false,
      projectConfig: {
        name: 'xklaim-bpmn-project',
        groupId: 'com.example',
        artifactId: 'xklaim-bpmn-project',
        version: '1.0-SNAPSHOT'
      },
      bpmnProcesses: new Map([
        ['main', { xml: '', name: 'Main Process' }]
      ]),
      activeProcess: 'main',
      isSwitchingProcess: false
    };
  },

  computed: {
    hasEventSubprocesses() {
      return Object.keys(this.eventSubProcesses).length > 0;
    },
    hasAndBranchProcs() {
      return Object.keys(this.andBranchProcs).length > 0;
    },
    availableTabs() {
      return this.allTabs.filter(tab => {
        if (tab === 'event-subprocesses') {
          return this.hasEventSubprocesses;
        }
        if (tab === 'and-branches') {
          return this.hasAndBranchProcs;
        }
        return true;
      });
    },
    processesArray() {
      return Array.from(this.bpmnProcesses.entries());
    }
  },

  async mounted() {
    _bpmnModeler = new BpmnModeler({
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

    // Forward document keyboard events to the bpmn-js keyboard handler
    // so Ctrl+Z/Y and other shortcuts work without needing to click the canvas first
    const keyboard = _bpmnModeler.get('keyboard');
    document.addEventListener('keydown', (event) => {
      // Skip if user is typing in an input, textarea, or contenteditable
      const tag = event.target.tagName;
      if (tag === 'INPUT' || tag === 'TEXTAREA' || event.target.isContentEditable) {
        return;
      }
      keyboard._keyHandler(event, 'keyboard.keydown');
    });
    document.addEventListener('keyup', (event) => {
      const tag = event.target.tagName;
      if (tag === 'INPUT' || tag === 'TEXTAREA' || event.target.isContentEditable) {
        return;
      }
      keyboard._keyHandler(event, 'keyboard.keyup');
    });

    const eventBus = _bpmnModeler.get('eventBus');

    // Listen for drill-down into Call Activities
    eventBus.on('callActivity.drilldown', (event) => {
      this.openCallActivityProcess(event.element);
    });

    // Auto-sync participant name to its process and auto-save to localStorage
    let isSaving = false;
    eventBus.on('commandStack.changed', () => {
      // Skip auto-save during tab switches to prevent overwriting process XML
      if (this.isSwitchingProcess) return;
      // Prevent re-entrancy when removeElements triggers another commandStack.changed
      if (isSaving) return;
      isSaving = true;

      const elementRegistry = _bpmnModeler.get('elementRegistry');

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

      // Auto-set calledElement to the call activity's name so users can
      // use call activities without drilling down to design a sub-process
      elementRegistry.filter(el => el.type === 'bpmn:CallActivity').forEach(callActivity => {
        const bo = callActivity.businessObject;
        if (bo.name && bo.name.trim()) {
          const sanitizedName = bo.name.replace(/[^a-zA-Z0-9_]/g, '_');
          if (!bo.calledElement || bo.calledElement !== sanitizedName) {
            bo.calledElement = sanitizedName;
          }
        }
      });

      // Auto-save after every change
      this.saveToLocalStorage().finally(() => { isSaving = false; });
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
        if (_bpmnModeler) {
          // Remove incomplete edges (no target) before saving
          const elementRegistry = _bpmnModeler.get('elementRegistry');
          const modeling = _bpmnModeler.get('modeling');
          const incompleteFlows = elementRegistry.filter(
            el => el.type === 'bpmn:SequenceFlow' && (!el.target || !el.source)
          );
          if (incompleteFlows.length) {
            modeling.removeElements(incompleteFlows);
          }

          const currentProcess = this.bpmnProcesses.get(this.activeProcess);
          if (currentProcess) {
            const currentXML = await _bpmnModeler.saveXML({ format: true });
            currentProcess.xml = currentXML.xml;
          }
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
            await _bpmnModeler.importXML(processData.xml);
            this.activeProcess = activeKey;
            _bpmnModeler.get('canvas').zoom('fit-viewport');
            this.$forceUpdate();
            return;
          }
        }
      } catch (err) {
        console.error('Failed to restore from localStorage:', err);
      }

      // Fallback: load default empty diagram
      await _bpmnModeler.importXML(this.getDefaultDiagram());
      _bpmnModeler.get('canvas').zoom('fit-viewport');
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
      this.andBranchProcs = {};

      await _bpmnModeler.importXML(this.getDefaultDiagram());
      _bpmnModeler.get('canvas').zoom('fit-viewport');
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
        const modeling = _bpmnModeler.get('modeling');
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
        const modeling = _bpmnModeler.get('modeling');
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
      const modeling = _bpmnModeler.get('modeling');
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
          const { warnings } = await _bpmnModeler.importXML(content);
          if (warnings.length) {
            console.warn('Warnings while importing BPMN:', warnings);
          }
          _bpmnModeler.get('canvas').zoom('fit-viewport');
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
        await _bpmnModeler.importXML(processData.xml);
        this.activeProcess = activeKey;
        _bpmnModeler.get('canvas').zoom('fit-viewport');
      }
      this.$forceUpdate();
    },

    async saveBPMN() {
      try {
        // Save the currently active process XML first
        if (_bpmnModeler) {
          const currentProcess = this.bpmnProcesses.get(this.activeProcess);
          if (currentProcess) {
            const currentXML = await _bpmnModeler.saveXML({ format: true });
            currentProcess.xml = currentXML.xml;
          }
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
        const wasActive = this.activeProcess === processId;

        if (wasActive) {
          // Switch to main first, then delete
          this.activeProcess = 'main';
          this.isSwitchingProcess = true;
          const mainData = this.bpmnProcesses.get('main');
          if (mainData && mainData.xml) {
            await _bpmnModeler.importXML(mainData.xml);
            _bpmnModeler.get('canvas').zoom('fit-viewport');
          }
          this.isSwitchingProcess = false;
        }

        this.bpmnProcesses.delete(processId);
        this.saveToLocalStorage();
        this.$forceUpdate();
      }
    },

    async switchToProcess(processName) {
      try {
        if (_bpmnModeler) {
          const currentProcess = this.bpmnProcesses.get(this.activeProcess);
          if (currentProcess) {
            const currentXML = await _bpmnModeler.saveXML({ format: true });
            currentProcess.xml = currentXML.xml;
          }
        }

        // Load new process
        const processData = this.bpmnProcesses.get(processName);
        if (processData) {
          // Update activeProcess BEFORE importXML to prevent the commandStack.changed
          // handler from saving the new XML back to the old process entry
          this.activeProcess = processName;
          this.isSwitchingProcess = true;

          const result = await _bpmnModeler.importXML(processData.xml);

          this.isSwitchingProcess = false;

          if (result.warnings && result.warnings.length > 0) {
            console.warn('Warnings while switching process:', result.warnings);
          }

          _bpmnModeler.get('canvas').zoom('fit-viewport');
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
    <bpmn:startEvent id="${startEventId}" name="start" />
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
            errors.push(`Call Activity "${label(el)}" in "${tabName}" has no called element — give it a name or use the drill-down button to design its process`);
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
        if (_bpmnModeler) {
          const currentProcess = this.bpmnProcesses.get(this.activeProcess);
          if (currentProcess) {
            const currentXML = await _bpmnModeler.saveXML({ format: true });
            currentProcess.xml = currentXML.xml;
          }
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
        this.andBranchProcs = data.andBranchProcs || {};

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

      if (!this.collaboration && !this.processes.length && !Object.keys(this.callActivities).length && !Object.keys(this.scriptTaskProcs).length && !Object.keys(this.andBranchProcs).length) {
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

      // Track AND branch procs
      Object.keys(this.andBranchProcs).forEach(branchName => {
        elementLocations.set(branchName, `xklaim.branches.${branchName}`);
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
        
        // Check for AND branch procs
        Object.keys(this.andBranchProcs).forEach(branchName => {
          if (process.code.includes(`${branchName}(`)) {
            imports.add(`import ${elementLocations.get(branchName)}`);
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

        // Import AND branch procs referenced in collaboration
        Object.keys(this.andBranchProcs).forEach(branchName => {
          if (this.collaboration.includes(`${branchName}(`)) {
            collabImports.add(`import ${elementLocations.get(branchName)}`);
          }
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

      // --- AND Branch Proc Files ---
      Object.keys(this.andBranchProcs).forEach(branchName => {
        const branchData = this.andBranchProcs[branchName];
        const branchCode = Array.isArray(branchData) ? branchData.join('\n') :
                           (typeof branchData === 'object' ? (branchData.code || '') : branchData);

        const packageDeclaration = `package xklaim.branches\n\n`;
        const imports = `import klava.Locality\n`;

        const branchWithPackage = packageDeclaration + imports + "\n" + branchCode;
        const branchFilePath = `${srcMainJavaXklaimPath}branches/${branchName}.xklaim`;
        zip.file(branchFilePath, branchWithPackage);
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
:root {
  --primary: #2c7a7b;
  --primary-light: #38a89d;
  --primary-dark: #234e52;
  --accent: #e07050;
  --bg: #f0f2f5;
  --surface: #ffffff;
  --surface-alt: #f7f8fa;
  --border: #e2e8f0;
  --text: #2d3748;
  --text-muted: #718096;
  --shadow-sm: 0 1px 3px rgba(0,0,0,0.08);
  --shadow-md: 0 2px 8px rgba(0,0,0,0.1);
  --radius: 8px;
  --radius-sm: 5px;
}

* { box-sizing: border-box; }

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  margin: 0;
  padding: 0;
  color: var(--text);
  background-color: var(--bg);
  font-size: 13px;
  -webkit-font-smoothing: antialiased;
}

.app-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

/* ── Navigation Bar ── */
.topnav {
  background-color: var(--surface);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 16px;
  height: 48px;
  border-bottom: 1px solid var(--border);
  position: sticky;
  top: 0;
  z-index: 100;
}

.logo-container {
  display: flex;
  align-items: center;
  gap: 10px;
}

.logo-container a { display: flex; }

.logo {
  height: 28px;
}

.title-container {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.app-main-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--primary);
  letter-spacing: -0.3px;
}

.app-subtitle {
  font-size: 11px;
  font-weight: 400;
  color: var(--text-muted);
}

/* ── Nav Buttons ── */
.nav-buttons {
  display: flex;
  gap: 4px;
  align-items: center;
}

.nav-divider {
  width: 1px;
  height: 20px;
  background-color: var(--border);
  margin: 0 4px;
}

.nav-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 12px;
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 500;
  text-decoration: none;
  cursor: pointer;
  border: none;
  transition: all 0.15s ease;
  white-space: nowrap;
}

.nav-btn i { font-size: 11px; }

.nav-btn-ghost {
  background: transparent;
  color: var(--text-muted);
}

.nav-btn-ghost:hover {
  background-color: var(--surface-alt);
  color: var(--text);
}

.nav-btn-accent {
  background-color: var(--primary-light);
  color: white;
}

.nav-btn-accent:hover {
  background-color: var(--primary);
}

.nav-btn-primary {
  background-color: var(--primary);
  color: white;
}

.nav-btn-primary:hover {
  background-color: var(--primary-dark);
}

/* ── Main Content ── */
#main-content {
  padding: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
}

/* ── Process Tabs ── */
.process-tabs {
  display: flex;
  align-items: center;
  background-color: var(--surface);
  border-bottom: 1px solid var(--border);
  padding: 0 12px;
  gap: 0;
  min-height: 36px;
}

.process-tab {
  padding: 8px 14px;
  cursor: pointer;
  font-size: 12px;
  font-weight: 500;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--text-muted);
  border-bottom: 2px solid transparent;
  transition: all 0.15s ease;
  white-space: nowrap;
}

.process-tab:hover {
  color: var(--text);
  background-color: var(--surface-alt);
}

.process-tab.active {
  color: var(--primary);
  border-bottom-color: var(--primary);
  font-weight: 600;
}

.close-tab {
  margin-left: 6px;
  color: var(--text-muted);
  font-size: 14px;
  line-height: 1;
  cursor: pointer;
  border-radius: 3px;
  width: 16px;
  height: 16px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}

.close-tab:hover {
  background-color: rgba(220, 50, 50, 0.1);
  color: #dc3232;
}

.add-process-btn {
  background: none;
  border: none;
  color: var(--text-muted);
  cursor: pointer;
  font-size: 12px;
  padding: 4px 8px;
  border-radius: var(--radius-sm);
  margin-left: 4px;
  transition: all 0.15s;
}

.add-process-btn:hover {
  background-color: var(--surface-alt);
  color: var(--primary);
}

/* ── BPMN Editor ── */
#canvas-container {
  background-color: var(--surface);
  flex: 1;
  overflow: hidden;
}

.editor-panels {
  display: flex;
  height: calc(100vh - 130px);
  min-height: 500px;
}

.canvas-panel {
  flex: 1;
  border-right: 1px solid var(--border);
}

.properties-panel {
  width: 260px;
  min-width: 260px;
  overflow-y: auto;
  border-left: 1px solid var(--border);
  background-color: var(--surface);
}

/* ── Generated Code Section ── */
#code-section {
  background-color: var(--surface);
  border-top: 1px solid var(--border);
  padding: 0;
}

.code-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border);
}

.code-section-header h3 {
  font-size: 13px;
  font-weight: 600;
  color: var(--text);
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0;
}

.code-section-header h3 i {
  color: var(--primary);
}

.code-tabs {
  display: flex;
  gap: 2px;
}

.code-tab {
  background: none;
  border: none;
  padding: 6px 14px;
  cursor: pointer;
  font-size: 12px;
  font-weight: 500;
  color: var(--text-muted);
  border-radius: var(--radius-sm);
  transition: all 0.15s;
}

.code-tab:hover {
  background-color: var(--surface-alt);
  color: var(--text);
}

.code-tab--active {
  background-color: var(--primary) !important;
  color: white !important;
}

/* ── Project Config ── */
.project-config {
  margin: 12px 16px;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  overflow: hidden;
}

.config-header {
  padding: 10px 14px;
  background-color: var(--surface-alt);
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  font-weight: 600;
  color: var(--text);
  user-select: none;
}

.config-header i { color: var(--text-muted); font-size: 10px; }

.config-body {
  padding: 12px 14px;
  border-top: 1px solid var(--border);
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 10px;
}

.form-group {
  margin-bottom: 0;
}

.form-group label {
  display: block;
  margin-bottom: 4px;
  font-weight: 500;
  font-size: 11px;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.form-group input[type="text"] {
  width: 100%;
  padding: 7px 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  font-size: 12px;
  transition: border-color 0.2s;
  background-color: var(--surface);
}

.form-group input[type="text"]:focus {
  border-color: var(--primary);
  outline: none;
  box-shadow: 0 0 0 2px rgba(44, 122, 123, 0.15);
}

/* ── Code Cards ── */
.code-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 12px 16px;
}

.code-card {
  border: 1px solid var(--border);
  border-radius: var(--radius);
  overflow: hidden;
  background-color: var(--surface);
}

.code-card-header {
  padding: 8px 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: var(--surface-alt);
  border-bottom: 1px solid var(--border);
}

.code-card-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--text);
  display: flex;
  align-items: center;
  gap: 6px;
}

.code-card-title i {
  color: var(--primary);
  font-size: 11px;
}

.copy-btn {
  background: none;
  border: 1px solid var(--border);
  color: var(--text-muted);
  padding: 4px 10px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-size: 11px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  transition: all 0.15s;
}

.copy-btn:hover {
  background-color: var(--primary);
  color: white;
  border-color: var(--primary);
}

.code-editor {
  width: 100%;
  min-height: 180px;
  padding: 14px;
  border: none;
  font-family: 'JetBrains Mono', 'Fira Code', 'Consolas', monospace;
  font-size: 12px;
  line-height: 1.6;
  resize: vertical;
  background-color: #1e1e2e;
  color: #cdd6f4;
}

.code-editor:focus {
  outline: none;
}

/* ── Responsive ── */
@media (max-width: 768px) {
  .editor-panels {
    flex-direction: column;
    height: auto;
  }
  .canvas-panel { height: 400px; }
  .properties-panel {
    width: 100%;
    min-width: unset;
    height: 300px;
  }
  .form-grid { grid-template-columns: 1fr; }
  .topnav { padding: 0 8px; }
  .nav-buttons { gap: 2px; }
}

/* ── BPMN.js Overrides ── */
:deep(.djs-palette) {
  height: 500px;
  overflow-y: auto;
}

:deep(.djs-element) { font-size: 12px !important; }
:deep(.djs-overlay) { font-size: 12px !important; }
:deep(.djs-container) { font-size: 12px !important; }
</style>