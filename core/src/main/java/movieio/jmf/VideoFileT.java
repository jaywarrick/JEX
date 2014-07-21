/*
 * VIDEO LIBRARY FOR IMAGEJ
 * Author: Wilhelm Burger (wilbur@ieee.org)
 * Source: www.fh-hagenberg.at/staff/burger/imagej/
 */

package movieio.jmf;

public enum VideoFileT
{
	AVI("Windows AVI", "avi"), QT("QuickTime", "mov"), MPEG("Mpeg", "mpg");
	
	private final String description;
	private final String extension;
	
	VideoFileT(String description, String extension)
	{
		this.description = description;
		this.extension = extension;
	}
	
	public String description()
	{
		return description;
	}
	
	public String extension()
	{
		return extension;
	}
	
}
