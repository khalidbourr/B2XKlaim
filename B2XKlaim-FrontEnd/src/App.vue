<template>

  <div id="nav-bar" class="topnav" style="display: flex; justify-content: space-between; align-items: center;">
    <a href="index.html" class="active">
      <img src="./assets/b2xklaim.jpg" alt="B2Xklaim Web Client" style="height:62px">
    </a>
    <a href="#" id="Download" @click="exportCode"><i class="fa fa-download"></i> Download</a>
  </div>




  <div id="app">
    <div id="canvas-container" style="margin-top: 80px;">
      <div id="canvas" style="height: 500px; width: 80%; border: 5px solid #CEE1DF;"></div>
      <div id="properties" style="height: 500px; width: 20%; border: 5px solid #CEE1DF;"></div>
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
import JSZip from 'jszip';
import CustomPaletteProvider from './CustomPaletteProvider.js';


export default {
  name: "App",
  data() {
    return {
      bpmnModeler: null,
      activeTab: 'collaboration',
      showButtons: false,
      collaboration: '',
      processes: [],
      callActivities: []
    };
  },

  mounted() {
    this.bpmnModeler = new BpmnModeler({
      container: "#canvas",
      propertiesPanel: {
        parent: "#properties",
      },
      additionalModules: [
        {
          // these for keeping needed elements and removing the rest
          __init__: ['paletteProvider'],
          paletteProvider: ['type', CustomPaletteProvider]
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

        this.collaboration = data.collaboration || '';
        this.processes = data.processes || [];
        this.callActivities = data.callActivities || [];


      } catch (err) {
        console.error("Failed to generate code:", err);
      }
    },


    async exportCode() {
      if (!this.collaboration && !this.processes.length) {
        alert("No data available for download.");
        return;
      }

      const zip = new JSZip();

      if (this.collaboration) {
        zip.file("Main.xklaim", this.collaboration);
      }

      this.processes.forEach(process => {
        const filename = `${process.name}.xklaim`;
        zip.file(filename, process.code);
      });

      Object.keys(this.callActivities).forEach(activityName => {
        const activityCode = this.callActivities[activityName].join('\n');
        const filename = `${activityName}.xklaim`;
        zip.file(filename, activityCode);
      });

      try {
        const content = await zip.generateAsync({ type: "blob" });
        const link = document.createElement("a");
        link.href = URL.createObjectURL(content);
        link.download = "Xklaim.zip";
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(link.href);
      } catch (err) {
        console.error("Error generating zip file:", err);
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


 ::v-deep .djs-palette {
   height: 400px;
   overflow-y: auto;
 }


.code-container {
  flex: 0 0 auto;
  position: relative;
  border: 1px solid #e1e1e1;
  padding-left: 30px;
  margin-bottom: 20px;
  background-color: #CEE1DF;
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


.topnav {
  background-color: #CEE1DF;
  overflow: hidden;
  position: fixed;
  width: 100%;
  top: 0;

  #Download {
    margin-left: auto; /* Pushes the button towards the right */
    margin-right: 10%; /* Adjust this to move closer or farther from the corner */
    font-style: bold; /* Example style, you can choose normal, italic, oblique */
    color: #0f262e; /* Example green color, change as per your design */
    font-size: 16px; /* Example size, adjust as needed */
    font-family: Arial, sans-serif; /* Example font family, change as desired */
    /* Additional styling options */
    padding: 10px 15px;
    background-color: #75a2a8; /* Light grey background, change as per your design */
    border: 1px solid #ddd; /* Light grey border */
    border-radius: 4px; /* Rounded corners */
    text-decoration: none; /* Removes underline from text */
    cursor: pointer; /* Changes cursor to hand pointer */
    transition: background-color 0.3s; /* Smooth transition for hover effect */
  }

  #Download:hover {
    background-color: #e0e0e0; /* Slightly darker shade of grey on hover */
  }
}
</style>

