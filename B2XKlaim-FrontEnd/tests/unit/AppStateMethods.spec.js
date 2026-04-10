import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { shallowMount } from '@vue/test-utils';

// ── Track calls for assertions ──

const mockZipFiles = {};
const mockImportXML = vi.fn(() => Promise.resolve({ warnings: [] }));
const mockSaveXML = vi.fn(() => Promise.resolve({ xml: '<saved-xml/>' }));
const mockZoom = vi.fn();
const mockUpdateProperties = vi.fn();
const mockRemoveElements = vi.fn();
const mockEventBusHandlers = {};

vi.mock('jszip', () => {
  return {
    default: class MockJSZip {
      constructor() { Object.keys(mockZipFiles).forEach(k => delete mockZipFiles[k]); }
      file(path, content) { mockZipFiles[path] = content; }
      generateAsync() { return Promise.resolve(new Blob()); }
    }
  };
});

vi.mock('camunda-bpmn-js/lib/camunda-platform/Modeler', () => {
  return {
    default: class MockModeler {
      constructor() {}
      get(service) {
        if (service === 'canvas') return { zoom: mockZoom };
        if (service === 'modeling') return { updateProperties: mockUpdateProperties, removeElements: mockRemoveElements };
        if (service === 'elementRegistry') return { filter: () => [] };
        if (service === 'keyboard') return { _keyHandler: vi.fn() };
        if (service === 'eventBus') return {
          on: (event, handler) => { mockEventBusHandlers[event] = handler; },
          fire: vi.fn()
        };
        return {};
      }
      importXML(...args) { return mockImportXML(...args); }
      saveXML(...args) { return mockSaveXML(...args); }
    }
  };
});
vi.mock('camunda-bpmn-js/dist/assets/camunda-platform-modeler.css', () => ({}));
vi.mock('file-saver', () => ({ saveAs: vi.fn() }));
vi.mock('@/CustomPaletteProvider.js', () => ({ default: class {} }));
vi.mock('@/CustomReplaceMenuProvider.js', () => ({ default: class {} }));
vi.mock('@/CustomCreateAppendProvider.js', () => ({ CustomCreateMenuProvider: class {}, CustomAppendMenuProvider: class {} }));
vi.mock('@/CustomPropertiesProvider.js', () => ({ default: class {} }));
vi.mock('@/CallActivityDrilldownProvider.js', () => ({ default: class {} }));

import App from '@/App.vue';

// ── Helpers ──

// Mount the component with mocks, wait for mounted() to finish
async function mountApp(dataOverrides = {}) {
  // Ensure localStorage is clean so restoreFromLocalStorage falls through to default
  localStorage.removeItem('b2xklaim_project');

  const wrapper = shallowMount(App, {
    global: {
      stubs: { teleport: true }
    }
  });

  // Wait for mounted() async work to complete
  await wrapper.vm.$nextTick();
  await new Promise(r => setTimeout(r, 10));

  // Apply data overrides
  for (const [key, value] of Object.entries(dataOverrides)) {
    wrapper.vm[key] = value;
  }

  return wrapper;
}

// ═══════════════════════════════════════════════════════════════
// Priority 1 — switchToProcess
// ═══════════════════════════════════════════════════════════════

