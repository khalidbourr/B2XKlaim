

import { TextFieldEntry, ListGroup } from '@bpmn-io/properties-panel';
import { useService } from 'bpmn-js-properties-panel';

const ALLOWED_GROUPS = new Set([
  // Standard BPMN groups
  'general',                              // Name, ID
  'multiInstance',                         // Multi-instance (loop) configuration
  'timer',                                 // Timer event definitions
  'message',                               // Message event definitions
  'signal',                                // Signal event definitions
  'error',                                 // Error definitions
  'escalation',                            // Escalation definitions

  // Camunda groups relevant for translation
  'CamundaPlatform__CallActivity',         // Called element reference
  'CamundaPlatform__Condition',            // Sequence flow conditions (XOR gateway)
  'CamundaPlatform__Script',               // Script task content
]);

const SIGNAL_EXT_TYPES = new Set([
  'bpmn:IntermediateThrowEvent',
  'bpmn:IntermediateCatchEvent',
  'bpmn:StartEvent',
  'bpmn:EndEvent',
]);

function hasSignalDef(element) {
  var bo = element.businessObject || element;
  var eventDefs = bo.eventDefinitions || [];
  return eventDefs.some(function(def) { return def.$type === 'bpmn:SignalEventDefinition'; });
}

function hasMessageDef(element) {
  var bo = element.businessObject || element;
  var eventDefs = bo.eventDefinitions || [];
  return eventDefs.some(function(def) { return def.$type === 'bpmn:MessageEventDefinition'; });
}

function shouldShowExtensionProperties(element) {
  var type = element.type || (element.businessObject && element.businessObject.$type);
  return SIGNAL_EXT_TYPES.has(type) && hasSignalDef(element);
}

function isDataObject(element) {
  var type = element.type || (element.businessObject && element.businessObject.$type);
  return type === 'bpmn:DataObjectReference' && !hasDataInputExtension(element);
}

function hasDataInputExtension(element) {
  var bo = element.businessObject || element;
  var ext = bo.get('extensionElements');
  if (!ext || !ext.get('values')) return false;
  return ext.get('values').some(function(v) { return v.$type === 'b2x:DataInput'; });
}

function isDataInput(element) {
  var type = element.type || (element.businessObject && element.businessObject.$type);
  return type === 'bpmn:DataObjectReference' && hasDataInputExtension(element);
}

function isMessageCatchEvent(element) {
  var type = element.type || (element.businessObject && element.businessObject.$type);
  return hasMessageDef(element)
      && (type === 'bpmn:IntermediateCatchEvent' || type === 'bpmn:StartEvent');
}

function isMessageThrowEvent(element) {
  var type = element.type || (element.businessObject && element.businessObject.$type);
  return hasMessageDef(element)
      && (type === 'bpmn:IntermediateThrowEvent' || type === 'bpmn:EndEvent');
}

// ---- Data Object fields (b2x:Field) ----

function getB2xFields(businessObject) {
  var ext = businessObject.get('extensionElements');
  if (!ext) return [];
  return ext.get('values').filter(function(v) { return v.$type === 'b2x:Field'; });
}

function FieldNameEntry(props) {
  const { idPrefix, element, field } = props;
  const commandStack = useService('commandStack');
  const debounce = useService('debounceInput');
  return TextFieldEntry({
    element: field,
    id: idPrefix + '-name',
    label: 'Name',
    getValue: function() { return field.get('name') || ''; },
    setValue: function(value) {
      commandStack.execute('element.updateModdleProperties', {
        element: element,
        moddleElement: field,
        properties: { name: value }
      });
    },
    debounce: debounce
  });
}

function FieldValueEntry(props) {
  const { idPrefix, element, field } = props;
  const commandStack = useService('commandStack');
  const debounce = useService('debounceInput');
  return TextFieldEntry({
    element: field,
    id: idPrefix + '-value',
    label: 'Value',
    getValue: function() { return field.get('value') || ''; },
    setValue: function(value) {
      commandStack.execute('element.updateModdleProperties', {
        element: element,
        moddleElement: field,
        properties: { value: value || undefined }
      });
    },
    debounce: debounce
  });
}

