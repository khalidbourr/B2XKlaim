import { describe, it, expect, vi, beforeEach } from 'vitest';

// Mock dependencies imported by CustomPropertiesProvider
vi.mock('@bpmn-io/properties-panel', () => ({
  TextFieldEntry: vi.fn(),
}));
vi.mock('bpmn-js-properties-panel', () => ({
  useService: vi.fn(),
}));

// ─── CustomReplaceMenuProvider ───

// Shared variable so tests can control what the mock parent returns
let _mockParentEntries = {};

vi.mock('bpmn-js/lib/features/popup-menu/ReplaceMenuProvider', () => {
  function MockReplaceMenuProvider() {}
  MockReplaceMenuProvider.prototype.getPopupMenuEntries = function() {
    return _mockParentEntries;
  };
  return { default: MockReplaceMenuProvider };
});

import CustomReplaceMenuProvider from '@/CustomReplaceMenuProvider.js';

describe('CustomReplaceMenuProvider', () => {
  let provider;

  beforeEach(() => {
    _mockParentEntries = {};
    provider = Object.create(CustomReplaceMenuProvider.prototype);
  });

  it('has correct $inject dependencies', () => {
    expect(CustomReplaceMenuProvider.$inject).toEqual([
      'bpmnFactory', 'popupMenu', 'modeling', 'moddle',
      'bpmnReplace', 'rules', 'translate', 'moddleCopy'
    ]);
  });

  it('filters out unsupported entries', () => {
    _mockParentEntries = {
      'replace-with-none-start': { label: 'None Start' },
      'replace-with-exclusive-gateway': { label: 'XOR' },
      'replace-with-complex-gateway': { label: 'Complex GW' },
      'replace-with-inclusive-gateway': { label: 'Inclusive GW' },
      'replace-with-script-task': { label: 'Script Task' },
      'replace-with-service-task': { label: 'Service Task' },
      'replace-with-user-task': { label: 'User Task' },
    };

    const result = provider.getPopupMenuEntries({});

    // Supported entries should be kept
    expect(result).toHaveProperty('replace-with-none-start');
    expect(result).toHaveProperty('replace-with-exclusive-gateway');
    expect(result).toHaveProperty('replace-with-script-task');

    // Unsupported entries should be filtered out
    expect(result).not.toHaveProperty('replace-with-complex-gateway');
    expect(result).not.toHaveProperty('replace-with-inclusive-gateway');
    expect(result).not.toHaveProperty('replace-with-service-task');
    expect(result).not.toHaveProperty('replace-with-user-task');
  });

  it('keeps all supported event entries', () => {
    _mockParentEntries = {
      'replace-with-none-start': {},
      'replace-with-message-start': {},
      'replace-with-timer-start': {},
      'replace-with-signal-start': {},
      'replace-with-none-end': {},
      'replace-with-message-end': {},
      'replace-with-signal-end': {},
      'replace-with-terminate-end': {},
      'replace-with-message-intermediate-catch': {},
      'replace-with-message-intermediate-throw': {},
      'replace-with-timer-intermediate-catch': {},
      'replace-with-signal-intermediate-catch': {},
      'replace-with-signal-intermediate-throw': {},
      'replace-with-none-intermediate-throwing': {},
    };

    const result = provider.getPopupMenuEntries({});
    expect(Object.keys(result)).toHaveLength(14);
  });

  it('keeps gateway entries', () => {
    _mockParentEntries = {
      'replace-with-exclusive-gateway': {},
      'replace-with-parallel-gateway': {},
      'replace-with-event-based-gateway': {},
      'replace-with-inclusive-gateway': {},
    };

    const result = provider.getPopupMenuEntries({});
    expect(result).toHaveProperty('replace-with-exclusive-gateway');
    expect(result).toHaveProperty('replace-with-parallel-gateway');
    expect(result).toHaveProperty('replace-with-event-based-gateway');
    expect(result).not.toHaveProperty('replace-with-inclusive-gateway');
  });

  it('keeps sequence flow entries', () => {
    _mockParentEntries = {
      'replace-with-sequence-flow': {},
      'replace-with-default-flow': {},
      'replace-with-conditional-flow': {},
    };

    const result = provider.getPopupMenuEntries({});
    expect(Object.keys(result)).toHaveLength(3);
  });
});

