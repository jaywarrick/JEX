package preferences;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * A renderer for Date.
 * 
 * @author Ricardo Lopes
 */
public class DateRenderer extends DefaultCellRenderer {
	
	private static final long serialVersionUID = 1L;
	private DateFormat dateFormat;
	
	public DateRenderer()
	{
		this(DateFormat.getDateInstance(DateFormat.SHORT));
	}
	
	public DateRenderer(String formatString)
	{
		this(formatString, Locale.getDefault());
	}
	
	public DateRenderer(Locale l)
	{
		this(DateFormat.getDateInstance(DateFormat.SHORT, l));
	}
	
	public DateRenderer(String formatString, Locale l)
	{
		this(new SimpleDateFormat(formatString, l));
	}
	
	public DateRenderer(DateFormat dateFormat)
	{
		this.dateFormat = dateFormat;
	}
	
	@Override
	public void setValue(Object value)
	{
		if(value == null)
		{
			setText("");
		}
		else
		{
			setText(dateFormat.format((Date) value));
		}
	}
	
}