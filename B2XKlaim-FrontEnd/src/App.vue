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
        <a href="#" id="ImportBPMN" @click="importBPMN" class="nav-btn">
          <i class="fa fa-upload"></i> Import BPMN
        </a>
        <a href="#" id="SaveBPMN" @click="saveBPMN" class="nav-btn">
          <i class="fa fa-save"></i> Save BPMN
        </a>
        <a href="#" id="Download" @click="exportCode" class="download-btn">
          <i class="fa fa-download"></i> Download
        </a>
      </div>
      <!-- Hidden file input for import -->
      <input type="file" id="bpmn-file-input" accept=".bpmn,.xml" style="display: none;" @change="handleFileSelect">
    </div>

    <div id="main-content">
      <div class="process-tabs">
        <div v-for="[processId, processData] in processesArray"
             :key="processId"
             :class="{ 'tab': true, 'active': activeProcess === processId }"
             @click="switchToProcess(processId)">
          {{ processData.name }}
          <!-- Optional: Add close button for non-main processes -->
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
      <p>&copy; 2023 B2XKlaim. All rights reserved.</p>
    </footer>
  </div>
</template>

<script>
import BpmnModeler from "camunda-bpmn-js/lib/camunda-platform/Modeler";
import "camunda-bpmn-js/dist/assets/camunda-platform-modeler.css";
// Import the saveAs function from file-saver library
import JSZip from 'jszip';
import CustomPaletteProvider from './CustomPaletteProvider.js';
import CustomReplaceMenuProvider from './CustomReplaceMenuProvider.js';
import { CustomCreateMenuProvider, CustomAppendMenuProvider } from './CustomCreateAppendProvider.js';
import CustomPropertiesProvider from './CustomPropertiesProvider.js';

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

  mounted() {
    this.bpmnModeler = new BpmnModeler({
      container: "#canvas",
      propertiesPanel: {
        parent: "#properties",
      },
      additionalModules: [
        {
          __init__: ['paletteProvider', 'customPropertiesProvider'],
          paletteProvider: ['type', CustomPaletteProvider],
          replaceMenuProvider: ['type', CustomReplaceMenuProvider],
          createMenuProvider: ['type', CustomCreateMenuProvider],
          appendMenuProvider: ['type', CustomAppendMenuProvider],
          customPropertiesProvider: ['type', CustomPropertiesProvider]
        },
      ]
    });

    const someDiagram = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<bpmn:definitions xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\" xmlns:dc=\"http://www.omg.org/spec/DD/20100524/DC\" id=\"Definitions_1mpw3ap\" targetNamespace=\"http://bpmn.io/schema/bpmn\" exporter=\"bpmn-js (https://demo.bpmn.io)\" exporterVersion=\"14.0.0\">\n" +
        "  <bpmn:process id=\"Process_0d01xqv\" isExecutable=\"false\">\n" +
        "    <bpmn:startEvent id=\"StartEvent_08r9k21\" />\n" +
        "  </bpmn:process>\n" +
        "  <bpmndi:BPMNDiagram id=\"BPMNDiagram_1\">\n" +
        "    <bpmndi:BPMNPlane id=\"BPMNPlane_1\" bpmnElement=\"Process_0d01xqv\">\n" +
        "      <bpmndi:BPMNShape id=\"_BPMNShape_StartEvent_2\" bpmnElement=\"StartEvent_08r9k21\">\n" +
        "        <dc:Bounds x=\"152\" y=\"82\" width=\"36\" height=\"36\" />\n" +
        "      </bpmndi:BPMNShape>\n" +
        "    </bpmndi:BPMNPlane>\n" +
        "  </bpmndi:BPMNDiagram>\n" +
        "</bpmn:definitions>";

    try {
      this.bpmnModeler.importXML(someDiagram);
      console.log("success!");
      this.bpmnModeler.get("canvas").zoom("fit-viewport");
    } catch (err) {
      console.error("something went wrong:", err);
    }
  },
  methods: {
    // Import BPMN file function
    importBPMN() {
      // Trigger the hidden file input
      document.getElementById('bpmn-file-input').click();
    },

    // Handle the file selection
    handleFileSelect(event) {
      const file = event.target.files[0];
      if (!file) return;

      const reader = new FileReader();
      reader.onload = (e) => {
        const xml = e.target.result;

        try {
          this.bpmnModeler.importXML(xml)
              .then(({ warnings }) => {
                if (warnings.length) {
                  console.warn('Warnings while importing BPMN:', warnings);
                }
                this.bpmnModeler.get('canvas').zoom('fit-viewport');
                console.log('BPMN diagram imported successfully');
              })
              .catch(err => {
                console.error('Error importing BPMN diagram', err);
                alert('Error importing BPMN diagram: ' + err.message);
              });
        } catch (err) {
          console.error('Error handling BPMN import:', err);
          alert('Error handling BPMN import: ' + err.message);
        }
      };

      reader.onerror = (e) => {
        console.error('Error reading file:', e);
        alert('Error reading file: ' + e.target.error);
      };

      reader.readAsText(file);

      // Reset the file input so the same file can be imported again if needed
      event.target.value = '';
    },

    // Save BPMN diagram as XML
    async saveBPMN() {
      try {
        // Get the XML from the BPMN modeler with proper formatting
        const {xml} = await this.bpmnModeler.saveXML({format: true});

        // Create a Blob from the XML string
        // Setting type to application/xml ensures proper handling by browsers
        const blob = new Blob([xml], {type: 'application/xml'});

        // Generate filename with timestamp
        const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
        const filename = `bpmn-diagram-${timestamp}.bpmn`;

        // Create a URL for the blob
        const url = URL.createObjectURL(blob);

        // Create a temporary link element
        const link = document.createElement('a');
        link.href = url;
        link.download = filename;

        // Append to the document, click, and clean up
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);

        // Clean up the URL object
        URL.revokeObjectURL(url);

        console.log('BPMN diagram exported successfully');
      } catch (err) {
        console.error('Error exporting BPMN diagram:', err);
        alert('Error exporting BPMN diagram: ' + err.message);
      }
    },

    async addNewProcess() {
      const processName = prompt('Enter process name:');
      if (!processName || !processName.trim()) {
        return; // User cancelled or entered empty name
      }

      const trimmedName = processName.trim();

      // Check if process already exists
      if (this.bpmnProcesses.has(trimmedName)) {
        alert(`Process "${trimmedName}" already exists!`);
        return;
      }

      try {
        // Create new empty BPMN process
        const newProcessXML = this.createEmptyBPMNProcess(trimmedName);

        // Store it
        this.bpmnProcesses.set(trimmedName, {
          xml: newProcessXML,
          name: trimmedName
        });

        // Switch to the new process
        await this.switchToProcess(trimmedName);

        // Force Vue to update the DOM
        this.$forceUpdate();

        console.log(`New process "${trimmedName}" created successfully`);
      } catch (error) {
        console.error('Error creating new process:', error);
        alert(`Error creating process "${trimmedName}": ${error.message}`);

        // Clean up if process was partially created
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
        // Remove from the map
        this.bpmnProcesses.delete(processId);

        // If we're currently viewing the deleted process, switch to main
        if (this.activeProcess === processId) {
          await this.switchToProcess('main');
        }

        // Force update to refresh the tabs
        this.$forceUpdate();
      }
    },

    async switchToProcess(processName) {
      try {
        // Save current process first
        if (this.bpmnModeler) {
          const currentXML = await this.bpmnModeler.saveXML({ format: true });
          this.bpmnProcesses.get(this.activeProcess).xml = currentXML.xml;
        }

        // Load new process
        const processData = this.bpmnProcesses.get(processName);
        if (processData) {
          const result = await this.bpmnModeler.importXML(processData.xml);

          // Handle warnings if any
          if (result.warnings && result.warnings.length > 0) {
            console.warn('Warnings while switching process:', result.warnings);
          }

          // Update active process and fit viewport
          this.activeProcess = processName;
          this.bpmnModeler.get('canvas').zoom('fit-viewport');

          console.log(`Successfully switched to process: ${processName}`);
        } else {
          throw new Error(`Process data not found for: ${processName}`);
        }
      } catch (error) {
        console.error('Error switching process:', error);
        alert(`Error switching to process "${processName}": ${error.message}`);
      }
    },

    createEmptyBPMNProcess(processName) {
      // Generate unique IDs to avoid conflicts
      const processId = `Process_${processName.replace(/[^a-zA-Z0-9]/g, '_')}`;
      const startEventId = `StartEvent_${processName.replace(/[^a-zA-Z0-9]/g, '_')}`;
      const flowId = `Flow_${processName.replace(/[^a-zA-Z0-9]/g, '_')}`;
      const diagramId = `BPMNDiagram_${processName.replace(/[^a-zA-Z0-9]/g, '_')}`;
      const planeId = `BPMNPlane_${processName.replace(/[^a-zA-Z0-9]/g, '_')}`;
      const startShapeId = `_BPMNShape_StartEvent_${processName.replace(/[^a-zA-Z0-9]/g, '_')}`;


      return `<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
                  xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  id="Definitions_${processName.replace(/[^a-zA-Z0-9]/g, '_')}"
                  targetNamespace="http://bpmn.io/schema/bpmn"
                  exporter="bpmn-js (https://demo.bpmn.io)"
                  exporterVersion="14.0.0">
  <bpmn:process id="${processId}" name="${processName}" isExecutable="false">
    <bpmn:startEvent id="${startEventId}" name="start">
      <bpmn:outgoing>${flowId}</bpmn:outgoing>
    </bpmn:startEvent>
  </bpmn:process>

  <bpmndi:BPMNDiagram id="${diagramId}">
    <bpmndi:BPMNPlane id="${planeId}" bpmnElement="${processId}">
      <bpmndi:BPMNShape id="${startShapeId}" bpmnElement="${startEventId}">
        <dc:Bounds x="152" y="102" width="36" height="36" />
        <bpmndi:BPMNLabel>
          <dc:Bounds x="157" y="145" width="25" height="14" />
        </bpmndi:BPMNLabel>
      </bpmndi:BPMNShape>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</bpmn:definitions>`;
    },



    async generateCode() {
      try {
        this.showButtons = true;

        // Save current active process first
        if (this.bpmnModeler) {
          const currentXML = await this.bpmnModeler.saveXML({ format: true });
          this.bpmnProcesses.get(this.activeProcess).xml = currentXML.xml;
        }

        // Collect all processes
        const allProcesses = {};
        for (let [processId, processData] of this.bpmnProcesses.entries()) {
          allProcesses[processId] = processData.xml;
        }

        console.log("Sending all processes:", Object.keys(allProcesses));

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
  font-size: 14px; /* Reduced base font size */
}

.app-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

#main-content {
  padding: 10px; /* Reduced padding */
  flex: 1;
}

