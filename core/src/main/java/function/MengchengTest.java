package function;

import java.util.TreeMap;

import logs.Logs;
import miscellaneous.FileUtility;
import tables.DimensionMap;

public class MengchengTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		testPathConversion();

	}

	public static void testPathConversion()
	{
		String path = "/Users/jaywarrick/Images/Image_myRow001_myCol004_myColor005_yourColor78.tif";
		String separator = "_";
		// Figure out how to do it here...

		String name = FileUtility.getFileNameWithoutExtension(path);
		String[] names = name.split(separator);
		
		DimensionMap dimMap = new DimensionMap();
		String dimValue, dimName, temp;
		int splitIndex = 0;
		
		for (int i = 0; i < names.length; i++){
			temp = names[i];
			
			// find the first Digit in the string in order to separate dimName and dimValue
			for (int j = 0; j < temp.length(); j++){
				if (Character.isDigit(temp.charAt(j))){
					splitIndex = j;
					break;
				}
				else
					splitIndex = 0;
			}
			
			// if the string is not a dimName followed by a dimValue then skip it.
			if (splitIndex != 0) {
				dimName = temp.substring(0, splitIndex);
				dimValue = temp.substring(splitIndex);
				
				dimMap.put(dimName, dimValue);
			}
		}
		
		Logs.log(dimMap.toString(), MengchengTest.class);
	}



}
