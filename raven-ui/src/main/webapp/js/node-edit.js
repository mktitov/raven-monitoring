/* 
 * Copyright 2014 tim.
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


scriptThis = this
function openInEditor(attrName) {
  var editor = window.open("", "raven-editor on "+serverHost, "height=800;width=1024")
  if (editor) {
    if (editor.location.host==="") {
      //editor.initialTab = {nodePath:nodePath, attrName:attrName}
      editor.location.href = '../raven-editor.html'          
      waitAndOpen(editor, nodePath, attrName)
    } else
      editor.openTab(nodePath, attrName)
  }
}

function waitAndOpen(editor, nodePath, attrName) {
  if (editor.editorLoaded) {
    editor.document.title = 'Raven-editor on '+serverHost
    editor.openTab(nodePath, attrName)
  }
  else {
    console.log("Wating...")
    setTimeout(function(){waitAndOpen(editor,nodePath, attrName)}, 100)
  }
}

function changePathType(reqName, button) {
  var buttons = $(button).parent().find('button')
  var input = $(button).parent().parent().find('input')
  buttons.attr('disabled','true')
  $.ajax({
    url:'../sri/system/nodes/'+reqName,
    dataType: 'json',
    type: 'POST',
    data: {
      nodePath:nodePath,
      path:$(input).val(),
    },
    complete: function() {
      buttons.removeAttr('disabled')
    },
    success: function(res) {
      if (res.success) 
        $(input).val(res.data)
    }
  })
}

$(document).ready(function(){
  $(document).delegate("table.node-reference input", 'dragover', function(e){
    e.originalEvent.preventDefault()
    var input = $(this)
//    console.log("drag over")
    if (!input.is(":data(droppable)")) {      
//      console.log('attaching ondrop')
      input.data("droppable", true)
      input.bind('drop', function(e){
        e.originalEvent.preventDefault()
        input.val(e.originalEvent.dataTransfer.getData('text/plain'))
//            e.originalEvent.stopPropagation()
        console.log("dropped")
      })
    }
  })
  $(document).delegate("table.node-reference input", 'mouseenter', function(){
    var input = $(this)
    if (!input.is(':data(uiAutocomplete)')) {
      console.log('attaching autocomplete for node references')
      input.autocomplete({
        source: function(request, response){
          console.log("Executing autocomplete")
          $.getJSON('../sri/system/nodes/resolve-path', {
            pathPart:request.term,
            nodePath:nodePath
          }, response)
        }
      })
//      var region = input.parents('div')
//          .first()
//          .bind('dragover', function(e){
//            e.originalEvent.preventDefault()
////            e.originalEvent.stopPropagation()
//          })
////          .bind('dragenter', function(e){
////            e.originalEvent.preventDefault()
////            e.originalEvent.stopPropagation()
////          })
////          .bind('dragleave', function(e){
////            e.originalEvent.preventDefault()
////            e.originalEvent.stopPropagation()
////          })
//          .bind('drop', function(e){
//            e.originalEvent.preventDefault()
////            e.originalEvent.stopPropagation()
//            console.log("dropped")
//          })
          
      
//      region.bind('dragenter', function(ev) {
////        if (ev.originalEvent.preventDefault)
//          ev.originalEvent.preventDefault()
//        console.log('drag enter')
//        var data = ev.originalEvent.dataTransfer
//        data.dropEffect = 'none'
////        console.log('dragover data: '+ev.originalEvent.dataTransfer.getData('Text'))
//        console.log(ev.originalEvent.dataTransfer)
////        return false
//      })
////      region.bind('dragover', function(ev) {
//////        if (ev.originalEvent.preventDefault)
////          ev.originalEvent.preventDefault()
////        console.log('drag over')
////        var data = ev.originalEvent.dataTransfer
////        data.dropEffect = 'none'
//////        console.log('dragover data: '+ev.originalEvent.dataTransfer.getData('Text'))
////        console.log(ev.originalEvent.dataTransfer)
//////        return false
////      })
//      region.bind('drop'), function(ev) {
//        var path = ev.originalEvent.dataTransfer.getData('text/plain')
//        console.log("dropped new path: "+path)
//        input.val(path)
//        ev.originalEvent.preventDefault()
//      }
    }
  })
  $(document).delegate('button.convert-path', 'click', function(){
    var button = $(this)
    var input = $(button).parent().parent().find('input')
    button.attr('disabled','true')
    $.ajax({
      url:'../sri/system/nodes/convert-path',
      dataType: 'json',
      type: 'POST',
      data: {
        nodePath:nodePath,
        path:$(input).val(),
      },
      complete: function() {
        button.removeAttr('disabled')
      },
      success: function(res) {
        if (res.success) 
          $(input).val(res.data)
      }
    })
  })
})