package preferences;

import java.text.NumberFormat;

import javax.swing.JFormattedTextField;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;

/**
 * Base editor for numbers. <br>
 */
public class NumberPropertyEditor extends AbstractPropertyEditor<JFormattedTextField> {
	
	@SuppressWarnings("rawtypes")
	private final Class type;
	private Object lastGoodValue;
	
	@SuppressWarnings("rawtypes")
	public NumberPropertyEditor(Class type)
	{
		if(!Number.class.isAssignableFrom(type))
		{
			throw new IllegalArgumentException("type must be a subclass of Number");
		}
		
		editor = new JFormattedTextField();
		this.type = type;
		editor.setValue(getDefaultValue());
		editor.setBorder(LookAndFeelTweaks.EMPTY_BORDER);
		
		// use a custom formatter to have numbers with up to 64 decimals
		NumberFormat format = NumberConverters.getDefaultFormat();
		
		editor.setFormatterFactory(new DefaultFormatterFactory(new NumberFormatter(format)));
	}
	
	@Override
	public Object getValue()
	{
		String text = ((JTextField) editor).getText();
		if(text == null || text.trim().length() == 0)
		{
			return getDefaultValue();
		}
		
		// allow comma or colon
		text = text.replace(',', '.');
		
		// collect all numbers from this textfield
		StringBuffer number = new StringBuffer();
		number.ensureCapacity(text.length());
		for (int i = 0, c = text.length(); i < c; i++)
		{
			char character = text.charAt(i);
			if('.' == character || '-' == character || (Double.class.equals(type) && 'E' == character) || (Float.class.equals(type) && 'E' == character) || Character.isDigit(character))
			{
				number.append(character);
			}
			else if(' ' == character)
			{
				continue;
			}
			else
			{
				break;
			}
		}
		
		try
		{
			lastGoodValue = ConverterRegistry.instance().convert(type, number.toString());
		}
		catch (Exception e)
		{
			UIManager.getLookAndFeel().provideErrorFeedback(editor);
		}
		
		return lastGoodValue;
	}
	
	@Override
	public void setValue(Object value)
	{
		if(value instanceof Number)
		{
			editor.setText(value.toString());
		}
		else
		{
			editor.setValue(getDefaultValue());
		}
		lastGoodValue = value;
	}
	
	@SuppressWarnings("unchecked")
	private Object getDefaultValue()
	{
		try
		{
			return type.getConstructor(new Class[] { String.class }).newInstance(new Object[] { "0" });
		}
		catch (Exception e)
		{
			// will not happen
			throw new RuntimeException(e);
		}
	}
	
}