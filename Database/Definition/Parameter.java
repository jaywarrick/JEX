package Database.Definition;

import miscellaneous.ArrayUtility;

public class Parameter {
	
	public static int TEXTFIELD = 0;
	public static int DROPDOWN = 1;
	public static int FILECHOOSER = 2;
	public static int CHECKBOX = 3;
	public static int PASSWORD = 4;
	
	public String title;
	public String note;
	public int type;
	public String[] options;
	public String result = "";
	int defaultOption = 0;
	
	/**
	 * Create a form line
	 * 
	 * @param title
	 */
	public Parameter(String title)
	{
		this.title = title;
		this.note = "";
		this.type = TEXTFIELD;
		this.options = new String[] { "value" };
	}
	
	/**
	 * Create a form line
	 * 
	 * @param title
	 * @param note
	 * @param defaultValue
	 */
	public Parameter(String title, String note, String defaultValue)
	{
		this.title = title;
		this.note = note;
		this.type = TEXTFIELD;
		this.result = defaultValue;
		this.options = new String[] { defaultValue };
	}
	
	/**
	 * Typically used for checkbox and filechooser
	 * 
	 * @param title
	 * @param note
	 * @param defaultValue
	 */
	public Parameter(String title, String note, int type, String defaultValue)
	{
		this.title = title;
		this.note = note;
		this.type = type;
		this.result = defaultValue;
		this.options = new String[] { this.result };
	}
	
	/**
	 * Typically used for checkbox
	 * 
	 * @param title
	 * @param note
	 * @param defaultValue
	 */
	public Parameter(String title, String note, int type, boolean defaultValue)
	{
		this.title = title;
		this.note = note;
		this.type = type;
		this.result = "" + defaultValue;
		this.options = new String[] { this.result };
	}
	
	/**
	 * Create a form line
	 * 
	 * @param title
	 * @param note
	 * @param type
	 * @param options
	 */
	public Parameter(String title, String note, int type, String[] options)
	{
		this.title = title;
		this.note = note;
		this.type = type;
		this.options = options;
		this.result = options[0];
	}
	
	/**
	 * Create a form line
	 * 
	 * @param title
	 * @param note
	 * @param type
	 * @param options
	 */
	public Parameter(String title, String note, int type, String[] options, int defaultOption)
	{
		this.title = title;
		this.note = note;
		this.type = type;
		this.options = options;
		this.defaultOption = defaultOption;
		if(options.length >= defaultOption)
		{
			this.result = options[defaultOption];
		}
		else
		{
			this.defaultOption = 0;
			this.result = options[0];
		}
	}
	
	/**
	 * Set the current value of the formline
	 * 
	 * @param value
	 */
	public void setValue(String value)
	{
		this.result = value;
	}
	
	/**
	 * Return the current value of the formline
	 * 
	 * @return value of the formline
	 */
	public String getValue()
	{
		return this.result;
	}
	
	/**
	 * Returns the name of the parameter
	 * 
	 * @return
	 */
	public String getTitle()
	{
		return this.title;
	}
	
	public Parameter duplicate()
	{
		Parameter ret = new Parameter(this.title, this.note, this.type, ArrayUtility.duplicateStringArray(this.options), this.defaultOption);
		ret.setValue(this.result);
		return ret;
	}
	
}
