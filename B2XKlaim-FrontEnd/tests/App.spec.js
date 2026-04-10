import { describe, it, expect, beforeEach } from 'vitest';

// Extract the component options from App.vue for unit testing pure logic.
// We import the default export which gives us the component options object.
// Since BpmnModeler and other heavy deps are imported at module level,
// we mock them to avoid loading the entire BPMN stack in tests.
import { vi } from 'vitest';

// Mock heavy dependencies before importing App
vi.mock('camunda-bpmn-js/lib/camunda-platform/Modeler', () => ({
  default: class MockModeler {
    constructor() {}
    get() { return {}; }
    importXML() { return Promise.resolve({ warnings: [] }); }
    saveXML() { return Promise.resolve({ xml: '' }); }
  }
}));
vi.mock('camunda-bpmn-js/dist/assets/camunda-platform-modeler.css', () => ({}));
vi.mock('jszip', () => ({
  default: class MockJSZip {
    file() {}
    generateAsync() { return Promise.resolve(new Blob()); }
  }
}));
vi.mock('file-saver', () => ({ saveAs: vi.fn() }));
vi.mock('@/CustomPaletteProvider.js', () => ({ default: class {} }));
vi.mock('@/CustomReplaceMenuProvider.js', () => ({ default: class {} }));
vi.mock('@/CustomCreateAppendProvider.js', () => ({ CustomCreateMenuProvider: class {}, CustomAppendMenuProvider: class {} }));
vi.mock('@/CustomPropertiesProvider.js', () => ({ default: class {} }));
vi.mock('@/CallActivityDrilldownProvider.js', () => ({ default: class {} }));

// Now we can safely import
import App from '@/App.vue';

describe('App.vue - Data Defaults', () => {
  let dataFn;

  beforeEach(() => {
    dataFn = typeof App.data === 'function' ? App.data() : App.data;
  });

  it('should have correct default data values', () => {
    expect(dataFn.activeTab).toBe('collaboration');
    expect(dataFn.showButtons).toBe(false);
    expect(dataFn.collaboration).toBe('');
    expect(dataFn.processes).toEqual([]);
    expect(dataFn.callActivities).toEqual({});
    expect(dataFn.scriptTaskProcs).toEqual({});
    expect(dataFn.eventSubProcesses).toEqual({});
    expect(dataFn.andBranchProcs).toEqual({});
  });

  it('should have correct allTabs', () => {
    expect(dataFn.allTabs).toEqual([
      'collaboration', 'processes', 'event-subprocesses', 'and-branches'
    ]);
  });

  it('should have default project config', () => {
    expect(dataFn.projectConfig).toEqual({
      name: 'xklaim-bpmn-project',
      groupId: 'com.example',
      artifactId: 'xklaim-bpmn-project',
      version: '1.0-SNAPSHOT'
    });
  });

  it('should initialize bpmnProcesses with main process', () => {
    expect(dataFn.bpmnProcesses).toBeInstanceOf(Map);
    expect(dataFn.bpmnProcesses.has('main')).toBe(true);
    expect(dataFn.bpmnProcesses.get('main')).toEqual({
      xml: '', name: 'Main Process'
    });
  });

  it('should set activeProcess to main', () => {
    expect(dataFn.activeProcess).toBe('main');
  });

  it('should set isSwitchingProcess to false', () => {
    expect(dataFn.isSwitchingProcess).toBe(false);
  });
});

