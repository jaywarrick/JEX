package miscellaneous;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class StringUtility implements Comparator<String> {
	
	// ------------------------------------
	// --------- STRING UTILITIES ---------
	// ------------------------------------
	/**
	 * Return true if and only if String STR is contained in ARRAY
	 */
	public static boolean isMember(String str, String[] array)
	{
		if(array == null || str == null)
		{
			return false;
		}
		
		for (String s : array)
		{
			if(s.equals(str))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Add a string to a string array
	 * 
	 * @return String array
	 */
	public static String[] addToArray(String[] array, String s)
	{
		String[] result = new String[array.length + 1];
		System.arraycopy(array, 0, result, 0, array.length);
		result[result.length - 1] = s;
		return result;
	}
	
	/**
	 * Remove a string from a string array
	 * 
	 * @return String array
	 */
	public static String[] removeFromArray(String[] array, String s)
	{
		if(!StringUtility.isMember(s, array))
		{
			return array;
		}
		
		String[] result = new String[array.length - 1];
		int index = 0;
		for (String str : array)
		{
			if(s.equals(str))
			{
				continue;
			}
			result[index] = str;
			index++;
		}
		return result;
	}
	
	// find and replace a string
	public static String findReplace(String main, String find, String replace)
	{
		int index = main.indexOf(find);
		if(index < 0)
		{
			return null;
		}
		String result = main.substring(0, index) + replace + main.substring(index + find.length(), main.length());
		return result;
	}
	
	// fill a string on the left up to a size
	public static String fillLeft(String s, int size, String character)
	{
		String result = s;
		while (result.length() < size)
		{
			result = character + result;
		}
		return result;
	}
	
	// fill a string on the left up to a size
	public static String fillRight(String s, int size, String character)
	{
		String result = s;
		while (result.length() < size)
		{
			result = result + character;
		}
		return result;
	}
	
	/**
	 * Return true if and only if S starts with string PREFIX
	 * 
	 * @param s
	 * @param prefix
	 * @return
	 */
	public static boolean begginWith(String s, String prefix)
	{
		int index = s.indexOf(prefix);
		if(index == 0)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	/**
	 * Return the string S with the prefix PREFIX removed
	 * 
	 * @param s
	 * @param prefix
	 * @return
	 */
	public static String endingString(String s, String prefix)
	{
		if(!begginWith(s, prefix))
		{
			return null;
		}
		String result = s.substring(prefix.length() - 1);
		return result;
	}
	
	/**
	 * Transform a string into an inputstream
	 */
	public static InputStream parseStringToIS(String xml)
	{
		if(xml == null)
		{
			return null;
		}
		xml = xml.trim();
		InputStream in = null;
		try
		{
			in = new ByteArrayInputStream(xml.getBytes("UTF-8"));
		}
		catch (Exception ex)
		{}
		return in;
	}
	
	/**
	 * Returns true is all characters in string s are digits
	 * 
	 * @param s
	 * @return
	 */
	public static boolean isNumeric(String s)
	{
		char[] cArray = s.toCharArray();
		for (int i = 0, len = cArray.length; i < len; i++)
		{
			if(!Character.isDigit(cArray[i]))
			{
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Save a string in a file at location PATH
	 * 
	 * @param text
	 * @param path
	 */
	public static void saveText(String text, String path)
	{
		File outFile = new File(path);
		try
		{
			FileWriter out = new FileWriter(outFile);
			out.write(text);
			out.close();
		}
		catch (IOException e)
		{
			System.out.println("ERROR saving text file");
		}
	}
	
	/**
	 * Return the screen width of a given string displayed with font FONT
	 * 
	 * @param str
	 * @return width of string in pixel
	 */
	public static int getStringWidth(String str, Font font)
	{
		if(font == null)
		{
			font = new Font("sans serif", Font.PLAIN, 11);
		}
		
		// TODO
		// java.awt.FontMetrics metric = new java.awt.FontMetrics(font);
		// return metric.stringWidth(str);
		// return 0;
		int result = str.length() * 8;
		return result;
	}
	
	/**
	 * Return the width of string str with font FONT in the program JEX
	 * 
	 * @param str
	 * @param font
	 * @return width of string in pixel
	 */
	public static int getStringWidthv2(Graphics g, String str, Font font)
	{
		FontMetrics metrics = g.getFontMetrics(font);
		return metrics.stringWidth(str);
	}
	
	public static String truncateStringToWidth(String str, Font font, int length)
	{
		return str;
	}
	
	public static void sortStringList(List<String> strings)
	{
		Collections.sort(strings, new StringUtility());
	}
	
	/**
	 * Path comparator method for sorting fileLists
	 */
	@Override
	public int compare(String thisString, String thatString)
	{
		return compareString(thisString, thatString);
	}
	
	public static String removeWhiteSpaceOnEnds(String s)
	{
		String temp = s;
		while (temp.startsWith(" ") || temp.startsWith("\t"))
		{
			temp = temp.substring(1);
		}
		while (temp.endsWith(" ") || temp.endsWith("\t"))
		{
			temp = temp.substring(0, temp.length() - 1);
		}
		return temp;
	}
	
	public static String removeAllWhitespace(String toFix)
	{
		return toFix.replaceAll("\\s+","");
	}
	
	/**
	 * Alphanumeric string sorter
	 * 
	 * @param thisString
	 * @param thatString
	 * @return
	 */
	public static int compareString(String thisString, String thatString)
	{
		String string1 = thisString;
		String string2 = thatString;
		
		if(string2 == null || string1 == null)
		{
			return 0;
		}
		
		int lengthFirstStr = string1.length();
		int lengthSecondStr = string2.length();
		
		int index1 = 0;
		int index2 = 0;
		
		while (index1 < lengthFirstStr && index2 < lengthSecondStr)
		{
			char ch1 = string1.charAt(index1);
			char ch2 = string2.charAt(index2);
			
			int maxLength = Math.max(lengthFirstStr, lengthSecondStr);
			char[] space1 = new char[maxLength];
			char[] space2 = new char[maxLength];
			
			int loc1 = 0;
			int loc2 = 0;
			
			do
			{
				space1[loc1++] = ch1;
				index1++;
				
				if(index1 < lengthFirstStr)
				{
					ch1 = string1.charAt(index1);
				}
				else
				{
					break;
				}
			}
			while (Character.isDigit(ch1) == Character.isDigit(space1[0]));
			
			do
			{
				space2[loc2++] = ch2;
				index2++;
				
				if(index2 < lengthSecondStr)
				{
					ch2 = string2.charAt(index2);
				}
				else
				{
					break;
				}
			}
			while (Character.isDigit(ch2) == Character.isDigit(space2[0]));
			
			String str1 = new String(space1);
			String str2 = new String(space2);
			
			int result;
			
			if(Character.isDigit(space1[0]) && Character.isDigit(space2[0]))
			{
				Long firstNumberToCompare = new Long(Long.parseLong(str1.trim()));
				Long secondNumberToCompare = new Long(Long.parseLong(str2.trim()));
				result = firstNumberToCompare.compareTo(secondNumberToCompare);
			}
			else
			{
				result = str1.compareTo(str2);
			}
			
			if(result != 0)
			{
				return result;
			}
		}
		return lengthFirstStr - lengthSecondStr;
	}
	
	public static CSVList getCSVListAndRemoveWhiteSpaceOnEnds(String param)
	{
		CSVList temp = new CSVList(param);
		CSVList ret = new CSVList();
		for(String p : temp)
		{
			// This list may not be the same length as the channel dim but we'll test for that elsewhere.
			ret.add(StringUtility.removeWhiteSpaceOnEnds(p));
		}
		return ret;
	}
}
