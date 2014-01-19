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
var draggingNode;
var dragoverNode;

$(document).ready(function(){
  $(document).delegate("table.tree-node", "mouseenter", function() {
    var table = $(this)
    if (table.attr('draggable')!=='true') {
      console.log('making draggable')
      table.attr("draggable", 'true')
      table.bind('dragstart', function(ev){
        draggindNode = $(this)
        console.log('Dragging')
        console.log(draggindNode)
        var data = ev.originalEvent.dataTransfer
        data.effectAllowed = "copyMove"
        data.dropEffect = "move"
        data.setDragImage(table[0], 1, 1)
        data.setData('text/plain', getNodePath(table))
        console.log(data)
      })
      table.bind('dragend', function(){
        console.log('drag finished')
        removeInsertPlaces(dragoverNode)
        dragoverNode = null
        draggingNode = null
        return false
      })
    }
  })
  
  $(document).delegate("table.tree-node", "dragover", function(ev) {
    var node = $(this)
    if (!node.prev().is('div')) {
      removeInsertPlaces(dragoverNode)
      dragoverNode = node
      console.log('inserting div')
      var template = "<div class='insert-node-place'></div>"
      var nodePath = getNodePath(node)
      var before = $(template).insertBefore(node)
      configureInsertPlace(before, nodePath, false)
      var after = $(template).insertAfter(node)
      configureInsertPlace(after, nodePath, true)
    }
    if (getNodePath(node)!==getNodePath(draggindNode)) {
      ev.originalEvent.preventDefault()
//      console.log('drop effect: '+ev.originalEvent.dataTransfer.dropEffect)
      node.addClass('dragover')
    }
  })
  
  $(document).delegate("table.tree-node", "dragleave", function() {
    $(this).removeClass('dragover')
  })

  $(document).delegate("table.tree-node", "drop", function(ev) {
    ev.originalEvent.preventDefault()
    $(this).removeClass('dragover')
    var dropEffect = ev.originalEvent.dataTransfer.dropEffect
    if (!dropEffect || dropEffect==='none') 
      dropEffect = ev.altKey? 'copy' : 'move'
    console.log('original dropEffect: ' + ev.originalEvent.dataTransfer.dropEffect)
    console.log('computed dropEffect: ' + dropEffect)
//    console.log(ev.originalEvent.dataTransfer)
  })
})

function getNodePath(elem) {
  return elem.find('a span').attr('title')
}

function removeInsertPlaces(node) {
  if (node) 
    node.parent().find('div.insert-node-place').remove()
}

function configureInsertPlace(insElem, nodePath, after) {
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
    console.log('Dropped in insert place')
    insElem.removeClass('dragover')    
  })
}