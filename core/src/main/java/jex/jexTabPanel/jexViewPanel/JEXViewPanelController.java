package jex.jexTabPanel.jexViewPanel;

import javax.swing.JPanel;

import jex.jexTabPanel.JEXTabPanelController;

public class JEXViewPanelController extends JEXTabPanelController {
	
	private JEXViewPanel centerPane;
	private JEXViewRightPanel rightPane;
	
	public JEXViewPanelController()
	{
		centerPane = new JEXViewPanel();
		rightPane = new JEXViewRightPanel();
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
		return rightPane;
	}
	
	public void closeTab()
	{
		if(rightPane != null)
			rightPane.deInitialize();
		if(centerPane != null)
			centerPane.deInitialize();
		centerPane = null;
		rightPane = null;
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
