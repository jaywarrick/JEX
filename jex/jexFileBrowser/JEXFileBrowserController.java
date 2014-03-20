package jex.jexFileBrowser;

import java.awt.event.ActionEvent;

public class JEXFileBrowserController {
	
	private JEXFileBrowser panel;
	
	public JEXFileBrowserController()
	{
		initialize();
	}
	
	/**
	 * Initialize
	 */
	private void initialize()
	{
		panel = new JEXFileBrowser(this);
	}
	
	// ---------------------------------------------
	// Getters and setters
	// ---------------------------------------------
	
	public JEXFileBrowser getPanel()
	{
		return panel;
	}
	
	// ---------------------------------------------
	// Events
	// ---------------------------------------------
	/**
	 * Captured from the panel
	 */
	public void actionPerformed(ActionEvent e)
	{   
		
	}
}
