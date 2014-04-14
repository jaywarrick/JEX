package events;

import org.scijava.event.SciJavaEvent;


public class MyEvent extends SciJavaEvent{
	
	Object subscriber;
	
	public MyEvent(Object subscriber)
	{
		this.subscriber = subscriber;
	}
	
}