function FieldTypeEntry(props) {
  const { idPrefix, element, field } = props;
  const commandStack = useService('commandStack');
  const debounce = useService('debounceInput');
  return TextFieldEntry({
    element: field,
    id: idPrefix + '-type',
    label: 'Type',
    getValue: function() { return field.get('type') || ''; },
    setValue: function(value) {
      commandStack.execute('element.updateModdleProperties', {
        element: element,
        moddleElement: field,
        properties: { type: value }
      });
    },
    debounce: debounce
  });
}

function FieldTypeFromValueEntry(props) {
  const { idPrefix, element, field } = props;
  const commandStack = useService('commandStack');
  const debounce = useService('debounceInput');
  return TextFieldEntry({
    element: field,
    id: idPrefix + '-type',
    label: 'Type',
    getValue: function() { return field.get('value') || ''; },
    setValue: function(value) {
      commandStack.execute('element.updateModdleProperties', {
        element: element,
        moddleElement: field,
        properties: { value: value || undefined }
      });
    },
    debounce: debounce
  });
}

function dataObjectFieldEntries({ idPrefix, element, field }) {
  return [
    { id: idPrefix + '-name',  component: FieldNameEntry,       idPrefix, element, field },
    { id: idPrefix + '-type',  component: FieldTypeFromValueEntry, idPrefix, element, field }
  ];
}

function dataInputFieldEntries({ idPrefix, element, field }) {
  return [
    { id: idPrefix + '-name',  component: FieldNameEntry,    idPrefix, element, field },
    { id: idPrefix + '-value', component: FieldValueEntry,   idPrefix, element, field },
    { id: idPrefix + '-type',  component: FieldTypeEntry,    idPrefix, element, field }
  ];
}

function addFieldFactory({ element, bpmnFactory, commandStack }) {
  return function(event) {
    event.stopPropagation();
    const bo = element.businessObject;
    const commands = [];

    let ext = bo.get('extensionElements');
    if (!ext) {
      ext = bpmnFactory.create('bpmn:ExtensionElements', { values: [] });
      ext.$parent = bo;
      commands.push({
        cmd: 'element.updateModdleProperties',
        context: { element, moddleElement: bo, properties: { extensionElements: ext } }
      });
    }

    const field = bpmnFactory.create('b2x:Field', { name: '', type: '' });
    field.$parent = ext;
    commands.push({
      cmd: 'element.updateModdleProperties',
      context: {
        element,
        moddleElement: ext,
        properties: { values: [...ext.get('values'), field] }
      }
    });

    commandStack.execute('properties-panel.multi-command-executor', commands);
  };
}

function removeFieldFactory({ element, field, commandStack }) {
  return function(event) {
    event.stopPropagation();
    const ext = element.businessObject.get('extensionElements');
    if (!ext) return;
    const values = ext.get('values').filter(function(v) { return v !== field; });
    commandStack.execute('element.updateModdleProperties', {
      element, moddleElement: ext, properties: { values }
    });
  };
}

function DataObjectFieldsGroup(element, injector, kind) {
  const bpmnFactory = injector.get('bpmnFactory');
  const commandStack = injector.get('commandStack');
  const fields = getB2xFields(element.businessObject);
  var entriesFn = kind === 'input' ? dataInputFieldEntries : dataObjectFieldEntries;

  const items = fields.map(function(field, index) {
    const id = element.id + '-b2xField-' + index;
    return {
      id,
      label: field.get('name') || '<unnamed field>',
      entries: entriesFn({ idPrefix: id, element, field }),
      autoFocusEntry: id + '-name',
      remove: removeFieldFactory({ element, field, commandStack })
    };
  });

  return {
    id: 'B2XKlaim__DataObjectFields',
    label: 'Extension properties',
    component: ListGroup,
    items,
    add: addFieldFactory({ element, bpmnFactory, commandStack })
  };
}


