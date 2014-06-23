/*
 * SIMPLE VIDEO LIBRARY FOR IMAGEJ
 * Author: Wilhelm Burger (wilbur@ieee.org)
 * Source: http://www.fh-hagenberg.at/staff/burger/imagej/
 */

package movieio.jmf;

import ij.io.OpenDialog;
import ij.io.SaveDialog;

import java.awt.Color;

public class Util {
	
	public static String askForOpenPath()
	{
		OpenDialog od = new OpenDialog("Open Movie...", "");
		String dir = od.getDirectory();
		String name = od.getFileName();
		if(name == null)
			return null;
		return encodeURL(dir + name);
	}
	
	private static String encodeURL(String url)
	{
		// url = url.replaceAll(" ","%20"); // this doesn't work with spaces
		url = url.replace('\\', '/');
		return url;
	}
	
	public static String askForSavePath(String header, String defaultName, String extension)
	{
		SaveDialog sd = new SaveDialog(header, defaultName, "." + extension);
		String dir = sd.getDirectory();
		String file = sd.getFileName();
		if(file == null)
			return null;
		return encodeURL(dir + file);
	}
	
	// useful?
	public static String setExtension(String path, String ext)
	{
		if(ext == null)
			return path;
		int dotPos = path.lastIndexOf('.');
		if(dotPos >= 0)
		{
			return path.substring(0, dotPos + 1) + ext;
		}
		else
			return path + "." + ext;
	}
	
	public static Color makeRandomColor()
	{
		double saturation = 0.25;
		double brightness = 0.50;
		float h = (float) Math.random();
		float s = (float) (1 - saturation * Math.random());
		float b = (float) (1 - brightness * Math.random());
		return new Color(Color.HSBtoRGB(h, s, b));
	}
	
}