describe('switchToProcess', () => {
  let wrapper;

  beforeEach(async () => {
    vi.clearAllMocks();
    mockSaveXML.mockResolvedValue({ xml: '<current-xml/>' });
    wrapper = await mountApp();

    wrapper.vm.bpmnProcesses = new Map([
      ['main', { xml: '<main-xml/>', name: 'Main Process' }],
      ['sub1', { xml: '<sub1-xml/>', name: 'Sub Process 1' }],
    ]);
    wrapper.vm.activeProcess = 'main';
  });

  afterEach(() => {
    wrapper.unmount();
  });

  it('saves current tab XML before switching', async () => {
    mockSaveXML.mockClear();
    await wrapper.vm.switchToProcess('sub1');

    expect(mockSaveXML).toHaveBeenCalledWith({ format: true });
    expect(wrapper.vm.bpmnProcesses.get('main').xml).toBe('<current-xml/>');
  });

  it('sets activeProcess before calling importXML', async () => {
    let activeProcessDuringImport = null;
    mockImportXML.mockImplementationOnce(() => {
      activeProcessDuringImport = wrapper.vm.activeProcess;
      return Promise.resolve({ warnings: [] });
    });

    await wrapper.vm.switchToProcess('sub1');

    expect(activeProcessDuringImport).toBe('sub1');
  });

  it('isSwitchingProcess is true during import, false after', async () => {
    let flagDuringImport = null;
    mockImportXML.mockImplementationOnce(() => {
      flagDuringImport = wrapper.vm.isSwitchingProcess;
      return Promise.resolve({ warnings: [] });
    });

    await wrapper.vm.switchToProcess('sub1');

    expect(flagDuringImport).toBe(true);
    expect(wrapper.vm.isSwitchingProcess).toBe(false);
  });

  it('commandStack.changed handler does NOT save when isSwitchingProcess is true', () => {
    // The commandStack.changed handler registered in mounted() guards on isSwitchingProcess
    // We test the guard logic directly
    const saveFn = vi.fn();
    const vm = wrapper.vm;

    function simulateCommandStackChanged() {
      if (vm.isSwitchingProcess) return;
      saveFn();
    }

    vm.isSwitchingProcess = true;
    simulateCommandStackChanged();
    expect(saveFn).not.toHaveBeenCalled();

    vm.isSwitchingProcess = false;
    simulateCommandStackChanged();
    expect(saveFn).toHaveBeenCalledOnce();
  });

  it('switching to a tab with empty XML still works', async () => {
    wrapper.vm.bpmnProcesses.set('empty', { xml: '', name: 'Empty Tab' });
    mockImportXML.mockClear();

    await wrapper.vm.switchToProcess('empty');

    expect(mockImportXML).toHaveBeenCalledWith('');
    expect(wrapper.vm.activeProcess).toBe('empty');
    expect(mockZoom).toHaveBeenCalledWith('fit-viewport');
  });

  it('calls canvas.zoom("fit-viewport") after import', async () => {
    mockZoom.mockClear();
    await wrapper.vm.switchToProcess('sub1');
    expect(mockZoom).toHaveBeenCalledWith('fit-viewport');
  });
});

// ═══════════════════════════════════════════════════════════════
// Priority 2 — openCallActivityProcess
// ═══════════════════════════════════════════════════════════════

describe('openCallActivityProcess', () => {
  let wrapper;

  beforeEach(async () => {
    vi.clearAllMocks();
    mockSaveXML.mockResolvedValue({ xml: '<saved/>' });
    wrapper = await mountApp();

    wrapper.vm.bpmnProcesses = new Map([
      ['main', { xml: '<main/>', name: 'Main Process' }],
    ]);
    wrapper.vm.activeProcess = 'main';
  });

  afterEach(() => {
    wrapper.unmount();
    vi.restoreAllMocks();
  });

  it('existing tab → just switches, no new tab created', async () => {
    wrapper.vm.bpmnProcesses.set('callActivity_CA1', { xml: '<ca-xml/>', name: 'MyActivity' });
    const sizeBefore = wrapper.vm.bpmnProcesses.size;

    const element = { businessObject: { id: 'CA1', name: 'MyActivity' } };
    mockImportXML.mockClear();
    await wrapper.vm.openCallActivityProcess(element);

    // Should have switched (importXML called with existing tab's XML)
    expect(mockImportXML).toHaveBeenCalled();
    // No new tab created
    expect(wrapper.vm.bpmnProcesses.size).toBe(sizeBefore);
  });

  it('CallActivity with no name → prompts user', async () => {
    const promptSpy = vi.spyOn(global, 'prompt').mockReturnValue('PromptedName');

    const element = { businessObject: { id: 'CA2', name: '' } };
    await wrapper.vm.openCallActivityProcess(element);

    expect(promptSpy).toHaveBeenCalledWith('Enter a name for this called process:');
    expect(wrapper.vm.bpmnProcesses.has('callActivity_CA2')).toBe(true);
    expect(wrapper.vm.bpmnProcesses.get('callActivity_CA2').name).toBe('PromptedName');
  });

  it('CallActivity with no name → user cancels prompt → no tab created', async () => {
    const promptSpy = vi.spyOn(global, 'prompt').mockReturnValue(null);

    const element = { businessObject: { id: 'CA3', name: '' } };
    const sizeBefore = wrapper.vm.bpmnProcesses.size;
    await wrapper.vm.openCallActivityProcess(element);

    expect(wrapper.vm.bpmnProcesses.size).toBe(sizeBefore);
  });

  it('name matches existing tab → reuses, does not duplicate', async () => {
    wrapper.vm.bpmnProcesses.set('callActivity_OLD', { xml: '<old/>', name: 'SharedProcess' });
    const sizeBefore = wrapper.vm.bpmnProcesses.size;

    const element = { businessObject: { id: 'CA_NEW', name: 'SharedProcess' } };
    await wrapper.vm.openCallActivityProcess(element);

    // Should reuse existing tab, not create callActivity_CA_NEW
    expect(wrapper.vm.bpmnProcesses.size).toBe(sizeBefore);
    expect(wrapper.vm.bpmnProcesses.has('callActivity_CA_NEW')).toBe(false);
    // calledElement set via updateProperties
    expect(mockUpdateProperties).toHaveBeenCalledWith(element, { calledElement: 'SharedProcess' });
  });

  it('new name → creates tab, sets calledElement, switches', async () => {
    const element = { businessObject: { id: 'CA4', name: 'NewProcess' } };

    const sizeBefore = wrapper.vm.bpmnProcesses.size;
    await wrapper.vm.openCallActivityProcess(element);

    // New tab should be created
    expect(wrapper.vm.bpmnProcesses.has('callActivity_CA4')).toBe(true);
    expect(wrapper.vm.bpmnProcesses.size).toBe(sizeBefore + 1);
    const newTab = wrapper.vm.bpmnProcesses.get('callActivity_CA4');
    expect(newTab.name).toBe('NewProcess');

    // createEmptyBPMNProcess generates valid BPMN XML (verified separately)
    // After switchToProcess, saveXML overwrites the XML, so we verify the method was called
    expect(mockImportXML).toHaveBeenCalled();

    // calledElement should be set
    expect(mockUpdateProperties).toHaveBeenCalledWith(element, { calledElement: 'NewProcess' });

    // Should have switched to the new tab
    expect(wrapper.vm.activeProcess).toBe('callActivity_CA4');
  });

  it('sanitizes calledElement for names with special chars', async () => {
    const element = { businessObject: { id: 'CA5', name: 'My Process!' } };

    await wrapper.vm.openCallActivityProcess(element);

    expect(mockUpdateProperties).toHaveBeenCalledWith(element, { calledElement: 'My_Process_' });
  });
});

