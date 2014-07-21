package weka.core.converters;

public class WekaConverterUtility {
	
	public static void addNewFileExtensionToViewer(String ext)
	{
		ConverterUtils.m_FileLoaders.put("." + ext, ConverterUtils.m_FileLoaders.get(".arff"));
	}
	
}
