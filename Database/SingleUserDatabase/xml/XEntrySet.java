package Database.SingleUserDatabase.xml;

public class XEntrySet extends XElement {
	
	private static final long serialVersionUID = 1L;
	
	public static final String ELEMENTNAME = "XEntrySet";
	
	public XEntrySet()
	{
		super(ELEMENTNAME);
		// this.setName(ELEMENTNAME);
	}
	
	public void addMeta(String key, String value)
	{
		this.setAtt(key, value, "");
	}
	
	public void addEntry(XEntry entry)
	{
		this.addXElement(entry);
	}
}
