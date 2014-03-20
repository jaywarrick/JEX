package jex.jexTabPanel.jexFunctionPanel;

import javax.swing.JPanel;

import jex.jexTabPanel.JEXTabPanelController;

public class JEXFunctionPanelController extends JEXTabPanelController {
	
	private JEXFunctionPanel centerPane;
	// private JEXFunctionRightPanel rightPane;
	private FunctionChooserPanel rightPane;
	
	public JEXFunctionPanelController()
	{
		// this.fixedPanelWidth = 200;
		// this.resizeWeight = 0.0;
		// rightPane = new JEXFunctionRightPanel();
		
		centerPane = new JEXFunctionPanel();
		this.rightPane = new FunctionChooserPanel(centerPane);
	}
	
	// ////
	// //// JEXTabPanel interface
	// ////
	
	public JPanel getMainPanel()
	{
		return centerPane;
	}
	
	public JPanel getLeftPanel()
	{
		return null;
	}
	
	public JPanel getRightPanel()
	{
		return rightPane.panel();
	}
	
	public void closeTab()
	{
		// if (centerPane != null) centerPane.deInitialize();
		// if (rightPane != null) rightPane.deInitialize();
		// centerPane = null;
		// rightPane = null;
	}
	
	public int getFixedPanelWidth()
	{
		return this.fixedPanelWidth;
	}
	
	public void setFixedPanelWidth(int width)
	{
		this.fixedPanelWidth = width;
	}
	
	public double getResizeWeight()
	{
		return this.resizeWeight;
	}
	
}
