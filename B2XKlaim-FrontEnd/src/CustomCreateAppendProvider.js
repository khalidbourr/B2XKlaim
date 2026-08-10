// Custom CreateMenuProvider that filters unsupported elements
export function CustomCreateMenuProvider(
    elementFactory, popupMenu, create,
    autoPlace, mouse, translate
) {
  this._elementFactory = elementFactory;
  this._popupMenu = popupMenu;
  this._create = create;
  this._autoPlace = autoPlace;
  this._mouse = mouse;
  this._translate = translate;

  this._popupMenu.registerProvider('bpmn-create', this);
}

CustomCreateMenuProvider.$inject = [
  'elementFactory',
  'popupMenu',
  'create',
  'autoPlace',
  'mouse',
  'translate'
];

CustomCreateMenuProvider.prototype.getPopupMenuEntries = function() {
  const entries = {};
  const translate = this._translate;
  const elementFactory = this._elementFactory;
  const create = this._create;
  const mouse = this._mouse;
  const popupMenu = this._popupMenu;

  // Build supported entries from our whitelist
  const CREATE_OPTIONS = getSupportedCreateOptions();

  CREATE_OPTIONS.forEach(option => {
    const { actionName, className, label, target, description, group, search, rank } = option;

    const createAction = (event) => {
      popupMenu.close();
      let newElement;
      if (target.type === 'bpmn:Participant') {
        newElement = elementFactory.createParticipantShape(target);
      } else {
        newElement = elementFactory.create('shape', target);
      }
      if (event instanceof KeyboardEvent) {
        event = mouse.getLastMoveEvent();
      }
      return create.start(event, newElement);
    };

    entries[`create-${actionName}`] = {
      label: label && translate(label),
      className,
      description,
      group: group && { ...group, name: translate(group.name) },
      search,
      rank,
      action: { click: createAction, dragstart: createAction }
    };
  });

  return entries;
};

// Custom AppendMenuProvider that filters unsupported elements
export function CustomAppendMenuProvider(
    elementFactory, popupMenu, create,
    autoPlace, rules, mouse, translate
) {
  this._elementFactory = elementFactory;
  this._popupMenu = popupMenu;
  this._create = create;
  this._autoPlace = autoPlace;
  this._rules = rules;
  this._mouse = mouse;
  this._translate = translate;

  this._popupMenu.registerProvider('bpmn-append', this);
}

CustomAppendMenuProvider.$inject = [
  'elementFactory',
  'popupMenu',
  'create',
  'autoPlace',
  'rules',
  'mouse',
  'translate'
];

CustomAppendMenuProvider.prototype.getPopupMenuEntries = function(element) {
  const rules = this._rules;
  const translate = this._translate;
  const elementFactory = this._elementFactory;
  const create = this._create;
  const autoPlace = this._autoPlace;
  const mouse = this._mouse;
  const popupMenu = this._popupMenu;

  if (!rules.allowed('shape.append', { element: element })) {
    return [];
  }

  const entries = {};
  const APPEND_OPTIONS = getSupportedCreateOptions().filter(option => {
    const { type } = option.target;
    // Filter out elements that cannot be appended (participants, boundary events)
    if (type === 'bpmn:Participant' || type === 'bpmn:BoundaryEvent') return false;
    return true;
  });

  APPEND_OPTIONS.forEach(option => {
    const { actionName, className, label, target, description, group, search, rank } = option;

    const appendAction = (event) => {
      popupMenu.close();
      const newElement = elementFactory.create('shape', target);
      if (event instanceof KeyboardEvent) {
        event = mouse.getLastMoveEvent();
      }
      if (autoPlace) {
        return autoPlace.append(element, newElement);
      }
      return create.start(event, newElement, { source: element });
    };

    entries[`append-${actionName}`] = {
      label: label && translate(label),
      className,
      description,
      group: group && { ...group, name: translate(group.name) },
      search,
      rank,
      action: { click: appendAction, dragstart: (event) => {
        popupMenu.close();
        const newElement = elementFactory.create('shape', target);
        return create.start(event, newElement, { source: element });
      }}
    };
  });

  return entries;
};

