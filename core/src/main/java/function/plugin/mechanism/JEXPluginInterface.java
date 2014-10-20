package function.plugin.mechanism;

import org.scijava.plugin.SciJavaPlugin;

import Database.DBObjects.JEXEntry;
import cruncher.Ticket;


public interface JEXPluginInterface extends SciJavaPlugin {
	
	public boolean run(JEXEntry optionalEntry);
	
	public void finalizeEntry();
	
	public void prepareTicket();
	
	public void finalizeTicket(Ticket ticket);
	
	public int getMaxThreads();
	
}