// ─── CustomPropertiesProvider ───

import CustomPropertiesProvider from '@/CustomPropertiesProvider.js';

describe('CustomPropertiesProvider', () => {
  it('has correct $inject', () => {
    expect(CustomPropertiesProvider.$inject).toEqual(['propertiesPanel']);
  });

  it('registers itself with propertiesPanel at priority 200', () => {
    const mockPanel = { registerProvider: vi.fn() };
    const provider = new CustomPropertiesProvider(mockPanel);
    expect(mockPanel.registerProvider).toHaveBeenCalledWith(200, provider);
  });

  describe('getGroups filter', () => {
    let provider;

    beforeEach(() => {
      provider = new CustomPropertiesProvider({ registerProvider: vi.fn() });
    });

    it('keeps allowed standard BPMN groups', () => {
      const element = { type: 'bpmn:Task' };
      const filterFn = provider.getGroups(element);

      const groups = [
        { id: 'general' },
        { id: 'multiInstance' },
        { id: 'timer' },
        { id: 'message' },
        { id: 'signal' },
        { id: 'error' },
        { id: 'escalation' },
      ];

      const result = filterFn(groups);
      expect(result).toHaveLength(7);
    });

    it('keeps allowed Camunda groups', () => {
      const element = { type: 'bpmn:CallActivity' };
      const filterFn = provider.getGroups(element);

      const groups = [
        { id: 'CamundaPlatform__CallActivity', entries: [
          { id: 'calledElement' },
          { id: 'calledElementBinding' },
          { id: 'calledElementTenantId' },
        ]},
        { id: 'CamundaPlatform__Condition' },
        { id: 'CamundaPlatform__Script' },
      ];

      const result = filterFn(groups);
      expect(result).toHaveLength(3);
      // CallActivity group should only keep calledElement entry
      const caGroup = result.find(g => g.id === 'CamundaPlatform__CallActivity');
      expect(caGroup.entries).toHaveLength(1);
      expect(caGroup.entries[0].id).toBe('calledElement');
    });

    it('removes Camunda platform-specific groups', () => {
      const element = { type: 'bpmn:Task' };
      const filterFn = provider.getGroups(element);

      const groups = [
        { id: 'general' },
        { id: 'CamundaPlatform__AsyncContinuation' },
        { id: 'CamundaPlatform__InputOutput' },
        { id: 'CamundaPlatform__ConnectorDetails' },
        { id: 'CamundaPlatform__FieldInjection' },
        { id: 'CamundaPlatform__ExecutionListener' },
      ];

      const result = filterFn(groups);
      expect(result).toHaveLength(1);
      expect(result[0].id).toBe('general');
    });

    it('shows ExtensionProperties for DataObjectReference with relabeled group', () => {
      const element = { type: 'bpmn:DataObjectReference' };
      const filterFn = provider.getGroups(element);

      const groups = [
        { id: 'general' },
        { id: 'CamundaPlatform__ExtensionProperties', label: 'Extension properties',
          items: [
            { id: 'el-extensionProperty-0', entries: [
              { id: 'el-extensionProperty-0-name', component: function(){} },
              { id: 'el-extensionProperty-0-value', component: function(){} },
            ]}
          ]},
      ];

      const result = filterFn(groups);
      expect(result).toHaveLength(2);
      const extGroup = result.find(g => g.id === 'CamundaPlatform__ExtensionProperties');
      expect(extGroup.label).toBe('Data Object Fields');
    });

    it('shows ExtensionProperties for message events with relabeled group', () => {
      const element = {
        type: 'bpmn:IntermediateThrowEvent',
        businessObject: {
          eventDefinitions: [{ $type: 'bpmn:MessageEventDefinition' }]
        }
      };
      const filterFn = provider.getGroups(element);

      const groups = [
        { id: 'general' },
        { id: 'CamundaPlatform__ExtensionProperties', label: 'Extension properties',
          items: [
            { id: 'el-extensionProperty-0', entries: [
              { id: 'el-extensionProperty-0-name', component: function(){} },
              { id: 'el-extensionProperty-0-value', component: function(){} },
            ]}
          ]},
      ];

      const result = filterFn(groups);
      expect(result).toHaveLength(2);
      const extGroup = result.find(g => g.id === 'CamundaPlatform__ExtensionProperties');
      expect(extGroup.label).toBe('Message Payload');
    });

    it('keeps default label for signal events', () => {
      const element = {
        type: 'bpmn:IntermediateThrowEvent',
        businessObject: {
          eventDefinitions: [{ $type: 'bpmn:SignalEventDefinition' }]
        }
      };
      const filterFn = provider.getGroups(element);

      const groups = [
        { id: 'general' },
        { id: 'CamundaPlatform__ExtensionProperties', label: 'Extension properties',
          items: [] },
      ];

      const result = filterFn(groups);
      const extGroup = result.find(g => g.id === 'CamundaPlatform__ExtensionProperties');
      expect(extGroup.label).toBe('Extension properties');
    });

    it('hides ExtensionProperties for non-event elements', () => {
      const element = { type: 'bpmn:Task' };
      const filterFn = provider.getGroups(element);

      const groups = [
        { id: 'general' },
        { id: 'CamundaPlatform__ExtensionProperties' },
      ];

      const result = filterFn(groups);
      expect(result).toHaveLength(1);
      expect(result[0].id).toBe('general');
    });

    it('hides ExtensionProperties for events without message/signal def', () => {
      const element = {
        type: 'bpmn:StartEvent',
        businessObject: {
          eventDefinitions: [{ $type: 'bpmn:TimerEventDefinition' }]
        }
      };
      const filterFn = provider.getGroups(element);

      const groups = [
        { id: 'general' },
        { id: 'CamundaPlatform__ExtensionProperties' },
      ];

      const result = filterFn(groups);
      expect(result).toHaveLength(1);
    });
  });
});

