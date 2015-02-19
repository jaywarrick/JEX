package miscellaneous;

import java.util.List;

/**
  * This class Semicolon Separate Variable List (SSVList) 
 * separates a collection of variables and save each variable into a String Vector
 * by fixed separators
 * 
 * @author Jay Warrick, commented by Mengcheng
 *
 */
public class SSVList extends SVList {
	
	private static final long serialVersionUID = 1L;
	// separator for variable splitting up
	private static String sepReg = "\\;";
	// separator for variable building back 
	private static String sep = ";";
	
	/**
	 * Class constructor
	 * 
	 * instance a SVList specifying separators
	 */
	public SSVList()
	{
		super(sepReg, sep);
	}
	
	/**
	 * Class constructor 
	 * Split String svl by the fixed separators to the String Vector 
	 * 	 
	 * @param svl
	 */
	public SSVList(String svl)
	{
		super(svl, sepReg, sep);
	}
	
	/**
	 * Class constructor 
	 * Add all strings inside the given List to the String Vector
	 * 
	 * @param values a String List
	 * */
	public SSVList(List<String> values)
	{
		super(values, sepReg, sep);
	}
	
	/**
	 * Class constructor 
	 * Add certain element in a List of String array given array index into the String Vector 
	 * 
	 * @param values a List of String array
	 * @param arrayIndex
	 */
	public SSVList(List<String[]> values, int arrayIndex)
	{
		super(values, arrayIndex, sepReg, sep);
	}
	
	/**
	 * Class constructor 
	 * Add all Objects inside the given Object array to the String Vector
	 * 
	 * @param s an Object array
	 */
	public SSVList(Object[] s)
	{
		super(s, sepReg, sep);
	}
}
