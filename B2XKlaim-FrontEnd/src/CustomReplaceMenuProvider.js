import ReplaceMenuProvider from 'bpmn-js/lib/features/popup-menu/ReplaceMenuProvider';

import { is } from 'bpmn-js/lib/util/ModelUtil';

// Supported action names based on what the backend can translate
const SUPPORTED_ACTIONS = new Set([
  // Start events
  'replace-with-none-start',
  'replace-with-message-start',
  'replace-with-timer-start',
  'replace-with-signal-start',

  // Intermediate events
  'replace-with-none-intermediate-throwing',
  'replace-with-none-intermediate-throw',
  'replace-with-message-intermediate-catch',
  'replace-with-message-intermediate-throw',
  'replace-with-timer-intermediate-catch',
  'replace-with-signal-intermediate-catch',
  'replace-with-signal-intermediate-throw',

  // End events
  'replace-with-none-end',
  'replace-with-message-end',
  'replace-with-signal-end',
  'replace-with-terminate-end',

  // Gateways
  'replace-with-exclusive-gateway',
  'replace-with-parallel-gateway',
  'replace-with-event-based-gateway',

  // Tasks/Activities
  'replace-with-script-task',
  'replace-with-call-activity',

  // Event sub-process
  'replace-with-event-subprocess',

  // Sequence flows
  'replace-with-sequence-flow',
  'replace-with-default-flow',
  'replace-with-conditional-flow',

  // Participant
  'replace-with-expanded-pool',
  'replace-with-collapsed-pool',

  // Event sub-process start events (interrupting only for supported types)
  'replace-with-non-interrupting-message-start',
  'replace-with-non-interrupting-timer-start',
  'replace-with-non-interrupting-signal-start',
]);

export default function CustomReplaceMenuProvider(
    bpmnFactory, popupMenu, modeling, moddle,
    bpmnReplace, rules, translate, moddleCopy) {

  ReplaceMenuProvider.call(this,
    bpmnFactory, popupMenu, modeling, moddle,
    bpmnReplace, rules, translate, moddleCopy);

  this._bpmnFactory = bpmnFactory;
  this._modeling = modeling;
}

CustomReplaceMenuProvider.$inject = [
  'bpmnFactory',
  'popupMenu',
  'modeling',
  'moddle',
  'bpmnReplace',
  'rules',
  'translate',
  'moddleCopy'
];

// Inherit from ReplaceMenuProvider
CustomReplaceMenuProvider.prototype = Object.create(ReplaceMenuProvider.prototype);
CustomReplaceMenuProvider.prototype.constructor = CustomReplaceMenuProvider;

// Override getPopupMenuEntries to filter unsupported elements
const originalGetEntries = ReplaceMenuProvider.prototype.getPopupMenuEntries;

CustomReplaceMenuProvider.prototype.getPopupMenuEntries = function(target) {
  const entries = originalGetEntries.call(this, target);

  // Filter out unsupported entries (keys are actionNames)
  const filtered = {};
  for (const [key, entry] of Object.entries(entries)) {
    if (SUPPORTED_ACTIONS.has(key)) {
      filtered[key] = entry;
    }
  }

  if (is(target, 'bpmn:DataObjectReference')) {
    var bo = target.businessObject;
    var ext = bo.get('extensionElements');
    var isDataInput = ext && ext.get('values') && ext.get('values').some(function(v) { return v.$type === 'b2x:DataInput'; });

    if (isDataInput) {
      filtered['replace-with-data-object'] = {
        label: 'Data object',
        className: 'bpmn-icon-data-object',
        action: function() {
          var values = ext.get('values').filter(function(v) { return v.$type !== 'b2x:DataInput'; });
          var props = { id: target.id.replace(/^DataInputReference_/, 'DataObjectReference_') };
          props.extensionElements = values.length ? this._bpmnFactory.create('bpmn:ExtensionElements', { values: values }) : undefined;
          this._modeling.updateProperties(target, props);
        }.bind(this)
      };
    } else {
      filtered['replace-with-data-input'] = {
        label: 'Data input',
        className: 'bpmn-icon-data-input',
        action: function() {
          var values = ext ? ext.get('values') : [];
          var dataInput = this._bpmnFactory.create('b2x:DataInput');
          var newExt = this._bpmnFactory.create('bpmn:ExtensionElements', { values: values.concat([dataInput]) });
          this._modeling.updateProperties(target, {
            id: target.id.replace(/^DataObjectReference_/, 'DataInputReference_'),
            extensionElements: newExt
          });
        }.bind(this)
      };
    }
  }

  return filtered;
};