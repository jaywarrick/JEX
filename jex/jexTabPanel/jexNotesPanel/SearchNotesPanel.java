package jex.jexTabPanel.jexNotesPanel;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import jex.statics.DisplayStatics;

public class SearchNotesPanel extends JPanel {
	
	private static final long serialVersionUID = 1L;
	
	public SearchNotesPanel()
	{
		initialize();
	}
	
	private void initialize()
	{
		this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		this.setBackground(DisplayStatics.lightBackground);
	}
	
	public void changeDetected()
	{   
		
	}
}