describe('App.vue - Computed Properties', () => {
  function makeContext(overrides = {}) {
    const defaults = {
      eventSubProcesses: {},
      andBranchProcs: {},
      allTabs: ['collaboration', 'processes', 'event-subprocesses', 'and-branches'],
      bpmnProcesses: new Map([['main', { xml: '', name: 'Main Process' }]]),
    };
    const ctx = { ...defaults, ...overrides };
    // Bind computed properties that depend on other computed
    ctx.hasEventSubprocesses = Object.keys(ctx.eventSubProcesses).length > 0;
    ctx.hasAndBranchProcs = Object.keys(ctx.andBranchProcs).length > 0;
    return ctx;
  }

  describe('hasEventSubprocesses', () => {
    it('returns false when eventSubProcesses is empty', () => {
      const ctx = makeContext();
      expect(ctx.hasEventSubprocesses).toBe(false);
    });

    it('returns true when eventSubProcesses has entries', () => {
      const ctx = makeContext({ eventSubProcesses: { esp1: ['code'] } });
      expect(ctx.hasEventSubprocesses).toBe(true);
    });
  });

  describe('hasAndBranchProcs', () => {
    it('returns false when andBranchProcs is empty', () => {
      const ctx = makeContext();
      expect(ctx.hasAndBranchProcs).toBe(false);
    });

    it('returns true when andBranchProcs has entries', () => {
      const ctx = makeContext({ andBranchProcs: { branch1: ['code'] } });
      expect(ctx.hasAndBranchProcs).toBe(true);
    });
  });

  describe('availableTabs', () => {
    it('shows only collaboration and processes when no ESP or AND branches', () => {
      const ctx = makeContext();
      const tabs = App.computed.availableTabs.call(ctx);
      expect(tabs).toEqual(['collaboration', 'processes']);
    });

    it('includes event-subprocesses tab when ESPs exist', () => {
      const ctx = makeContext({ eventSubProcesses: { esp1: ['code'] } });
      ctx.hasEventSubprocesses = true;
      const tabs = App.computed.availableTabs.call(ctx);
      expect(tabs).toContain('event-subprocesses');
    });

    it('includes and-branches tab when AND branches exist', () => {
      const ctx = makeContext({ andBranchProcs: { b1: ['code'] } });
      ctx.hasAndBranchProcs = true;
      const tabs = App.computed.availableTabs.call(ctx);
      expect(tabs).toContain('and-branches');
    });

    it('includes all tabs when both ESP and AND branches exist', () => {
      const ctx = makeContext({
        eventSubProcesses: { esp1: ['code'] },
        andBranchProcs: { b1: ['code'] }
      });
      ctx.hasEventSubprocesses = true;
      ctx.hasAndBranchProcs = true;
      const tabs = App.computed.availableTabs.call(ctx);
      expect(tabs).toEqual(['collaboration', 'processes', 'event-subprocesses', 'and-branches']);
    });
  });

  describe('processesArray', () => {
    it('converts bpmnProcesses Map to array of entries', () => {
      const ctx = makeContext();
      const arr = App.computed.processesArray.call(ctx);
      expect(arr).toEqual([['main', { xml: '', name: 'Main Process' }]]);
    });

    it('includes multiple processes', () => {
      const map = new Map([
        ['main', { xml: '', name: 'Main Process' }],
        ['sub1', { xml: '<xml/>', name: 'Sub Process' }],
      ]);
      const ctx = makeContext({ bpmnProcesses: map });
      const arr = App.computed.processesArray.call(ctx);
      expect(arr).toHaveLength(2);
      expect(arr[1][0]).toBe('sub1');
    });
  });
});

