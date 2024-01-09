import PropertiesActivator from 'bpmn-js-properties-panel/lib/PropertiesActivator';

// Define groups
function createGeneralGroup(element, translate) {
// Create an array to hold the properties
    var generalGroup = {
        label: translate('General'),
        entries: []
    };

    // Name property
    generalGroup.entries.push(entryFactory.textBox(translate, {
        id: 'name',
        description: translate('The name of the element'),
        label: translate('Name'),
        modelProperty: 'name'
    }));

    // ID property
    generalGroup.entries.push(entryFactory.textBox(translate, {
        id: 'id',
        description: translate('The ID of the element'),
        label: translate('ID'),
        modelProperty: 'id',
        disabled: function() {
            // Disable editing of the ID
            return true;
        }
    }));

    return generalGroup;
}

export default class CustomPropertiesProvider extends PropertiesActivator {
    constructor(eventBus, translate) {
        super(eventBus);

        this.getTabs = function(element) {
            const generalGroup = createGeneralGroup(element, translate);

            return [
                {
                    id: 'general',
                    label: translate('General'),
                    groups: [generalGroup]
                }
            ];
        };
    }
}

CustomPropertiesProvider.$inject = ['eventBus', 'translate'];