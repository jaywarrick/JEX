package signals;

import java.lang.ref.ReferenceQueue;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

public class SSCenter {
	
	/**
	 * Null marker class simplifies equals() logic for Signal class below.
	 */
	public static final Object NullMarker = new Object();
	
	public static final String SIG_CHANGED = "changed";
	public static final String SIG_CHANGING = "changing";
	public static final String SIG_CHANGEABORTED = "changeAborted";
	
	private static SSCenter defaultCenter = null;
	
	/**
	 * A Map of (signalName,signalProvider) pairs to a List of (slotProvider,slotMethod) pairs.
	 */
	private Hashtable<Signal,Vector<Slot>> slots; // thread-safe
	
	/**
	 * Default constructor creates a new SSCenter.
	 */
	public SSCenter()
	{
		this.slots = new Hashtable<Signal,Vector<Slot>>();
	}
	
	/**
	 * Returns the system default center, creating one if it has not yet been created.
	 */
	static public SSCenter defaultCenter()
	{
		if(defaultCenter == null)
		{
			defaultCenter = new SSCenter();
		}
		return defaultCenter;
	}
	
	/**
	 * Adds the specified signal-slot mapping to the SSCenter to enable signaling from the signalProviders to the slotProviders
	 * 
	 * @param slotProvider
	 *            The observer that wishes to be notified (required).
	 * @param slotMethod
	 *            The method that will be invoked that has arguments of types specified in variable length Class[] slotArgTypes (optional, left empty means no arguments).
	 * @param signalName
	 *            The name of the signal (required).
	 * @param signalProvider
	 *            The object that can send signals to its mapped slotProviders. If null, any object can emit this signal.
	 */
	public void connect(Object signalProvider, String signalName, Object slotProvider, String slotMethod, Class<?>... slotArgTypes)
	{
		// Logs.log("SSCenter connect: count " + this.size(),
		// 0, this);
		// remove freed objects
		this.processSignalQueue();
		
		if(signalName == null || slotProvider == null)
		{
			return;
		}
		if(signalProvider == null)
		{
			signalProvider = NullMarker;
		}
		Signal signal = new Signal(signalProvider, signalName);
		Slot slot = new Slot(slotProvider, slotMethod, slotArgTypes);
		
		this._connect(signal, slot);
	}
	
	/**
	 * Adds the specified signal-slot mapping to the SSCenter to enable signaling from the signalProviders to the slotProviders
	 * 
	 * @param slotProvider
	 *            The observer that wishes to be notified (required).
	 * @param slotMethod
	 *            The method that will be invoked that has arguments of types specified in variable length Class[] slotArgTypes (optional, left empty means no arguments).
	 * @param signalName
	 *            The name of the signal (required).
	 * @param signalProvider
	 *            The object that can send signals to its mapped slotProviders. If null, any object can emit this signal.
	 */
	public void connectWithOneStringArgument(Object signalProvider, String signalName, Object slotProvider, String slotMethod)
	{
		this.connect(signalProvider, signalName, slotProvider, slotMethod, new Class[] { String.class });
	}
	
	/**
	 * This connect method is used for connecting to static methods.
	 * 
	 * @param signalProvider
	 * @param signalName
	 * @param slotProvider
	 * @param slotMethod
	 * @param slotArgTypes
	 */
	public void connect(Object signalProvider, String signalName, Class<?> slotProvider, String slotMethod, Class<?>... slotArgTypes)
	{
		// remove freed objects
		this.processSignalQueue();
		
		if(signalName == null || slotProvider == null)
		{
			return;
		}
		if(signalProvider == null)
		{
			signalProvider = NullMarker;
		}
		Signal signal = new Signal(signalProvider, signalName);
		Slot slot = new Slot(slotProvider, slotMethod, slotArgTypes);
		
		this._connect(signal, slot);
	}
	
	private void _connect(Signal signal, Slot slot)
	{
		
		Vector<Slot> list = this.slots.get(signal);
		
		// Logs.log("\n Connect SSCenter size: " +
		// SSCenter.defaultCenter().size(), 0, this);
		// if(list != null) Logs.log("Slot list size: " +
		// list.size(), 0, this);
		// else Logs.log("Slot list size: " + list, 0, this);
		if(list == null)
		{
			// create new list with value and put it in map
			list = new Vector<Slot>(); // thread-safe
			list.add(slot); // no reference queue for slots because we'll remove
			// when we encounter null values later
			this.slots.put(new Signal(this.signalQueue, signal.signalProvider(), signal.signalName()), list);
		}
		else
		{
			// add only if not already in list
			for (Slot s : list)
			{
				if(s == slot)
				{
					return;
				}
			}
			list.add(slot);
		}
		// Logs.log("SSCenter size: " +
		// SSCenter.defaultCenter().size(), 0, this);
		// Logs.log("Slot list size: " + list.size(), 0, this);
	}
	
