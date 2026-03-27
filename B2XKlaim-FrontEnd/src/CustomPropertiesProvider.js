// Only keep property groups relevant for B2XKlaim BPMN-to-XKlaim translation.
// All Camunda Platform-specific groups (async continuations, mappings, listeners, etc.)
// are removed since they have no translation equivalent.

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

// Element types that can have extension properties
const EXTENSION_PROPS_TYPES = new Set([
  'bpmn:DataObjectReference',
  'bpmn:IntermediateThrowEvent',
  'bpmn:IntermediateCatchEvent',
  'bpmn:StartEvent',
  'bpmn:EndEvent',
]);

function hasMessageOrSignalDef(element) {
  var bo = element.businessObject || element;
  var eventDefs = bo.eventDefinitions || [];
  return eventDefs.some(function(def) {
    return def.$type === 'bpmn:MessageEventDefinition' || def.$type === 'bpmn:SignalEventDefinition';
  });
}

function shouldShowExtensionProperties(element) {
  var type = element.type || (element.businessObject && element.businessObject.$type);
  if (type === 'bpmn:DataObjectReference') return true;
  if (EXTENSION_PROPS_TYPES.has(type) && hasMessageOrSignalDef(element)) return true;
  return false;
}

export default function CustomPropertiesProvider(propertiesPanel) {
  // Use low priority (200) so this runs AFTER Camunda (500) and BPMN (1000) providers
  // In diagram-js, higher priority = runs first, lower = runs later
  propertiesPanel.registerProvider(200, this);
}

CustomPropertiesProvider.$inject = ['propertiesPanel'];

CustomPropertiesProvider.prototype.getGroups = function(element) {
  return function(groups) {
    return groups
      .filter(function(group) {
        if (group.id === 'CamundaPlatform__ExtensionProperties') {
          return shouldShowExtensionProperties(element);
        }
        return ALLOWED_GROUPS.has(group.id);
      })
      .map(function(group) {
        // Strip Camunda-specific entries from Called element group,
        // keep only the calledElement text field
        if (group.id === 'CamundaPlatform__CallActivity' && group.entries) {
          group.entries = group.entries.filter(function(entry) {
            return entry.id === 'calledElement';
          });
        }
        return group;
      });
  };
};