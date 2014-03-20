package signals;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

/**
 * Key combining a name with an object. The object is weakly referenced, and keys are deallocated by reference queue. equals() compares by reference.
 */
public class Signal extends WeakReference<Object> {
	
	private String signalName;
	private int hashCode;
	
	/**
	 * Creates compound key. Neither name nor object may be null. Use NullMarker to represent null in either name or object.
	 * 
	 * This constructor is used for creating keys to find a slot without adding to the signalQueue
	 */
	public Signal(Object signalProvider, String signalName)
	{
		super(signalProvider);
		this.signalName = signalName;
		hashCode = signalName.hashCode() + signalProvider.hashCode();
	}
	
	/**
	 * Creates compound key with queue. Neither name nor object may be null. Use NullMarker to represent null in either name or object.
	 */
	public Signal(ReferenceQueue<Object> signalQueue, Object signalProvider, String signalName)
	{
		super(signalProvider, signalQueue);
		this.signalName = signalName;
		hashCode = this.signalName.hashCode() + signalProvider.hashCode();
	}
	
	public String signalName()
	{
		return signalName;
	}
	
	public Object signalProvider()
	{
		// gets the weak reference made to the signal provider.
		return this.get();
	}
	
	@Override
	public int hashCode()
	{
		return hashCode;
	}
	
	@Override
	public boolean equals(Object anObject)
	{
		if(this == anObject)
			return true;
		// assumes only used with other compound keys
		Signal testSignal = (Signal) anObject;
		if(this.signalName() == testSignal.signalName() || (this.signalName() != null && this.signalName().equals(testSignal.signalName())))
		{
			Object thisSignalProvider = this.signalProvider();
			if(thisSignalProvider != null)
			{
				// compares by reference
				if(thisSignalProvider == (testSignal.signalProvider()))
				{
					return true;
				}
			}
		}
		return false;
	}
	
	@Override
	public String toString()
	{
		return "[Signal:" + this.signalName() + ":" + this.signalProvider() + "]";
	}
}