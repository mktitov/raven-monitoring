$(function(){
    //Layout definition
    layout = $('body').layout({
          west:{size:300}
        , north:{
            resizable:false, slidable:false, closable:false, spacing_open:-1, togglerLength_open: 0,
            togglerLength_open: 0, togglerLength_closed: -1, fxName:"none"
        }
    });

    //Tree definition
    $("#tree").jstree({
        themes : {theme:"apple"},
        json_data: {
            ajax:{
                url:"@{Tree.childs}",
                data:function(node){ return {path:node==-1? "" : node[0].id}; }
            }
        },
        hotkeys:{
            "del": function(node){
                alert("Creating new node")
            },
            "insert": function(node){
                $("#addNodeDialog").dialog("open");
            }
        },
        plugins:["themes","json_data","ui","hotkeys"]
    });

    //Add new node Dialog
    $("#addNodeDialog").dialog({
        modal:true,
        autoOpen:false,
        position:['center','top'],
        width:600,
        show:'blind',hide:'blind'
    });

    $("#nodeTypesTable").dataTable({
        bJQueryUI: true,
        sDom:"<fl>t",
        sAjaxSource:'@{Tree.childNodeTypes}'
	})
});
