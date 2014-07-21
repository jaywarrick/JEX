package function.plugin.mechanism;

import Database.DBObjects.JEXEntry;

import miscellaneous.Cancelable;
import miscellaneous.Canceler;

public abstract class JEXPlugin implements JEXPluginInterface, Canceler, Cancelable {
	
	public Canceler canceler = null;

	public abstract boolean run(JEXEntry optionalEntry);

	public int getMaxThreads()
	{
		return 5;
	}

	public void setCanceler(Canceler canceler)
	{
		this.canceler = canceler;
	}
	
	public Canceler getCanceler()
	{
		return this.canceler;
	}
	
	public boolean isCanceled()
	{
		return this.canceler.isCanceled();
	}
}
