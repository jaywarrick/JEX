package Database.Definition;

import java.util.HashMap;

public class Bookmark {
	
	HashMap<String,String> bookmark;
	
	public Bookmark()
	{
		bookmark = new HashMap<String,String>();
	}
	
	public String get(String key)
	{
		if(bookmark == null)
			return null;
		return bookmark.get(key);
	}
	
}
