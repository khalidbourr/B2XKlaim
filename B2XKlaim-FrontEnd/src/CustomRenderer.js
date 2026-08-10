import BaseRenderer from 'diagram-js/lib/draw/BaseRenderer';
import { is } from 'bpmn-js/lib/util/ModelUtil';
import { isLabel } from 'diagram-js/lib/util/ModelUtil';
import { append as svgAppend, create as svgCreate } from 'tiny-svg';
import inherits from 'inherits-browser';

var BLACK = '#000000';
var WHITE = '#FFFFFF';

function hasDataInputExtension(bo) {
  if (!bo) return false;
  var ext = bo.get('extensionElements');
  if (!ext || !ext.get('values')) return false;
  return ext.get('values').some(function(v) { return v.$type === 'b2x:DataInput'; });
}

export default function CustomRenderer(eventBus, modeling, bpmnFactory, pathMap) {
  BaseRenderer.call(this, eventBus, 1500);

  this._pathMap = pathMap;

  eventBus.on('commandStack.shape.create.postExecuted', function(e) {
    var element = e.context.shape;
    if (!element.__dataInput) return;
    delete element.__dataInput;
    var bo = element.businessObject;
    var existingExt = bo.get('extensionElements');
    var values = existingExt ? existingExt.get('values') : [];
    var dataInput = bpmnFactory.create('b2x:DataInput');
    var newValues = values.concat([dataInput]);
    var ext = bpmnFactory.create('bpmn:ExtensionElements', { values: newValues });
    modeling.updateProperties(element, {
      extensionElements: ext,
      id: element.id.replace(/^DataObjectReference_/, 'DataInputReference_')
    });
  });
}

CustomRenderer.$inject = ['eventBus', 'modeling', 'bpmnFactory', 'pathMap'];

inherits(CustomRenderer, BaseRenderer);

CustomRenderer.prototype.canRender = function(element) {
  if (isLabel(element)) return false;
  if (!is(element, 'bpmn:DataObjectReference')) return false;
  return hasDataInputExtension(element.businessObject) || element.__dataInput;
};

CustomRenderer.prototype.drawShape = function(parentGfx, element) {
  return drawDataInputShape(parentGfx, element, this._pathMap);
};

CustomRenderer.prototype.getShapePath = function(element) {
  var width = element.width || 50;
  var height = element.height || 62;
  var fold = 10;
  return [
    'M 0,0',
    'L ' + (width - fold) + ',0',
    'L ' + width + ',' + fold,
    'L ' + width + ',' + height,
    'L 0,' + height,
    'Z'
  ].join(' ');
};

function drawDataInputShape(parentGfx, element, pathMap) {
  var shapeG = svgCreate('g');
  svgAppend(parentGfx, shapeG);

  var docPathData = pathMap.getScaledPath('DATA_OBJECT_PATH', {
    xScaleFactor: 1,
    yScaleFactor: 1,
    containerWidth: element.width,
    containerHeight: element.height,
    position: {
      mx: 0.474,
      my: 0.296
    }
  });

  var docPath = svgCreate('path', {
    d: docPathData,
    fill: WHITE,
    fillOpacity: 1,
    stroke: BLACK,
    strokeWidth: 2,
    strokeLinecap: 'round',
    strokeLinejoin: 'round'
  });
  svgAppend(shapeG, docPath);

  var arrowData = pathMap.getRawPath('DATA_ARROW');
  var arrow = svgCreate('path', {
    d: arrowData,
    fill: 'none',
    stroke: BLACK,
    strokeWidth: 1
  });
  svgAppend(shapeG, arrow);

  return shapeG;
}
