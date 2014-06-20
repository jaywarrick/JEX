package preferences;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import miscellaneous.XMLUtility;

import org.jdom.Content;
import org.jdom.Element;

import Database.SingleUserDatabase.xml.Attribute;

public class XMLPreferences_XElement extends Element {
	
	private static final long serialVersionUID = 1L;
	
	public static final String ELEMENTNAME = "XElement";
	
	protected XMLPreferences_AttributeList meta;
	protected XMLPreferences_Collection collection;
	
	public XMLPreferences_XElement()
	{
		super(ELEMENTNAME);
	}
	
	public XMLPreferences_XElement(String species)
	{
		super(ELEMENTNAME);
		
		if(this.getChild(XMLPreferences_AttributeList.ELEMENTNAME) != null)
		{
			this.removeContent(this.getChild(XMLPreferences_AttributeList.ELEMENTNAME));
			// this.removeChildren(AttributeList_20110212.ELEMENTNAME);
		}
		this.meta = new XMLPreferences_AttributeList();
		this.addContent(this.meta);
		
		if(this.getChild(XMLPreferences_Collection.ELEMENTNAME) != null)
		{
			this.removeContent(this.getChild(XMLPreferences_Collection.ELEMENTNAME));
			// this.removeChildren(Collection_20110212.ELEMENTNAME);
		}
		this.collection = new XMLPreferences_Collection();
		this.addContent(this.collection);
	}
	
	private void initializeElement()
	{
		this.meta = (XMLPreferences_AttributeList) this.getChild(XMLPreferences_AttributeList.ELEMENTNAME);
		if(this.meta == null)
		{
			this.meta = new XMLPreferences_AttributeList();
			this.addContent(this.meta);
		}
		
		this.collection = (XMLPreferences_Collection) this.getChild(XMLPreferences_Collection.ELEMENTNAME);
		if(this.collection == null)
		{
			this.collection = new XMLPreferences_Collection();
			this.addContent(this.collection);
		}
	}
	
	// ---------------------------------------------
	// Meta bud convenience methods
	// ---------------------------------------------
	
	@Override
	public Element addContent(Content child)
	{
		if(child instanceof Element)
		{
			// Cast to element
			Element toAdd = (Element) child;
			
			// Get the name
			String toAddName = toAdd.getName();
			
			// If the name is an attlist then check if one already exists
			if(toAddName.equals(XMLPreferences_AttributeList.ELEMENTNAME))
			{
				if(this.getChild(XMLPreferences_AttributeList.ELEMENTNAME) != null)
				{
					this.removeContent(this.getChild(XMLPreferences_AttributeList.ELEMENTNAME));
				}
				Element e = super.addContent(child);
				this.meta = (XMLPreferences_AttributeList) toAdd;
				return e;
			}
			
			// If the name is an collection then check if one already exists
			if(toAddName.equals(XMLPreferences_Collection.ELEMENTNAME))
			{
				if(this.getChild(XMLPreferences_Collection.ELEMENTNAME) != null)
				{
					this.removeContent(this.getChild(XMLPreferences_Collection.ELEMENTNAME));
				}
				Element e = super.addContent(child);
				this.collection = (XMLPreferences_Collection) toAdd;
				return e;
			}
		}
		
		Element e = super.addContent(child);
		return e;
	}
	
	public boolean hasAtt(String key)
	{
		if(this.meta == null)
		{
			this.initializeElement();
		}
		Element e = this.meta.getAttWithName(key);
		return e != null;
	}
	
	public String getAtt(String key)
	{
		if(this.meta == null)
		{
			this.initializeElement();
		}
		return this.meta.getValueOfAttWithName(key);
	}
	
	public XMLPreferences_Attribute getFullAttribute(String key)
	{
		if(this.meta == null)
		{
			this.initializeElement();
		}
		return this.meta.getAttWithName(key);
	}
	
	public void setAtt(String key, String value)
	{
		this.setAtt(key, value, Attribute.DEFAULTCATEGORY);
	}
	
	public void setAtt(String key, String value, String category)
	{
		if(this.meta == null)
		{
			this.initializeElement();
		}
		
		XMLPreferences_Attribute att = null;
		if(this.hasAtt(key))
		{
			att = this.meta.getAttWithName(key);
			att.setAttValue(value);
			att.setAttCategory(category);
		}
		else
		// Make and add attribute
		{
			att = new XMLPreferences_Attribute(key, value, category);
			this.meta.addAtt(att);
		}
	}
	
	public void removeAtt(String key)
	{
		if(this.meta == null)
		{
			return;
		}
		
		this.meta.removeAttWithName(key);
	}
	
	public List<String> getAttNames()
	{
		if(this.meta == null)
		{
			this.initializeElement();
		}
		
		List<String> result = this.meta.getAttNames();
		return result;
	}
	
	//
	// public BudAttribute getAttributeWithName(String name)
	// {
	// if (meta == null) initializeElement();
	//
	// BudAttribute result = meta.getAttWithName(name);
	// return result;
	// }
	
	// ---------------------------------------------
	// Collection bud convenience methods
	// ---------------------------------------------
	
	public void addXElement(XMLPreferences_XElement child)
	{
		this.collection.addBud(child);
	}
	
	public void removeXElement(XMLPreferences_XElement child)
	{
		this.collection.removeBud(child);
	}
	
	@SuppressWarnings("unchecked")
	public List<Element> getCollectionChildren()
	{
		if(this.collection == null)
		{
			this.initializeElement();
		}
		List<Element> result = this.collection.getChildren();
		return result;
	}
	
	public List<XMLPreferences_XElement> getXElements()
	{
		if(this.collection == null)
		{
			this.initializeElement();
		}
		
		@SuppressWarnings("unchecked")
		List<Element> children = this.collection.getChildren();
		List<XMLPreferences_XElement> result = new ArrayList<XMLPreferences_XElement>();
		for (Element elem : children)
		{
			if(elem instanceof XMLPreferences_XElement)
			{
				result.add((XMLPreferences_XElement) elem);
			}
		}
		
		return result;
	}
	
	public XMLPreferences_XElement getXElementParent()
	{
		Element parent = this.getParentElement();
		if(parent != null)
		{
			Element grandparent = parent.getParentElement();
			if(grandparent instanceof XMLPreferences_XElement)
			{
				return (XMLPreferences_XElement) grandparent;
			}
		}
		return null;
	}
	
	public Iterator<XMLPreferences_XElement> iterator()
	{
		return this.getXElements().iterator();
	}
	
	@Override
	public String toString()
	{
		String result = XMLUtility.toXML(this);
		return result;
	}
}
