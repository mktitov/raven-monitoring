var nodeTypesTable;
var tree;
var selectedNode;
var childsOfSelectedNode;
var addNodeDialogTip;
var newNodePath;
var nodesToDelete;
var moveEvent;
var moveData;
var nodeTypes;
var throughNodeTypes;

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
    layout = $('body').layout({
          west:{size:300}
        , north:{
            resizable:false, slidable:false, closable:false, spacing_open:-1
            , togglerLength_open: 0, togglerLength_closed: -1, fxName:"none"
        }
    });
}

function tree_init()
{
    tree = $("#tree").jstree({
        themes : {theme:"apple"},
        json_data: {
            ajax:{
                url:"@{Tree.childs}",
                data:function(node){return {path:node==-1? "" : node[0].id};}
            }
        },
        hotkeys:{
            "del": function(node){
                var nodes = tree.jstree("get_selected")
                //TODO: check the rights to the node
                if (nodes && nodes.length>0){
                    nodes.sort( function(a,b){ return a.id.length>b.id.length })
                    nodesToDelete = []
                    $(nodes).each(function(index, val){
                        var found = false;
                        for (var i=0; i<nodesToDelete.length; ++i)
                            if (val.id.indexOf(nodesToDelete[i].id)==0){
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
                childsOfSelectedNode = {}
                this.open_node(selectedNode, function(){
                    //Lets create a hash of the child node names of the selectedNode
                    $(selectedNode).find('a').each(function(index, node){
                        name = $(node).text().trim()
                        if (name)
                            childsOfSelectedNode[name]=true
                    })
                    addNodeDialog_open(selectedNode)
                })
            }
        },
        dnd:{
            
        }
        , plugins:["themes","json_data","ui","hotkeys", "dnd"]
    });
    tree.bind("refresh.jstree", function(){
        if (newNodePath){
            setTimeout(function(){
                tree.jstree("deselect_all")
                tree.jstree("open_node", selectedNode)
                newNode = document.getElementById(newNodePath)
                tree.jstree("hover_node", newNode)
                tree.jstree("select_node", newNode);
                newNodePath = undefined
            }, 500)
        }
    })
    tree.bind("move_node.jstree", function(event, data){
        moveEvent = event;
        moveData = data;
        alert('node moved')
        $.jstree.rollback(data.rlbk)
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
                        newNodePath = data
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
    $("#addNodeDialog_parent").val(parentNode.id)
    addNodeDialog_validate()
    var items;
    $('#addNodeDialog_name, #addNodeDialog_type').bind('keyup', function(event){
        addNodeDialog_validate()
    })
    $.ajax({
        async:false
        , url: '@{Tree.childNodeTypes}'
        , data: {path: parentNode.id}
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
                var ids=[]
                //the nodes which be refreshed after delete
                var parents = []
                $(nodesToDelete).each(function(index, node){
                    ids.push(node.id);
                    var found=false;
                    var parent = $(node).parents('li')[0];
                    for (var i=0; i<parents.length; ++i)
                        if (parent.id.indexOf(parents[i]==0)){
                            found = true;
                            break;
                        }
                    if (!found) parents.push(parent)
                })
                $.post("@{Tree.deleteNodes}", {'nodes':ids}, function(){
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
        $('#deleteNodesDialog ol').append('<li>'+node.id+'</li>')
    })
    $('#deleteNodesDialog').dialog("open")
}


//
function getRightsForNode(node){
    return parseInt($(node).attr('rights'))
}