package Database.SingleUserDatabase.xml;

import java.util.List;
import java.util.Vector;

import miscellaneous.XMLUtility;

import org.jdom.Element;

public class Collection extends Element {
	
	private static final long serialVersionUID = 1L;
	
	public static final String ELEMENTNAME = "Collection";
	
	public Collection()
	{
		super(ELEMENTNAME);
	}
	
	/**
	 * Add bud b to the collection bud
	 */
	public void addBud(XElement e)
	{
		super.addContent(e);
	}
	
	/**
	 * Remove bud b from the collection list and the dictionary if required
	 */
	public void removeBud(XElement e)
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
	public List<XElement> getXElements()
	{
		List<XElement> ret = new Vector<XElement>();
		List<Element> children = this.getChildren();
		for (Element e : children)
		{
			ret.add((XElement) e);
		}
		return ret;
	}
	
	/**
	 * Return bud at index INDEX
	 */
	public XElement getBudAtIndex(int index)
	{
		XElement bud = (XElement) this.getContent(index);
		return bud;
	}
	
	/**
	 * Return the first bud of this collectionBud
	 */
	public XElement getSingletonBud()
	{
		return (XElement) this.getContent(0);
	}
	
	@Override
	public String toString()
	{
		String result = XMLUtility.toXML(this);
		return result;
	}
	
}
