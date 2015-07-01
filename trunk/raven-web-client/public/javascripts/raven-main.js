var nodeTypesTable;
var tree;
var selectedNode;
var childsOfSelectedNode;
var addNodeDialogTip;
var newNodeId;
var nodesToDelete;
var nodeTypes;
var throughNodeTypes;
var copyIndicator;
var treeCopyNode;
var treeMovingNodes;
var treeNodeStartStopControl;
var treeLastHoveredNode;
var treeSelectedNode;
var layoutWest;
var nodeTabs;

//variables for the test purposes
var testEvent;
var testData;

$(function(){
    //Layout definition
    layout_init()

    //Tree definition
    tree_init()

    //Init node tabs
    nodeTabs_init()

    //Add new node Dialog
    addNodeDialog_init()

    //Delete node Dialog
    deleteNodesDialog_init()

    //load node types and through node types
    loadNodeTypes()
    //init test functionality
    test_init()
});

function test_init() {
    $('#stop-node-button').click(function(){
        var selected = tree.jstree('get_selected')
        if (selected)
            $(selected).attr('started', '0')
    })
    $('#start-node-button').click(function(){
        var selected = tree.jstree('get_selected')
        if (selected)
            $(selected).attr('started', '1')
//            $(selected).children('a').removeClass('stoped-node')
    })
}

function loadNodeTypes()
{
   $.getJSON('@{Tree.nodeTypes}', null, function(data){
       nodeTypes = {}
       for (var i=0; i<data.length; ++i)
           nodeTypes[data[i].type]=data[i]
   })
   $.getJSON('@{Tree.throughNodeTypes}', null, function(data){
       throughNodeTypes = data
   })
}

function layout_init()
{
    layoutWest = $('#layout-west')
    var layout = $('body').layout({
          west:{size:300}
        , north:{
            resizable:false, slidable:false, closable:false, spacing_open:-1
            , togglerLength_open: 0, togglerLength_closed: -1, fxName:"none"
        }
    });
}

function nodeTabs_init()
{
    $('#layout-center').children().hide()
    $('#node-tabs-control').buttonset()
    $('#view-node-button').next().children('span').addClass('view-node-icon')
    $('#edit-node-button').next().children('span').addClass('edit-node-icon')
    $('#node-tabs-control').children('input').click(function(e){
        $('#layout-center').children().hide()
        $('#'+$(e.target).attr('tab')).show()
    })
    nodeEditTab_init()
}

function nodeEditTab_init()
{
    $('#edit-tab-sections a').click(function(e){
        $(e.target)
            .prev().toggleClass('ui-icon-triangle-1-s').toggleClass('ui-icon-triangle-1-e')
            .parent().toggleClass('ui-state-active')
            .next().slideToggle()
    })
}

