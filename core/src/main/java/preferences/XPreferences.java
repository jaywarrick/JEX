package preferences;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import logs.Logs;
import miscellaneous.XMLUtility;

import org.jdom.Element;

public class XPreferences implements Property {
	
	private static final long serialVersionUID = 1L;
	
	// Statics
	public final static String XPREFERENCES_NODENAMEKEY = "Node Name";
	
	// Class variables
	private XMLPreferences_XElement xelem;
	private String path;
	
	public XPreferences()
	{
		xelem = new XMLPreferences_XElement(XMLPreferences_XElement.ELEMENTNAME);
		xelem.setAtt(XPREFERENCES_NODENAMEKEY, "Root");
	}
	
	public XPreferences(String path)
	{
		// this();
		loadFromPath(path);
		if(xelem == null)
		{
			xelem = new XMLPreferences_XElement(XMLPreferences_XElement.ELEMENTNAME);
			xelem.setAtt(XPREFERENCES_NODENAMEKEY, "Root");
		}
	}
	
	// ---------------------------------------------
	// Loading and saving the preferences
	// ---------------------------------------------
	
	/**
	 * Load the preferences from a given path
	 * 
	 * @param path
	 */
	public void loadFromPath(String path)
	{
		Logs.log("Loading preference file " + path, 1, this);
		Element e = XMLUtility.XMLload(path, new XMLPreferences_ObjectFactory());
		XMLPreferences_XElement element = (XMLPreferences_XElement) e;
		// Get the file return if it doesn't exist
		if(element == null)
			return;
		
		xelem = element;
		
		// Set the path
		this.path = path;
	}
	
	/**
	 * Save the preferences at a given location
	 * 
	 * @param path
	 */
	public boolean saveToPath(String path)
	{
		// Get the xml string
		String xmlString = XMLUtility.toHardXML(xelem);
		
		// Save the file
		return XMLUtility.XMLsave(path, xmlString);
	}
	
	/**
	 * Save the preferences at the preset location
	 */
	public void save()
	{
		// If the path is null
		if(path == null)
		{
			Logs.log("Path not set for saving preferences", 0, this);
			return;
		}
		
		// Get the xml string
		String xmlString = XMLUtility.toHardXML(xelem);
		
		// Save the file
		XMLUtility.XMLsave(path, xmlString);
	}
	
	// ---------------------------------------------
	// Getters and setters
	// ---------------------------------------------
	
	/**
	 * Set the path field
	 * 
	 * @param path
	 */
	public String getPath()
	{
		return this.path;
	}
	
	/**
	 * Set the path field
	 * 
	 * @param path
	 */
	public void setPath(String path)
	{
		this.path = path;
	}
	
	/**
	 * Set the xml element model
	 * 
	 * @param xelem
	 */
	public void setElement(XMLPreferences_XElement xelem)
	{
		this.xelem = xelem;
	}
	
	// ---------------------------------------------
	// Navigating the preference tree
	// ---------------------------------------------
	
	public boolean hasChildNode(String nodeName)
	{
		// Get the children nodes
		if(xelem == null)
			return false;
		List<XMLPreferences_XElement> children = xelem.getXElements();
		
		// Loop though
		for (XMLPreferences_XElement child : children)
		{
			// if the child has the right name grab it
			String nName = child.getAtt(XPREFERENCES_NODENAMEKEY);
			if(!nodeName.equals(nName))
				continue;
			
			return true;
		}
		return false;
	}
	
	/**
	 * Return a child XPreferences object with the key name NODENAME the node will be created if it doesn't exist
	 * 
	 * @param nodeName
	 * @return
	 */
	public XPreferences getChildNode(String nodeName)
	{
		// Get the children nodes
		if(xelem == null)
			return null;
		List<XMLPreferences_XElement> children = xelem.getXElements();
		
		// Loop though
		for (XMLPreferences_XElement child : children)
		{
			// if the child has the right name grab it
			String nName = child.getAtt(XPREFERENCES_NODENAMEKEY);
			if(!nodeName.equals(nName))
				continue;
			
			// Create a new XPreferences set and return it
			XPreferences childPrefs = new XPreferences();
			childPrefs.setElement(child);
			return childPrefs;
		}
		
		// The node doesn't exist so create it
		XMLPreferences_XElement child = new XMLPreferences_XElement(XMLPreferences_XElement.ELEMENTNAME);
		child.setAtt(XPREFERENCES_NODENAMEKEY, nodeName);
		
		// Add the new element
		xelem.addXElement(child);
		
		// Return the new XPreferences
		XPreferences childPrefs = new XPreferences();
		childPrefs.setElement(child);
		return childPrefs;
	}
	
