var nodeTypesTable;
var tree;
var selectedNode;
var childsOfSelectedNode;
var addNodeDialogTip;
var newNodeId;
var nodesToDelete;
var nodeTypes;
var throughNodeTypes;
var treeCopyNode;
var copyIndicator;

$(function(){
    //Layout definition
    layout_init()

    //Tree definition
    tree_init()

    //Add new node Dialog
    addNodeDialog_init()

    //Delete node Dialog
    deleteNodesDialog_init()

    //load node types and through node types
    loadNodeTypes()
});

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
    var layout = $('body').layout({
          west:{size:300}
        , north:{
            resizable:false, slidable:false, closable:false, spacing_open:-1
            , togglerLength_open: 0, togglerLength_closed: -1, fxName:"none"
        }
    });
}

function tree_init()
{
    copyIndicator = $('#copy-indicator').hide()
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
                    nodes.sort( function(a,b){
                        return $(a).attr('path').length>$(b).attr('path').length
                    })
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
        for (var i=0; i<r.o.length; ++i)
            paths.push(getPath(r.o[i]))
        $.ajax({
            url: "@{Tree.moveNodes}"
            , data: {destination:getPath(r.np[0]), nodes:paths, position:r.cp, copy:treeCopyNode}
            , type: 'POST'
            , async: false
            , success: function(data){
                tree.jstree("refresh", r.np[0])
            }
            , error: function(request, status){
                $.jstree.rollback(data.rlbk)
            }
        })
//        alert('node moved: calculated position is '+data.args[0].cp)
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
                    +'<td style="width:32px;padding-right:16px;background: url(\'@{Tree.icon}?path='+encodeURIComponent(item.icon)+'\') no-repeat center"></td>'
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