describe('App.vue - Methods', () => {
  describe('getDefaultDiagram', () => {
    it('returns valid BPMN XML with definitions and process', () => {
      const xml = App.methods.getDefaultDiagram();
      expect(xml).toContain('<?xml version="1.0"');
      expect(xml).toContain('bpmn:definitions');
      expect(xml).toContain('bpmn:process');
      expect(xml).toContain('bpmn:startEvent');
      expect(xml).toContain('bpmndi:BPMNDiagram');
    });

    it('has process name "Main"', () => {
      const xml = App.methods.getDefaultDiagram();
      expect(xml).toContain('name="Main"');
    });
  });

  describe('createEmptyBPMNProcess', () => {
    it('creates valid BPMN XML for a given process name', () => {
      const xml = App.methods.createEmptyBPMNProcess('MyProcess');
      expect(xml).toContain('bpmn:definitions');
      expect(xml).toContain('id="MyProcess"');
      expect(xml).toContain('name="MyProcess"');
      expect(xml).toContain('StartEvent_MyProcess');
    });

    it('sanitizes special characters in process name', () => {
      const xml = App.methods.createEmptyBPMNProcess('My Process!@#');
      expect(xml).toContain('id="My_Process___"');
      expect(xml).toContain('name="My Process!@#"');
    });

    it('includes diagram elements', () => {
      const xml = App.methods.createEmptyBPMNProcess('Test');
      expect(xml).toContain('BPMNDiagram_Test');
      expect(xml).toContain('BPMNPlane_Test');
      expect(xml).toContain('Shape_StartEvent_Test');
    });
  });

  describe('generatePomXml', () => {
    it('generates POM with project config values', () => {
      const ctx = {
        projectConfig: {
          name: 'test-project',
          groupId: 'com.test',
          artifactId: 'test-artifact',
          version: '2.0-SNAPSHOT'
        }
      };
      const pom = App.methods.generatePomXml.call(ctx);
      expect(pom).toContain('<groupId>com.test</groupId>');
      expect(pom).toContain('<artifactId>test-artifact</artifactId>');
      expect(pom).toContain('<version>2.0-SNAPSHOT</version>');
      expect(pom).toContain('xklaim.runtime');
      expect(pom).toContain('maven-assembly-plugin');
    });
  });

  describe('generateReadme', () => {
    it('includes project name and structure info', () => {
      const ctx = {
        projectConfig: {
          name: 'my-project',
          artifactId: 'my-artifact',
          version: '1.0'
        }
      };
      const readme = App.methods.generateReadme.call(ctx);
      expect(readme).toContain('# my-project');
      expect(readme).toContain('Collaboration.xklaim');
      expect(readme).toContain('processes/');
      expect(readme).toContain('activities/');
      expect(readme).toContain('my-artifact-1.0-jar-with-dependencies.jar');
    });
  });

  describe('generateGitIgnore', () => {
    it('includes common ignore patterns', () => {
      const gitignore = App.methods.generateGitIgnore();
      expect(gitignore).toContain('target/');
      expect(gitignore).toContain('.idea/');
      expect(gitignore).toContain('src-gen/');
      expect(gitignore).toContain('.DS_Store');
    });
  });

  describe('generateProject', () => {
    it('generates Eclipse .project file with project name', () => {
      const ctx = { projectConfig: { groupId: 'com.example' } };
      const proj = App.methods.generateProject.call(ctx);
      expect(proj).toContain('<name>com.example</name>');
      expect(proj).toContain('xtextBuilder');
      expect(proj).toContain('maven2Nature');
    });
  });

  describe('generateClassPath', () => {
    it('generates Eclipse .classpath file', () => {
      const cp = App.methods.generateClassPath();
      expect(cp).toContain('classpathentry');
      expect(cp).toContain('src/main/java');
      expect(cp).toContain('src-gen');
      expect(cp).toContain('JavaSE-21');
    });
  });
});

