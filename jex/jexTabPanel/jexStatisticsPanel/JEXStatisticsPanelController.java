package jex.jexTabPanel.jexStatisticsPanel;

import javax.swing.JPanel;

import jex.jexTabPanel.JEXTabPanelController;

public class JEXStatisticsPanelController extends JEXTabPanelController {
	
	private JEXStatisticsPanel centerPane;
	private JEXStatisticsRightPanel rightPane;
	
	public JEXStatisticsPanelController()
	{
		centerPane = new JEXStatisticsPanel();
		rightPane = new JEXStatisticsRightPanel();
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