h1, h2, h3, h4 {
  margin: 0;
  color: var(--secondary-color);
}

h1 {
  font-size: 20px;
}

/* Reduced heading sizes */
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
  margin-bottom: 8px; /* Reduced margin */
  border-bottom: 1px solid var(--accent-color); /* Thinner border */
  padding-bottom: 3px; /* Reduced padding */
}

/* Navigation Bar */
.topnav {
  background-color: var(--header-bg-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 10px; /* Reduced padding */
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1); /* Smaller shadow */
  position: sticky;
  top: 0;
  z-index: 100;
  height: 40px; /* Fixed smaller height */
}

.logo-container {
  display: flex;
  align-items: center;
}

.logo {
  height: 32px; /* Smaller logo */
  margin-right: 10px; /* Reduced margin */
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
  margin: 50;
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
  padding: 6px 12px; /* Smaller button */
  border-radius: 10px; /* Smaller radius */
  text-decoration: none;
  font-weight: bold;
  transition: background-color 0.3s, transform 0.2s;
  border: none;
  display: flex;
  align-items: baseline;
  gap: 6px;
  font-size: 13px; /* Smaller font */
}

.nav-btn:hover {
  background-color: var(--secondary-color);
  transform: translateY(-1px); /* Smaller transform */
}

/* Download button (special styling) */
.download-btn {
  background-color: var(--primary-color);
  color: white;
  padding: 6px 12px; /* Smaller button */
  border-radius: 10px; /* Smaller radius */
  text-decoration: none;
  font-weight: bold;
  transition: background-color 0.3s, transform 0.2s;
  border: none;
  display: flex;
  align-items: baseline;
  gap: 6px;
  font-size: 13px; /* Smaller font */
}

