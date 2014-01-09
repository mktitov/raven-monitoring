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

var editors = {} //
var tabsCounter = 0
var tabTemplate = "<li title='_2_'><a href='#_1_' class='center'>_3_</a><span class='ui-icon ui-icon-close' role='presentation'>Remove Tab</span></li>"
var waitingImage = "<img class='editor-loading-image' src='images/ui-anim_basic_16x16.gif'/>"
var tabs
var currentTheme = "eclipse"
var loaded = 2
var pendingTabs = []
var buttons;
var saveButton;
var editorParams
var errorElem;

$(document).ready(function(){
  buttons = $('button')
  saveButton = $('button#save')
  errorElem = $('div#error')
  loadModes()
  loadThemes()
  setInterval(checkChanges, 100)
  $("#add-tab").on("click", function(){
    openTab("/nodes", "test")
  })
  $(window).resize(function(){
    resizeTabs()
  })
  $('#themes').on("change", function(e){
    currentTheme = $(e.target).val()
    $.each(editors, function(k,e){
      setTheme(e.editor, currentTheme)
    })
  })
  $("#modes").change(function(e){
    var newMode = $(e.target).val()
    if (editorParams && editorParams.editor) {
      setMode(editorParams.editor, newMode)
    }
  })
  tabs = $("#tabs").tabs({
    heightStyle: "fill",
    activate: function(event, ui) {
      newTab = ui.newTab
      editorParams = editors[ui.newTab.attr('title')]
      activateEditor(editorParams)
      syncState(editorParams)
      console.debug(event, ui)  
    }
  })
})

function checkChanges() {
  var p = editorParams
  if (p 
      && p.lastChangeTime 
      && p.hasChanges 
      && p.lastFullCheckTime!==p.lastChangeTime
      && (new Date().getTime() - p.lastChangeTime > 200)) 
  {
    p.lastFullCheckTime = p.lastChangeTime
    p.hasChanges = p.value!==p.editor.getValue()
    if (!p.hasChanges)
      syncState(p)
  }
}

function activateEditor(editorParams) {
  resizeTab(editorParams)
  if (editorParams.editor) {
    editorParams.editor.focus()
    $('#modes').val(editorParams.mode)
  }
}

function syncState(params) {
  if (params===editorParams) {
    if (params.waiting) 
      buttons.attr('disabled','true')
    else 
      buttons.removeAttr('disabled')
    if (params.hasChanges) {
      saveButton.removeAttr('disabled')
      params.tabHeader.find('a').css('color','blue')
    } else {
      saveButton.attr('disabled','true')
      params.tabHeader.find('a').css('color','')
    }
    if (params.hasErrors) {
      params.tabHeader.find('a').css('color','red')
      errorElem.val(params.error)
      errorElem.css('visibility','visible')
    } else {
      errorElem.css('visibility','hidden')
    }
  }
}

function setTheme(editor, theme) {
  editor.setTheme('ace/theme/'+theme)
}

function setMode(editor, mode) {
  editor.getSession().setMode("ace/mode/"+mode);
}

function loadModes() {
  loadSelectOptions("modes", 'get-modes')
}

function loadThemes() {
  loadSelectOptions("themes", 'get-themes')
}

function loadSelectOptions(selectId, path) {
  $.getJSON('sri/system/editor/'+path, function(res){
    --loaded
    var modesSelect = $("#"+selectId)
    $.each(res, function(i, v) {
      modesSelect.append('<option value="'+v+'">'+v+'</option>')
    })
    if (selectId==='themes')
      modesSelect.val(currentTheme)
    if (loaded===0) 
      $.each(pendingTabs, function(i,v){
        openTab(v.nodePath, v.attrName)
      })
  })
}

function resizeTabs() {
  $.each(editors, function(k,v){
    resizeTab(v)
  })
}

function switchToWaiting(editorParams) {
  editorParams.waiting = true
  editorParams.tabHeader.find('a').prepend(waitingImage)
  syncState(editorParams)
}

function switchToWorking(editorParams) {
  editorParams.waiting = false
  editorParams.tabHeader.find('a img').remove()
  syncState(editorParams)
}

function resizeTab(editorParams) {
//  editorParams.tab.css("height", ""+Math.round(78+tabs.innerHeight()*7/410)+"%")
  var h = tabs.outerHeight() - tabs.find("ul").outerHeight()-30
  console.log("new height: "+h)
  editorParams.tab.css("height", ''+h+'px' )
  if (editorParams.editor)
    editorParams.editor.resize(true)  
}

