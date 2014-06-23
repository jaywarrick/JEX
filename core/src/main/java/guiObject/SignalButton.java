package guiObject;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;

import signals.SSCenter;

public class SignalButton extends JButton implements ActionListener {
	
	private static final long serialVersionUID = 1L;
	// statics
	public static final String SIG_ButtonClicked_NULL = "clicked";
	
	public SignalButton()
	{
		super();
		this.addActionListener(this);
	}
	
	public SignalButton(String text)
	{
		super(text);
		this.addActionListener(this);
	}
	
	/**
	 * Emit signal when button clicked
	 */
	public void actionPerformed(ActionEvent e)
	{
		SSCenter.defaultCenter().emit(this, SIG_ButtonClicked_NULL, (Object[]) null);
	}
}
