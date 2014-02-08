/* 
 * Copyright 2014 Mikhail Titov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var dragoverRow;
var draggingRow;
var dragoverEvent = null;

$(document).ready(function() {
  console.log("Document loaded!!!")
  //node type autocomplete
//  $(document).delegate("#body\\:typeList", "mouseenter", function() {
  $(document).delegate("tr.node-type-input input", "mouseenter focus", function() {
    var input = $(this)
    if (!input.is(':data(uiAutocomplete)')) {
      input.autocomplete({ 
        source: function(request, response) {
            console.log("Executing autocomplete")
            $.getJSON('../projects/system/nodes/child-types', {
              filter:request.term,
              nodePath:nodePath
            }, response)
        }
      })
    }
  })
  //
  var selector = "div.child-nodes table[summary='Nodes'] tr:has(td)"
  $(document).delegate(selector, 'mouseenter', function() {
    var child = $(this)
    if (child.attr('draggable')!=='true') {
      console.log('making draggable child node row')
      child.attr("draggable", 'true')
      child.bind('dragstart', function(ev){
        draggingRow = $(this)
        console.log('Dragging')
        var data = ev.originalEvent.dataTransfer
        var checkedNodes = child.parent().find("tr:has(td):has(input:checked)").map(function(i, r){
          return getNodePath($(r))
        })
        checkedNodes = checkedNodes.length===0? [getNodePath(child)] : checkedNodes.toArray()
        console.log("SELECTED rows count: "+checkedNodes.length)
        console.log(checkedNodes)
//        data.effectAllowed = "copyMove"
//        data.dropEffect = "move"
        data.setDragImage(child[0], 1, 1)
        data.setData('text/plain', checkedNodes.join(' / '))
        console.log(data)
      })
      child.bind('dragend', function(ev) {        
        try {
          console.log('drag finished')
          console.log(ev.originalEvent.dataTransfer)
          removeInsertPlaces()
        } finally {
          draggingRow = null
        }
//        dragoverNode = null
//        draggingNode = null
        return false
      })
    }
  })
  $(document).delegate(selector, 'dragover', function(ev) {
    var row = $(this)
    if (!row.prev().is('tr.insert-node-place')) {
      dragoverEvent = ev;
      removeInsertPlaces()
      var template = "<tr class='insert-node-place'><th colspan=5 class='insert-node-place'></th></tr>"
      dragoverRow = row
      var before = $(template).insertBefore(row)
      configureInsertPlace(before, getNodePath(row), false)
      var after = $(template).insertAfter(row)
      configureInsertPlace(after, getNodePath(row), true)
//      if (getNodePath(row)!==getNodePath(draggingRow))
//        row.addClass('dragover')
    }
  })
  $(document).delegate(selector, 'dragleave', function(ev) {
    $(this).removeClass('dragover')
  })
  
//  var selector = "table[summary='Nodes'] tbody"
//  $(selector).parents('fieldset').delegate(selector, 'mouseenter', function() {
//    var table = $(this)
//    if (!table.is(':data(draggable)'))
//      table.sortable({
//        items: 'tr:has(td)',
//        stop: function(e, u) {
//          item = u.item
//          var movedNodeName = getNodeName(u.item)
//          var insertBefore = true
//          var targetNodeName = getNodeName(u.item.next())
//          if (!targetNodeName) {
//            insertBefore = false
//            targetNodeName = getNodeName(u.item.prev())
//          }
//          console.log("moved node name: " + movedNodeName + "; target node: " + targetNodeName + "; insertBefore: " + insertBefore)
//          $.getJSON('../projects/system/nodes/reorder', {
//            type: 'POST',
//            nodePath: path,
//            movedNodeName: movedNodeName,
//            targetNodeName: targetNodeName,
//            insertBefore: insertBefore
//          })
//        }
//      })
//  })
})
//function removeInsertPlaces(row) {
//  if (row) 
//    row.parent().find('tr.insert-node-place').remove()
//}

function getNodeName(elem) {
  var td = elem.find('td:nth-child(3)')
  if (td && td.length>0)
    return td.text()
  else
    return null
}

function getNodePath(elem) {  
  return elem.find('span.node-name').attr('title')
}

function configureInsertPlace(insElem, nodePath, after) {
  console.log("nodePath: "+nodePath)
  insElem.bind('dragover', function(ev) {
    ev.originalEvent.preventDefault()
    console.log('dragover insert place')
    $(this).addClass('dragover')
  })
  insElem.bind('dragleave', function(ev) {
    $(this).removeClass('dragover')
  })
  insElem.bind('drop', function(ev){
    ev.originalEvent.preventDefault()
    console.log('Dropped into insert place')
    insElem.removeClass('dragover')    
    var sourceNodePath = getNodePathFromData(ev)
    var targetNodePath = getParentNodePath(nodePath)
    var positionNodePath = nodePath        
//    transferNode(sourceNodePath, targetNodePath, getDropEffect(ev)==='move', ev.shiftKey, positionNodePath, after)
    transferNode(sourceNodePath, targetNodePath, getDropEffect(dragoverEvent)==='move', dragoverEvent.shiftKey, positionNodePath, after)
    dragoverEvent = null
  })
}

//function getDropEffect(ev) {
//  var dropEffect = ev.originalEvent.dataTransfer.dropEffect
//  if (!dropEffect || dropEffect==='none') 
//    dropEffect = ev.altKey? 'copy' : 'move'
//  return dropEffect  
//}
