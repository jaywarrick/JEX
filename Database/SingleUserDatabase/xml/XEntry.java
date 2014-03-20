package Database.SingleUserDatabase.xml;

public class XEntry extends XElement {
	
	private static final long serialVersionUID = 1L;
	
	public static final String ELEMENTNAME = "XEntry";
	
	public XEntry()
	{
		super(ELEMENTNAME);
	}
	
	public void addMeta(String key, String value)
	{
		this.setAtt(key, value, "");
	}
	
	public void addData(XData data)
	{
		this.addXElement(data);
	}
	
}
