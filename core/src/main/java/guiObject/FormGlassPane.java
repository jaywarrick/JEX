package guiObject;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpec;
import com.jgoodies.forms.layout.RowSpec;
import com.jgoodies.forms.layout.Sizes;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JTextField;

import jex.statics.DisplayStatics;
import logs.Logs;

public class FormGlassPane extends DialogGlassCenterPanel implements ActionListener {
	
	private static final long serialVersionUID = 1L;
	private LinkedHashMap<String,String> form;
	private LinkedHashMap<String,JTextField> formField;
	private Method method;
	private Object listener;
	
	public FormGlassPane(LinkedHashMap<String,String> form)
	{
		this.form = form;
		this.formField = new LinkedHashMap<String,JTextField>();
		
		// initialize
		initialize();
	}
	
	/**
	 * Initialize
	 */
	private void initialize()
	{
		if(form == null || form.size() == 0)
		{
			JLabel infoLabel = new JLabel("No information to display");
			
			this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
			this.setBackground(DisplayStatics.lightBackground);
			this.add(Box.createVerticalGlue());
			this.add(infoLabel);
			this.add(Box.createVerticalGlue());
			
			return;
		}
		
		// Make the form layout
		ColumnSpec column1 = new ColumnSpec(ColumnSpec.FILL, Sizes.dluX(70), FormSpec.NO_GROW);
		ColumnSpec column2 = new ColumnSpec(ColumnSpec.FILL, Sizes.dluX(70), FormSpec.DEFAULT_GROW);
		ColumnSpec[] cspecs = new ColumnSpec[] { column1, column2 };
		
		int size = form.size();
		RowSpec[] rspecs = new RowSpec[size];
		for (int i = 0; i < size; i++)
			rspecs[i] = new RowSpec(RowSpec.CENTER, Sizes.dluX(14), FormSpec.NO_GROW);
		
		FormLayout layout = new FormLayout(cspecs, rspecs);
		
		CellConstraints cc = new CellConstraints();
		this.setLayout(layout);
		this.setBackground(DisplayStatics.lightBackground);
		
		// Fill the layout
		int index = 1;
		for (String key : form.keySet())
		{
			String value = form.get(key);
			
			JLabel nameLabel = new JLabel(key);
			this.add(nameLabel, cc.xy(1, index));
			JTextField nameField = new JTextField(value);
			this.add(nameField, cc.xy(2, index));
			
			formField.put(key, nameField);
			index++;
		}
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
	
	public Map<String,String> form()
	{
		return this.form;
	}
	
	/**
	 * Called when clicked yes on the dialog panel
	 */
	@Override
	public void yes()
	{
		Logs.log("Form pane validated", 1, this);
		
		for (String name : formField.keySet())
		{
			JTextField field = formField.get(name);
			String value = field.getText();
			form.put(name, value);
		}
		
		try
		{
			this.method().invoke(this.listener(), new Object[] { this });
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
		Logs.log("Form pane cancelled", 1, this);
	}
	
	// ----------------------------------------------------
	// --------- EVENT HANDLING FUNCTIONS -----------------
	// ----------------------------------------------------
	
	public void actionPerformed(ActionEvent e)
	{}
}