function CatchPayloadNameEntry(props) {
  const { idPrefix, element, field } = props;
  const commandStack = useService('commandStack');
  const debounce = useService('debounceInput');
  return TextFieldEntry({
    element: field,
    id: idPrefix + '-name',
    label: 'Name',
    getValue: function() { return field.get('name') || ''; },
    setValue: function(value) {
      commandStack.execute('element.updateModdleProperties', {
        element, moddleElement: field, properties: { name: value }
      });
    },
    debounce
  });
}

function ThrowPayloadNameEntry(props) {
  const { idPrefix, element, field } = props;
  const commandStack = useService('commandStack');
  const debounce = useService('debounceInput');
  return TextFieldEntry({
    element: field,
    id: idPrefix + '-name',
    label: 'Name',
    getValue: function() { return field.get('name') || ''; },
    setValue: function(value) {
      commandStack.execute('element.updateModdleProperties', {
        element, moddleElement: field, properties: { name: value }
      });
    },
    debounce
  });
}

function catchPayloadEntries({ idPrefix, element, field }) {
  return [
    { id: idPrefix + '-name', component: CatchPayloadNameEntry, idPrefix, element, field },
    { id: idPrefix + '-type', component: FieldTypeEntry,        idPrefix, element, field }
  ];
}

function throwPayloadEntries({ idPrefix, element, field }) {
  return [
    { id: idPrefix + '-name', component: ThrowPayloadNameEntry, idPrefix, element, field }
  ];
}

function MessagePayloadGroup(element, injector, kind) {
  const bpmnFactory = injector.get('bpmnFactory');
  const commandStack = injector.get('commandStack');
  const fields = getB2xFields(element.businessObject);
  const entriesFor = kind === 'catch' ? catchPayloadEntries : throwPayloadEntries;

  const items = fields.map(function(field, index) {
    const id = element.id + '-b2xPayload-' + index;
    return {
      id,
      label: field.get('name') || '<unnamed>',
      entries: entriesFor({ idPrefix: id, element, field }),
      autoFocusEntry: id + '-name',
      remove: removeFieldFactory({ element, field, commandStack })
    };
  });

  return {
    id: 'B2XKlaim__MessagePayload',
    label: 'Extension properties',
    component: ListGroup,
    items,
    add: addFieldFactory({ element, bpmnFactory, commandStack })
  };
}

// ---- Provider ----

export default function CustomPropertiesProvider(propertiesPanel, injector) {
  this._injector = injector;
  propertiesPanel.registerProvider(200, this);
}

CustomPropertiesProvider.$inject = ['propertiesPanel', 'injector'];

CustomPropertiesProvider.prototype.getGroups = function(element) {
  const injector = this._injector;
  return function(groups) {
    const filtered = groups
      .filter(function(group) {
        if (group.id === 'CamundaPlatform__ExtensionProperties') {
          return shouldShowExtensionProperties(element);
        }
        return ALLOWED_GROUPS.has(group.id);
      })
      .map(function(group) {
        if (group.id === 'CamundaPlatform__CallActivity' && group.entries) {
          group.entries = group.entries.filter(function(entry) {
            return entry.id === 'calledElement';
          });
        }
        return group;
      });

    if (isDataObject(element)) {
      filtered.push(DataObjectFieldsGroup(element, injector, 'object'));
    } else if (isDataInput(element)) {
      filtered.push(DataObjectFieldsGroup(element, injector, 'input'));
    } else if (isMessageCatchEvent(element)) {
      filtered.push(MessagePayloadGroup(element, injector, 'catch'));
    } else if (isMessageThrowEvent(element)) {
      filtered.push(MessagePayloadGroup(element, injector, 'throw'));
    }

    return filtered;
  };
};