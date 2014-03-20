package guiObject;

public class JFormEntry {
	
	String title;
	String note;
	String type;
	String[] options;
	String result = "";
	int selected = 0;
	
	/**
	 * Create a form line
	 * 
	 * @param title
	 * @param note
	 * @param type
	 * @param options
	 */
	public JFormEntry(String title, String note, String type, String[] options)
	{
		this.title = title;
		this.note = note;
		this.type = type;
		this.options = options;
	}
	
	/**
	 * Create a form line
	 * 
	 * @param title
	 * @param note
	 * @param type
	 * @param options
	 * @param selected
	 */
	public JFormEntry(String title, String note, String type, String[] options, int selected)
	{
		this.title = title;
		this.note = note;
		this.type = type;
		this.options = options;
		this.selected = selected;
	}
}