package events;


public class Publisher {
	
	public static void main(String[] args)
	{
		Subscriber s = new Subscriber();
		JEXEvents.publish(new MyEvent(s));
	}
	
}
