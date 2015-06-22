package events;

import logs.Logs;

import org.scijava.event.EventHandler;


public class Listener {
	
	public Listener()
	{
		JEXEvents.subscribe(this);
	}
	
	@EventHandler
	public void onEvent(JEXEvent e)
	{
		if(!e.isConsumed())
		{
			Logs.log(e.source.getClass().getSimpleName(), this);
			e.setConsumed(true);
		}
	}
	
}