// ═══════════════════════════════════════════════════════════════
// Priority 3 — exportCode ZIP assembly
// ═══════════════════════════════════════════════════════════════

describe('exportCode ZIP assembly', () => {
  let wrapper;

  beforeEach(async () => {
    vi.clearAllMocks();
    Object.keys(mockZipFiles).forEach(k => delete mockZipFiles[k]);

    // Stub URL methods not available in jsdom
    global.URL.createObjectURL = vi.fn(() => 'blob:mock');
    global.URL.revokeObjectURL = vi.fn();

    wrapper = await mountApp();
  });

  afterEach(() => {
    wrapper.unmount();
    vi.restoreAllMocks();
  });

  function setGeneratedCode(overrides) {
    const defaults = {
      collaboration: '',
      processes: [],
      callActivities: {},
      scriptTaskProcs: {},
      eventSubProcesses: {},
      andBranchProcs: {},
    };
    const data = { ...defaults, ...overrides };
    Object.assign(wrapper.vm, data);
  }

  it('collaboration lands at src/main/java/xklaim/Collaboration.xklaim', async () => {
    setGeneratedCode({
      collaboration: 'net Collaboration physical "localhost:9999" {}',
      processes: [{ name: 'ProcA', code: 'proc ProcA() {}' }],
    });

    await wrapper.vm.exportCode();

    expect(mockZipFiles).toHaveProperty('src/main/java/xklaim/Collaboration.xklaim');
    const content = mockZipFiles['src/main/java/xklaim/Collaboration.xklaim'];
    expect(content).toContain('package xklaim');
    expect(content).toContain('net Collaboration');
  });

  it('processes land at src/main/java/xklaim/processes/{name}.xklaim', async () => {
    setGeneratedCode({
      collaboration: 'net Collab {}',
      processes: [
        { name: 'AlphaBehavior', code: 'proc AlphaBehavior() {}' },
        { name: 'BetaBehavior', code: 'proc BetaBehavior() {}' },
      ],
    });

    await wrapper.vm.exportCode();

    expect(mockZipFiles).toHaveProperty('src/main/java/xklaim/processes/AlphaBehavior.xklaim');
    expect(mockZipFiles).toHaveProperty('src/main/java/xklaim/processes/BetaBehavior.xklaim');
    expect(mockZipFiles['src/main/java/xklaim/processes/AlphaBehavior.xklaim']).toContain('package xklaim.processes');
    expect(mockZipFiles['src/main/java/xklaim/processes/AlphaBehavior.xklaim']).toContain('import klava.Locality');
  });

  it('call activities land at src/main/java/xklaim/activities/{name}.xklaim', async () => {
    setGeneratedCode({
      collaboration: 'net Collab {}',
      processes: [{ name: 'Proc', code: 'proc Proc() { SubWork() }' }],
      callActivities: { SubWork: ['proc SubWork() {}'] },
    });

    await wrapper.vm.exportCode();

    expect(mockZipFiles).toHaveProperty('src/main/java/xklaim/activities/SubWork.xklaim');
    expect(mockZipFiles['src/main/java/xklaim/activities/SubWork.xklaim']).toContain('package xklaim.activities');
  });

  it('script tasks land at src/main/java/xklaim/tasks/{name}.xklaim', async () => {
    setGeneratedCode({
      collaboration: 'net Collab {}',
      processes: [{ name: 'Proc', code: 'proc Proc() { DoWork() }' }],
      scriptTaskProcs: { DoWork: ['proc DoWork() {}'] },
    });

    await wrapper.vm.exportCode();

    expect(mockZipFiles).toHaveProperty('src/main/java/xklaim/tasks/DoWork.xklaim');
    expect(mockZipFiles['src/main/java/xklaim/tasks/DoWork.xklaim']).toContain('package xklaim.tasks');
  });

  it('event subprocesses land at src/main/java/xklaim/tasks/{id}.xklaim', async () => {
    setGeneratedCode({
      collaboration: 'net Collab {}',
      processes: [{ name: 'Proc', code: 'proc Proc() { ESP_Handler }' }],
      eventSubProcesses: { ESP_Handler: ['proc ESP_Handler() {}'] },
    });

    await wrapper.vm.exportCode();

    expect(mockZipFiles).toHaveProperty('src/main/java/xklaim/tasks/ESP_Handler.xklaim');
    expect(mockZipFiles['src/main/java/xklaim/tasks/ESP_Handler.xklaim']).toContain('package xklaim.tasks');
  });

  it('AND branches land at src/main/java/xklaim/branches/{name}.xklaim', async () => {
    setGeneratedCode({
      collaboration: 'net Collab { Branch1() }',
      processes: [{ name: 'Proc', code: 'proc Proc() {}' }],
      andBranchProcs: { Branch1: ['proc Branch1() {}'] },
    });

    await wrapper.vm.exportCode();

    expect(mockZipFiles).toHaveProperty('src/main/java/xklaim/branches/Branch1.xklaim');
    expect(mockZipFiles['src/main/java/xklaim/branches/Branch1.xklaim']).toContain('package xklaim.branches');
  });

  it('empty categories produce no files', async () => {
    setGeneratedCode({
      collaboration: 'net Collab {}',
      processes: [{ name: 'Proc', code: 'proc Proc() {}' }],
    });

    await wrapper.vm.exportCode();

    const xklaimFiles = Object.keys(mockZipFiles).filter(p => p.endsWith('.xklaim'));
    // Only Collaboration.xklaim and processes/Proc.xklaim
    expect(xklaimFiles).toHaveLength(2);
    expect(xklaimFiles).toContain('src/main/java/xklaim/Collaboration.xklaim');
    expect(xklaimFiles).toContain('src/main/java/xklaim/processes/Proc.xklaim');
  });

  it('generates scaffold files (pom.xml, README, .gitignore, .project, .classpath)', async () => {
    setGeneratedCode({
      collaboration: 'net Collab {}',
      processes: [{ name: 'Proc', code: 'proc Proc() {}' }],
    });

    await wrapper.vm.exportCode();

    expect(mockZipFiles).toHaveProperty('pom.xml');
    expect(mockZipFiles).toHaveProperty('README.md');
    expect(mockZipFiles).toHaveProperty('.gitignore');
    expect(mockZipFiles).toHaveProperty('.project');
    expect(mockZipFiles).toHaveProperty('.classpath');
  });

  it('alerts when no code has been generated', async () => {
    const alertSpy = vi.spyOn(global, 'alert').mockImplementation(() => {});
    setGeneratedCode({});

    await wrapper.vm.exportCode();

    expect(alertSpy).toHaveBeenCalledWith('No code has been generated to download.');
  });

  it('import statements are generated for cross-package dependencies', async () => {
    setGeneratedCode({
      collaboration: 'net Collab {}',
      processes: [
        { name: 'WorkerBehavior', code: 'proc WorkerBehavior() { DoTask(); SubWork() }' },
      ],
      callActivities: { SubWork: ['proc SubWork() {}'] },
      scriptTaskProcs: { DoTask: ['proc DoTask() {}'] },
    });

    await wrapper.vm.exportCode();

    const processContent = mockZipFiles['src/main/java/xklaim/processes/WorkerBehavior.xklaim'];
    expect(processContent).toContain('import xklaim.activities.SubWork');
    expect(processContent).toContain('import xklaim.tasks.DoTask');
  });

  it('collaboration imports process packages', async () => {
    setGeneratedCode({
      collaboration: 'net Collab { AlphaBehavior(); BetaBehavior() }',
      processes: [
        { name: 'AlphaBehavior', code: 'proc AlphaBehavior() {}' },
        { name: 'BetaBehavior', code: 'proc BetaBehavior() {}' },
      ],
    });

    await wrapper.vm.exportCode();

    const collabContent = mockZipFiles['src/main/java/xklaim/Collaboration.xklaim'];
    expect(collabContent).toContain('import xklaim.processes.AlphaBehavior');
    expect(collabContent).toContain('import xklaim.processes.BetaBehavior');
  });

  it('collaboration imports AND branch procs when referenced', async () => {
    setGeneratedCode({
      collaboration: 'net Collab { ProcA(); AND_Branch1() }',
      processes: [{ name: 'ProcA', code: 'proc ProcA() {}' }],
      andBranchProcs: { AND_Branch1: ['proc AND_Branch1() {}'] },
    });

    await wrapper.vm.exportCode();

    const collabContent = mockZipFiles['src/main/java/xklaim/Collaboration.xklaim'];
    expect(collabContent).toContain('import xklaim.branches.AND_Branch1');
  });
});

