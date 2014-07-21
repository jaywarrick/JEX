package preferences;

import java.awt.Insets;

/**
 * InsetsPropertyEditor. <br>
 * 
 */
public class InsetsPropertyEditor extends StringConverterPropertyEditor {
	
	@Override
	protected Object convertFromString(String text)
	{
		return ConverterRegistry.instance().convert(Insets.class, text);
	}
	
}
