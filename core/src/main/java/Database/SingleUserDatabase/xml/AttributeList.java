package Database.SingleUserDatabase.xml;

import java.util.ArrayList;
import java.util.List;

import miscellaneous.XMLUtility;

import org.jdom.Element;

public class AttributeList extends Element {
	
	private static final long serialVersionUID = 1L;
	
	public static final String ELEMENTNAME = "AttList";
	
	public AttributeList()
	{
		super(ELEMENTNAME);
	}
	
	public AttributeList(List<Attribute> listOfAttributes)
	{
		this();
		for (Attribute att : listOfAttributes)
		{
			this.addAtt(att);
		}
	}
	
	public boolean hasAtt(String name)
	{
		Attribute att = this.getAttWithName(name);
		return att != null;
	}
	
	public void addAtt(Attribute att)
	{
		this.addContent(att);
	}
	
	public void removeAtt(Attribute att)
	{
		if(att != null)
		{
			this.removeContent(att);
		}
	}
	
	public void removeAttWithName(String name)
	{
		this.removeAtt(this.getAttWithName(name));
	}
	
	@SuppressWarnings("unchecked")
	public Attribute getAttWithName(String name)
	{
		List<Element> children = this.getChildren();
		for (Element elem : children)
		{
			Attribute att = (Attribute) elem;
			if(att.getAttName().equals(name))
			{
				return att;
			}
		}
		return null;
	}
	
	public String getValueOfAttWithName(String name)
	{
		Attribute att = this.getAttWithName(name);
		if(att == null)
		{
			return null;
		}
		return att.getAttValue();
	}
	
	public List<Attribute> getAttsWithCategory(String catName)
	{
		List<Attribute> atts = this.getAtts();
		List<Attribute> result = new ArrayList<Attribute>(0);
		for (Attribute att : atts)
		{
			if(att.getAttCategory().equals(catName))
			{
				result.add(att);
			}
		}
		return result;
	}
	
	@SuppressWarnings("unchecked")
	public List<Attribute> getAtts()
	{
		List<Element> children = this.getChildren();
		List<Attribute> result = new ArrayList<Attribute>(0);
		for (Element child : children)
		{
			result.add((Attribute) child);
		}
		return result;
	}
	
	public List<String> getAttNames()
	{
		List<String> result = new ArrayList<String>(0);
		List<Attribute> atts = this.getAtts();
		for (Attribute att : atts)
		{
			result.add(att.getAttName());
		}
		return result;
	}
	
	@Override
	public String toString()
	{
		String result = XMLUtility.toXML(this);
		return result;
	}
}
