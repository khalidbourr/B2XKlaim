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


        // Define the Call Activity creation function
        const createCallActivity = (event) => {
            const shape = this._elementFactory.createShape({ type: 'bpmn:CallActivity' });
            this._create.start(event, shape);
        };



        // Define the Event Subprocess creation function
        const createEventSubProcess = (event) => {
            const shape = this._elementFactory.createShape({ type: 'bpmn:SubProcess', isExpanded: true, triggeredByEvent: true });
            this._create.start(event, shape);
        };

        // Define the Script Task creation function
        const createScriptTask = (event) => {
            const shape = this._elementFactory.createShape({ type: 'bpmn:ScriptTask' });
            this._create.start(event, shape);
        };

        // Add the Call Activity to the palette entries
        actions['create.call-activity'] = {
            group: 'activity',
            className: 'bpmn-icon-call-activity',
            title: this._translate('Create CallActivity'),
            action: {
                dragstart: createCallActivity,
                click: createCallActivity
            }
        };

        // Add the Event Subprocess to the palette entries
        actions['create.event-subprocess'] = {
            group: 'activity',
            className: 'bpmn-icon-subprocess-expanded', // Use the appropriate icon class
            title: this._translate('Create EventSubProcess'),
            action: {
                dragstart: createEventSubProcess,
                click: createEventSubProcess
            }
        };

        // Add the Script Task to the palette entries
        actions['create.script-task'] = {
            group: 'activity',
            className: 'bpmn-icon-script-task',
            title: this._translate('Create ScriptTask'),
            action: {
                dragstart: createScriptTask,
                click: createScriptTask
            }
        };

        // Remove undesired entries
        delete actions['create.data-store'];
        delete actions['create.cancel-event'];
        delete actions['create.escalation-event'];
        delete actions['create.task'];
        delete actions['create.subprocess'];
        delete actions['create.subprocess-expanded'];

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
