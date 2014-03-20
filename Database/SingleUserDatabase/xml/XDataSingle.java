package Database.SingleUserDatabase.xml;

import Database.Definition.Type;

public class XDataSingle extends XElement {
	
	private static final long serialVersionUID = 1L;
	
	public static final String ELEMENTNAME = "XDataSingle";
	
	public XDataSingle()
	{
		super(ELEMENTNAME);
	}
	
	public XDataSingle(Type type)
	{
		super(ELEMENTNAME);
		// This XElement attribute (which is different from and Att, see setAtt())
		// is purely for readability of the XML
		// We don't access this information otherwise and shouldn't rely on it.
		this.setAttribute("XMLReadabilityNote", type.toString());
	}
	
	public void addMeta(String key, String value)
	{
		this.setAtt(key, value);
	}
	
	public void addMeta(String key, String value, String category)
	{
		this.setAtt(key, value, category);
	}
	
}
