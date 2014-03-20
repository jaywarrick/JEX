package preferences;

import miscellaneous.XMLUtility;

import org.jdom.DefaultJDOMFactory;
import org.jdom.Element;

public class XPreferences_Utilities {
	
	public static XPreferences updateFromVersion3(String pathToOldInfoFile)
	{
		XMLPreferences_XElement element = (XMLPreferences_XElement) XMLUtility.XMLload(pathToOldInfoFile, new XMLPreferences_OldObjectFactory());
		
		XPreferences result = new XPreferences();
		result.setElement(element);
		
		// XPreferences result = new XPreferences(pathToOldInfoFile);
		return result;
	}
	
}

class XMLPreferences_OldObjectFactory extends DefaultJDOMFactory {
	
	public XMLPreferences_OldObjectFactory()
	{}
	
	@Override
	public Element element(String name)
	{
		if(name.equals(XMLPreferences_Attribute.ELEMENTNAME))
		{
			return new XMLPreferences_Attribute();
		}
		if(name.equals(XMLPreferences_AttributeList.ELEMENTNAME))
		{
			return new XMLPreferences_AttributeList();
		}
		if(name.equals(XMLPreferences_Collection.ELEMENTNAME))
		{
			return new XMLPreferences_Collection();
		}
		if(name.equals(XMLPreferences_XElement.ELEMENTNAME))
		{
			return new XMLPreferences_XElement();
		}
		
		if(name.equals("XData"))
		{
			return new XMLPreferences_XElement();
		}
		if(name.equals("MetaBud"))
		{
			return new XMLPreferences_AttributeList();
		}
		if(name.equals("AttBud"))
		{
			return new XMLPreferences_Attribute();
		}
		if(name.equals("CollectionBud"))
		{
			return new XMLPreferences_Collection();
		}
		if(name.equals("XDataSingle"))
		{
			return new XMLPreferences_AttributeList();
		}
		if(name.equals("Element"))
		{
			return new XMLPreferences_XElement();
		}
		
		Element b = new Element(name);
		return b;
	}
	
}