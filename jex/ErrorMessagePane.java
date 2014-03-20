package jex;

import guiObject.DialogGlassCenterPanel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;

import jex.statics.DisplayStatics;
import logs.Logs;

public class ErrorMessagePane extends DialogGlassCenterPanel implements ActionListener {
	
	private static final long serialVersionUID = 1L;
	private JLabel message = new JLabel();
	
	public ErrorMessagePane(String message)
	{
		this.message.setText(message);
		
		// initialize
		initialize();
	}
	
	/**
	 * Initialize
	 */
	private void initialize()
	{
		this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		this.setBackground(DisplayStatics.lightBackground);
		
		// Fill the layout
		message.setAlignmentX(CENTER_ALIGNMENT);
		this.add(Box.createVerticalStrut(20));
		this.add(message);
		this.add(Box.createVerticalGlue());
	}
	
	/**
	 * Called when clicked yes on the dialog panel
	 */
	@Override
	public void yes()
	{
		Logs.log("Message passed", 1, this);
	}
	
	/**
	 * Called when clicked cancel on the dialog panel
	 */
	@Override
	public void cancel()
	{
		Logs.log("Message passed", 1, this);
	}
	
	// ----------------------------------------------------
	// --------- EVENT HANDLING FUNCTIONS -----------------
	// ----------------------------------------------------
	
	public void actionPerformed(ActionEvent e)
	{}
}
