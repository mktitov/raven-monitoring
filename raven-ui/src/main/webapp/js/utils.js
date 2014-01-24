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

function transferNode(sourceNodePath, targetNodePath, isMoveOp, askNewName, positionNodePath, after, onSuccess) {
  var newName=null;
  if (askNewName) {
    newName = prompt("Введите новое имя узла", getNodeName(sourceNodePath))
    if (newName===null)
      return;
    else if (newName==='') {
      alert("Имя не может быть пустым")
      return;
    }
  }
  console.log("tree      dragging node: "+top.frame1.draggingNode)
  console.log("tree-edit dragging node: "+top.frame2.draggingRow)
  var reloadFrame2 = top.frame1.draggingNode===null || top.frame2.draggingRow
  $.ajax({
    url:"../sri/system/nodes/transfer",
    dataType:"json",
    type:'POST',
    data: {
      sourceNodePath:sourceNodePath,
      targetNodePath:targetNodePath,
      isMoveOp: isMoveOp,
      newName:newName,
      positionNodePath: positionNodePath,
      insertBefore: !after
    },
    success: function(res) {
      if (res.success) {
        //refresh tree or node
        var tree = top.frame1._adftreetree1
        if (tree)
          tree.treeState.action('refresh','0',this)
//        if (onSuccess)
//          onSuccess()
        if (reloadFrame2)
          top.frame2.location.reload()
      } else
        alert('Возникла ошибка при копировании/перемещении объекта. '+res.error)
    },
    error: function() {
      alert('Возникла ошибка при копировании/перемещении объекта')
    }
  })
}

function getNodePathFromData(ev) {
  return ev.originalEvent.dataTransfer.getData('text/plain')
}

function getParentNodePath(nodePath) {
  var elems = nodePath.split('/')
  elems.splice(elems.length-2,1)
  return elems.join('/')
}

function removeInsertPlaces() {
  if (top.frame1.dragoverNode) 
    top.frame1.dragoverNode.parent().find('.insert-node-place').remove()
  if (top.frame2.dragoverRow)
    top.frame2.dragoverRow.parent().find('.insert-node-place').remove()
}

