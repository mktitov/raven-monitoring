package org.raven.ui.vo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.apache.myfaces.trinidad.component.core.data.CoreColumn;
import org.apache.myfaces.trinidad.component.core.data.CoreTable;
import org.apache.myfaces.trinidad.component.core.nav.CoreCommandLink;
import org.apache.myfaces.trinidad.component.core.output.CoreOutputText;
import org.apache.myfaces.trinidad.model.CollectionModel;
import org.apache.myfaces.trinidad.model.SortableModel;

public class DynamicTable 
{
	    private SortableModel model;
	    private List<String> columnNames;
	    private CoreTable coreTable;
	    private String[] columns = {"col_1","col_2","col_3","col_4"};
	    private Object[][] data = {
	    		{"1","2","3",new Integer(4)},
	    		{"11","12","13",new Integer(14)},
	    		{"111","112","113",new Integer(114)},
	    		{"211","212","213",new Integer(1114)}
	    };
	    private List<Object> xdata = new ArrayList<Object>(); 

	    public Object[][] getData() {return data;}
	    
	    /*
	    @SuppressWarnings("unchecked")
		public void createColumn(int numOfCols)
	    {
	    	if(coreTable==null) return;
	    	List<UIComponent> tableChildList = coreTable.getChildren();

	    	for(int i=0;i<numOfCols;i++)
	    	{
	    		CoreColumn newCol= new CoreColumn();
	    		//set HeaderText for new col
	    		CoreOutputText newText = new CoreOutputText();
	    		// setValue binding of newText
	    		List colChildList = newCol.getChildren();
	    		colChildList.add(newText);
	    		newCol.setHeaderText("11111111");
	    		tableChildList.add(newCol);
	    	}
	    }
*/
		private void createColumn()
	    {
	    	if(coreTable==null) return;
	    	List<UIComponent> tableChildList = coreTable.getChildren();

    		//FacesContext context = FacesContext.getCurrentInstance();
    		ELContext ec = FacesContext.getCurrentInstance().getELContext();
    		ExpressionFactory ef = ExpressionFactory.newInstance();	    		
	    	
	    	for(int i=0;i<columns.length;i++)
	    	{
	    		CoreColumn newCol= new CoreColumn();
	    		newCol.setHeaderText(columns[i]);
	    		newCol.setAlign("center");
	    		Object x = data[0][i];
	    		UIComponent uic;
	    		ValueExpression ve = ef.createValueExpression(ec, "#{row["+i+"]}", Object.class);
	    		if (x instanceof Integer) {
	    			uic = new CoreCommandLink();	    		
	    			uic.setValueExpression("text", ve);
				}
	    		else {
	    			uic = new CoreOutputText();
	    			uic.setValueExpression("value", ve);
	    		}
	    		newCol.getChildren().add(uic);
	    		//newText.setValue("test");
	    		// setValue binding of newText
	    		//newCol.setValueExpression(arg0, arg1)
                //din = (DynamicImageNode) context.getELContext().getELResolver().getValue
//(ec, null, parName);
                /*
                RowExplorer re = (RowExplorer) context.getELContext().getELResolver().getV
alue(ec, null, parName);
                NodeWrapper nw = (NodeWrapper) re.getRow();
*/
	    		//ValueBinding vb = 
	    		//	FacesContext.getCurrentInstance().getApplication().createValueBinding("#{catalog.catalogid}");
	    		tableChildList.add(newCol);
	    	}
	    }
	    
	    
	    public DynamicTable() 
	    {
	    	xdata.add(data[0]);
	    	xdata.add(data[1]);
	    	xdata.add(data[2]);
	    	xdata.add(data[3]);
	    	getCollectionModel();
	        columnNames = new ArrayList<String>();
	        for(String x: columns) columnNames.add(x);
	        //addColumnN("first");
	        //addColumnN("second");
	        //addRow(null);
	        
	    }
	    // Add a new row to the model
	    @SuppressWarnings("unchecked")
		public void addRow(ActionEvent evt)
	    {
	        if(!columnNames.isEmpty()){
	            Map newRow = createNewRowMap();
	            ((List)model.getWrappedData()).add(newRow);
	        }
	    }
	
	    // Get table model
	    @SuppressWarnings("unchecked")
		public CollectionModel getCollectionModel()
	    {
	        if(model == null)
	            //model = new SortableModel(new ArrayList<Map>());
	        	model = new SortableModel(data);
	        return model;
	
	    }
	
	    // Get Column Names
	    @SuppressWarnings("unchecked")
		public Collection getColumnNames()
	    {
	        return columnNames;
	    }
	

	    @SuppressWarnings("unchecked")
		public void addColumnN(String colName)
	    {
	        // Add column name to list of names
	        columnNames.add(colName);
	        // Create new row
	        Map newRow = createNewRowMap();
	        // Add new column name to new row
	        newRow.put(colName,null);
	        // Update existing model
	        addColumnToCurrentModel(colName);
	      //  createColumn(1);
	    }
	    
	    
	    // Add column
	    @SuppressWarnings("unchecked")
		public void addColumn(ActionEvent evt)
	    {
	        // Create new column name
	        String colName = (columnNames.size()+1) + "";
	        // Add column name to list of names
	        columnNames.add(colName);
	        // Create new row
	        Map newRow = createNewRowMap();
	        // Add new column name to new row
	        newRow.put(colName,null);
	        // Update existing model
	        addColumnToCurrentModel(colName);
	     //   createColumn(1);
	    }
	
	    // Add column to existing rows
	    @SuppressWarnings("unchecked")
		private void addColumnToCurrentModel(String name)
	    {
	        List<Map> list = (List<Map>)model.getWrappedData();
	        for(Map row:list)
	            row.put(name,null);
	    }
	
	    @SuppressWarnings("unchecked")
		private Map createNewRowMap()
	    {
	        Map newRow = new HashMap();
	        for(String colName:columnNames)
	            newRow.put(colName,"zzzz");
	        return newRow;
	
	    }

		public CoreTable getCoreTable() {
			return coreTable;
		}

		public void setCoreTable(CoreTable table) 
		{
			this.coreTable = table;
			createColumn();
		}

		public List<Object> getXdata() {
			return xdata;
		}

}
