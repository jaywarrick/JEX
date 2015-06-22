package events;

import org.scijava.event.SciJavaEvent;

import function.plugin.IJ2.IJ2PluginUtility;


public class JEXEvents {
	
	public static void publish(SciJavaEvent e)
	{
		IJ2PluginUtility.ij().event().publish(e);
	}
	
	public static void subscribe(Object o)
	{
		IJ2PluginUtility.ij().event().subscribe(o);
	}
	
}
