package jex.statics;

import java.util.ArrayList;
import java.util.List;

import logs.Logs;
import preferences.XPreferences;
import Database.SingleUserDatabase.Repository;

public class PrefsUtility {
	
	public static final String ATT_NODENAME = XPreferences.XPREFERENCES_NODENAMEKEY;
	
	public static final String ROOTNODE_USERPREFS = "User Preferences";
	public static final String ATT_LASTPATH = "Last Path";
	public static final String ATT_CONSOLIDATEDATABASE = "Consolidate Database";
	public static final String ATT_AUTOREMOVEONDELETE = "Auto-remove";
	public static final String ATT_EXTERNALPLUGINSFOLDER = "External Plugins Folder";
	
	public static final String USERNODE_OCTAVE = "Octave";
	public static final String ATT_OCTAVEBINARY = "Octave Binary";
	public static final String ATT_OCTAVEAUTODRAWNOW = "AutoDrawNow";
	public static final String ATT_OCTAVEHISTORY = "History Length";
	
	public static final String USERNODE_R = "R";
	public static final String ATT_RBINARY = "R Binary";
	
	public static final String ROOTNODE_REPOSITORIES = "Repositories";
	public static final String ATT_REPOSITORYPATH = "Path";
	public static final String ATT_REPOSITORYUSERNAME = "UserName";
	public static final String ATT_REPOSITORYPASSWORD = "Password";
	
	public static XPreferences preferences;
	
	public static XPreferences getRootPrefs()
	{
		return preferences;
	}
	
	public static XPreferences getUserPrefs()
	{
		return preferences.getChildNode(ROOTNODE_USERPREFS);
	}
	
	public static String getLastPath()
	{
		return getUserPrefs().get(ATT_LASTPATH, "/");
	}
	
	public static void setLastPath(String path)
	{
		getUserPrefs().put(ATT_LASTPATH, path);
	}
	
	public static boolean getConsolidateDatabase()
	{
		try
		{
			return Boolean.parseBoolean(getUserPrefs().get(ATT_CONSOLIDATEDATABASE, "false"));
		}
		catch (Exception e) // Repair the preference
		{
			getUserPrefs().put(ATT_CONSOLIDATEDATABASE, "false");
			savePrefs();
			Logs.log("Repaired invalid preference value", 0, "PrefsUtility");
			return Boolean.parseBoolean(getUserPrefs().get(ATT_CONSOLIDATEDATABASE, "false"));
		}
	}
	
	public static boolean getAutoRemoveOnDelete()
	{
		try
		{
			return Boolean.parseBoolean(getUserPrefs().get(ATT_AUTOREMOVEONDELETE, "false"));
		}
		catch (Exception e) // Repair the preference
		{
			getUserPrefs().put(ATT_AUTOREMOVEONDELETE, "false");
			savePrefs();
			Logs.log("Repaired invalid preference value", 0, "PrefsUtility");
			return Boolean.parseBoolean(getUserPrefs().get(ATT_AUTOREMOVEONDELETE, "false"));
		}
	}
	
	public static String getExternalPluginsFolder()
	{
		try
		{
			return getUserPrefs().get(ATT_EXTERNALPLUGINSFOLDER, "");
		}
		catch (Exception e) // Repair the preference
		{
			getUserPrefs().put(ATT_EXTERNALPLUGINSFOLDER, "");
			savePrefs();
			Logs.log("Repaired invalid preference value", 0, "PrefsUtility");
			return getUserPrefs().get(ATT_EXTERNALPLUGINSFOLDER, "");
		}
	}
	
	public static XPreferences getOctavePrefs()
	{
		return getUserPrefs().getChildNode(USERNODE_OCTAVE);
	}
	
	public static String getOctaveBinary()
	{
		return getOctavePrefs().get(ATT_OCTAVEBINARY, "/Applications/Octave.app/Contents/Resources/bin/octave");
	}
	
	public static boolean getOctaveAutoDrawNow()
	{
		try
		{
			return Boolean.parseBoolean(getOctavePrefs().get(ATT_OCTAVEAUTODRAWNOW, "false"));
		}
		catch (Exception e) // Repair the preference
		{
			getOctavePrefs().put(ATT_OCTAVEAUTODRAWNOW, "false");
			savePrefs();
			Logs.log("Repaired invalid preference value", 0, "PrefsUtility");
			return Boolean.parseBoolean(getOctavePrefs().get(ATT_OCTAVEAUTODRAWNOW, "false"));
		}
	}
	
	public static int getOctaveHistoryLength()
	{
		try
		{
			return Integer.parseInt(getOctavePrefs().get(ATT_OCTAVEHISTORY, "100"));
		}
		catch (Exception e) // Repair the preference
		{
			getOctavePrefs().put(ATT_OCTAVEHISTORY, "100");
			savePrefs();
			Logs.log("Repaired invalid preference value", 0, "PrefsUtility");
			return Integer.parseInt(getOctavePrefs().get(ATT_OCTAVEHISTORY, "100"));
		}
	}
	
	public static XPreferences getRPrefs()
	{
		return getUserPrefs().getChildNode(USERNODE_R);
	}
	