	/**
	 * Emits the specified signal with the args passed to the slotMethod of the connected slots.
	 * 
	 * @param aNotification
	 *            The notification that will be passed to the observers selector.
	 */
	public void emit(Object signalProvider, String signalName, Object... args)
	{
		// System.out.println("SSCenter emit: count " + this.size());
		// remove freed objects
		this.processSignalQueue();
		
		if(signalName == null)
		{
			return;
		}
		
		// get slots connected to given signal (null signalProvider changed to
		// NullMarker in _getSlots)
		List<Slot> mergedList = this._getSlots(signalProvider, signalName);
		
		Slot tempSlot;
		Iterator<Slot> it = mergedList.iterator();
		while (it.hasNext())
		{
			tempSlot = it.next();
			
			// Clean up garbage collected slots
			if(tempSlot.slotProvider() == null)
			{
				it.remove();
			}
			else
			{
				tempSlot.invoke(args);
			}
		}
		
	}
	
	private List<Slot> _getSlots(Object signalProvider, String signalName)
	{
		List<Slot> ret = new LinkedList<Slot>();
		List<Slot> tempSlotList;
		
		if(signalProvider != null)
		{ // both are specified
			tempSlotList = this.slots.get(new Signal(signalProvider, signalName));
			if(tempSlotList != null)
			{
				ret.addAll(tempSlotList);
			}
			// we would use this if we didn't want to have to remember to put
			// null
			// for the provider when emitting anonymous signals.
			// tempSlotList = slots.get( new Signal( NullMarker, signalName ) );
			// if ( tempSlotList != null )
			// {
			// ret.addAll( tempSlotList );
			// }
		}
		else
		{ // object is null
			tempSlotList = this.slots.get(new Signal(NullMarker, signalName));
			if(tempSlotList != null)
			{
				ret.addAll(tempSlotList);
			}
		}
		
		return ret;
	}
	
	/**
	 * removes any connections related to the given signalProvider and signalName
	 * 
	 * @param anObserver
	 *            The observer to be unregistered.
	 */
	public void disconnect(Object signalProvider, String signalName)
	{
		// remove freed objects
		this.processSignalQueue();
		
		// return immediately if no signalName
		if(signalName == null)
		{
			return;
		}
		
		// replace null with NullMarker if necessary
		if(signalProvider == null)
		{
			signalProvider = NullMarker;
		}
		
		this.slots.remove(new Signal(signalProvider, signalName));
	}
	
	/**
	 * removes any connections to the slotProvider
	 * 
	 * @param anObserver
	 *            The observer to be unregistered.
	 */
	public void disconnect(Object slotProvider)
	{
		// remove freed objects
		this.processSignalQueue();
		
		// return if no slotProvider given
		if(slotProvider == null)
		{
			return;
		}
		
		Iterator<Signal> it = new LinkedList<Signal>(this.slots.keySet()).iterator();
		while (it.hasNext())
		{
			this._disconnect(slotProvider, it.next());
		}
	}
	
	/**
	 * Removes the specific signal connected to the given slotProvider
	 * 
	 * @param signalProvider
	 * @param signalName
	 * @param slotProvider
	 */
	public void disconnect(Object signalProvider, String signalName, Object slotProvider)
	{
		// remove freed objects
		this.processSignalQueue();
		// System.out.println("SSCenter disconnect(before): count " +
		// this.size());
		
		// return immediately if no signalName
		if(signalName == null)
		{
			return;
		}
		
		// replace null with NullMarker if necessary
		if(signalProvider == null)
		{
			signalProvider = NullMarker;
		}
		
		// call private disconnect method
		this._disconnect(slotProvider, new Signal(signalProvider, signalName));
		// System.out.println("SSCenter disconnect(after): count " +
		// this.size());
		
	}
	
	/**
	 * Disconnects the signal from the slotProvider. All slot providers are checked to see if they are null in case they need to be removed. Removes the signal from the SSCenter if there are no more slotProviders connected.
	 */
	private void _disconnect(Object slotProvider, Signal signal)
	{
		List<Slot> list = this.slots.get(signal);
		
		// Logs.log("\nDISconnect SSCenter size: " +
		// SSCenter.defaultCenter().size(), 0, this);
		// if(list != null) Logs.log("Slot list size: " +
		// list.size(), 0, this);
		// else Logs.log("Slot list size: " + list, 0, this);
		
		if(list == null)
		{
			return;
		}
		
		// remove specified observer from list
		Object tempSlotProvider;
		Iterator<Slot> it = list.iterator();
		while (it.hasNext())
		{
			tempSlotProvider = it.next().slotProvider();
			if((tempSlotProvider == null) || (slotProvider == tempSlotProvider))
			{
				// remove if match or freed object
				it.remove();
				// do not return; process entire list
			}
		}
		if(list.size() == 0)
		{
			this.slots.remove(signal);
		}
		
		// Logs.log("SSCenter size: " +
		// SSCenter.defaultCenter().size(), 0, this);
		// list = (List<Slot>) slots.get( signal );
		// if(list == null) Logs.log("Slot list size: " + list,
		// 0, this);
		// else Logs.log("Slot list size: " + list.size(), 0,
		// this);
	}
	
