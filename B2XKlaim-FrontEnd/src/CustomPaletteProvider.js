import inherits from 'inherits';
import PaletteProvider from 'camunda-bpmn-js/lib/features/palette/PaletteProvider';

function CustomPaletteProvider(palette) {
    PaletteProvider.call(this, palette);
}

inherits(CustomPaletteProvider, PaletteProvider);

CustomPaletteProvider.prototype.getPaletteEntries = function(element) {
    var entries = PaletteProvider.prototype.getPaletteEntries.apply(this, [element]);
    delete entries['create.task'];
    delete entries['create.data-store'];
    // ... other deletions ...
    return entries;
};

export default CustomPaletteProvider;
