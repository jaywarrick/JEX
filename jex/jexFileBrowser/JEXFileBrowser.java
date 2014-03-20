package jex.jexFileBrowser;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;
import javax.swing.JTree;

public class JEXFileBrowser extends JPanel implements ActionListener {
	
	private static final long serialVersionUID = 1L;
	
	// Class variables
	private JEXFileBrowserController controller;
	
	// Graphics
	@SuppressWarnings("unused")
	private JTree fileTree;
	
	public JEXFileBrowser(JEXFileBrowserController controller)
	{
		this.controller = controller;
		
		initialize();
	}
	
	private void initialize()
	{
		fileTree = new JTree();
	}
	
	// ---------------------------------------------
	// Events
	// ---------------------------------------------
	
	/**
	 * Forward the event to the controller
	 */
	public void actionPerformed(ActionEvent e)
	{
		controller.actionPerformed(e);
	}
}
