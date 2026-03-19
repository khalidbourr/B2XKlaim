import PaletteProvider from 'bpmn-js/lib/features/palette/PaletteProvider';

export default class MyPaletteProvider extends PaletteProvider {
    constructor(palette, create, elementFactory, spaceTool, lassoTool, handTool, globalConnect, translate, eventBus) {
        super(palette, create, elementFactory, spaceTool, lassoTool, handTool, globalConnect, translate);

        this._eventBus = eventBus;
        this._translate = translate;
        this._create = create;
        this._elementFactory = elementFactory;
    }

    getPaletteEntries(element) {
        const actions = super.getPaletteEntries(element);

        const createShape = (type, options) => (event) => {
            const shape = this._elementFactory.createShape({ type, ...options });
            this._create.start(event, shape);
        };

        const callActivity = createShape('bpmn:CallActivity');
        const eventSubProcess = createShape('bpmn:SubProcess', { isExpanded: true, triggeredByEvent: true });
        const scriptTask = createShape('bpmn:ScriptTask');

        actions['create.call-activity'] = {
            group: 'activity',
            className: 'bpmn-icon-call-activity',
            title: this._translate('Create CallActivity'),
            action: { dragstart: callActivity, click: callActivity }
        };

        actions['create.event-subprocess'] = {
            group: 'activity',
            className: 'bpmn-icon-subprocess-expanded',
            title: this._translate('Create EventSubProcess'),
            action: { dragstart: eventSubProcess, click: eventSubProcess }
        };

        actions['create.script-task'] = {
            group: 'activity',
            className: 'bpmn-icon-script-task',
            title: this._translate('Create ScriptTask'),
            action: { dragstart: scriptTask, click: scriptTask }
        };

        delete actions['create.data-store'];
        delete actions['create.data-object'];
        delete actions['create.cancel-event'];
        delete actions['create.escalation-event'];
        delete actions['create.task'];
        delete actions['create.subprocess'];
        delete actions['create.subprocess-expanded'];
        delete actions['create.group'];

        return actions;
    }
}

MyPaletteProvider.$inject = [
    'palette',
    'create',
    'elementFactory',
    'spaceTool',
    'lassoTool',
    'handTool',
    'globalConnect',
    'translate',
    'eventBus'
];
