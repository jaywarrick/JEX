package function.plugin.mechanism;

import org.scijava.plugin.SciJavaPlugin;

import Database.DBObjects.JEXEntry;


public interface JEXPluginInterface extends SciJavaPlugin {
	
	public boolean run(JEXEntry optionalEntry);
	
	public int getMaxThreads();
	
}
