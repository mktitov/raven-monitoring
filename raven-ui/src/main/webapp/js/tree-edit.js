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

$(document).ready(function() {
  console.log("Document loaded!!!")
  //node type autocomplete
  $(document).delegate("#body\\:typeList", "mouseenter", function() {
    var input = $(this)
    if (!input.is(':data(uiAutocomplete)')) {
      input.autocomplete({ 
        source: function(request, response) {
            console.log("Executing autocomplete")
            $.getJSON('../sri/system/nodes/child-types', {
              filter:request.term,
              nodePath:path
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
//        draggindNode = $(this)
        console.log('Dragging')
        var data = ev.originalEvent.dataTransfer
        data.effectAllowed = "copyMove"
        data.dropEffect = "move"
        data.setDragImage(child[0], 1, 1)
        data.setData('text/plain', getNodePath(child))
        console.log(data)
      })
      child.bind('dragend', function(){
        console.log('drag finished')
//        removeInsertPlaces(dragoverNode)
//        dragoverNode = null
//        draggingNode = null
        return false
      })
    }
  })
  $(document).delegage(selector, 'dragover', function(ev){
    
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
//          $.getJSON('../sri/system/nodes/reorder', {
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

function getNodeName(obj) {
  var td = obj.find('td:nth-child(3)')
  if (td && td.length>0)
    return td.text()
  else
    return null
}

function getNodePath(elem) {
  elem.find('span.node-name').attr('title')
}