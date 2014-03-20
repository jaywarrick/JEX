package preferences;

import java.awt.Rectangle;

/**
 * RectanglePropertyEditor. <br>
 * 
 */
public class RectanglePropertyEditor extends StringConverterPropertyEditor {
	
	@Override
	protected Object convertFromString(String text)
	{
		return ConverterRegistry.instance().convert(Rectangle.class, text);
	}
	
}
