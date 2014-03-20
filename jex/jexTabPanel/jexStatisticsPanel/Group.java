package jex.jexTabPanel.jexStatisticsPanel;

import Database.DBObjects.JEXEntry;

public class Group {
	
	JEXEntry entry;
	String groupInfo = "";
	// String groupID = "";
	String toDisplay = "";
	
	// Group(String groupID, String groupInfo, String toDisplay){
	// this.groupID = groupID;
	// this.groupInfo = groupInfo;
	// this.toDisplay = toDisplay;
	// }
	
	Group(JEXEntry entry, String groupInfo, String toDisplay)
	{
		this.entry = entry;
		this.groupInfo = groupInfo;
		this.toDisplay = toDisplay;
	}
}