// ─── CustomCreateAppendProvider ───

import { CustomCreateMenuProvider, CustomAppendMenuProvider } from '@/CustomCreateAppendProvider.js';

describe('CustomCreateMenuProvider', () => {
  it('has correct $inject', () => {
    expect(CustomCreateMenuProvider.$inject).toEqual([
      'elementFactory', 'popupMenu', 'create', 'autoPlace', 'mouse', 'translate'
    ]);
  });

  it('registers with popupMenu for bpmn-create', () => {
    const mockPopupMenu = { registerProvider: vi.fn() };
    const provider = new CustomCreateMenuProvider(
      {}, mockPopupMenu, {}, {}, {}, v => v
    );
    expect(mockPopupMenu.registerProvider).toHaveBeenCalledWith('bpmn-create', provider);
  });

  it('returns entries for supported elements', () => {
    const mockElementFactory = { create: vi.fn(), createParticipantShape: vi.fn() };
    const mockPopupMenu = { registerProvider: vi.fn(), close: vi.fn() };
    const mockCreate = { start: vi.fn() };
    const mockMouse = { getLastMoveEvent: vi.fn() };
    const translate = v => v;

    const provider = new CustomCreateMenuProvider(
      mockElementFactory, mockPopupMenu, mockCreate, {}, mockMouse, translate
    );

    const entries = provider.getPopupMenuEntries();

    // Should have entries for supported elements
    expect(entries).toHaveProperty('create-exclusive-gateway');
    expect(entries).toHaveProperty('create-parallel-gateway');
    expect(entries).toHaveProperty('create-event-based-gateway');
    expect(entries).toHaveProperty('create-script-task');
    expect(entries).toHaveProperty('create-call-activity');
    expect(entries).toHaveProperty('create-event-subprocess');
    expect(entries).toHaveProperty('create-none-start-event');
    expect(entries).toHaveProperty('create-none-end-event');
    expect(entries).toHaveProperty('create-message-start');
    expect(entries).toHaveProperty('create-timer-start');
    expect(entries).toHaveProperty('create-signal-start');
    expect(entries).toHaveProperty('create-expanded-pool');
    expect(entries).toHaveProperty('create-collapsed-pool');

    // Should NOT have unsupported elements like user-task, service-task
    expect(entries).not.toHaveProperty('create-user-task');
    expect(entries).not.toHaveProperty('create-service-task');
  });

  it('each entry has click and dragstart actions', () => {
    const mockElementFactory = { create: vi.fn(), createParticipantShape: vi.fn() };
    const mockPopupMenu = { registerProvider: vi.fn(), close: vi.fn() };
    const translate = v => v;

    const provider = new CustomCreateMenuProvider(
      mockElementFactory, mockPopupMenu, { start: vi.fn() }, {}, { getLastMoveEvent: vi.fn() }, translate
    );

    const entries = provider.getPopupMenuEntries();

    for (const [key, entry] of Object.entries(entries)) {
      expect(entry.action).toBeDefined();
      expect(typeof entry.action.click).toBe('function');
      expect(typeof entry.action.dragstart).toBe('function');
    }
  });
});

