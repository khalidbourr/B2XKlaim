// Adds a "drill down" button to the context pad of Call Activity elements.
// Clicking it opens/creates a sub-process tab linked to the Call Activity.

export default function CallActivityDrilldownProvider(contextPad, eventBus) {
  contextPad.registerProvider(this);
  this._eventBus = eventBus;
}

CallActivityDrilldownProvider.$inject = ['contextPad', 'eventBus'];

CallActivityDrilldownProvider.prototype.getContextPadEntries = function(element) {
  if (element.type !== 'bpmn:CallActivity') {
    return {};
  }

  const eventBus = this._eventBus;

  return {
    'open-call-activity': {
      group: 'edit',
      className: 'bpmn-icon-subprocess-expanded',
      title: 'Open called process',
      action: {
        click: function() {
          eventBus.fire('callActivity.drilldown', { element: element });
        }
      }
    }
  };
};