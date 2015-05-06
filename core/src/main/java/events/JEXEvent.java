package events;

import org.scijava.event.SciJavaEvent;


public class JEXEvent extends SciJavaEvent{
	
	Object source;
	
	public JEXEvent(Object source)
	{
		this.source = source;
	}
	
}