function openTab(nodePath, attrName) {
  if (loaded!==0) {
    console.log("Editor not ready! Wating...")
    pendingTabs.push({nodePath:nodePath, attrName:attrName})
  } else {
    var key = nodePath+'@'+attrName
    var params = editors[key]
    if (!params) {
      //creating new
      tabsCounter++
      var nodeName = getNodeName(nodePath)
      var title = nodeName+'@'+attrName
      var id = 'tab-'+tabsCounter
      var editorId = 'editor-'+tabsCounter
      var tabHeader = tabTemplate.replace('_1_', id).replace('_2_', key).replace('_3_', title)
      tabs.find(".ui-tabs-nav").append(tabHeader)
      tabs.append("<div id='" + id + "' style='height:90%'><div id='"+editorId+"' style='height:100%'>function(){ \nreturn 'test'\n}</div></div>")
      tabs.tabs("refresh")
      params = {
        key: key,
        nodePath: nodePath,
        attrName: attrName,
  //      editor: editor,
        tab: $('#'+id),
        tabId: id,
        hasChanges: false,
        editorId: editorId,
        tabHeader: $('a[href="#'+id+'"]').parent()
      }
      editors[key] = params
      editorParams = params
      loadEditorContent(params)
    }
    var tabIndex = tabs.find("a[href='#"+params.tabId+"']").parent().index()
    tabs.tabs("option", "active", tabIndex)
  }
}

function saveEditorContent(params) {
  switchToWaiting(params)
  $.ajax({
    url: 'sri/system/nodes/attrs/set-value',
    dataType: 'json',
    type: "POST",
    data: {
      nodePath: params.nodePath,
      attrName: params.attrName,
      value: params.editor.getValue()
    },
    complete: function() {
      switchToWorking(params)
    },
    success: function(res) {
      console.log("SAVE result")
      console.log(res)
      if (res.success) {
        params.value = params.editor.getValue()
        params.hasChanges = false
        params.hasErrors = false
        params.error = null
        syncState(params)
        clearRavenErrors(params.editor)
      } else {
        params.hasErrors = true
        params.error = res.error
        clearRavenErrors(params.editor)
        params.editor.gotoLine(res.lineNumber, res.colNumber-1, true)
        addErrorToEditor(params.editor, res)
        syncState(params)
      }
    }
  })
  console.log("Saving content for editor: "+params.key)
}

function clearRavenErrors(editor) {
  var ann = []
  for (a in editor.getSession().getAnnotations())
    if (!a.ravenError)
      ann.push(a)
  editor.getSession().setAnnotations(ann)
}

function addErrorToEditor(editor, err) {
  var anns = editor.getSession().getAnnotations()
  anns.push({row:err.lineNumber-1,column:err.colNumber-1,text:err.error,type:'error'})
  editor.getSession().setAnnotations(anns)
}

function loadEditorContent(params) {
  switchToWaiting(params)
  $.ajax({
    url:'sri/system/nodes/attrs/get-value',
    dataType: 'json',
    data: {
      nodePath:params.nodePath,
      attrName:params.attrName
    },
    complete: function() {
      switchToWorking(params)
    },
    success: function(res) {
      console.debug(res)
      var editor = ace.edit(params.editorId);
      params.mode = detectMode(res.mimeType)
      params.value = res.data
      editor.setValue(res.data)
      setTheme(editor, currentTheme)
      setMode(editor, params.mode)
      editor.resize(true)
      editor.getSession().setTabSize(2)
      editor.commands.addCommand({
        name: 'Save',
        bindKey: {win: 'Ctrl-S',  mac: 'Command-S'},
        exec: function() {
          saveEditorContent(params)
        },
        readOnly: true // false if this command should not apply in readOnly mode
      });
      editor.on("change", function(){
        params.lastChangeTime = new Date().getTime()
        if (!params.hasChanges) {
          params.hasChanges = true
          syncState(params)
        }
      })
      params.editor = editor
      if (editorParams===params)
        activateEditor(params)
    },
    error: function(xhr, status, error) {
      
    }
  })
}

function detectMode(mimeType) {
  var mode = null
  if (mimeType==='text/plain') mode = 'text'
  else mode = mimeType.split('/')[1] 
  return mode
}

function getNodeName(nodePath) {
  var nodeName = nodePath.split('/')
  nodeName = nodeName[nodeName.length-1]
  nodeName = nodeName.replace(/"/g, '')
  return nodeName  
}