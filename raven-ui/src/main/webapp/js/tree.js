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
var draggingNode = null;
var dragoverNode = null;
var dragoverEvent = null;

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
//        data.effectAllowed = "copyMove"
//        data.dropEffect = "move"
        data.setDragImage(table[0], 1, 1)
        data.setData('text/plain', getNodePath(table))
        console.log(data)
      })
      table.bind('dragend', function(ev){
        console.log('drag finished')
        removeInsertPlaces()
        dragoverNode = null
        draggingNode = null
        return false
      })
    }
    //adding context menu
  })
  $(document).delegate("table.tree-node", "contextmenu", function(ev) {
    ev.preventDefault()
    createContextMenu($(this), ev)
  })
  
  $(document).delegate("table.tree-node", "dragover", function(ev) {
    var node = $(this)
    if (!node.prev().is('div')) {
      removeInsertPlaces()
      dragoverNode = node
      console.log('inserting div')
      var template = "<div class='insert-node-place'></div>"
      var nodePath = getNodePath(node)
      var before = $(template).insertBefore(node)
      configureInsertPlace(before, nodePath, false)
      var after = $(template).insertAfter(node)
      configureInsertPlace(after, nodePath, true)
    }
    if (getNodePath(node)!==getNodePath(draggingNode)) {
      dragoverEvent = ev
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
    var targetNode = $(this)
    targetNode.removeClass('dragover')
    var dropEffect = getDropEffect(ev)
    var sourceNodePath = getNodePathFromData(ev)
    console.log("DROPPED")
    console.log(ev)
//    transferNode(sourceNodePath, getNodePath(targetNode), dropEffect==='move', ev.shiftKey)
    transferNode(sourceNodePath, getNodePath(targetNode), getDropEffect(dragoverEvent)==='move', dragoverEvent.shiftKey)
    dragoverEvent = null
//    console.log('original dropEffect: ' + ev.originalEvent.dataTransfer.dropEffect)
//    console.log('computed dropEffect: ' + dropEffect)
//    console.log(ev.originalEvent.dataTransfer)
  })
})

function getNodeName(nodePath) {
  var elems = nodePath.split('/')
  return elems[elems.length-2].replace(/"/g, '')
}

function getParentNodePath(nodePath) {
  var elems = nodePath.split('/')
  elems.splice(elems.length-2,1)
  return elems.join('/')
}

function getNodePathFromData(ev) {
  return ev.originalEvent.dataTransfer.getData('text/plain')
}

function getNodePath(elem) {
  return elem? elem.find('a span').attr('title') : null
}

function createContextMenu(nodeElem, event) {
  var nodePath = getNodePath(nodeElem)
  if (nodeElem.data('menu-loading')) {
    console.log("Context menu already loading for: " + nodePath)
    return;
  }
  nodeElem.data('menu-loading', true)
  console.log("Loading menu for node:", nodePath)
  $.ajax({
    url:"../projects/system/nodes/actions",
    dataType:"json",
    type:'GET',
    data: {
      nodePath:getNodePath(nodeElem)
    },
    success: function(res) {
      console.log("Menu loaded", res)
      var items = {}
      $.each(res, function(ind, item){
        items[item.cmd] = item
      })
      nodeElem.data('menu-items', items)
      nodeElem.contextmenu({
        menu: res,
        position: {my: "left top", at: "center", of: event, collision: "fit"},
        select: function(event, ui) {
          console.log("Item pressed...")
          console.log(ui)
          console.log($(ui).data())
          executeMenuAction(nodeElem, ui.cmd)
        },
        close: function() {
          nodeElem.contextmenu("destroy")
        }
      })  
      nodeElem.contextmenu("open", nodeElem)
    },
    error: function() {
      
    },
    complete: function() {
      nodeElem.data('menu-loading', null)
    }
  })
}

function executeMenuAction(nodeElem, cmd) {
  if (cmd==='refresh') 
    refreshTree()
  else {
    var item = nodeElem.data('menu-items')[cmd]
    console.log("Handling item click for: ", item)
    var execute = true
    if (item.confirmMessage)
      execute = confirm(item.confirmMessage)
    if (execute) 
      $.ajax({
        url: item.url,
        type: 'GET',
        data: {
          nodePath: getNodePath(nodeElem)
        },
        success: function() {
          if (item.refreshTree)
            refreshTree()
        },
        error: function(mess) {
          alert('Возникла ошибка при выполнении действия. '+mess)
        }
      })
  }
}

function configureInsertPlace(insElem, nodePath, after) {
  insElem.bind('dragover', function(ev) {
    ev.originalEvent.preventDefault()
    console.log('dragover insert place')
    dragoverEvent = ev
    $(this).addClass('dragover')
  })
  
  insElem.bind('dragleave', function(ev) {
    $(this).removeClass('dragover')
  })
  
  insElem.bind('drop', function(ev){
    ev.originalEvent.preventDefault()
    console.log('Dropped in insert place')
//    console.log(ev)
//    console.log(dragoverevent)
    insElem.removeClass('dragover')    
    var sourceNodePath = getNodePathFromData(ev)
    var targetNodePath = getParentNodePath(nodePath)
    var positionNodePath = nodePath    
    transferNode(sourceNodePath, targetNodePath, getDropEffect(dragoverEvent)==='move', dragoverEvent.shiftKey, positionNodePath, after)
    dragoverEvent = null
  })
}