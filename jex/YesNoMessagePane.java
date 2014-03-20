package jex;

import guiObject.DialogGlassCenterPanel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;

import jex.statics.DisplayStatics;
import logs.Logs;

public class YesNoMessagePane extends DialogGlassCenterPanel implements ActionListener {
	
	private static final long serialVersionUID = 1L;
	private JLabel message = new JLabel();
	private Method method;
	private Object listener;
	
	public YesNoMessagePane(String message)
	{
		this.message.setText(message);
		
		// initialize
		initialize();
	}
	
	public void callMethodOfClassOnAcceptance(Object listener, String methodName)
	{
		this.listener = listener;
		try
		{
			method = listener.getClass().getMethod(methodName, new Class<?>[0]);
		}
		catch (SecurityException e)
		{
			Logs.log("Couldn't find method to create the desired Slot object", 0, this);
		}
		catch (NoSuchMethodException e)
		{
			Logs.log("Couldn't find method to create the desired Slot object", 0, this);
		}
	}
	
	public void callMethodOfClassOnAcceptance(Object listener, String methodName, Class<?>... slotArgTypes)
	{
		this.listener = listener;
		try
		{
			method = listener.getClass().getMethod(methodName, slotArgTypes);
		}
		catch (SecurityException e)
		{
			Logs.log("Couldn't find method to create the desired Slot object", 0, this);
		}
		catch (NoSuchMethodException e)
		{
			Logs.log("Couldn't find method to create the desired Slot object", 0, this);
		}
	}
	
	public Method method()
	{
		return this.method;
	}
	
	public Object listener()
	{
		return this.listener;
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
		Logs.log("Message passed - ok answered", 1, this);
		try
		{
			this.method().invoke(this.listener(), (Object[]) null);
		}
		catch (IllegalArgumentException e)
		{
			e.printStackTrace();
		}
		catch (IllegalAccessException e)
		{
			e.printStackTrace();
		}
		catch (InvocationTargetException e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Called when clicked cancel on the dialog panel
	 */
	@Override
	public void cancel()
	{
		Logs.log("Message passed - no answered", 1, this);
	}
	
	// ----------------------------------------------------
	// --------- EVENT HANDLING FUNCTIONS -----------------
	// ----------------------------------------------------
	
	public void actionPerformed(ActionEvent e)
	{}
}