function tree_init()
{
    copyIndicator = $('#copy-indicator').hide()
    treeNodeStartStopControl = $('#start-stop-control').hide().click(function(){
        var started = treeLastHoveredNode.attr('started')=='1'
        if (treeLastHoveredNode){
            $.ajax({
                async: false
                , url: "@{Tree.startStopNode()}"
                , data: {path: getPath(treeLastHoveredNode), start:!started}
                , success: function(data){
                    treeNodeStartStopControl.toggleClass('start-node').toggleClass('stop-node')
                    changeNodeStatus(treeLastHoveredNode.attr('id'), started?'0':'1')
                }
            })
        }
    })
    
    tree = $("#tree").jstree({
        themes : {theme:"apple"},
        json_data: {
            ajax:{
                url:"@{Tree.childs}",
                data:function(node){
                    return {path:node==-1? "" : node.attr('path')};
                }
            }
        },
        hotkeys:{
            "del": function(node){
                var nodes = tree.jstree("get_selected")
                //TODO: check the rights for the node
                if (nodes && nodes.length>0){
                    sortNodesByPathLength(nodes)
                    nodesToDelete = []
                    $(nodes).each(function(index, val){
                        var found = false;
                        for (var i=0; i<nodesToDelete.length; ++i)
                            if (getPath(val).indexOf(getPath(nodesToDelete[i]))==0){
                                found=true;
                                break;
                            }
                        if (!found) nodesToDelete.push(val)
                    })
                    deleteNodesDialog_open()
                }
            },
            "insert": function(){
                selectedNode = this.get_selected()[0]
                if ( getRightsForNode(selectedNode)<8 )
                    return;
                getChildNodeNames(selectedNode, function(names){
                    childsOfSelectedNode = names
                    addNodeDialog_open(selectedNode)
                })
            }
        },
        dnd:{
            copy_modifier: 'none'
        }
        , crrm:{
            move: {
                check_move: function(req){
                    return checkChildNodeType(req.np[0], req.o)
                }
            }
        }
        , plugins:["themes","json_data","ui","hotkeys", "dnd", "crrm"]
    });
    tree.bind("refresh.jstree", function(){
        if (newNodeId){
            setTimeout(function(){
                tree.jstree("deselect_all")
                tree.jstree("open_node", selectedNode)
                var newNode = document.getElementById(newNodeId)
                tree.jstree("hover_node", newNode)
                tree.jstree("select_node", newNode);
                newNodeId = undefined
            }, 500)
        }
    })
    tree.bind("move_node.jstree", function(event, data){
        var r = data.args[0]
        var paths = []
        var parents = []
        var i
        for (i=0; i<r.o.length; ++i) {
            paths.push(getPath(r.o[i]))
            console.log("path: "+paths[i])
        }
        parents.push(r.np[0]);
        for (i=0; i<treeMovingNodes.length; ++i)
            parents.push(treeMovingNodes[i])
//        parents.concat(treeMovingNodes)
        parents = sortNodesByPathLength(parents)
        parents = getTopNodes(parents)
        $.ajax({
            url: "@{Tree.moveNodes}"
            , data: {destination:getPath(r.np[0]), nodes:paths, position:r.cp, copy:treeCopyNode}
            , type: 'POST'
            , async: false
            , success: function(data){
                for (var i=0; i<parents.length; ++i) {
                    console.log("parent: "+getPath(parents[i]))
                    tree.jstree("refresh", parents[i])
                }
            }
            , error: function(request, status){
                $.jstree.rollback(data.rlbk)
            }
        })
//        alert('node moved: calculated position is '+data.args[0].cp)
    })
    tree.hover(null, function(){
        treeNodeStartStopControl.hide()
    })
    treeNodeStartStopControl.hover(function(){
        treeNodeStartStopControl.show()
    }, null)
    tree.bind("hover_node.jstree", function(e, data){
        testData = data;
        testEvent = e;
        var node = $(data.rslt.obj[0])
        if (node.attr('path')=='/')
            return;
        treeLastHoveredNode = node
        if (node.attr('started')=='1') 
            treeNodeStartStopControl.removeClass('start-node').addClass('stop-node')
        else
            treeNodeStartStopControl.removeClass('stop-node').addClass('start-node')
        var o = node.offset()
        var a = node.children('a')
        var ao = a.offset()
        var lo = layoutWest.offset()
        var x = Math.min(ao.left+a.width()+10, lo.left+layoutWest.width()-4)
//        var x = o.left-18
        var y = o.top+Math.floor((a.height()-16)/2)
        treeNodeStartStopControl.css({left:x+'px', top: y+'px'})
        treeNodeStartStopControl.show()
    })
    tree.bind("select_node.jstree", function(e, data){
        treeSelectedNode = data.rslt.obj[0]
        refresh_nodeEditTab()
    })
    $(document).bind("mousemove", function(event){
        if (treeCopyNode)
            copyIndicator.css({left:event.pageX+10+'px', top:event.pageY-10+'px'})
    })
    $(document).bind("drag_start.vakata", function(event, data){
        if (data.event.metaKey) {
            treeCopyNode=true;
            copyIndicator.show()
        }
    })
    $(document).bind("drag_stop.vakata", function(event, data){
        if (treeCopyNode) {
            treeCopyNode=false;
            copyIndicator.hide()
        }
    })
}

function addNodeDialog_init()
{
    $("#addNodeDialog").dialog({
        modal:true
        , autoOpen:false
        , position:['center','top']
        , width:700
        , height:500
        , close: function() {
            $('#addNodeDialog_type').autocomplete("destroy")
            $('#addNodeDialog_name, #addNodeDialog_type').unbind('keyup')
            $('#addNodeDialog_name').btOff()
        }
        , buttons: {
            "&{'addNodeDialog_createButton'}" : function (){
                $(this).dialog("close")
                $('#addNodeDialog_form').ajaxSubmit({
                    data: {parent: $('#addNodeDialog_parent').val()}
                    , success: function(data){
                        newNodeId = data
                        tree.jstree("refresh", selectedNode)
                    }
                })
            }
            , "&{'cancelButton'}" : function (){
                $(this).dialog("close")
            }
        }
    });
    $('#addNodeDialog_name').bt("&{'addNodeDialog_nameAlreadyUsed'}", {trigger:'none'})
}

