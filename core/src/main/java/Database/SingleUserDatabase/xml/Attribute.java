package Database.SingleUserDatabase.xml;

import org.jdom.Element;

public class Attribute extends Element {
	
	private static final long serialVersionUID = 1L;
	
	// Common Values
	public static String DEFAULTCATEGORY = "DefaultCat";
	public static final String ELEMENTNAME = "Att";
	
	// Standard Fields
	public static String NAME = "Name";
	public static String VALUE = "Value";
	public static String CATEGORY = "Category";
	
	public Attribute()
	{
		super(ELEMENTNAME);
	}
	
	public Attribute(String attName, Object attValue, String category)
	{
		this();
		
		this.setAttName(attName);
		this.setAttValue(attValue);
		this.setAttCategory(category);
	}
	
	public Attribute(String attName, Object attValue)
	{
		this();
		
		this.setAttName(attName);
		this.setAttValue(attValue);
		this.setAttCategory(Attribute.DEFAULTCATEGORY);
	}
	
	public String getAttName()
	{
		String value = this.getAttributeValue(Attribute.NAME);
		return value;
	}
	
	public void setAttName(String attName)
	{
		this.setAttribute(Attribute.NAME, attName);
	}
	
	public String getAttValue()
	{
		String value = this.getAttributeValue(Attribute.VALUE);
		return value;
	}
	
	public void setAttValue(Object o)
	{
		this.setAttribute(Attribute.VALUE, o.toString());
	}
	
	public String getAttCategory()
	{
		String categoryStr = this.getAttributeValue(Attribute.CATEGORY);
		if(categoryStr != null)
		{
			return categoryStr;
		}
		return Attribute.DEFAULTCATEGORY;
	}
	
	public void setAttCategory(String category)
	{
		this.setAttribute(Attribute.CATEGORY, category);
	}
	
}
