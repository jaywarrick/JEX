package function.plugin.mechanism;

import jex.statics.JEXStatics;
import miscellaneous.Cancelable;
import miscellaneous.Canceler;

import org.scijava.plugin.SciJavaPlugin;

import Database.DBObjects.JEXEntry;
import cruncher.Ticket;

public abstract class JEXPlugin implements SciJavaPlugin, Canceler, Cancelable {
	
	public Canceler canceler = null;
	
	public abstract boolean run(JEXEntry optionalEntry);
	
	// run after ending
	public void finalizeEntry()
	{
		JEXStatics.statusBar.setProgressPercentage(0);
	}
	
	public void prepareTicket()
	{   
		
	}
	
	public void finalizeTicket(Ticket ticket)
	{   
		
	}
	
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