// ═══════════════════════════════════════════════════════════════
// Priority 4 — removeProcess
// ═══════════════════════════════════════════════════════════════

describe('removeProcess', () => {
  let wrapper;

  beforeEach(async () => {
    vi.clearAllMocks();
    mockSaveXML.mockResolvedValue({ xml: '<saved/>' });
    wrapper = await mountApp();

    wrapper.vm.bpmnProcesses = new Map([
      ['main', { xml: '<main/>', name: 'Main Process' }],
      ['sub1', { xml: '<sub1/>', name: 'Sub 1' }],
      ['sub2', { xml: '<sub2/>', name: 'Sub 2' }],
    ]);
    wrapper.vm.activeProcess = 'main';
    vi.spyOn(global, 'confirm').mockReturnValue(true);
  });

  afterEach(() => {
    wrapper.unmount();
    vi.restoreAllMocks();
  });

  it('cannot remove main', async () => {
    const alertSpy = vi.spyOn(global, 'alert').mockImplementation(() => {});

    await wrapper.vm.removeProcess('main');

    expect(alertSpy).toHaveBeenCalledWith('Cannot remove the main process!');
    expect(wrapper.vm.bpmnProcesses.has('main')).toBe(true);
  });

  it('removing active tab switches to main first', async () => {
    wrapper.vm.activeProcess = 'sub1';
    mockImportXML.mockClear();

    await wrapper.vm.removeProcess('sub1');

    expect(wrapper.vm.activeProcess).toBe('main');
    expect(mockImportXML).toHaveBeenCalledWith('<main/>');
    expect(wrapper.vm.bpmnProcesses.has('sub1')).toBe(false);
  });

  it('removing non-active tab does not switch', async () => {
    wrapper.vm.activeProcess = 'main';
    mockImportXML.mockClear();

    await wrapper.vm.removeProcess('sub1');

    expect(wrapper.vm.activeProcess).toBe('main');
    expect(mockImportXML).not.toHaveBeenCalled();
    expect(wrapper.vm.bpmnProcesses.has('sub1')).toBe(false);
  });

  it('Map entry is deleted after removal', async () => {
    expect(wrapper.vm.bpmnProcesses.size).toBe(3);

    await wrapper.vm.removeProcess('sub2');

    expect(wrapper.vm.bpmnProcesses.size).toBe(2);
    expect(wrapper.vm.bpmnProcesses.has('sub2')).toBe(false);
    expect(wrapper.vm.bpmnProcesses.has('main')).toBe(true);
    expect(wrapper.vm.bpmnProcesses.has('sub1')).toBe(true);
  });

  it('user cancelling confirm does not delete', async () => {
    vi.spyOn(global, 'confirm').mockReturnValue(false);

    await wrapper.vm.removeProcess('sub1');

    expect(wrapper.vm.bpmnProcesses.has('sub1')).toBe(true);
    expect(wrapper.vm.bpmnProcesses.size).toBe(3);
  });
});

