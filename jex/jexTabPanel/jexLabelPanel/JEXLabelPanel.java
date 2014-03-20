package jex.jexTabPanel.jexLabelPanel;

import javax.swing.JPanel;

import jex.statics.DisplayStatics;

public class JEXLabelPanel {
	
	private JPanel panel;
	
	JEXLabelPanel()
	{
		this.initialize();
	}
	
	private void initialize()
	{
		this.panel = new JPanel();
		this.panel.setBackground(DisplayStatics.background);
	}
	
	public JPanel panel()
	{
		return this.panel;
	}
	
}
