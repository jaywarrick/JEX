package preferences;

import javax.swing.JTextField;
import javax.swing.text.JTextComponent;

/**
 * StringPropertyEditor.<br>
 * 
 */
public class StringPropertyEditor extends AbstractPropertyEditor {
	
	public StringPropertyEditor()
	{
		editor = new JTextField();
		((JTextField) editor).setBorder(LookAndFeelTweaks.EMPTY_BORDER);
	}
	
	@Override
	public Object getValue()
	{
		return ((JTextComponent) editor).getText();
	}
	
	@Override
	public void setValue(Object value)
	{
		if(value == null)
		{
			((JTextComponent) editor).setText("");
		}
		else
		{
			((JTextComponent) editor).setText(String.valueOf(value));
		}
	}
	
}