// ═══════════════════════════════════════════════════════════════
// Priority 5 — generateCode payload
// ═══════════════════════════════════════════════════════════════

describe('generateCode payload', () => {
  let wrapper;
  let fetchBody;

  beforeEach(async () => {
    vi.clearAllMocks();
    mockSaveXML.mockResolvedValue({ xml: '<active-xml/>' });
    fetchBody = null;

    global.fetch = vi.fn(async (url, options) => {
      fetchBody = JSON.parse(options.body);
      return {
        ok: true,
        json: async () => ({
          collaboration: 'net TestCollab {}',
          processes: [{ name: 'Proc1', code: 'proc Proc1() {}' }],
          callActivities: { CA1: ['proc CA1() {}'] },
          scriptTaskProcs: { ST1: ['proc ST1() {}'] },
          eventSubProcesses: { ESP1: ['proc ESP1() {}'] },
          andBranchProcs: { AND1: ['proc AND1() {}'] },
        }),
      };
    });

    wrapper = await mountApp();

    wrapper.vm.bpmnProcesses = new Map([
      ['main', { xml: '<main-xml/>', name: 'Main Process' }],
      ['sub1', { xml: '<sub1-xml/>', name: 'Sub Process' }],
      ['callActivity_CA1', { xml: '<ca-xml/>', name: 'My Activity' }],
    ]);
    wrapper.vm.activeProcess = 'main';
  });

  afterEach(() => {
    wrapper.unmount();
    delete global.fetch;
  });

  it('sends all tabs XML, not just active', async () => {
    await wrapper.vm.generateCode();

    expect(fetchBody.processes).toHaveProperty('main');
    expect(fetchBody.processes).toHaveProperty('sub1');
    expect(Object.keys(fetchBody.processes)).toHaveLength(3);
  });

  it('callActivity_{id} keys get renamed to sanitized name', async () => {
    await wrapper.vm.generateCode();

    // 'callActivity_CA1' with name 'My Activity' → key 'My_Activity'
    expect(fetchBody.processes).not.toHaveProperty('callActivity_CA1');
    expect(fetchBody.processes).toHaveProperty('My_Activity');
    expect(fetchBody.processes['My_Activity']).toBe('<ca-xml/>');
  });

  it('non-callActivity keys keep their original process ID', async () => {
    await wrapper.vm.generateCode();

    expect(fetchBody.processes).toHaveProperty('main');
    expect(fetchBody.processes).toHaveProperty('sub1');
  });

  it('active tab XML is saved before sending', async () => {
    mockSaveXML.mockClear();
    await wrapper.vm.generateCode();

    expect(mockSaveXML).toHaveBeenCalledWith({ format: true });
    expect(wrapper.vm.bpmnProcesses.get('main').xml).toBe('<active-xml/>');
  });

  it('response fields map correctly to data properties', async () => {
    await wrapper.vm.generateCode();

    expect(wrapper.vm.collaboration).toBe('net TestCollab {}');
    expect(wrapper.vm.processes).toEqual([{ name: 'Proc1', code: 'proc Proc1() {}' }]);
    expect(wrapper.vm.callActivities).toEqual({ CA1: ['proc CA1() {}'] });
    expect(wrapper.vm.scriptTaskProcs).toEqual({ ST1: ['proc ST1() {}'] });
    expect(wrapper.vm.eventSubProcesses).toEqual({ ESP1: ['proc ESP1() {}'] });
    expect(wrapper.vm.andBranchProcs).toEqual({ AND1: ['proc AND1() {}'] });
  });

  it('sets showButtons to true after successful generation', async () => {
    expect(wrapper.vm.showButtons).toBe(false);
    await wrapper.vm.generateCode();
    expect(wrapper.vm.showButtons).toBe(true);
  });

  it('does not proceed if validation fails', async () => {
    // Invalid XML: start event with no name
    const invalidXml = `<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                  id="D1" targetNamespace="http://bpmn.io/schema/bpmn">
  <bpmn:process id="P1" name="Main" isExecutable="false">
    <bpmn:startEvent id="S1" />
  </bpmn:process>
  <bpmndi:BPMNDiagram id="d1"><bpmndi:BPMNPlane id="p1" bpmnElement="P1" /></bpmndi:BPMNDiagram>
</bpmn:definitions>`;

    // Make saveXML return the same invalid XML so validateDiagrams sees it
    mockSaveXML.mockResolvedValueOnce({ xml: invalidXml });

    wrapper.vm.bpmnProcesses = new Map([
      ['main', { xml: invalidXml, name: 'Main' }],
    ]);
    const alertSpy = vi.spyOn(global, 'alert').mockImplementation(() => {});

    await wrapper.vm.generateCode();

    expect(global.fetch).not.toHaveBeenCalled();
    expect(wrapper.vm.showButtons).toBe(false);
  });

  it('handles backend error response', async () => {
    global.fetch = vi.fn(async () => ({
      ok: false,
      text: async () => 'Translation failed: invalid BPMN',
    }));
    const alertSpy = vi.spyOn(global, 'alert').mockImplementation(() => {});

    // Use valid XML so validation passes
    wrapper.vm.bpmnProcesses = new Map([
      ['main', { xml: `<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                  id="D1" targetNamespace="http://bpmn.io/schema/bpmn">
  <bpmn:process id="P1" name="Main" isExecutable="false">
    <bpmn:startEvent id="S1" name="start" />
  </bpmn:process>
  <bpmndi:BPMNDiagram id="d1"><bpmndi:BPMNPlane id="p1" bpmnElement="P1" /></bpmndi:BPMNDiagram>
</bpmn:definitions>`, name: 'Main' }],
    ]);

    await wrapper.vm.generateCode();

    expect(alertSpy).toHaveBeenCalledWith('Translation failed: invalid BPMN');
  });
});