	public static String getRBinary()
	{
		return getRPrefs().get(ATT_RBINARY, "/Library/Frameworks/R.framework/Resources/bin/R64");
	}
	
	// public static String getRServeBinary()
	// {
	// return getRPrefs().get(ATT_RSERVEBINARY,
	// "/Library/Frameworks/R.framework/Versions/2.14/Resources/bin/Rserve");
	// }
	//
	// public static int getRServeStartDelay()
	// {
	// try
	// {
	// return (int)
	// (1000*Double.parseDouble(getRPrefs().get(ATT_RSERVESTARTDELAY, "7")));
	// }
	// catch(Exception e) // Repair the preference
	// {
	// getRPrefs().put(ATT_RSERVESTARTDELAY, "7");
	// savePrefs();
	// Logs.log("Repaired invalid preference value", 0,
	// "PrefsUtility");
	// return (int)
	// (1000*Double.parseDouble(getRPrefs().get(ATT_RSERVESTARTDELAY, "7")));
	// }
	// }
	
	public static XPreferences getRepositoryPrefs()
	{
		return preferences.getChildNode(ROOTNODE_REPOSITORIES);
	}
	
	public static boolean addRepository(Repository rep)
	{
		// Make a new node in the preferences
		if(rep.getPath() == null || rep.getPath().equals(""))
		{
			return false;
		}
		XPreferences repositoryNode = getRepositoryPrefs().getChildNode(rep.getPath());
		
		// Save the parameters of the repository
		repositoryNode.put(ATT_REPOSITORYPATH, rep.getPath());
		repositoryNode.put(ATT_REPOSITORYUSERNAME, rep.getUserName());
		repositoryNode.put(ATT_REPOSITORYPASSWORD, rep.getPassword());
		
		// Save
		savePrefs();
		
		return true;
	}
	
	/**
	 * Save updated repository
	 * 
	 * @param rep
	 * @return
	 */
	public static boolean updateRepository(Repository rep)
	{
		if(rep.getPath() == null || rep.getPath().equals(""))
		{
			return false;
		}
		
		// Get the repository node
		XPreferences repositoryNode = getRepositoryPrefs().getChildNode(rep.getPath());
		
		// Save the parameters of the repository
		// Save the parameters of the repository
		repositoryNode.put(ATT_REPOSITORYPATH, rep.getPath());
		repositoryNode.put(ATT_REPOSITORYUSERNAME, rep.getUserName());
		repositoryNode.put(ATT_REPOSITORYPASSWORD, rep.getPassword());
		
		// Save
		savePrefs();
		
		return true;
	}
	
	public static boolean removeRepository(Repository rep)
	{
		// Remove the repository node from the preferences
		if(rep.getPath() == null || rep.getPath().equals(""))
		{
			return false;
		}
		getRepositoryPrefs().removeNode(rep.getPath());
		
		// Save
		savePrefs();
		
		return true;
	}
	
	public static List<Repository> getRepositoryList()
	{
		// Get the list of repository keys
		List<String> repCodes = getRepositoryPrefs().getChildNodeNames();
		List<Repository> repositoryList = new ArrayList<Repository>(0);
		for (String repCode : repCodes)
		{
			// Get the repository
			XPreferences repositoryNode = getRepositoryPrefs().getChildNode(repCode);
			
			// Get the parameters of the repository
			String path = repositoryNode.get(ATT_REPOSITORYPATH, "/Users");
			String usr = repositoryNode.get(ATT_REPOSITORYUSERNAME, "");
			String pswd = repositoryNode.get(ATT_REPOSITORYPASSWORD, "");
			
			// Make the repository
			Repository rep = new Repository(path, usr, pswd);
			repositoryList.add(rep);
		}
		return repositoryList;
	}
	
	public static void loadPrefs(String filePath)
	{
		// By doing all this stuff upon loading, we make sure that any
		// new preferences we add will show up in the list of prefs
		// and be saved, thereby updating old pref files to a new pref file.
		preferences = new XPreferences(filePath);
		initializeAnyNewPrefs();
		savePrefs();
		reloadPrefs();
	}
	
	/**
	 * This is called upon opening the prefs window, canceling changes, or closing the prefs window to ensure that only saved preferences are used. However, if the user changes the prefs and leaves the window open without saving, the current changes
	 * are used by JEX until canceling or closing
	 */
	public static void reloadPrefs()
	{
		preferences = new XPreferences(preferences.getPath());
	}
	
	public static void savePrefs()
	{
		preferences.save();
	}
	
	public static void initializeAnyNewPrefs()
	{
		// Call all the getters in a sensible order to make sure
		// each possible node and attribute has been loaded
		getUserPrefs();
		getLastPath();
		getConsolidateDatabase();
		getAutoRemoveOnDelete();
		getExternalPluginsFolder();
		getOctavePrefs();
		getOctaveBinary();
		getOctaveAutoDrawNow();
		getOctaveHistoryLength();
		getRPrefs();
		getRBinary();
		// getRServeBinary();
		// getRServeStartDelay();
		getRepositoryPrefs();
	}
	
}
