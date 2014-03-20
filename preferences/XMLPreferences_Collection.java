package preferences;

import java.util.List;
import java.util.Vector;

import miscellaneous.XMLUtility;

import org.jdom.Element;

public class XMLPreferences_Collection extends Element {
	
	private static final long serialVersionUID = 1L;
	
	public static final String ELEMENTNAME = "Collection";
	
	public XMLPreferences_Collection()
	{
		super(ELEMENTNAME);
	}
	
	/**
	 * Add bud b to the collection bud
	 */
	public void addBud(XMLPreferences_XElement e)
	{
		super.addContent(e);
	}
	
	/**
	 * Remove bud b from the collection list and the dictionary if required
	 */
	public void removeBud(XMLPreferences_XElement e)
	{
		if(e == null)
		{
			return;
		}
		this.removeContent(e);
	}
	
	/**
	 * Remove all buds from this collection
	 */
	public void removeBuds()
	{
		this.removeContent();
	}
	
	@SuppressWarnings("unchecked")
	public List<XMLPreferences_XElement> getXElements()
	{
		List<XMLPreferences_XElement> ret = new Vector<XMLPreferences_XElement>();
		List<Element> children = this.getChildren();
		for (Element e : children)
		{
			ret.add((XMLPreferences_XElement) e);
		}
		return ret;
	}
	
	/**
	 * Return bud at index INDEX
	 */
	public XMLPreferences_XElement getBudAtIndex(int index)
	{
		XMLPreferences_XElement bud = (XMLPreferences_XElement) this.getContent(index);
		return bud;
	}
	
	/**
	 * Return the first bud of this collectionBud
	 */
	public XMLPreferences_XElement getSingletonBud()
	{
		return (XMLPreferences_XElement) this.getContent(0);
	}
	
	@Override
	public String toString()
	{
		String result = XMLUtility.toXML(this);
		return result;
	}
	
}