.download-btn:hover {
  background-color: var(--secondary-color);
  transform: translateY(-1px); /* Smaller transform */
}

/* BPMN Editor Section */
#canvas-container {
  background-color: var(--panel-color);
  border-radius: 6px; /* Smaller radius */
  box-shadow: 0 1px 5px rgba(0, 0, 0, 0.05); /* Smaller shadow */
  margin-bottom: 15px; /* Reduced margin */
  overflow: hidden;
}

.panel-header {
  background-color: var(--header-bg-color);
  padding: 8px 12px; /* Reduced padding */
  border-bottom: 1px solid var(--border-color);
}

.editor-panels {
  display: flex;
  height: 600px; /* Increased canvas height */
}

.canvas-panel {
  flex: 5; /* Increased the flex ratio for canvas */
  border-right: 1px solid var(--border-color);
}

.properties-panel {
  flex: 1;
  padding: 8px; /* Reduced padding */
  overflow-y: auto;
  max-width: 240px; /* Limit properties panel width */
}

.center-button-container {
  padding: 8px; /* Reduced padding */
  text-align: center;
  border-top: 1px solid var(--border-color);
}

.primary-button {
  background-color: var(--primary-color);
  color: white;
  border: none;
  padding: 8px 16px; /* Smaller button */
  border-radius: 3px; /* Smaller radius */
  font-size: 14px; /* Smaller font */
  cursor: pointer;
  transition: background-color 0.3s, transform 0.2s;
}

