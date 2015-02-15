package miscellaneous;

import java.util.List;

/**
 * This class Separate Variable List (CSVList) 
 * separates a collection of variables and save each variable into a String Vector
 * by fixed separators
 * 
 * @author Jay Warrick, commented by Mengcheng
 *
 */
public class CSVList extends SVList {
	
	private static final long serialVersionUID = 1L;
	// separator for variable splitting up
	private static String sepReg = "\\,";
	// separator for variable building back 
	private static String sep = ",";
	
	/**
	 * Class constructor
	 * 
	 * instance a SVList specifying separators
	 */
	public CSVList()
	{
		super(sepReg, sep);
	}
	
	/**
	 * Class constructor 
	 * Split String svl by the fixed separators to the String Vector 
	 * 	 
	 * @param svl
	 */
	public CSVList(String svl)
	{
		super(svl, sepReg, sep);
	}
	
	/**
	 * Class constructor 
	 * Add all strings inside the given List to the String Vector
	 * 
	 * @param values a String List
	 * */
	public CSVList(List<String> values)
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
	public CSVList(List<String[]> values, int arrayIndex)
	{
		super(values, arrayIndex, sepReg, sep);
	}
	
	/**
	 * Class constructor 
	 * Add all Objects inside the given Object array to the String Vector
	 * 
	 * @param s an Object array
	 */
	public CSVList(Object[] s)
	{
		super(s, sepReg, sep);
	}
}
