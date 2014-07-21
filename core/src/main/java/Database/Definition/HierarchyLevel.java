package Database.Definition;

import Database.DBObjects.JEXEntry;

import java.util.TreeMap;
import java.util.TreeSet;

public interface HierarchyLevel {
	
	// Information on the hierarchy level itself
	public String getName();
	
	public String getType();
	
	// Retrieve entries
	public TreeSet<JEXEntry> getEntries();
	
	public JEXEntry getRepresentativeEntry();
	
	public boolean containsEntry(JEXEntry entry);
	
	// Retrieve filtered version of the hierarchy level, in lists or arrays
	public TreeMap<String,HierarchyLevel> getSublevelList();
	
	public TreeMap<String,TreeMap<String,HierarchyLevel>> getSublevelArray();
}