// Define only the supported BPMN element options
function getSupportedCreateOptions() {
  const EVENT_GROUP = { id: 'events', name: 'Events' };
  const TASK_GROUP = { id: 'tasks', name: 'Tasks' };
  const PARTICIPANT_GROUP = { id: 'participants', name: 'Participants' };
  const SUBPROCESS_GROUP = { id: 'subprocess', name: 'Sub-processes' };
  const GATEWAY_GROUP = { id: 'gateways', name: 'Gateways' };

  return [
    // Gateways
    { label: 'Exclusive gateway', actionName: 'exclusive-gateway', className: 'bpmn-icon-gateway-xor', target: { type: 'bpmn:ExclusiveGateway' }, group: GATEWAY_GROUP },
    { label: 'Parallel gateway', actionName: 'parallel-gateway', className: 'bpmn-icon-gateway-parallel', target: { type: 'bpmn:ParallelGateway' }, group: GATEWAY_GROUP },
    { label: 'Event-based gateway', actionName: 'event-based-gateway', className: 'bpmn-icon-gateway-eventbased', target: { type: 'bpmn:EventBasedGateway', instantiate: false, eventGatewayType: 'Exclusive' }, group: GATEWAY_GROUP },

    // Tasks
    { label: 'Script task', actionName: 'script-task', className: 'bpmn-icon-script', target: { type: 'bpmn:ScriptTask' }, group: TASK_GROUP },

    // Sub-processes
    { label: 'Call activity', actionName: 'call-activity', className: 'bpmn-icon-call-activity', target: { type: 'bpmn:CallActivity' }, group: SUBPROCESS_GROUP },
    { label: 'Event sub-process', search: 'subprocess', actionName: 'event-subprocess', className: 'bpmn-icon-event-subprocess-expanded', target: { type: 'bpmn:SubProcess', triggeredByEvent: true, isExpanded: true }, group: SUBPROCESS_GROUP },

    // None events
    { label: 'Start event', actionName: 'none-start-event', className: 'bpmn-icon-start-event-none', target: { type: 'bpmn:StartEvent' }, group: EVENT_GROUP },
    { label: 'Intermediate throw event', actionName: 'none-intermediate-throwing', className: 'bpmn-icon-intermediate-event-none', target: { type: 'bpmn:IntermediateThrowEvent' }, group: EVENT_GROUP },
    { label: 'End event', actionName: 'none-end-event', className: 'bpmn-icon-end-event-none', target: { type: 'bpmn:EndEvent' }, group: EVENT_GROUP },

    // Typed start events
    { label: 'Message start event', actionName: 'message-start', className: 'bpmn-icon-start-event-message', target: { type: 'bpmn:StartEvent', eventDefinitionType: 'bpmn:MessageEventDefinition' }, group: EVENT_GROUP },
    { label: 'Timer start event', actionName: 'timer-start', className: 'bpmn-icon-start-event-timer', target: { type: 'bpmn:StartEvent', eventDefinitionType: 'bpmn:TimerEventDefinition' }, group: EVENT_GROUP },
    { label: 'Signal start event', actionName: 'signal-start', className: 'bpmn-icon-start-event-signal', target: { type: 'bpmn:StartEvent', eventDefinitionType: 'bpmn:SignalEventDefinition' }, group: EVENT_GROUP },

    // Typed intermediate events
    { label: 'Message intermediate catch event', actionName: 'message-intermediate-catch', className: 'bpmn-icon-intermediate-event-catch-message', target: { type: 'bpmn:IntermediateCatchEvent', eventDefinitionType: 'bpmn:MessageEventDefinition' }, group: EVENT_GROUP },
    { label: 'Message intermediate throw event', actionName: 'message-intermediate-throw', className: 'bpmn-icon-intermediate-event-throw-message', target: { type: 'bpmn:IntermediateThrowEvent', eventDefinitionType: 'bpmn:MessageEventDefinition' }, group: EVENT_GROUP },
    { label: 'Timer intermediate catch event', actionName: 'timer-intermediate-catch', className: 'bpmn-icon-intermediate-event-catch-timer', target: { type: 'bpmn:IntermediateCatchEvent', eventDefinitionType: 'bpmn:TimerEventDefinition' }, group: EVENT_GROUP },
    { label: 'Signal intermediate catch event', actionName: 'signal-intermediate-catch', className: 'bpmn-icon-intermediate-event-catch-signal', target: { type: 'bpmn:IntermediateCatchEvent', eventDefinitionType: 'bpmn:SignalEventDefinition' }, group: EVENT_GROUP },
    { label: 'Signal intermediate throw event', actionName: 'signal-intermediate-throw', className: 'bpmn-icon-intermediate-event-throw-signal', target: { type: 'bpmn:IntermediateThrowEvent', eventDefinitionType: 'bpmn:SignalEventDefinition' }, group: EVENT_GROUP },

    // Typed end events
    { label: 'Message end event', actionName: 'message-end', className: 'bpmn-icon-end-event-message', target: { type: 'bpmn:EndEvent', eventDefinitionType: 'bpmn:MessageEventDefinition' }, group: EVENT_GROUP },
    { label: 'Signal end event', actionName: 'signal-end', className: 'bpmn-icon-end-event-signal', target: { type: 'bpmn:EndEvent', eventDefinitionType: 'bpmn:SignalEventDefinition' }, group: EVENT_GROUP },
    { label: 'Terminate end event', actionName: 'terminate-end', className: 'bpmn-icon-end-event-terminate', target: { type: 'bpmn:EndEvent', eventDefinitionType: 'bpmn:TerminateEventDefinition' }, group: EVENT_GROUP },

    // Data
    { label: 'Data object', actionName: 'data-object', className: 'bpmn-icon-data-object', target: { type: 'bpmn:DataObjectReference' }, group: { id: 'data', name: 'Data' } },

    // Participants
    { label: 'Expanded pool/participant', search: 'Non-empty pool/participant', actionName: 'expanded-pool', className: 'bpmn-icon-participant', target: { type: 'bpmn:Participant', isExpanded: true }, group: PARTICIPANT_GROUP },
    { label: 'Empty pool/participant', search: 'Collapsed pool/participant', actionName: 'collapsed-pool', className: 'bpmn-icon-lane', target: { type: 'bpmn:Participant', isExpanded: false }, group: PARTICIPANT_GROUP },
  ];
}