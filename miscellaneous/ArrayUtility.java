package miscellaneous;

import java.util.Arrays;

public class ArrayUtility {
	
	/**
	 * Increment the counter vector
	 * 
	 * @param compteur
	 * @param iniNb
	 * @param maxNb
	 * @param stepNb
	 * @return incremented counter
	 */
	public static int[] getNextCompteur(int[] compteur, int[] iniNb, int[] maxNb, int[] stepNb)
	{
		int len = compteur.length;
		int k = len - 1;
		compteur[k] = compteur[k] + stepNb[k];
		// boolean increasing = true;
		// if (stepNb[k] < 0) {increasing = false;}
		while (k > 0)
		{
			if((compteur[k] > maxNb[k]) && (stepNb[k] >= 0))
			{
				compteur[k] = iniNb[k];
				compteur[k - 1] = compteur[k - 1] + stepNb[k - 1];
				k--;
			}
			else if((compteur[k] < maxNb[k]) && (!(stepNb[k] >= 0)))
			{
				compteur[k] = iniNb[k];
				compteur[k - 1] = compteur[k - 1] + stepNb[k - 1];
				k--;
			}
			else
			{
				return compteur;
			}
		}
		if((compteur[0] > maxNb[0]) && (stepNb[0] >= 0))
		{
			for (int i = 0; (i < len); i++)
			{
				compteur[i] = iniNb[i];
			}
		}
		else if((compteur[0] < iniNb[0]) && (!(stepNb[0] >= 0)))
		{
			for (int i = 0; (i < len); i++)
			{
				compteur[i] = iniNb[i];
			}
		}
		return compteur;
	}
	
	/**
	 * Return the mean value of a float array
	 * 
	 * @param array
	 * @return
	 */
	public static float mean(float[] array)
	{
		float result = 0;
		int len = array.length;
		for (float f : array)
		{
			result = result + f / len;
		}
		return result;
	}
	
	/**
	 * Return the mean value of a float array by removing PERCENTILE from each side
	 * 
	 * @param array
	 * @return
	 */
	public static float mean(float[] array, float percentile)
	{
		// clone and sort the array
		float[] sorted = array.clone();
		Arrays.sort(sorted);
		
		// Define the elements to remove form the mean
		int nbToRemove = (int) (sorted.length * percentile);
		
		float result = 0;
		int len = sorted.length - nbToRemove;
		for (int i = nbToRemove; i < len; i++)
		{
			result = result + sorted[i] / len;
		}
		
		return result;
	}
	
	public static String[] duplicateStringArray(String[] s)
	{
		String[] ret = new String[s.length];
		int i = 0;
		for (String item : s)
		{
			ret[i] = item;
			i = i + 1;
		}
		return ret;
	}
	
	// /**
	// * Is an object part of a vector?
	// */
	// public static boolean isMember(Object o, Object[] l){
	// if (o == null)
	// return false;
	// for (int k=0, len = l.length; (k<len); k++) {
	// if (o.equals(l[k])){return true;}
	// }
	// return false;
	// }
	//
	// /**
	// * Is an object part of a vector?
	// */
	// public static boolean isStringMember(String o, String[] l){
	// if (o == null)
	// return false;
	// for (int k=0, len = l.length; (k<len); k++) {
	// if (o.equals(l[k])){return true;}
	// }
	// return false;
	// }
	//
	//
	// public static TypeName[] duplicateTypeNameArray(TypeName[] s)
	// {
	// TypeName[] ret = new TypeName[s.length];
	// int i = 0;
	// for(TypeName item : s)
	// {
	// ret[i] = item.duplicate();
	// i = i + 1;
	// }
	// return ret;
	// }
	//
	// /**
	// * Add a jexentry to a jexentry array
	// * @return JEXEntry array
	// */
	// public static JEXEntry[] addToArray(JEXEntry[] array, JEXEntry o){
	// JEXEntry[] result = new JEXEntry[array.length+1];
	// System.arraycopy(array, 0, result, 0, array.length);
	// result[result.length-1] = o;
	// return result;
	// }
	//
	// /**
	// * Add a JEXData to a JEXData array
	// * @return JEXObject array
	// */
	// public static JEXData[] addToArray(JEXData[] array, JEXData o){
	// JEXData[] result = new JEXData[array.length+1];
	// System.arraycopy(array, 0, result, 0, array.length);
	// result[result.length-1] = o;
	// return result;
	// }
	//
	// /**
	// * Make a JEXData array into a list
	// * @param dataList
	// * @return JEXData array
	// */
	// public static JEXData[] list2Array(List<JEXData> dataList){
	// JEXData[] result = new JEXData[dataList.size()];
	// for (int i=0, len=dataList.size(); i<len; i++){
	// result[i] = dataList.get(i);
	// }
	// return result;
	// }
	//
	// /**
	// * Add a string to a string array
	// * @return String array
	// */
	// public static String[] addToArray(String[] array, String s){
	// String[] result = new String[array.length+1];
	// System.arraycopy(array, 0, result, 0, array.length);
	// result[result.length-1] = s;
	// return result;
	// }
	//
	// /**
	// * Make a list of exp group descriptors into an array
	// * @param listOfExp
	// * @return Array of ExperimentalGroupDescriptor
	// */
	// public static ExperimentalGroup[] toArray(List<ExperimentalGroup>
	// listOfExp){
	// int len = listOfExp.size();
	// ExperimentalGroup[] result = new ExperimentalGroup[len];
	// for (int i=0; i<len; i++){
	// result[i] = listOfExp.get(i);
	// }
	// return result;
	// }
	
}