describe('App.vue - validateDiagrams', () => {
  function makeValidatorContext(processesMap) {
    return {
      bpmnProcesses: processesMap
    };
  }

  const ns = 'http://www.omg.org/spec/BPMN/20100524/MODEL';

  function wrapInBpmn(content) {
    return `<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="${ns}"
                  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
                  id="Definitions_1" targetNamespace="http://bpmn.io/schema/bpmn">
  <bpmn:process id="Process_1" name="TestProcess" isExecutable="false">
    ${content}
  </bpmn:process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="Process_1" />
  </bpmndi:BPMNDiagram>
</bpmn:definitions>`;
  }

  it('returns empty errors for empty process XML', () => {
    const ctx = makeValidatorContext(new Map([
      ['main', { xml: '', name: 'Main' }]
    ]));
    const errors = App.methods.validateDiagrams.call(ctx);
    expect(errors).toContain('Tab "Main" has no BPMN content');
  });

  it('returns no errors for valid BPMN with named elements', () => {
    const xml = wrapInBpmn(`
      <bpmn:startEvent id="Start1" name="start" />
      <bpmn:endEvent id="End1" name="end" />
    `);
    const ctx = makeValidatorContext(new Map([['main', { xml, name: 'Main' }]]));
    const errors = App.methods.validateDiagrams.call(ctx);
    expect(errors).toEqual([]);
  });

  it('detects unnamed start event', () => {
    const xml = wrapInBpmn(`<bpmn:startEvent id="Start1" />`);
    const ctx = makeValidatorContext(new Map([['main', { xml, name: 'Main' }]]));
    const errors = App.methods.validateDiagrams.call(ctx);
    expect(errors).toContainEqual(expect.stringContaining('Start Event'));
    expect(errors).toContainEqual(expect.stringContaining('has no name'));
  });

  it('detects unnamed end event', () => {
    const xml = wrapInBpmn(`<bpmn:endEvent id="End1" />`);
    const ctx = makeValidatorContext(new Map([['main', { xml, name: 'Main' }]]));
    const errors = App.methods.validateDiagrams.call(ctx);
    expect(errors).toContainEqual(expect.stringContaining('End Event'));
  });

  it('detects unnamed participant', () => {
    const xml = `<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="${ns}" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                  id="Definitions_1" targetNamespace="http://bpmn.io/schema/bpmn">
  <bpmn:collaboration id="Collab1">
    <bpmn:participant id="P1" processRef="Process_1" />
  </bpmn:collaboration>
  <bpmn:process id="Process_1" name="Main" isExecutable="false">
    <bpmn:startEvent id="Start1" name="start" />
  </bpmn:process>
  <bpmndi:BPMNDiagram id="d1">
    <bpmndi:BPMNPlane id="p1" bpmnElement="Collab1" />
  </bpmndi:BPMNDiagram>
</bpmn:definitions>`;
    const ctx = makeValidatorContext(new Map([['main', { xml, name: 'Main' }]]));
    const errors = App.methods.validateDiagrams.call(ctx);
    expect(errors).toContainEqual(expect.stringContaining('participant'));
    expect(errors).toContainEqual(expect.stringContaining('has no name'));
  });

  it('detects call activity without name', () => {
    const xml = wrapInBpmn(`<bpmn:callActivity id="CA1" />`);
    const ctx = makeValidatorContext(new Map([['main', { xml, name: 'Main' }]]));
    const errors = App.methods.validateDiagrams.call(ctx);
    expect(errors).toContainEqual(expect.stringContaining('Call Activity'));
    expect(errors).toContainEqual(expect.stringContaining('has no name'));
  });

  it('detects call activity without calledElement', () => {
    const xml = wrapInBpmn(`<bpmn:callActivity id="CA1" name="MyCA" />`);
    const ctx = makeValidatorContext(new Map([['main', { xml, name: 'Main' }]]));
    const errors = App.methods.validateDiagrams.call(ctx);
    expect(errors).toContainEqual(expect.stringContaining('has no called element'));
  });

  it('detects unnamed script task', () => {
    const xml = wrapInBpmn(`<bpmn:scriptTask id="ST1" />`);
    const ctx = makeValidatorContext(new Map([['main', { xml, name: 'Main' }]]));
    const errors = App.methods.validateDiagrams.call(ctx);
    expect(errors).toContainEqual(expect.stringContaining('Script Task'));
  });

  it('detects message start event without messageRef', () => {
    const xml = wrapInBpmn(`
      <bpmn:startEvent id="Start1" name="msgStart">
        <bpmn:messageEventDefinition id="MED1" />
      </bpmn:startEvent>
    `);
    const ctx = makeValidatorContext(new Map([['main', { xml, name: 'Main' }]]));
    const errors = App.methods.validateDiagrams.call(ctx);
    expect(errors).toContainEqual(expect.stringContaining('Message Start Event'));
    expect(errors).toContainEqual(expect.stringContaining('no message reference'));
  });

  it('detects signal start event without signalRef', () => {
    const xml = wrapInBpmn(`
      <bpmn:startEvent id="Start1" name="sigStart">
        <bpmn:signalEventDefinition id="SED1" />
      </bpmn:startEvent>
    `);
    const ctx = makeValidatorContext(new Map([['main', { xml, name: 'Main' }]]));
    const errors = App.methods.validateDiagrams.call(ctx);
    expect(errors).toContainEqual(expect.stringContaining('Signal Start Event'));
    expect(errors).toContainEqual(expect.stringContaining('no signal reference'));
  });

  it('detects timer start event without timer definition', () => {
    const xml = wrapInBpmn(`
      <bpmn:startEvent id="Start1" name="timerStart">
        <bpmn:timerEventDefinition id="TED1" />
      </bpmn:startEvent>
    `);
    const ctx = makeValidatorContext(new Map([['main', { xml, name: 'Main' }]]));
    const errors = App.methods.validateDiagrams.call(ctx);
    expect(errors).toContainEqual(expect.stringContaining('Timer Start Event'));
    expect(errors).toContainEqual(expect.stringContaining('no timer definition'));
  });

  it('accepts timer start event with timeDuration', () => {
    const xml = wrapInBpmn(`
      <bpmn:startEvent id="Start1" name="timerStart">
        <bpmn:timerEventDefinition id="TED1">
          <bpmn:timeDuration>PT5S</bpmn:timeDuration>
        </bpmn:timerEventDefinition>
      </bpmn:startEvent>
    `);
    const ctx = makeValidatorContext(new Map([['main', { xml, name: 'Main' }]]));
    const errors = App.methods.validateDiagrams.call(ctx);
    expect(errors).toEqual([]);
  });

  it('detects message end event without messageRef', () => {
    const xml = wrapInBpmn(`
      <bpmn:endEvent id="End1" name="msgEnd">
        <bpmn:messageEventDefinition id="MED1" />
      </bpmn:endEvent>
    `);
    const ctx = makeValidatorContext(new Map([['main', { xml, name: 'Main' }]]));
    const errors = App.methods.validateDiagrams.call(ctx);
    expect(errors).toContainEqual(expect.stringContaining('Message End Event'));
  });

  it('detects signal end event without signalRef', () => {
    const xml = wrapInBpmn(`
      <bpmn:endEvent id="End1" name="sigEnd">
        <bpmn:signalEventDefinition id="SED1" />
      </bpmn:endEvent>
    `);
    const ctx = makeValidatorContext(new Map([['main', { xml, name: 'Main' }]]));
    const errors = App.methods.validateDiagrams.call(ctx);
    expect(errors).toContainEqual(expect.stringContaining('Signal End Event'));
  });

  it('detects unnamed intermediate catch event', () => {
    const xml = wrapInBpmn(`<bpmn:intermediateCatchEvent id="ICE1" />`);
    const ctx = makeValidatorContext(new Map([['main', { xml, name: 'Main' }]]));
    const errors = App.methods.validateDiagrams.call(ctx);
    expect(errors).toContainEqual(expect.stringContaining('Intermediate Catch Event'));
  });

  it('detects message intermediate catch event without messageRef', () => {
    const xml = wrapInBpmn(`
      <bpmn:intermediateCatchEvent id="ICE1" name="msgCatch">
        <bpmn:messageEventDefinition id="MED1" />
      </bpmn:intermediateCatchEvent>
    `);
    const ctx = makeValidatorContext(new Map([['main', { xml, name: 'Main' }]]));
    const errors = App.methods.validateDiagrams.call(ctx);
    expect(errors).toContainEqual(expect.stringContaining('Message Intermediate Catch Event'));
  });

  it('detects timer intermediate catch event without timer definition', () => {
    const xml = wrapInBpmn(`
      <bpmn:intermediateCatchEvent id="ICE1" name="timerCatch">
        <bpmn:timerEventDefinition id="TED1" />
      </bpmn:intermediateCatchEvent>
    `);
    const ctx = makeValidatorContext(new Map([['main', { xml, name: 'Main' }]]));
    const errors = App.methods.validateDiagrams.call(ctx);
    expect(errors).toContainEqual(expect.stringContaining('Timer Intermediate Catch Event'));
  });

  it('detects unnamed intermediate throw event', () => {
    const xml = wrapInBpmn(`<bpmn:intermediateThrowEvent id="ITE1" />`);
    const ctx = makeValidatorContext(new Map([['main', { xml, name: 'Main' }]]));
    const errors = App.methods.validateDiagrams.call(ctx);
    expect(errors).toContainEqual(expect.stringContaining('Intermediate Throw Event'));
  });

  it('detects message intermediate throw event without messageRef', () => {
    const xml = wrapInBpmn(`
      <bpmn:intermediateThrowEvent id="ITE1" name="msgThrow">
        <bpmn:messageEventDefinition id="MED1" />
      </bpmn:intermediateThrowEvent>
    `);
    const ctx = makeValidatorContext(new Map([['main', { xml, name: 'Main' }]]));
    const errors = App.methods.validateDiagrams.call(ctx);
    expect(errors).toContainEqual(expect.stringContaining('Message Intermediate Throw Event'));
  });

  it('detects XOR gateway outgoing flow without condition', () => {
    const xml = wrapInBpmn(`
      <bpmn:exclusiveGateway id="GW1" name="xorGw">
        <bpmn:outgoing>Flow1</bpmn:outgoing>
        <bpmn:outgoing>Flow2</bpmn:outgoing>
      </bpmn:exclusiveGateway>
      <bpmn:sequenceFlow id="Flow1" sourceRef="GW1" targetRef="End1" />
      <bpmn:sequenceFlow id="Flow2" sourceRef="GW1" targetRef="End2" />
      <bpmn:endEvent id="End1" name="end1" />
      <bpmn:endEvent id="End2" name="end2" />
    `);
    const ctx = makeValidatorContext(new Map([['main', { xml, name: 'Main' }]]));
    const errors = App.methods.validateDiagrams.call(ctx);
    expect(errors).toContainEqual(expect.stringContaining('XOR gateway'));
    expect(errors).toContainEqual(expect.stringContaining('has no condition'));
  });

  it('skips default flow when validating XOR gateway conditions', () => {
    const xml = wrapInBpmn(`
      <bpmn:exclusiveGateway id="GW1" name="xorGw" default="Flow2">
        <bpmn:outgoing>Flow1</bpmn:outgoing>
        <bpmn:outgoing>Flow2</bpmn:outgoing>
      </bpmn:exclusiveGateway>
      <bpmn:sequenceFlow id="Flow1" sourceRef="GW1" targetRef="End1">
        <bpmn:conditionExpression>x &gt; 5</bpmn:conditionExpression>
      </bpmn:sequenceFlow>
      <bpmn:sequenceFlow id="Flow2" sourceRef="GW1" targetRef="End2" />
      <bpmn:endEvent id="End1" name="end1" />
      <bpmn:endEvent id="End2" name="end2" />
    `);
    const ctx = makeValidatorContext(new Map([['main', { xml, name: 'Main' }]]));
    const errors = App.methods.validateDiagrams.call(ctx);
    expect(errors).toEqual([]);
  });

  it('does not flag single outgoing flow from XOR gateway', () => {
    const xml = wrapInBpmn(`
      <bpmn:exclusiveGateway id="GW1" name="xorGw">
        <bpmn:outgoing>Flow1</bpmn:outgoing>
      </bpmn:exclusiveGateway>
      <bpmn:sequenceFlow id="Flow1" sourceRef="GW1" targetRef="End1" />
      <bpmn:endEvent id="End1" name="end1" />
    `);
    const ctx = makeValidatorContext(new Map([['main', { xml, name: 'Main' }]]));
    const errors = App.methods.validateDiagrams.call(ctx);
    expect(errors).toEqual([]);
  });

  it('validates across multiple process tabs', () => {
    const xml1 = wrapInBpmn(`<bpmn:startEvent id="S1" />`);
    const xml2 = wrapInBpmn(`<bpmn:endEvent id="E1" />`);
    const ctx = makeValidatorContext(new Map([
      ['main', { xml: xml1, name: 'Main' }],
      ['sub', { xml: xml2, name: 'SubProcess' }],
    ]));
    const errors = App.methods.validateDiagrams.call(ctx);
    // Both tabs should have errors
    expect(errors.some(e => e.includes('Main'))).toBe(true);
    expect(errors.some(e => e.includes('SubProcess'))).toBe(true);
  });

  it('detects unnamed process', () => {
    const xml = `<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="${ns}" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                  id="Definitions_1" targetNamespace="http://bpmn.io/schema/bpmn">
  <bpmn:process id="Process_1" isExecutable="false">
    <bpmn:startEvent id="Start1" name="start" />
  </bpmn:process>
  <bpmndi:BPMNDiagram id="d1">
    <bpmndi:BPMNPlane id="p1" bpmnElement="Process_1" />
  </bpmndi:BPMNDiagram>
</bpmn:definitions>`;
    const ctx = makeValidatorContext(new Map([['main', { xml, name: 'Main' }]]));
    const errors = App.methods.validateDiagrams.call(ctx);
    expect(errors).toContainEqual(expect.stringContaining('Process'));
    expect(errors).toContainEqual(expect.stringContaining('has no name'));
  });
});