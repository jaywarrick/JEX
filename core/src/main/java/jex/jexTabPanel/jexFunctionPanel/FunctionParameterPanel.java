package jex.jexTabPanel.jexFunctionPanel;

import Database.Definition.ParameterSet;
import cruncher.JEXFunction;
import guiObject.JParameterListPanel;

import javax.swing.JLabel;
import javax.swing.JPanel;

import jex.statics.DisplayStatics;
import miscellaneous.FontUtility;
import net.miginfocom.swing.MigLayout;

public class FunctionParameterPanel {
	
	// Selected function and parameter pane
	private JEXFunction selectedFunction = null;
	private JPanel panel = new JPanel();
	private JPanel headerPane1;
	private JLabel title1;
	
	public FunctionParameterPanel()
	{
		initialize();
	}
	
	public JPanel panel()
	{
		return this.panel;
	}
	
	private void initialize()
	{
		// Make the parameter panel
		this.panel.setBackground(DisplayStatics.lightBackground);
		this.panel.setLayout(new MigLayout("flowy,ins 2", "[fill,grow]", "[]2[fill,grow]"));
		// Build the selector header
		title1 = new JLabel("No function selected");
		headerPane1 = new JPanel(new MigLayout("flowy,center,ins 1", "[grow,center]", "[center]"));
		headerPane1.setBackground(DisplayStatics.menuBackground);
		title1.setFont(FontUtility.boldFont);
		headerPane1.add(title1, "wmin 0");
		this.panel.add(this.headerPane1, "growx");
	}
	
	public void selectFunction(JEXFunction function)
	{
		this.selectedFunction = function;
		this.panel.removeAll();
		
		if(this.selectedFunction == null)
		{
			this.title1.setText("No function selected");
			this.panel.add(headerPane1, "growx");
		}
		else
		{
			ParameterSet parameters = this.selectedFunction.getParameters();
			JParameterListPanel paramPanel = new JParameterListPanel(parameters);
			
			String funName = (function == null) ? "No function selected" : function.getFunctionName();
			this.title1.setText(funName);
			this.title1.setToolTipText(funName);
			
			this.panel.add(headerPane1, "growx");
			this.panel.add(paramPanel.panel(), "grow");
		}
		this.panel.revalidate();
		this.panel.repaint();
	}
	
}