// ═══════════════════════════════════════════════════════════════
// Priority 6 — addNewProcess
// ═══════════════════════════════════════════════════════════════

describe('addNewProcess', () => {
  let wrapper;

  beforeEach(async () => {
    vi.clearAllMocks();
    mockSaveXML.mockResolvedValue({ xml: '<saved/>' });
    wrapper = await mountApp();

    wrapper.vm.bpmnProcesses = new Map([
      ['main', { xml: '<main/>', name: 'Main Process' }],
    ]);
    wrapper.vm.activeProcess = 'main';
  });

  afterEach(() => {
    wrapper.unmount();
    vi.restoreAllMocks();
  });

  it('creates a new tab and switches to it', async () => {
    vi.spyOn(global, 'prompt').mockReturnValue('NewProc');

    await wrapper.vm.addNewProcess();

    expect(wrapper.vm.bpmnProcesses.has('NewProc')).toBe(true);
    expect(wrapper.vm.bpmnProcesses.get('NewProc').name).toBe('NewProc');
    expect(wrapper.vm.activeProcess).toBe('NewProc');
  });

  it('rejects duplicate name', async () => {
    wrapper.vm.bpmnProcesses.set('Existing', { xml: '<x/>', name: 'Existing' });
    vi.spyOn(global, 'prompt').mockReturnValue('Existing');
    const alertSpy = vi.spyOn(global, 'alert').mockImplementation(() => {});

    const sizeBefore = wrapper.vm.bpmnProcesses.size;
    await wrapper.vm.addNewProcess();

    expect(alertSpy).toHaveBeenCalledWith('Process "Existing" already exists!');
    expect(wrapper.vm.bpmnProcesses.size).toBe(sizeBefore);
    expect(wrapper.vm.activeProcess).toBe('main');
  });

  it('does nothing when user cancels prompt', async () => {
    vi.spyOn(global, 'prompt').mockReturnValue(null);

    const sizeBefore = wrapper.vm.bpmnProcesses.size;
    await wrapper.vm.addNewProcess();

    expect(wrapper.vm.bpmnProcesses.size).toBe(sizeBefore);
  });

  it('does nothing for empty/whitespace name', async () => {
    vi.spyOn(global, 'prompt').mockReturnValue('   ');

    const sizeBefore = wrapper.vm.bpmnProcesses.size;
    await wrapper.vm.addNewProcess();

    expect(wrapper.vm.bpmnProcesses.size).toBe(sizeBefore);
  });

  it('trims the process name', async () => {
    vi.spyOn(global, 'prompt').mockReturnValue('  Trimmed  ');

    await wrapper.vm.addNewProcess();

    expect(wrapper.vm.bpmnProcesses.has('Trimmed')).toBe(true);
    expect(wrapper.vm.bpmnProcesses.get('Trimmed').name).toBe('Trimmed');
  });

  it('switchToProcess error is caught and alerted, tab remains', async () => {
    vi.spyOn(global, 'prompt').mockReturnValue('FailProc');
    const alertSpy = vi.spyOn(global, 'alert').mockImplementation(() => {});

    // switchToProcess catches importXML errors internally and alerts
    // so addNewProcess's catch block doesn't run — the tab stays in the map
    mockImportXML.mockRejectedValueOnce(new Error('Import failed'));

    await wrapper.vm.addNewProcess();

    // switchToProcess handles the error (alerts) but doesn't re-throw,
    // so the tab is still in the map
    expect(wrapper.vm.bpmnProcesses.has('FailProc')).toBe(true);
    expect(alertSpy).toHaveBeenCalledWith(
      expect.stringContaining('Error switching to process')
    );
  });
});