describe('CustomAppendMenuProvider', () => {
  it('has correct $inject', () => {
    expect(CustomAppendMenuProvider.$inject).toEqual([
      'elementFactory', 'popupMenu', 'create', 'autoPlace', 'rules', 'mouse', 'translate'
    ]);
  });

  it('registers with popupMenu for bpmn-append', () => {
    const mockPopupMenu = { registerProvider: vi.fn() };
    const provider = new CustomAppendMenuProvider(
      {}, mockPopupMenu, {}, {}, {}, {}, v => v
    );
    expect(mockPopupMenu.registerProvider).toHaveBeenCalledWith('bpmn-append', provider);
  });

  it('returns empty if append not allowed', () => {
    const mockRules = { allowed: vi.fn(() => false) };
    const mockPopupMenu = { registerProvider: vi.fn() };

    const provider = new CustomAppendMenuProvider(
      {}, mockPopupMenu, {}, {}, mockRules, {}, v => v
    );

    const entries = provider.getPopupMenuEntries({ type: 'bpmn:EndEvent' });
    expect(entries).toEqual([]);
  });

  it('excludes participants from append entries', () => {
    const mockRules = { allowed: vi.fn(() => true) };
    const mockPopupMenu = { registerProvider: vi.fn(), close: vi.fn() };
    const mockElementFactory = { create: vi.fn() };
    const mockCreate = { start: vi.fn() };
    const mockAutoPlace = { append: vi.fn() };
    const mockMouse = { getLastMoveEvent: vi.fn() };

    const provider = new CustomAppendMenuProvider(
      mockElementFactory, mockPopupMenu, mockCreate, mockAutoPlace, mockRules, mockMouse, v => v
    );

    const entries = provider.getPopupMenuEntries({ type: 'bpmn:Task' });

    // Participants should not be in append menu
    expect(entries).not.toHaveProperty('append-expanded-pool');
    expect(entries).not.toHaveProperty('append-collapsed-pool');

    // Regular elements should be present
    expect(entries).toHaveProperty('append-exclusive-gateway');
    expect(entries).toHaveProperty('append-script-task');
    expect(entries).toHaveProperty('append-none-end-event');
  });
});

// ─── CallActivityDrilldownProvider ───

import CallActivityDrilldownProvider from '@/CallActivityDrilldownProvider.js';

describe('CallActivityDrilldownProvider', () => {
  it('has correct $inject', () => {
    expect(CallActivityDrilldownProvider.$inject).toEqual(['contextPad', 'eventBus']);
  });

  it('registers itself with contextPad', () => {
    const mockContextPad = { registerProvider: vi.fn() };
    const mockEventBus = { fire: vi.fn() };
    const provider = new CallActivityDrilldownProvider(mockContextPad, mockEventBus);
    expect(mockContextPad.registerProvider).toHaveBeenCalledWith(provider);
  });

  it('returns empty entries for non-CallActivity elements', () => {
    const mockContextPad = { registerProvider: vi.fn() };
    const mockEventBus = { fire: vi.fn() };
    const provider = new CallActivityDrilldownProvider(mockContextPad, mockEventBus);

    expect(provider.getContextPadEntries({ type: 'bpmn:Task' })).toEqual({});
    expect(provider.getContextPadEntries({ type: 'bpmn:StartEvent' })).toEqual({});
    expect(provider.getContextPadEntries({ type: 'bpmn:SubProcess' })).toEqual({});
  });

  it('returns drill-down entry for CallActivity elements', () => {
    const mockContextPad = { registerProvider: vi.fn() };
    const mockEventBus = { fire: vi.fn() };
    const provider = new CallActivityDrilldownProvider(mockContextPad, mockEventBus);

    const entries = provider.getContextPadEntries({ type: 'bpmn:CallActivity' });
    expect(entries).toHaveProperty('open-call-activity');
    expect(entries['open-call-activity'].group).toBe('edit');
    expect(entries['open-call-activity'].title).toBe('Open called process');
    expect(typeof entries['open-call-activity'].action.click).toBe('function');
  });

  it('fires callActivity.drilldown event on click', () => {
    const mockContextPad = { registerProvider: vi.fn() };
    const mockEventBus = { fire: vi.fn() };
    const provider = new CallActivityDrilldownProvider(mockContextPad, mockEventBus);

    const element = { type: 'bpmn:CallActivity', id: 'CA_1' };
    const entries = provider.getContextPadEntries(element);
    entries['open-call-activity'].action.click();

    expect(mockEventBus.fire).toHaveBeenCalledWith('callActivity.drilldown', { element });
  });
});

