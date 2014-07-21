package signals;

public interface Observable {
	
	/**
	 * Implementation of this interface means that all setter methods call...
	 * 
	 * SSCenter.changing(this, "accessorName")
	 * 
	 * before changing the property. And then call
	 * 
	 * SSCenter.changed(this, "accessorName")
	 * 
	 * after changing the property.
	 */
}
