package function.plugin.mechanism;

import Database.DBObjects.JEXEntry;

import org.scijava.plugin.SciJavaPlugin;

public interface JEXPlugin extends SciJavaPlugin {
	
	public boolean run(JEXEntry optionalEntry);
	
	public int getMaxThreads();
	
}
