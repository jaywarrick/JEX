package Database.SingleUserDatabase;

import java.io.File;

public class Repository implements Comparable<Repository> {
	
	// public static String LOCAL_DBNAME_CURRENTVERSION = "JEX4Database.xml";
	
	private String path = ""; // required field
	private String usr; // doesn't have to be defined
	private String pswd; // doesn't have to be defined
	
	// ---------------------------------------------
	// Creators
	// ---------------------------------------------
	
	/**
	 * Create a repository with a type, url and path
	 * 
	 * @param type
	 * @param url
	 * @param path
	 */
	public Repository(String path, String usr, String password)
	{
		this.path = path;
		this.usr = usr;
		this.pswd = password;
	}
	
	// ---------------------------------------------
	// Getter / Setters
	// ---------------------------------------------
	
	/**
	 * Return the path of the repository
	 * 
	 * @return path
	 */
	public String getPath()
	{
		return path;
	}
	
	/**
	 * Returns the username to connect to the repository
	 * 
	 * @return username
	 */
	public String getUserName()
	{
		return usr;
	}
	
	/**
	 * Returns the password to connect to the repository
	 * 
	 * @return password
	 */
	public String getPassword()
	{
		return pswd;
	}
	
	/**
	 * Set the path of the repository
	 */
	public void setPath(String path)
	{
		this.path = path;
	}
	
	/**
	 * Set the username to connect to the repository
	 */
	public void setUserName(String usr)
	{
		this.usr = usr;
	}
	
	/**
	 * Set the password to connect to the repository
	 */
	public void setPassword(String pswd)
	{
		this.pswd = pswd;
	}
	
	// ---------------------------------------------
	// Methods
	// ---------------------------------------------
	
	/**
	 * Return true if repository exists
	 * 
	 * @return true or false
	 */
	public boolean exists()
	{
		File rep = new File(this.getPath());
		return rep.exists();
	}
	
	/**
	 * Make the repository if it doesn't exist
	 * 
	 * @return true if repository exists or if the creation was successful
	 */
	public boolean make()
	{
		if(this.exists())
			return true;
		File rep = new File(this.getPath());
		boolean made = rep.mkdirs();
		return made;
	}
	
	/**
	 * Return true if the two repositories are the same
	 * 
	 * @param rep
	 * @return boolean
	 */
	public boolean equals(Repository rep)
	{
		if(this.getPath().equals(rep.getPath()))
			return true;
		return false;
	}
	
	/**
	 * Return an alphabetic comparison of the path of the repository
	 */
	public int compareTo(Repository o)
	{
		return this.getPath().compareTo(o.getPath());
	}
	
}
