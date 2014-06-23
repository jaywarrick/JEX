package Database.SingleUserDatabase.xml;

import org.jdom.DefaultJDOMFactory;
import org.jdom.Element;
import org.jdom.Namespace;

public class ObjectFactory extends DefaultJDOMFactory {
	
	// public static String version = "2010-12-10";
	
	public ObjectFactory()
	{}
	
	@Override
	public Element element(String name)
	{
		if(name.equals(Attribute.ELEMENTNAME))
		{
			return new Attribute();
		}
		if(name.equals(AttributeList.ELEMENTNAME))
		{
			return new AttributeList();
		}
		if(name.equals(Collection.ELEMENTNAME))
		{
			return new Collection();
		}
		if(name.equals(XElement.ELEMENTNAME))
		{
			return new XElement();
		}
		
		if(name.equals(XDataSingle.ELEMENTNAME))
		{
			return new XDataSingle();
		}
		if(name.equals(XData.ELEMENTNAME))
		{
			return new XData();
		}
		if(name.equals(XEntry.ELEMENTNAME))
		{
			return new XEntry();
		}
		if(name.equals(XEntrySet.ELEMENTNAME))
		{
			return new XEntrySet();
		}
		
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