	/**
	 * Return the XPrefences parent node
	 * 
	 * @return
	 */
	public XPreferences getParent()
	{
		// IF the element is null return null
		if(xelem == null)
			return null;
		
		// Get the xelement parent
		XMLPreferences_XElement parent = xelem.getXElementParent();
		if(parent == null)
			return null;
		
		// Create a new XPreferences set and return it
		XPreferences parentPrefs = new XPreferences();
		parentPrefs.setElement(parent);
		return parentPrefs;
	}
	
	/**
	 * Return a list of all children node names
	 * 
	 * @return
	 */
	public List<String> getChildNodeNames()
	{
		// Get the children nodes
		if(xelem == null)
			return null;
		List<XMLPreferences_XElement> children = xelem.getXElements();
		
		// Make the result list
		List<String> result = new ArrayList<String>();
		
		// Loop though
		for (XMLPreferences_XElement child : children)
		{
			// if the child has the right name grab it
			String nName = child.getAtt(XPREFERENCES_NODENAMEKEY);
			if(nName == null)
				continue;
			
			// Add node name to result list
			result.add(nName);
		}
		
		return result;
	}
	
	/**
	 * Return a list of the child nodes
	 * 
	 * @return
	 */
	public List<XPreferences> getChildNodes()
	{
		// Get the children nodes
		if(xelem == null)
			return null;
		List<XMLPreferences_XElement> children = xelem.getXElements();
		
		// Make the result list
		List<XPreferences> result = new ArrayList<XPreferences>();
		
		// Loop though
		for (XMLPreferences_XElement child : children)
		{
			// Create a new XPreferences set and return it
			XPreferences childPrefs = new XPreferences();
			childPrefs.setElement(child);
			
			// Add node name to result list
			result.add(childPrefs);
		}
		
		return result;
	}
	
	/**
	 * Remove the child node with name NODENAME
	 * 
	 * @param nodeName
	 */
	public void removeNode(String nodeName)
	{
		// Get the children nodes
		if(xelem == null)
			return;
		
		// Get the child node
		List<XMLPreferences_XElement> children = xelem.getXElements();
		
		// Loop though
		for (XMLPreferences_XElement child : children)
		{
			// if the child has the right name grab it
			String nName = child.getAtt(XPREFERENCES_NODENAMEKEY);
			if(!nodeName.equals(nName))
				continue;
			
			// Remove it from the collection bud
			xelem.removeXElement(child);
		}
	}
	
	// ---------------------------------------------
	// Adding/Getting preferences
	// ---------------------------------------------
	
	public boolean has(String key)
	{
		return xelem.hasAtt(key);
	}
	
	/**
	 * Set the preference named KEY with value VALUE
	 */
	public boolean put(String key, String value)
	{
		if(xelem == null)
			return false;
		
		// Put the value in the xml node
		xelem.setAtt(key, value);
		return true;
	}
	
	/**
	 * Return a list of the metaData keys in order
	 */
	public List<String> getAllKeys()
	{
		return xelem.getAttNames();
	}
	
	/**
	 * Return a list of the metaData without the nodeName
	 */
	public List<String> getMostKeys()
	{
		List<String> ret = this.getAllKeys();
		ret.remove(XPREFERENCES_NODENAMEKEY);
		return ret;
	}
	
	/**
	 * Remove a preference from a node
	 */
	public void remove(String key)
	{
		this.xelem.removeAtt(key);
	}
	
	/**
	 * Rename a preference in a node
	 */
	public void rename(String oldKey, String newKey)
	{
		String attValue = this.xelem.getAtt(oldKey);
		if(attValue == null)
			return;
		this.xelem.removeAtt(oldKey);
		this.xelem.setAtt(newKey, attValue);
	}
	
	/**
	 * Set the preference named KEY with value VALUE
	 */
	public boolean putInt(String key, int value)
	{
		if(xelem == null)
			return false;
		
		// Put the value in the xml node
		xelem.setAtt(key, "" + value);
		return true;
	}
	
	/**
	 * Set the preference named KEY with value VALUE
	 */
	public boolean putBoolean(String key, boolean value)
	{
		if(xelem == null)
			return false;
		
		// Put the value in the xml node
		xelem.setAtt(key, "" + value);
		return true;
	}
	
	/**
	 * Return the value stored for prefence named KEY If none is set or if an error occurs return the default value DEFAULTVALUE
	 * 
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public String get(String key, String defaultValue)
	{
		// If there is no xml element return default
		if(xelem == null)
			return defaultValue;
		
		// Fetch the value in the meta bud
		String value = xelem.getAtt(key);
		
		// If null return default
		if(value == null)
		{
			this.xelem.setAtt(key, defaultValue);
		}
		
		// Fetch the value again to confirm it is there
		value = xelem.getAtt(key);
		
		// Return the value
		return value;
	}
	
	/**
	 * Return the value stored for prefence named KEY If none is set or if an error occurs return the default value null
	 * 
	 * @param key
	 * @return
	 */
	public String get(String key)
	{
		// If there is no xml element return default
		if(xelem == null)
			return null;
		
		// Fetch the value in the meta bud
		String value = xelem.getAtt(key);
		
		// If null return default
		if(value == null)
			return null;
		return value;
	}
	
