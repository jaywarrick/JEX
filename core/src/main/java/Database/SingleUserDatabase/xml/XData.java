package Database.SingleUserDatabase.xml;

import Database.DBObjects.JEXData;
import Database.Definition.Type;

import java.util.ArrayList;
import java.util.List;

public class XData extends XElement {
	
	private static final long serialVersionUID = 1L;
	
	public static final String ELEMENTNAME = "XData";
	
	public XData()
	{
		super(ELEMENTNAME);
	}
	
	public XData(Type type)
	{
		super(ELEMENTNAME);
		// This XElement attribute (which is different from and Att, see setAtt())
		// is purely for readability of the XML
		// We don't access this information otherwise and shouldn't rely on it.
		this.setAttribute("XMLReadabilityNote", type.toString());
	}
	
	public void addMeta(String key, String value)
	{
		this.setAtt(key, value, "");
	}
	
	public void addMetaWithCategory(String key, String category, String value)
	{
		this.setAtt(key, value, category);
	}
	
	public void addDataSingle(XDataSingle data)
	{
		this.addXElement(data);
	}
	
	public String getTypeField()
	{
		return this.getAtt(JEXData.TYPE);
	}
	
	public XDataSingle getFirstDataElement()
	{
		XDataSingle result = (XDataSingle) this.collection.getContent(0);
		return result;
	}
	
	public List<XDataSingle> getSingleDataElements()
	{
		List<XDataSingle> result = new ArrayList<XDataSingle>(0);
		List<XElement> children = this.getXElements();
		for (XElement child : children)
		{
			result.add((XDataSingle) child);
		}
		return result;
	}
	
}
