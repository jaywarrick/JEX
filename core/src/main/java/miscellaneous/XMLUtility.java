package miscellaneous;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import jex.statics.JEXStatics;
import logs.Logs;

import org.jdom.DefaultJDOMFactory;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

public class XMLUtility {
	
	/**
	 * Loads XML from a file
	 */
	public static Element XMLload(String path, DefaultJDOMFactory factory)
	{
		File f = new File(path);
		if(!f.exists())
			return null;
		return XMLload(f, factory);
	}
	
	/**
	 * Loads XML from a file
	 */
	public static Element XMLload(File f, DefaultJDOMFactory factory)
	{
		SAXBuilder sb = new SAXBuilder();
		sb.setFactory(factory);
		Document dataDoc = null;
		try
		{
			dataDoc = sb.build(f);
		}
		catch (JDOMException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		Element result = dataDoc.getRootElement();
		dataDoc.removeContent(result);
		return result;
	}
	
	/**
	 * Saves the data base in a file
	 */
	public static boolean XMLsave(String fullPath, String toSave)
	{
		File outFile = new File(fullPath);
		File folder = outFile.getParentFile();
		if(!folder.exists())
		{
			if(!folder.mkdirs())
			{
				JEXStatics.statusBar.setStatusText("XML creation impossible, couldn't create necessary folder... " + folder.getPath());
				Logs.log("DB creation impossible, couldn't create necessary folder... " + folder.getPath(), XMLUtility.class);
				return false;
			}
		}
		return saveString(fullPath, toSave);
	}
	
	/**
	 * Saves the data base in a file
	 */
	public static void XMLsave(String fullPath, Element toSave)
	{
		saveString(fullPath, toHardXML(toSave));
	}
	
	/**
	 * Saves the data base in a file
	 */
	public static boolean XMLsavePretty(String fullPath, Element toSave)
	{
		return saveString(fullPath, toXML(toSave));
	}
	
	public static String toXML(Element e)
	{
		XMLOutputter outputter = new XMLOutputter(org.jdom.output.Format.getPrettyFormat());
		String XMLstring = outputter.outputString(e);
		return XMLstring;
	}
	
	public static String toHardXML(Element e)
	{
		XMLOutputter outputter = new XMLOutputter();
		String XMLstring = outputter.outputString(e);
		return XMLstring;
	}
	
	/**
	 * Saves the data base in a file
	 */
	public static void XMLsave(File atPath, Element toSave)
	{
		XMLsave(atPath.getPath(), toSave);
	}
	
	/**
	 * Keep this PRIVATE we don't want a million file writers around.
	 * 
	 * @param fullPath
	 * @param toSave
	 */
	private static boolean saveString(String fullPath, String toSave)
	{
		try
		{
			Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fullPath), "UTF-8"));
			writer.write(toSave);
			writer.close();
			return true;
		}
		catch (IOException e)
		{
			Logs.log("ERROR creatingfile", XMLUtility.class);
			return false;
		}
	}
	
}
