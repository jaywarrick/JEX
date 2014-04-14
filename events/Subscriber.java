package events;

import logs.Logs;

import org.scijava.event.EventHandler;
import org.scijava.event.EventSubscriber;


public class Subscriber {
	
	public Subscriber()
	{
		JEXEvents.subscribe(this);
	}
	
	@EventHandler
	public void onEvent(MyEvent e)
	{
		Logs.log("Yay!", this);
	}
	
}
