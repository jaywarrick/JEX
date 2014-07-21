package events;

import net.imagej.ImageJ;
import net.imagej.legacy.DefaultLegacyService;

import org.scijava.event.SciJavaEvent;


public class JEXEvents {
	
	static
	{
		DefaultLegacyService.preinit();
	}
	
	public static ImageJ ij = new ImageJ();
	
	public static void publish(SciJavaEvent e)
	{
		ij.event().publish(e);
	}
	
	public static void subscribe(Object o)
	{
		ij.event().subscribe(o);
	}
	
}