.primary-button:hover {
  background-color: var(--secondary-color);
  transform: translateY(-1px); /* Smaller transform */
}

/* Generated Code Section */
#code-section {
  background-color: var(--panel-color);
  border-radius: 6px; /* Smaller radius */
  box-shadow: 0 1px 5px rgba(0, 0, 0, 0.05); /* Smaller shadow */
  padding: 12px; /* Reduced padding */
}

.code-tabs {
  display: flex;
  margin-bottom: 10px; /* Reduced margin */
  border-bottom: 1px solid var(--border-color);
}

.code-tabs button {
  background-color: transparent;
  border: none;
  padding: 6px 12px; /* Smaller padding */
  cursor: pointer;
  font-size: 13px; /* Smaller font */
  color: var(--text-color);
  margin-right: 3px; /* Reduced margin */
  border-bottom: 2px solid transparent; /* Thinner border */
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
  padding: 12px; /* Reduced padding */
  border-radius: 4px; /* Smaller radius */
  margin-bottom: 15px; /* Reduced margin */
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr)); /* Smaller columns */
  gap: 10px; /* Reduced gap */
}

.form-group {
  margin-bottom: 8px; /* Reduced margin */
}

.form-group label {
  display: block;
  margin-bottom: 3px; /* Reduced margin */
  font-weight: 500;
  font-size: 12px; /* Smaller font */
}

.form-group input[type="text"] {
  width: 100%;
  padding: 6px 8px; /* Smaller padding */
  border: 1px solid var(--border-color);
  border-radius: 3px; /* Smaller radius */
  font-size: 12px; /* Smaller font */
  transition: border-color 0.3s;
  height: 28px; /* Fixed smaller height */
}

.form-group input[type="text"]:focus {
  border-color: var(--primary-color);
  outline: none;
}

.form-group input[type="checkbox"] {
  margin-right: 6px; /* Reduced margin */
}

/* Code Containers */
.code-container {
  background-color: var(--code-bg-color);
  border-radius: 4px; /* Smaller radius */
  margin-bottom: 15px; /* Reduced margin */
  overflow: hidden;
}

.code-header {
  background-color: var(--header-bg-color);
  padding: 6px 10px; /* Reduced padding */
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid var(--border-color);
  height: 30px; /* Fixed smaller height */
}

.code-actions {
  display: flex;
  gap: 6px; /* Reduced gap */
}

.copy-icon {
  cursor: pointer;
  padding: 3px; /* Reduced padding */
  border-radius: 3px; /* Smaller radius */
  transition: background-color 0.3s;
  font-size: 12px; /* Smaller icon */
}

.copy-icon:hover {
  background-color: rgba(0, 0, 0, 0.1);
}

.textarea-wrapper {
  position: relative;
}

.code-editor {
  width: 100%;
  min-height: 150px; /* Reduced height */
  padding: 10px; /* Reduced padding */
  border: none;
  font-family: 'Fira Code', monospace;
  font-size: 12px; /* Smaller font */
  line-height: 1.4; /* Reduced line height */
  resize: vertical;
  background-color: var(--code-bg-color);
  color: var(--text-color);
}

.code-editor:focus {
  outline: none;
}

.collaboration {
  border-left: 2px solid var(--primary-color); /* Thinner border */
}

.process {
  border-left: 2px solid var(--accent-color); /* Thinner border */
}

.code-processes {
  display: flex;
  flex-direction: column;
  gap: 12px; /* Reduced gap */
}

/* Footer */
.footer {
  background-color: var(--header-bg-color);
  text-align: center;
  padding: 8px; /* Reduced padding */
  margin-top: 15px; /* Reduced margin */
  border-top: 1px solid var(--border-color);
  font-size: 12px; /* Smaller font */
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

/* Remove top border radius from canvas container when tabs are present */
#canvas-container {
  border-radius: 0 6px 6px 6px;
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
  height: 500px; /* Keep palette tall */
  overflow-y: auto;
}

/* Make diagram elements more visible */
:deep(.djs-element) {
  font-size: 12px !important; /* Ensure diagram text is readable */
}

:deep(.djs-overlay) {
  font-size: 12px !important;
}

:deep(.djs-container) {
  font-size: 12px !important;
}

</style>