package preferences;

import java.util.ArrayList;
import java.util.List;

import miscellaneous.XMLUtility;

import org.jdom.Content;
import org.jdom.Element;

public class XMLPreferences_AttributeList extends Element {
	
	private static final long serialVersionUID = 1L;
	
	public static final String ELEMENTNAME = "AttList";
	
	public XMLPreferences_AttributeList()
	{
		super(ELEMENTNAME);
	}
	
	public XMLPreferences_AttributeList(List<XMLPreferences_Attribute> listOfAttributes)
	{
		this();
		for (XMLPreferences_Attribute att : listOfAttributes)
		{
			this.addAtt(att);
		}
	}
	
	public boolean hasAtt(String name)
	{
		XMLPreferences_Attribute att = this.getAttWithName(name);
		return att != null;
	}
	
	public void addAtt(XMLPreferences_Attribute att)
	{
		this.addContent(att);
	}
	
	public void removeAtt(XMLPreferences_Attribute att)
	{
		System.out.println(this);
		if(att != null)
		{
			this.removeContent(att);
		}
		System.out.println(this);
	}
	
	public void removeAttWithName(String name)
	{
		this.removeAtt(this.getAttWithName(name));
	}
	
	@SuppressWarnings("unchecked")
	public XMLPreferences_Attribute getAttWithName(String name)
	{
		List<Element> children = this.getChildren();
		for (Element elem : children)
		{
			XMLPreferences_Attribute att = (XMLPreferences_Attribute) elem;
			if(att.getAttName().equals(name))
			{
				return att;
			}
		}
		return null;
	}
	
	public String getValueOfAttWithName(String name)
	{
		XMLPreferences_Attribute att = this.getAttWithName(name);
		if(att == null)
		{
			return null;
		}
		return att.getAttValue();
	}
	
	public List<XMLPreferences_Attribute> getAttsWithCategory(String catName)
	{
		List<XMLPreferences_Attribute> atts = this.getAtts();
		List<XMLPreferences_Attribute> result = new ArrayList<XMLPreferences_Attribute>(0);
		for (XMLPreferences_Attribute att : atts)
		{
			if(att.getAttCategory().equals(catName))
			{
				result.add(att);
			}
		}
		return result;
	}
	
	@SuppressWarnings("unchecked")
	public List<XMLPreferences_Attribute> getAtts()
	{
		List<Element> children = this.getChildren();
		List<XMLPreferences_Attribute> result = new ArrayList<XMLPreferences_Attribute>(0);
		for (Element child : children)
		{
			result.add((XMLPreferences_Attribute) child);
		}
		return result;
	}
	
	public List<String> getAttNames()
	{
		List<String> result = new ArrayList<String>(0);
		List<XMLPreferences_Attribute> atts = this.getAtts();
		for (XMLPreferences_Attribute att : atts)
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
	
	@Override
	public Element addContent(Content child)
	{
		return super.addContent(child);
	}
}