function addNodeDialog_open(parentNode)
{
    if (!parentNode)
        return;
    addNodeDialogTip = undefined
    $("#addNodeDialog").clearForm()
    $("#addNodeDialog_parent").val($(parentNode).attr('path'))
    addNodeDialog_validate()
    var items;
    $('#addNodeDialog_name, #addNodeDialog_type').bind('keyup', function(event){
        addNodeDialog_validate()
    })
    $.ajax({
        async:false
        , url: '@{Tree.childNodeTypes}'
        , data: {path: $(parentNode).attr('path')}
        , success: function(data){
            items = data
        }
    })
    $('#addNodeDialog_parent').attr('disabled', 'true')
    $('#addNodeDialog_type').autocomplete({
        source: items
        , minLength: 0
        , select: function(event, ui){
            addNodeDialog_validate()
        }
    })
    .data("autocomplete")._renderItem = function(ul, item){
        return $('<li></li>')
            .data("item.autocomplete", item)
            .append('<a>'
                    +'<table style="width:650px"><tr>'
                    +'<td style="width:32px;padding-right:16px;background: url(\''+getIconPath(item.icon)+'\') no-repeat center"></td>'
                    +'<td style="width:350px">'+item.label+'</td>'
                    +'<td>'+item.desc+'</td></tr></table></a>')
            .appendTo(ul)
    }
    $('.ui-autocomplete').css({
        "max-height": '300px'
        , 'overflow-y': 'auto'
    })
    $('#addNodeDialog').dialog('open')
}

function addNodeDialog_validate()
{
    var name = $('#addNodeDialog_name').val()
    var type = $('#addNodeDialog_type').val()
    var createButton = $('#addNodeDialog ~ .ui-dialog-buttonpane button').first()
    if (childsOfSelectedNode[name])
        $('#addNodeDialog_name').btOn()
    else
        $('#addNodeDialog_name').btOff()
    if (name && type && !childsOfSelectedNode[name])
        createButton.attr('disabled',false).removeClass('ui-state-disabled')
    else
        createButton.attr('disabled',true).addClass('ui-state-disabled')
        
}

function deleteNodesDialog_init()
{
    $('#deleteNodesDialog').dialog({
         modal:true
        , autoOpen:false
        , position:['center','top']
        , width:500
        , height:400
        , buttons: {
            "&{'deleteButton'}": function(){
                //the path of nodes to delete
                var paths=[]
                //the nodes which be refreshed after delete
                var parents = []
                $(nodesToDelete).each(function(index, node){
                    paths.push(getPath(node));
                    var found=false;
                    var parent = getParentNode(node);
                    for (var i=0; i<parents.length; ++i)
                        if (getPath(parent).indexOf(getPath(parents[i]))==0){
                            found = true;
                            break;
                        }
                    if (!found) parents.push(parent)
                })
                $.post("@{Tree.deleteNodes}", {'nodes':paths}, function(){
                    for (var i=0;i<parents.length; i++)
                        tree.jstree('refresh', parents[i]);
                })
                $('#deleteNodesDialog').dialog("close")
            }
            , "&{'cancelButton'}": function(){
                $('#deleteNodesDialog').dialog("close")
            }
        }
    })
}

function deleteNodesDialog_open()
{
    $('#deleteNodesDialog li').remove()
    $(nodesToDelete).each(function(index, node){
        $('#deleteNodesDialog ol').append('<li>'+$(node).attr('path')+'</li>')
    })
    $('#deleteNodesDialog').dialog("open")
}

//
function getRightsForNode(node){
    return parseInt($(node).attr('rights'))
}

function getParentNode(node){
    return $(node).parents('li')[0]
}

function checkChildNodeType(parent, nodes)
{
    if (!parent || getRightsForNode(parent)<32) return false

    var parentType = $(parent).attr('type');
    var checkPassed = true
    treeMovingNodes = []
    for (var i=0; i<nodes.length; ++i){
        var nodeParent = getParentNode(nodes[i])
        if ( !treeCopyNode && (nodeParent==parent || getChildNodeNames(parent)[getNodeName(nodes[i])]) )
            return false
        var nodeType = $(nodes[i]).attr('type')
        var desc = nodeTypes[parentType]
        if (!desc)
            return false;
        else if ( desc && desc.childTypes.indexOf(nodeType)==-1 ){
            if ( throughNodeTypes.indexOf(parentType)>=0 )
                checkPassed = checkChildNodeType(getParentNode(parent), parent)
            else
                checkPassed = false
            if (!checkPassed)
                return false
        }
        treeMovingNodes.push(getParentNode(nodes[i]))
    }
    return true;
}