	/* Reference queues for cleared WeakKeys */
	private ReferenceQueue<Object> signalQueue = new ReferenceQueue<Object>();
	
	/**
	 * Removes any signals whose signalProvider has been garbage collected. (Garbage collected slots are removed as they are encountered.)
	 */
	private void processSignalQueue()
	{
		Object temp;
		while ((temp = this.signalQueue.poll()) != null)
		{
			// System.out.println(
			// "SSCenter.processSignalQueue: removing object" );
			this.slots.remove(temp);
		}
	}
	
	public int size()
	{
		return this.slots.size();
	}
	
	public static void observe(Observer observer, Observable obj)
	{
		SSCenter.defaultCenter().connect(obj, SIG_CHANGING, observer, SIG_CHANGING, new Class[] { Observable.class, String.class });
		SSCenter.defaultCenter().connect(obj, SIG_CHANGED, observer, SIG_CHANGED, new Class[] { Observable.class, String.class });
		SSCenter.defaultCenter().connect(obj, SIG_CHANGEABORTED, observer, SIG_CHANGEABORTED, new Class[] { Observable.class, String.class });
	}
	
	public static void unobserve(Observer observer, Observable obj)
	{
		SSCenter.defaultCenter().disconnect(obj, SIG_CHANGING, observer);
		SSCenter.defaultCenter().disconnect(obj, SIG_CHANGED, observer);
		SSCenter.defaultCenter().disconnect(obj, SIG_CHANGEABORTED, observer);
	}
	
	public static void changing(Observable obj, String accessorName)
	{
		SSCenter.defaultCenter().emit(obj, SIG_CHANGING, new Object[] { obj, accessorName });
	}
	
	public static void changed(Observable obj, String accessorName)
	{
		SSCenter.defaultCenter().emit(obj, SIG_CHANGED, new Object[] { obj, accessorName });
	}
	
	public static void changeAborted(Observable obj, String accessorName)
	{
		SSCenter.defaultCenter().emit(obj, SIG_CHANGEABORTED, new Object[] { obj, accessorName });
	}
	
	/*
	 * public static void main( String[] argv ) { Object aSource = "aSource"; Object bSource = "bSource";
	 * 
	 * Object oneTest = new OneTest(); Object twoTest = new TwoTest(); String notifyMeOnce = new String( "notifyMeOnce", new Class[] { NSNotification.class } ); String notifyMeTwice = new String( "notifyMeTwice", new Class[] { NSNotification.class }
	 * );
	 * 
	 * NSNotificationCenter.defaultCenter().addObserver( oneTest, notifyMeOnce, "aMessage", null );
	 * 
	 * NSNotificationCenter.defaultCenter().addObserver( oneTest, notifyMeOnce, null, aSource );
	 * 
	 * NSNotificationCenter.defaultCenter().addObserver( twoTest, notifyMeOnce, "aMessage", aSource );
	 * 
	 * NSNotificationCenter.defaultCenter().addObserver( twoTest, notifyMeTwice, null, null );
	 * 
	 * NSNotificationCenter.defaultCenter().postNotification( "aMessage", aSource ); System.out.println(); NSNotificationCenter.defaultCenter().postNotification( "aMessage", bSource ); System.out.println();
	 * NSNotificationCenter.defaultCenter().postNotification( "bMessage", aSource ); System.out.println(); NSNotificationCenter.defaultCenter().postNotification( "bMessage", bSource ); System.out.println( "---" );
	 * 
	 * NSNotificationCenter.defaultCenter().removeObserver( oneTest, null, aSource );
	 * 
	 * NSNotificationCenter.defaultCenter().postNotification( "aMessage", aSource ); System.out.println(); NSNotificationCenter.defaultCenter().postNotification( "aMessage", bSource ); System.out.println();
	 * NSNotificationCenter.defaultCenter().postNotification( "bMessage", aSource ); System.out.println(); NSNotificationCenter.defaultCenter().postNotification( "bMessage", bSource ); System.out.println( "---" );
	 * 
	 * NSNotificationCenter.defaultCenter().removeObserver( null );
	 * 
	 * NSNotificationCenter.defaultCenter().postNotification( "aMessage", aSource ); System.out.println(); NSNotificationCenter.defaultCenter().postNotification( "aMessage", bSource ); System.out.println();
	 * NSNotificationCenter.defaultCenter().postNotification( "bMessage", aSource ); System.out.println(); NSNotificationCenter.defaultCenter().postNotification( "bMessage", bSource ); System.out.println( "---" ); }
	 * 
	 * static private class OneTest { public void notifyMeOnce( NSNotification aNotification ) { System.out.println( "OneTest.notifyMeOnce: " + aNotification ); } }
	 * 
	 * static private class TwoTest { public void notifyMeOnce( NSNotification aNotification ) { System.out.println( "TwoTest.notifyMeOnce: " + aNotification ); } public void notifyMeTwice( NSNotification aNotification ) { System.out.println(
	 * "TwoTest.notifyMeTwice: " + aNotification ); } }
	 */
}
