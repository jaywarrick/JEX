package jex.jexTabPanel.jexLabelPanel;

import javax.swing.JPanel;

import jex.jexTabPanel.JEXTabPanelController;

public class JEXLabelPanelController extends JEXTabPanelController {
	
	private LabelDistributionPanel centerPane;
	private JPanel rightPane;
	
	public JEXLabelPanelController()
	{
		this.fixedPanelWidth = 0;
		centerPane = new LabelDistributionPanel();
		this.rightPane = new JPanel();
	}
	
	// ////
	// //// JEXTabPanel interface
	// ////
	
	public JPanel getMainPanel()
	{
		return centerPane.panel();
	}
	
	public JPanel getLeftPanel()
	{
		return null;
	}
	
	public JPanel getRightPanel()
	{
		return this.rightPane;
	}
	
	public void closeTab()
	{
		if(centerPane != null)
			centerPane.deInitialize();
		centerPane = null;
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