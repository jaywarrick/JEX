package preferences;

import org.jdom.DefaultJDOMFactory;
import org.jdom.Element;
import org.jdom.Namespace;

public class XMLPreferences_ObjectFactory extends DefaultJDOMFactory {
	
	// public static String version = "2010-12-10";
	
	public XMLPreferences_ObjectFactory()
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
		
		// if (name.equals("XDataSingle")) return new XDataSingle_20101118();
		// if (name.equals("XData")) return new XData_20101118();
		// if (name.equals("XEntry")) return new XEntry_20101118();
		// if (name.equals("XEntrySet")) return new XEntrySet_20101118();
		// if (name.equals("XElement")) return new XMLPreferences_XElement();
		
		Element b = new Element(name);
		return b;
	}
	
	@Override
	public Element element(String name, Namespace namespace)
	{
		if(namespace.equals(Namespace.getNamespace("", "")))
		{
			return this.element(name);
		}
		return super.element(name, namespace);
	}
	
}