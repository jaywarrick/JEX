package plugins.plugin;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JDialog;
import javax.swing.WindowConstants;

import jex.statics.JEXStatics;
import jex.statics.KeyStatics;
import logs.Logs;

public class PlugIn extends JDialog implements KeyListener {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private PlugInController controller;
	
	public PlugIn(PlugInController controller)
	{
		super(JEXStatics.main, false);
		this.controller = controller;
		this.getRootPane().setFocusable(true);
		this.getRootPane().addKeyListener(this);
		this.getRootPane().requestFocusInWindow();
		JEXStatics.plugins.add(controller);
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
	}
	
	@Override
	public void dispose()
	{
		if(this.controller != null)
			this.controller.finalizePlugIn();
		if(JEXStatics.plugins.contains(controller))
			JEXStatics.plugins.remove(controller);
		controller = null;
		super.dispose();
		if(JEXStatics.plugins.size() > 0)
			JEXStatics.plugins.get(0).plugIn().setVisible(true);
		Logs.log("Disposing PlugIn Window", 0, this);
	}
	
	public void keyPressed(KeyEvent e)
	{
		if(KeyStatics.ctrlOrCmdIsDown() && e.getKeyCode() == KeyEvent.VK_W)
		{
			// Close the window
			this.dispose();
		}
	}
	
	public void keyReleased(KeyEvent e)
	{}
	
	public void keyTyped(KeyEvent e)
	{}
	
}