	/**
	 * Changes the name of a node
	 */
	public boolean setNodeName(String oldName, String newName)
	{
		if(!this.getChildNodeNames().contains(oldName))
			return false;
		XPreferences theNode = this.getChildNode(oldName);
		theNode.xelem.setAtt(XPREFERENCES_NODENAMEKEY, newName);
		return true;
	}
	
	// /**
	// * Return the value stored for prefence named KEY as an XPreference
	// * If none is set or if an error occurs return the default value null
	// * @param key
	// * @return
	// */
	// public XPreference getXPreference(String key)
	// {
	// // If there is no xml element return default
	// if (xelem == null) return null;
	//
	// // Fetch the value in the meta bud
	// String value = xelem.getAtt(key);
	// if(value == null) return null;
	// XPreference result = new XPreference();
	// result.setElement(new XMLPreferences_Attribute(key, value));
	//
	// return result;
	// }
	
	/**
	 * Return the value stored in key KEY as an int
	 * 
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public int getInt(String key, int defaultValue)
	{
		// If there is no xml element return default
		if(xelem == null)
			return defaultValue;
		
		// Fetch the value in the meta bud
		String value = xelem.getAtt(key);
		
		// If null return default
		if(value == null)
			return defaultValue;
		
		// Parse the value
		int result = new Integer(value);
		return result;
	}
	
	/**
	 * Return the value stored in key KEY as a boolean
	 * 
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public boolean getBoolean(String key, boolean defaultValue)
	{
		// If there is no xml element return default
		if(xelem == null)
			return defaultValue;
		
		// Fetch the value in the meta bud
		String value = xelem.getAtt(key);
		
		// If null return default
		if(value == null)
			return defaultValue;
		
		// Parse the value
		boolean result = new Boolean(value);
		return result;
	}
	
	/**
	 * Return a list of keys of properties of this node
	 * 
	 * @return
	 */
	public List<String> keys()
	{
		if(xelem == null)
			return null;
		
		// Get the attribute names
		List<String> result = xelem.getAttNames();
		return result;
	}
	
	// ---------------------------------------------
	// Property interface
	// ---------------------------------------------
	public String getName()
	{
		return this.get(XPREFERENCES_NODENAMEKEY);
	}
	
	public String getDisplayName()
	{
		return this.getName();
	}
	
	public String getShortDescription()
	{
		return "";
	}
	
	@SuppressWarnings("rawtypes")
	public Class getType()
	{
		return String.class;
	}
	
	public Object getValue()
	{
		return null;
		// if (xelem == null) return "";
		// String nName = xelem.getAtt(XPREFERENCES_NODENAMEKEY);
		// return nName;
	}
	
	public void setValue(Object value)
	{
		// if (xelem == null) return;
		// xelem.setAtt(XPREFERENCES_NODENAMEKEY, value.toString());
	}
	
	public boolean isEditable()
	{
		return false;
	}
	
	public String getCategory()
	{
		return "Admin";
	}
	
	public void readFromObject(Object object)
	{   
		
	}
	
	public void writeToObject(Object object)
	{   
		
	}
	
	public void addPropertyChangeListener(PropertyChangeListener listener)
	{   
		
	}
	
	public void removePropertyChangeListener(PropertyChangeListener listener)
	{   
		
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException
	{
		if(xelem == null)
			return null;
		XMLPreferences_XElement cloneElem = (XMLPreferences_XElement) xelem.clone();
		XPreferences clone = new XPreferences();
		clone.setElement(cloneElem);
		return clone;
	}
	
	public Property getParentProperty()
	{
		if(xelem == null)
			return null;
		return this.getParent();
	}
	
	public Property[] getSubProperties()
	{
		if(xelem == null)
			return null;
		List<Property> properties = new ArrayList<Property>(0);
		
		// Add the property list from the meta bud
		List<String> pList = this.keys();
		for (String attName : pList)
		{
			if(attName.equals(XPREFERENCES_NODENAMEKEY))
				continue;
			
			// Get the preference with the right name
			properties.add(this.xelem.getFullAttribute(attName));
		}
		
		// Add the sub nodes
		List<XPreferences> childNodes = getChildNodes();
		for (XPreferences node : childNodes)
		{
			properties.add(node);
		}
		
		// Make an array
		Property[] result = properties.toArray(new Property[0]);
		return result;
	}
}
