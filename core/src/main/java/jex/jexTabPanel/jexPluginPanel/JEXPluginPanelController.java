package jex.jexTabPanel.jexPluginPanel;

import javax.swing.JPanel;

import jex.jexTabPanel.JEXTabPanelController;
import jex.statics.DisplayStatics;
import signals.SSCenter;

public class JEXPluginPanelController extends JEXTabPanelController {
	
	private JEXPluginPanel centerPane;
	private PluginPanel rightPane;
	
	public JEXPluginPanelController()
	{
		centerPane = new JEXPluginPanel();
		rightPane = new PluginPanel();
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

class JEXPluginPanel extends JPanel {
	
	private static final long serialVersionUID = 1L;
	
	JEXPluginPanel()
	{
		initialize();
	}
	
	/**
	 * Detach the signals
	 */
	public void deInitialize()
	{
		SSCenter.defaultCenter().disconnect(this);
	}
	
	private void initialize()
	{
		this.setBackground(DisplayStatics.background);
		
	}
	
}