//Opens the parent node and returns the hash with node name in the key
function getChildNodeNames(parent, callback)
{
    var getNames = function(){
        var names={}
        $(parent).find('ul:first > li > a').each(function(index, node){
            names[$(node).text().trim()]=true
        })
        return names
    }
    if (callback) {
        tree.jstree("open_node", parent, function(){
            callback(getNames())
        })
        return null;
    } else
        return getNames()
}

//Returns the hash with node name in the key
//function getChildNodeNames(parent)
//{
//    names = {}
//    $(parent).find('ul:first > li > a').each(function(index, node){
//        names[$(node).text().trim()]=true
//    })
//    return names;
//}

function getNodeName(node){
    return $(node).children('a').text().trim()
}

function getPath(node){
    return $(node).attr('path')
}

function sortNodesByPathLength(nodes) {
    nodes.sort( function(a,b){
        return $(a).attr('path').length>$(b).attr('path').length
    })
    return nodes
}

function sortPathsByLength(paths) {
    paths.sort(function(a,b){
        return a.length>b.length
    })
}

function getTopPaths(paths) {
    var topPaths = []
    if (paths)
        for (var i=0; i<paths.length; ++i) {
            var found = false;
            for (var j=0; j<topPaths.length; ++j)
                if ( paths[i].indexOf(topPaths[j])==0 ){
                    found = true;
                    break;
                }
            if (!found) topPaths.push(paths[i])
        }
    return topPaths;
}

function getTopParents(nodes){
    var topParents = []
    if (nodes)
        for (var i=0; i<nodes.length; ++i) {
            var found = false;
            var parent = getParentNode(nodes[i])
//            console.log("checking node: "+getPath(nodes[i]))
            for (var j=0; j<topParents.length; ++j) {
                if (getPath(parent).indexOf(getPath(topParents[j]))==0) {
                    found=true;
                    break;
                }
            }
            if (!found) {
//                console.log("added parent: "+getPath(parent))
                topParents.push(parent)
            }
        }
    return topParents;
}
function getTopParents(nodes){
    var topParents = []
    if (nodes)
        for (var i=0; i<nodes.length; ++i) {
            var found = false;
            var parent = getParentNode(nodes[i])
//            console.log("checking node: "+getPath(nodes[i]))
            for (var j=0; j<topParents.length; ++j) {
                if (getPath(parent).indexOf(getPath(topParents[j]))==0) {
                    found=true;
                    break;
                }
            }
            if (!found) {
//                console.log("added parent: "+getPath(parent))
                topParents.push(parent)
            }
        }
    return topParents;
}

function getTopNodes(nodes){
    var topNodes = []
    if (nodes)
        for (var i=0; i<nodes.length; ++i) {
            console.log("checking node: "+getPath(nodes[i]))
            if (nodes[i]) {
                var found = false;
                for (var j=0; j<topNodes.length; ++j) {
                    if (getPath(nodes[i]).indexOf(getPath(topNodes[j]))==0) {
                        found=true;
                        break;
                    }
                }
                if (!found) {
                    topNodes.push(nodes[i])
                    console.log("added parent: "+getPath(nodes[i]))
                }
            }
        }
    return topNodes;
}

function refresh_nodeEditTab()
{
    var name = getNodeName(treeSelectedNode)
    name = name.length>0? name: '/'
    var type = $(treeSelectedNode).attr('type')
    var desc = nodeTypes[type].description
    if (!desc) {
        $.ajax({
            url: '@{Tree.typeDescription}'
            , data: {path: getPath(treeSelectedNode)}
            , async: false
            , dataType: 'json'
            , success: function(data){
                desc=data.description
            }
        })
        nodeTypes[type].description=desc
    }
    $('#edit-tab-node-name')
        .attr('style', 'float:left;width:26px;background: url(\''+getIconPath(nodeTypes[type].iconPath)+'\') no-repeat left center')
        .next().html(name)
        .next().html(' ('+type+')')
        .parent().attr({'node-id':$(treeSelectedNode).attr('id'), started:$(treeSelectedNode).attr('started')})
    $('#edit-tab-desc').html(desc)
}

function getIconPath(nodeType)
{
    return'@{Tree.icon}?path='+encodeURIComponent(nodeType)
}

function changeNodeStatus(nodeId, started)
{
    $('#'+nodeId).attr('started', started)
    var editNode = $('#edit-tab-node-name').parent();
    if (editNode.attr('node-id')==nodeId)
        editNode.attr('started', started)
    
}