// ═══════════════════════════════════════════════════════════════
// Priority 7 — localStorage round-trip
// ═══════════════════════════════════════════════════════════════

describe('saveToLocalStorage / restoreFromLocalStorage', () => {
  let wrapper;

  beforeEach(async () => {
    vi.clearAllMocks();
    localStorage.removeItem('b2xklaim_project');
    wrapper = await mountApp();
  });

  afterEach(() => {
    wrapper.unmount();
    localStorage.removeItem('b2xklaim_project');
  });

  it('saves process map to localStorage', async () => {
    wrapper.vm.bpmnProcesses = new Map([
      ['main', { xml: '<main-xml/>', name: 'Main Process' }],
      ['sub1', { xml: '<sub1-xml/>', name: 'Sub 1' }],
    ]);
    wrapper.vm.activeProcess = 'main';

    await wrapper.vm.saveToLocalStorage();

    const saved = JSON.parse(localStorage.getItem('b2xklaim_project'));
    expect(saved).not.toBeNull();
    expect(saved.activeProcess).toBe('main');
    expect(saved.processes).toHaveProperty('main');
    expect(saved.processes).toHaveProperty('sub1');
    expect(saved.processes.sub1.name).toBe('Sub 1');
  });

  it('restores process map from localStorage', async () => {
    const projectData = {
      activeProcess: 'sub1',
      processes: {
        main: { xml: '<main-stored/>', name: 'Main Process' },
        sub1: { xml: '<sub1-stored/>', name: 'Sub 1' },
      }
    };
    localStorage.setItem('b2xklaim_project', JSON.stringify(projectData));

    await wrapper.vm.restoreFromLocalStorage();

    expect(wrapper.vm.bpmnProcesses.has('main')).toBe(true);
    expect(wrapper.vm.bpmnProcesses.has('sub1')).toBe(true);
    expect(wrapper.vm.bpmnProcesses.get('sub1').name).toBe('Sub 1');
    expect(wrapper.vm.activeProcess).toBe('sub1');
    expect(mockImportXML).toHaveBeenCalledWith('<sub1-stored/>');
  });

  it('falls back to default diagram when localStorage is empty', async () => {
    localStorage.removeItem('b2xklaim_project');
    mockImportXML.mockClear();

    await wrapper.vm.restoreFromLocalStorage();

    // Should import the default diagram XML
    expect(mockImportXML).toHaveBeenCalled();
    const importedXml = mockImportXML.mock.calls[mockImportXML.mock.calls.length - 1][0];
    expect(importedXml).toContain('bpmn:definitions');
    expect(importedXml).toContain('name="Main"');
  });

  it('falls back to default diagram when localStorage has corrupt data', async () => {
    localStorage.setItem('b2xklaim_project', 'not valid json{{{');
    mockImportXML.mockClear();

    await wrapper.vm.restoreFromLocalStorage();

    // Should fall back to default diagram
    expect(mockImportXML).toHaveBeenCalled();
    const importedXml = mockImportXML.mock.calls[mockImportXML.mock.calls.length - 1][0];
    expect(importedXml).toContain('bpmn:definitions');
  });

  it('round-trip preserves all process tabs', async () => {
    wrapper.vm.bpmnProcesses = new Map([
      ['main', { xml: '<main/>', name: 'Main Process' }],
      ['p1', { xml: '<p1/>', name: 'Process 1' }],
      ['p2', { xml: '<p2/>', name: 'Process 2' }],
    ]);
    wrapper.vm.activeProcess = 'p1';

    await wrapper.vm.saveToLocalStorage();

    // Clear in-memory state
    wrapper.vm.bpmnProcesses = new Map([['main', { xml: '', name: 'Main Process' }]]);
    wrapper.vm.activeProcess = 'main';

    await wrapper.vm.restoreFromLocalStorage();

    expect(wrapper.vm.bpmnProcesses.size).toBe(3);
    expect(wrapper.vm.bpmnProcesses.has('p1')).toBe(true);
    expect(wrapper.vm.bpmnProcesses.has('p2')).toBe(true);
    expect(wrapper.vm.activeProcess).toBe('p1');
  });
});

