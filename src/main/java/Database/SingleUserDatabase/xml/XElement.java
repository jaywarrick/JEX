package Database.SingleUserDatabase.xml;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import miscellaneous.XMLUtility;

import org.jdom.Content;
import org.jdom.Element;

public class XElement extends Element {
	
	private static final long serialVersionUID = 1L;
	
	public static final String ELEMENTNAME = "XElement";
	
	protected AttributeList meta;
	protected Collection collection;
	
	public XElement()
	{
		super(ELEMENTNAME);
	}
	
	public XElement(String species)
	{
		super(species);
		
		if(this.getChild(AttributeList.ELEMENTNAME) != null)
		{
			this.removeContent(this.getChild(AttributeList.ELEMENTNAME));
			// this.removeChildren(AttributeList_20110212.ELEMENTNAME);
		}
		this.meta = new AttributeList();
		this.addContent(this.meta);
		
		if(this.getChild(Collection.ELEMENTNAME) != null)
		{
			this.removeContent(this.getChild(Collection.ELEMENTNAME));
			// this.removeChildren(Collection_20110212.ELEMENTNAME);
		}
		this.collection = new Collection();
		this.addContent(this.collection);
	}
	
	private void initializeElement()
	{
		this.meta = (AttributeList) this.getChild(AttributeList.ELEMENTNAME);
		if(this.meta == null)
		{
			this.meta = new AttributeList();
			this.addContent(this.meta);
		}
		
		this.collection = (Collection) this.getChild(Collection.ELEMENTNAME);
		if(this.collection == null)
		{
			this.collection = new Collection();
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
			if(toAddName.equals(AttributeList.ELEMENTNAME))
			{
				if(this.getChild(AttributeList.ELEMENTNAME) != null)
				{
					this.removeContent(this.getChild(AttributeList.ELEMENTNAME));
				}
				Element e = super.addContent(child);
				this.meta = (AttributeList) toAdd;
				return e;
			}
			
			// If the name is an collection then check if one already exists
			if(toAddName.equals(Collection.ELEMENTNAME))
			{
				if(this.getChild(Collection.ELEMENTNAME) != null)
				{
					this.removeContent(this.getChild(Collection.ELEMENTNAME));
				}
				Element e = super.addContent(child);
				this.collection = (Collection) toAdd;
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
		
		Attribute att = null;
		if(this.hasAtt(key))
		{
			att = this.meta.getAttWithName(key);
			att.setAttValue(value);
			att.setAttCategory(category);
		}
		else
		// Make and add attribute
		{
			att = new Attribute(key, value, category);
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
	
	public void addXElement(XElement child)
	{
		this.collection.addBud(child);
	}
	
	public void removeXElement(XElement child)
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
	
	public List<XElement> getXElements()
	{
		if(this.collection == null)
		{
			this.initializeElement();
		}
		
		@SuppressWarnings("unchecked")
		List<Element> children = this.collection.getChildren();
		List<XElement> result = new ArrayList<XElement>();
		for (Element elem : children)
		{
			if(elem instanceof XElement)
			{
				result.add((XElement) elem);
			}
		}
		
		return result;
	}
	
	public XElement getXElementParent()
	{
		Element parent = this.getParentElement();
		if(parent != null)
		{
			Element grandparent = parent.getParentElement();
			if(grandparent instanceof XElement)
			{
				return (XElement) grandparent;
			}
		}
		return null;
	}
	
	public Iterator<XElement> iterator()
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
