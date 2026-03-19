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

export default function CustomPropertiesProvider(propertiesPanel) {
  // Use low priority (200) so this runs AFTER Camunda (500) and BPMN (1000) providers
  // In diagram-js, higher priority = runs first, lower = runs later
  propertiesPanel.registerProvider(200, this);
}

CustomPropertiesProvider.$inject = ['propertiesPanel'];

CustomPropertiesProvider.prototype.getGroups = function() {
  return function(groups) {
    return groups.filter(function(group) {
      return ALLOWED_GROUPS.has(group.id);
    });
  };
};