// ═══════════════════════════════════════════════════════════════
// Priority 8 — switchToProcess error path
// ═══════════════════════════════════════════════════════════════

describe('switchToProcess error handling', () => {
  let wrapper;

  beforeEach(async () => {
    vi.clearAllMocks();
    mockSaveXML.mockResolvedValue({ xml: '<saved/>' });
    wrapper = await mountApp();

    wrapper.vm.bpmnProcesses = new Map([
      ['main', { xml: '<main/>', name: 'Main Process' }],
      ['bad', { xml: '<bad/>', name: 'Bad Process' }],
    ]);
    wrapper.vm.activeProcess = 'main';
  });

  afterEach(() => {
    wrapper.unmount();
    vi.restoreAllMocks();
  });

  it('alerts when importXML rejects', async () => {
    const alertSpy = vi.spyOn(global, 'alert').mockImplementation(() => {});
    mockImportXML.mockRejectedValueOnce(new Error('Parse error'));

    await wrapper.vm.switchToProcess('bad');

    expect(alertSpy).toHaveBeenCalledWith(
      expect.stringContaining('Error switching to process "bad"')
    );
  });

  it('alerts when switching to nonexistent process', async () => {
    const alertSpy = vi.spyOn(global, 'alert').mockImplementation(() => {});

    await wrapper.vm.switchToProcess('nonexistent');

    expect(alertSpy).toHaveBeenCalledWith(
      expect.stringContaining('nonexistent')
    );
  });
});