// ─── CustomPaletteProvider ───

vi.mock('bpmn-js/lib/features/palette/PaletteProvider', () => {
  class MockPaletteProvider {
    constructor() {}
    getPaletteEntries() {
      return {
        'hand-tool': {},
        'lasso-tool': {},
        'space-tool': {},
        'global-connect-tool': {},
        'create.start-event': {},
        'create.intermediate-event': {},
        'create.end-event': {},
        'create.exclusive-gateway': {},
        'create.task': {},
        'create.subprocess': {},
        'create.subprocess-expanded': {},
        'create.group': {},
        'create.participant-expanded': {},
        'create.data-object': {},
        'create.data-store': {},
      };
    }
  }
  return { default: MockPaletteProvider };
});

import CustomPaletteProvider from '@/CustomPaletteProvider.js';

describe('CustomPaletteProvider', () => {
  let provider;

  beforeEach(() => {
    const mockElementFactory = {
      createShape: vi.fn((opts) => ({ type: opts.type, ...opts }))
    };
    const mockCreate = { start: vi.fn() };
    const translate = v => v;

    provider = new CustomPaletteProvider(
      { registerProvider: vi.fn() },  // palette
      mockCreate,
      mockElementFactory,
      {},  // spaceTool
      {},  // lassoTool
      {},  // handTool
      {},  // globalConnect
      translate,
      { fire: vi.fn() }  // eventBus
    );
  });

  it('has correct $inject', () => {
    expect(CustomPaletteProvider.$inject).toEqual([
      'palette', 'create', 'elementFactory',
      'spaceTool', 'lassoTool', 'handTool',
      'globalConnect', 'translate', 'eventBus'
    ]);
  });

  it('adds call activity entry', () => {
    const entries = provider.getPaletteEntries();
    expect(entries).toHaveProperty('create.call-activity');
    expect(entries['create.call-activity'].title).toBe('Create CallActivity');
  });

  it('adds event subprocess entry', () => {
    const entries = provider.getPaletteEntries();
    expect(entries).toHaveProperty('create.event-subprocess');
    expect(entries['create.event-subprocess'].title).toBe('Create EventSubProcess');
  });

  it('adds script task entry', () => {
    const entries = provider.getPaletteEntries();
    expect(entries).toHaveProperty('create.script-task');
    expect(entries['create.script-task'].title).toBe('Create ScriptTask');
  });

  it('removes unsupported palette entries', () => {
    const entries = provider.getPaletteEntries();
    expect(entries).not.toHaveProperty('create.data-store');
    expect(entries).not.toHaveProperty('create.task');
    expect(entries).not.toHaveProperty('create.subprocess');
    expect(entries).not.toHaveProperty('create.subprocess-expanded');
    expect(entries).not.toHaveProperty('create.group');
  });

  it('keeps standard tool entries', () => {
    const entries = provider.getPaletteEntries();
    expect(entries).toHaveProperty('hand-tool');
    expect(entries).toHaveProperty('lasso-tool');
    expect(entries).toHaveProperty('space-tool');
    expect(entries).toHaveProperty('global-connect-tool');
  });

  it('each custom entry has click and dragstart actions', () => {
    const entries = provider.getPaletteEntries();
    for (const key of ['create.call-activity', 'create.event-subprocess', 'create.script-task']) {
      expect(typeof entries[key].action.click).toBe('function');
      expect(typeof entries[key].action.dragstart).toBe('function');
    }
  });
});