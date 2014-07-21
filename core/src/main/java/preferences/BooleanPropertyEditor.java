package preferences;

/**
 * BooleanPropertyEditor.<br>
 * 
 */
public class BooleanPropertyEditor extends ComboBoxPropertyEditor {
	
	public BooleanPropertyEditor()
	{
		super();
		Object[] values = new Object[] { new Value(Boolean.TRUE, ResourceManager.common().getString("true")), new Value(Boolean.FALSE, ResourceManager.common().getString("false")) };
		setAvailableValues(values);
	}
	
}
