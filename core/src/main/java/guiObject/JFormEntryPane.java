package guiObject;

import Database.Definition.Parameter;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class JFormEntryPane extends JPanel {
	
	private static final long serialVersionUID = 1L;
	
	// Model variable
	JFormEntry entry;
	
	// GUI variables
	JLabel titleField;
	JComponent resultField;
	Dimension size = new Dimension(100, 20);
	
	public JFormEntryPane(JFormEntry entry)
	{
		this.entry = entry;
		initialize();
	}
	
	/**
	 * Set the text label dimension
	 * 
	 * @param size
	 */
	public void setLabelSize(Dimension size)
	{
		this.size = size;
	}
	
	/**
	 * initialize the entry
	 */
	private void initialize()
	{
		// This
		this.setBackground(Color.white);
		this.setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		this.setMaximumSize(new Dimension(1000, 20));
		
		// GUI objects
		titleField = new JLabel(entry.title);
		if(entry.type.equals("DropDown") && entry.options != null)
		{
			resultField = new JComboBox(entry.options);
			((JComboBox) resultField).setSelectedIndex(entry.selected);
		}
		else if(entry.type.equals("CheckBox") && entry.options != null)
		{
			resultField = new JCheckBox();
		}
		else
		{
			resultField = new JTextField("");
			if(entry.options != null && entry.options.length > 0)
			{
				((JTextField) resultField).setText(entry.options[0]);
			}
		}
		
		titleField.setBackground(Color.white);
		titleField.setMaximumSize(size);
		titleField.setPreferredSize(size);
		resultField.setBackground(Color.white);
		resultField.setMaximumSize(new Dimension(1000, 20));
		if(entry.note != null)
			resultField.setToolTipText(entry.note);
		
		this.add(Box.createHorizontalStrut(5));
		this.add(titleField);
		this.add(resultField);
		this.add(Box.createHorizontalStrut(5));
	}
	
	/**
	 * Return the set value in the result Field
	 * 
	 * @return String value of the component
	 */
	public String getValue()
	{
		String result;
		if(entry.type.equals(Parameter.DROPDOWN) && entry.options != null)
		{
			result = ((JComboBox) resultField).getSelectedItem().toString();
		}
		else if(entry.type.equals(Parameter.CHECKBOX) && entry.options != null)
		{
			result = "" + ((JCheckBox) resultField).isSelected();
		}
		else
		{
			result = ((JTextField) resultField).getText();
		}
		
		return result;
	}
}