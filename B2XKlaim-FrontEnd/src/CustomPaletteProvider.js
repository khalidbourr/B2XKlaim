import PaletteProvider from "bpmn-js/lib/features/palette/PaletteProvider";

export default function CustomPaletteProvider(palette, create, elementFactory, spaceTool, lassoTool, handTool, globalConnect, translate) {
    PaletteProvider.call(this, palette, create, elementFactory, spaceTool, lassoTool, handTool, globalConnect, translate);

    console.log("CustomPaletteProvider is being called");

    const actions = this.getPaletteEntries();

    console.log("Original actions:", actions);

    // Modify actions to include only desired elements
    const customActions = {
        'create.start-event': actions['create.start-event'],
        'create.end-event': actions['create.end-event'],
        'create.exclusive-gateway': actions['create.exclusive-gateway'],
        'create.task': actions['create.task'],
        
        // ... keep other desired elements and exclude the unwanted ones
    };

    console.log("Custom actions:", customActions);

    this.getPaletteEntries = function() {
        return customActions;
    };
}

CustomPaletteProvider.$inject = [
    'palette',
    'create',
    'elementFactory',
    'spaceTool',
    'lassoTool',
    'handTool',
    'globalConnect',
    'translate'
];

CustomPaletteProvider.prototype = Object.create(PaletteProvider.prototype);
CustomPaletteProvider.prototype.constructor = CustomPaletteProvider;
