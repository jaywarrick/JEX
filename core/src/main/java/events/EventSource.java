package events;

import logs.Logs;

public class EventSource {
	
	public static void main(String[] args)
	{
		Listener l1 = new Listener();
		Listener l2 = new Listener();
		Listener l3 = new Listener();
		Logs.log(l2.toString(), EventSource.class);
		Logs.log(l3.toString(), EventSource.class);
		EventSource s = new EventSource();
		JEXEvents.publish(new JEXEvent(l1));
		JEXEvents.publish(new JEXEvent(s));
	}
	
}
