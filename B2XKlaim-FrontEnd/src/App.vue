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
          <button v-for="tab in tabs" :key="tab" @click="activeTab = tab" 
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
      tabs: ['collaboration', 'processes'],
      projectConfig: {
        name: 'xklaim-bpmn-project',
        groupId: 'com.example',
        artifactId: 'xklaim-bpmn-project',
        version: '1.0-SNAPSHOT'
      }
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
        const { xml } = await this.bpmnModeler.saveXML({ format: true });
        
        // Create a Blob from the XML string
        // Setting type to application/xml ensures proper handling by browsers
        const blob = new Blob([xml], { type: 'application/xml' });
        
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

    async generateCode() {
      try {
        this.showButtons = true;
        const result = await this.bpmnModeler.saveXML({ format: true });
        const xml = result.xml;

        console.log(xml);

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
        this.callActivities = data.callActivities || {}; 
        this.scriptTaskProcs = data.scriptTaskProcs || {}; 

        this.activeTab = 'collaboration'; 

      } catch (err) {
        console.error("Failed to generate code:", err);
        alert("Failed to generate code. Please check the console for details.");
      }
    },


    async exportCode() {
      // Check if there is any code generated
      if (!this.collaboration && !this.processes.length && !Object.keys(this.callActivities).length && !Object.keys(this.scriptTaskProcs).length) {
        alert("No code has been generated to download.");
        return;
      }

      const zip = new JSZip();
      const projectName = this.projectConfig.name || 'xklaim-bpmn-project'; // Use default if empty

      // --- Define Base Paths ---
      // Base path within the zip for source files
      const srcMainJavaXklaimPath = `src/main/java/xklaim/`;
      // Base path for activity/task procedure files
      const activitiesPath = `${srcMainJavaXklaimPath}activities/`;


      zip.file(`pom.xml`, this.generatePomXml());            
      zip.file(`README.md`, this.generateReadme());         
      zip.file(`.gitignore`, this.generateGitIgnore());     
      zip.file(`.project`, this.generateProject());      
      zip.file(`.classpath`, this.generateClassPath());     

      // --- Collaboration File ---
      if (this.collaboration) {
        // Dynamically generate imports based on process names
        const processImports = this.processes.map(process => {
           const normalizedName = process.name.replace(/[^a-zA-Z0-9]/g, '').toLowerCase();
           // Assume processes are in subpackages named after them below 'xklaim'
           return `import xklaim.${normalizedName}.${process.name}`;
        }).join('\n');

        const collaborationWithPackage =
          `package xklaim\n\n` + // Main package for collaboration entry point
          `${processImports}\n\n` +
          `${this.collaboration}`; // The generated net { ... } block

        // Place Collaboration file directly under the base xklaim path
        zip.file(`${srcMainJavaXklaimPath}Collaboration.xklaim`, collaborationWithPackage);
      }


      // --- Process Files ---
      this.processes.forEach(process => {
        const processName = process.name;
        const normalizedName = processName.replace(/[^a-zA-Z0-9]/g, '').toLowerCase();
        const processPackage = `xklaim.${normalizedName}`; // e.g., xklaim.mission1
        const packageDeclaration = `package ${processPackage}\n\n`;

        // --- Add klava.Locality import ---
        const klavaImports = 'import klava.Locality\n'; // <<< Define Locality import

        // Add import for activities/tasks if placeholders are generated
        const activityImports = (Object.keys(this.callActivities).length > 0 || Object.keys(this.scriptTaskProcs).length > 0)
            ? `import xklaim.activities.*\n` // Import activities package
            : '';

        // Combine package, imports, and the full proc code
        // process.code already contains the full "proc Name(...) { ... }"
        // <<< Add klavaImports here >>>
        const processWithPackage = packageDeclaration + klavaImports + activityImports + "\n" + process.code;

        // Create package folder for each process
        const processFilePath = `${srcMainJavaXklaimPath}${normalizedName}/${processName}.xklaim`;
        console.log("Adding process file:", processFilePath);
        zip.file(processFilePath, processWithPackage);
      });


      // --- Call Activity Placeholders ---
      const activitiesPackage = `xklaim.activities`; // Package name for placeholders
      console.debug("Adding call activity placeholders:", this.callActivities);
      Object.keys(this.callActivities).forEach(activityName => {
         // Backend sends a List<String> for each, join them.
         const activityCode = this.callActivities[activityName].join('\n');
         const activityWithPackage = `package ${activitiesPackage}\n\n${activityCode}`;
         const filePath = `${activitiesPath}${activityName}.xklaim`; // Use activitiesPath
         console.log("Adding call activity file:", filePath);
         zip.file(filePath, activityWithPackage);
      });


      // --- Script Task Placeholders (in activities folder) ---
      console.debug("Adding script task placeholders:", this.scriptTaskProcs);
      Object.keys(this.scriptTaskProcs).forEach(taskName => {
         const taskCode = this.scriptTaskProcs[taskName].join('\n');
         // *** Use the SAME 'activitiesPackage' ***
         const taskWithPackage = `package ${activitiesPackage};\n\n${taskCode}`;
         // *** Use the SAME 'activitiesPath' to put file in activities folder ***
         const filePath = `${activitiesPath}${taskName}.xklaim`;
         console.log("Adding script task file:", filePath);
         zip.file(filePath, taskWithPackage);
      });


      // --- Generate the zip file and trigger download ---
      try {
        console.log("Generating Zip file...");
        const content = await zip.generateAsync({ type: "blob" });
        const link = document.createElement("a");
        link.href = URL.createObjectURL(content);
        link.download = `${projectName}.zip`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(link.href);
        console.log("Zip file download initiated.");
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


    generateClassPath(){
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
</classpath>`; },


    generateReadme() {
      return `# ${this.projectConfig.name}

This XKlaim project was automatically generated using the B2XKlaim tool.

## Project Structure

- \`src/main/java/xklaim/\`: Contains the XKlaim source files
  - \`Main.xklaim\`: Collaboration coordination code
  - Participant-specific code in packages
- \`src-gen/\`: Generated Java code (populated when building)

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

h1 { font-size: 20px; } /* Reduced heading sizes */
h2 { font-size: 16px; }
h3 { font-size: 14px; }
h4 { font-size: 13px; }

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
    align-items: start;
    position: sticky;
}

.app-main-title {
    font-size: 1.0rem;
    font-weight: 1000;
    color: #388285;
    margin: 50;
    line-height: 0.5;
    letter-spacing: -1px;
    text-shadow: 1px 1px 2px rgba(0,0,0,0.2);
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