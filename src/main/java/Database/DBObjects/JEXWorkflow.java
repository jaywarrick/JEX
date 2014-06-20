package Database.DBObjects;

import java.io.File;
import java.util.TreeMap;
import java.util.Vector;

import jex.statics.JEXStatics;
import miscellaneous.DateUtility;
import miscellaneous.FileUtility;
import miscellaneous.Pair;
import tables.DimTable;
import tables.DimensionMap;
import Database.SingleUserDatabase.JEXDataIO;
import cruncher.JEXFunction;

public class JEXWorkflow extends Vector<JEXFunction> {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public static String WORKFLOW_INDEX = "Workflow Index";
	
	public String name;
	
	public JEXWorkflow(String name)
	{
		super();
		this.name = name;
	}
	
	public JEXWorkflow(JEXData data)
	{
		this(data.getTypeName().getName());
		TreeMap<Integer,Pair<String,Vector<JEXDataSingle>>> functions = new TreeMap<Integer,Pair<String,Vector<JEXDataSingle>>>();
		
		for (DimensionMap map : data.getDataMap().keySet())
		{
			String functionIndexString = map.get(JEXWorkflow.WORKFLOW_INDEX);
			Integer functionIndex = Integer.parseInt(functionIndexString);
			Pair<String,Vector<JEXDataSingle>> temp = functions.get(functionIndex);
			if(temp == null) // create the list for singles because it doesn't
			// already exist
			{
				temp = new Pair<String,Vector<JEXDataSingle>>(map.get(JEXFunction.FUNCTION_NAME), new Vector<JEXDataSingle>());
			}
			temp.p2.add(data.getDataMap().get(map));
			functions.put(functionIndex, temp);
		}
		for (Integer index : functions.keySet())
		{
			JEXFunction func = new JEXFunction(functions.get(index).p1, functions.get(index).p2);
			this.add(func);
		}
	}
	
	public String getName()
	{
		return this.name;
	}
	
	public void setName(String name)
	{
		this.name = name;
	}
	
	public JEXData toJEXData()
	{
		JEXData ret = new JEXData(JEXData.WORKFLOW, this.name);
		ret.setAuthor(JEXStatics.jexManager.getUserName());
		ret.setDataObjectDate(DateUtility.getDate());
		ret.setDataObjectModifDate(DateUtility.getDate());
		int count = 0;
		for (JEXFunction func : this)
		{
			Vector<JEXDataSingle> singles = func.getSingles();
			for (JEXDataSingle single : singles)
			{
				DimensionMap map = single.getDimensionMap();
				map.put(JEXWorkflow.WORKFLOW_INDEX, "" + count);
				ret.addData(map, single);
			}
			count = count + 1;
		}
		ret.setDimTable(new DimTable(ret.getDataMap()));
		return ret;
	}
	
	/**
	 * Save the list of functions into an arff file
	 * 
	 * @param Path
	 */
	public void saveWorkflow(File dst)
	{
		JEXData data = this.toJEXData();
		try
		{
			String path = JEXDataIO.createJXD(data, "jxw", false);
			File newWorkflowFile = new File(path);
			FileUtility.moveFileOrFolder(newWorkflowFile, dst, true);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Load function located at path FILE
	 * 
	 * @param file
	 */
	public static JEXWorkflow loadWorkflowFile(File file)
	{
		JEXData data = new JEXData(JEXData.WORKFLOW, FileUtility.getFileNameWithoutExtension(file.getPath()));
		JEXDataIO.loadJXD(data, file.getAbsolutePath());
		return new JEXWorkflow(data);
	}
}
