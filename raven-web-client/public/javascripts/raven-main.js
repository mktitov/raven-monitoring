$(function(){
    //Layout definition
    layout = $('body').layout({
          west:{size:300}
        , north:{
            resizable:false, slidable:false, closable:false, spacing_open:-1
            , togglerLength_open: 0, togglerLength_closed: -1, fxName:"none"
        }
    });

    //Tree definition
    $("#tree").jstree({
        themes : {theme:"apple"},
        json_data: {
            ajax:{
                url:"@{Tree.childs}",
                data:function(node){return {path:node==-1? "" : node[0].id};}
            }
        },
        hotkeys:{
            "del": function(node){
                alert("Creating new node")
            },
            "insert": function(){
//                this.disable_hotkeys()
                openAddNodeDialog(this.get_selected()[0])
            }
        },
        plugins:["themes","json_data","ui","hotkeys"]
    });

    //Add new node Dialog
    $("#addNodeDialog").dialog({
        modal:true
        , autoOpen:false
        , position:['center','top']
        , width:700
        , height:600
        , resizeStop: function() {
            $("#nodeTypesTable").dataTable().fnAdjustColumnSizing()
        }
        , close: function() {
//            $("#tree").jstree().enable_hotkeys()
        }
//        , show:'blind',hide:'blind'
    });

    datatable = $("#nodeTypesTable").dataTable({
        bJQueryUI: true
        , sScrollY: "250px"
        , "sScrollYInner": "110%"
        , bPaginate: false
//        , sDom:"<ft>"
        , sAjaxSource:'@{Tree.childNodeTypes}'
        , oLanguage: {sUrl:'/public/messages/dataTables.messages.${lang}'}
	});

    tableNav = new KeyTable({
        table: document.getElementById("nodeTypesTable")
        , "datatable": datatable
    });
});

function openAddNodeDialog(parentNode)
{
    if (!parentNode)
        return;
    $("#addNodeDialog #node").val(parentNode.id)
//    $("#addNodeDialog").dialog("open");
    table = $("#nodeTypesTable").dataTable()
    table.fnReloadAjax("@{Tree.childNodeTypes}?path="+encodeURIComponent(parentNode.id),
        function () {
            dialog = $("#addNodeDialog")
            dialog.dialog("open")
            dialog.height(dialog.height+50)
            table.fnAdjustColumnSizing();
        }
    )
//    table.fnDraw();
}

$.fn.dataTableExt.oApi.fnReloadAjax = function ( oSettings, sNewSource, fnCallback, bStandingRedraw )
{
	if ( typeof sNewSource != 'undefined' && sNewSource != null )
	{
		oSettings.sAjaxSource = sNewSource;
	}
	this.oApi._fnProcessingDisplay( oSettings, true );
	var that = this;
	var iStart = oSettings._iDisplayStart;

	oSettings.fnServerData( oSettings.sAjaxSource, null, function(json) {
		/* Clear the old information from the table */
		that.oApi._fnClearTable( oSettings );

		/* Got the data - add it to the table */
		for ( var i=0 ; i<json.aaData.length ; i++ )
		{
			that.oApi._fnAddData( oSettings, json.aaData[i] );
		}

		oSettings.aiDisplay = oSettings.aiDisplayMaster.slice();
		that.fnDraw( that );

		if ( typeof bStandingRedraw != 'undefined' && bStandingRedraw === true )
		{
			oSettings._iDisplayStart = iStart;
			that.fnDraw( false );
		}

		that.oApi._fnProcessingDisplay( oSettings, false );

		/* Callback user function - for event handlers etc */
		if ( typeof fnCallback == 'function' && fnCallback != null )
		{
			fnCallback( oSettings );
		}
	} );
}

