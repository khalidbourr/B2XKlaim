<template>
  <div id="app">
    <div id="canvas-container">
      <div id="canvas" style="height: 600px; width: 60%; border: 5px solid #ccc;"></div>
      <div id="properties" style="height: 600px; width: 20%; border: 5px solid #ccc;"></div>
    </div>

    <div class="center-button-container">
      <button @click="generateCode">Generate X-Klaim Code</button>
    </div>

    <div v-if="showButtons" class="button-container">
      <button v-for="tab in tabs" :key="tab" @click="activeTab = tab" :class="{ 'tab-button--active': activeTab === tab }">
        {{ tab }}
      </button>
    </div>

    <div class="app-container">

      <!-- Collaboration Code Textarea -->
      <div v-if="collaboration" class="code-container collaboration">
        <i class="fas fa-tree root-icon"></i>
        <h4>Collaboration</h4>
        <div class="textarea-wrapper">
          <span @click="copyToClipboard('collaboration')" class="copy-icon">📋</span>
          <textarea class="textarea" placeholder="Collaboration Code..." v-model="collaboration"></textarea>
        </div>
      </div>

      <!-- Participant's Process Code Textareas -->
      <div v-for="process in processes" :key="process.name" class="code-container process">
        <i class="fas fa-branch child-icon"></i>
        <h4>{{ process.name }}</h4>
        <div class="textarea-wrapper">
          <span @click="copyToClipboard(process.name)" class="copy-icon">📋</span>
          <textarea class="textarea" :placeholder="process.name + ' Code...'" v-model="process.code"></textarea>
        </div>
      </div>

    </div>
  </div>
</template>

<script>
import BpmnModeler from "camunda-bpmn-js/lib/camunda-platform/Modeler";
import "camunda-bpmn-js/dist/assets/camunda-platform-modeler.css";

export default {
  name: "App",
  data() {
    return {
      bpmnModeler: null,
      activeTab: 'collaboration',
      showButtons: false,
      collaboration: '',       // Added this data property to store collaboration code
      processes: []            // Added this data property to store processes
    };
  },

  computed: {
    filteredParticipants() {
      if (this.activeTab === 'collaboration') {
        return [];
      }
      return this.processes.filter(process => this.activeTab === process.name);
    },
    // ... other computed properties
  },
  mounted() {
    this.bpmnModeler = new BpmnModeler({
      container: "#canvas",
      propertiesPanel: {
        parent: "#properties",
      },
    });


    // You can replace 'someDiagram' with your BPMN XML data
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
        "</bpmn:definitions>"; // <-- Replace with your BPMN XML

    try {
      this.bpmnModeler.importXML(someDiagram);
      console.log("success!");
      this.bpmnModeler.get("canvas").zoom("fit-viewport");
    } catch (err) {
      console.error("something went wrong:", err);
    }
  },
  methods: {
    async generateCode() {
      try {
        this.showButtons = true;
        const result = await this.bpmnModeler.saveXML({ format: true });
        const xml = result.xml;

        console.log(xml)

        const response = await fetch("http://localhost:8081/generate-code", {
          method: "POST",
          headers: {
            "Content-Type": "text/xml"
          },
          body: xml
        });

        if (!response.ok) {
          const errorMessage = await response.text();
          alert(errorMessage);
          return;
        }

        const data = await response.json();

        this.collaboration = data.collaboration || ''; // Update the collaboration property
        this.processes = data.processes || [];

      } catch (err) {
        console.error("Failed to generate code:", err);
      }
    },

    copyToClipboard(refName) {
      this.$refs[refName].select();
      document.execCommand('copy');
      alert('Code copied to clipboard!');
    }
  }

};


</script>

<style scoped>
/* Global container styles */
#app {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
}

#canvas-container {
  display: flex;
  gap: 20px;
  width: 100%;
  justify-content: center;
}

.center-button-container {
  margin: 20px 0;
}

.app-container {
  width: 50%;
  margin: 0 auto;
  font-family: Arial, sans-serif;
}

.code-container {
  flex: 0 0 auto;
  position: relative;
  border: 1px solid #e1e1e1;
  padding-left: 30px;
  margin-bottom: 20px;
  background-color: lightgoldenrodyellow;
  min-width: 300px;
  border-radius: 6px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.collaboration {
  border-left: 3px solid #555;
  padding-left: 20px;
}

.process {
  border-left: 3px solid #555;
  margin-left: 20px;
  padding-left: 10px;
}

.root-icon, .child-icon {
  position: absolute;
  top: 50%;
  left: 10px;
  transform: translateY(-50%);
  color: #555;
}


h4 {
  font-style: normal;
  font-family: "Fira Code Retina";
  margin-top: 0;
  font-weight: bold;
  position: center;

}

.textarea-wrapper {
  position: relative;
}

.textarea {
  width: 100%;
  padding: 30px;
  box-sizing: border-box;
  font-family: "Fira Code Light";
}

.copy-icon {
  position: absolute;
  right: 5px;
  top: 5px;
  cursor: pointer;
}
</style>

