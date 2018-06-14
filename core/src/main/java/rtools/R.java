package rtools;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.TreeMap;
import java.util.Vector;
import java.util.regex.Pattern;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import Database.DBObjects.JEXData;
import Database.DataWriter.FileWriter;
import Database.DataWriter.ImageWriter;
import Database.SingleUserDatabase.JEXWriter;
import jex.statics.JEXDialog;
import jex.statics.OsVersion;
import logs.Logs;
import miscellaneous.CSVList;
import miscellaneous.DirectoryManager;
import miscellaneous.FileUtility;
import miscellaneous.LSVList;
import miscellaneous.SSVList;
import miscellaneous.StringUtility;
import tables.DimensionMap;
import tables.Table;

public class R {

	public static final String FONT_AVANTGARDE = "AvantGarde", FONT_BOOKMAN = "Bookman", FONT_COURIER = "Courier", FONT_HELVETICA = "Helvetica", FONT_HELVETICA_NARROW = "Helvetica-Narrow", FONT_NEWCENTURYSCHOOLBOOK = "NewCenturySchoolbook", FONT_PALATINO = "Palatino", FONT_TIMES = "Times";
	private static final String[] fonts = new String[] { FONT_AVANTGARDE, FONT_BOOKMAN, FONT_COURIER, FONT_HELVETICA, FONT_HELVETICA_NARROW, FONT_NEWCENTURYSCHOOLBOOK, FONT_PALATINO, FONT_TIMES };

	public static final String COMPRESSION_NONE="none", COMPRESSION_RLE="rle", COMPRESSION_LZW="lzw", COMPRESSION_JPEG="jpeg", COMPRESSION_ZIP="zip";
	public static final String[] compressions = new String[]{COMPRESSION_NONE, COMPRESSION_RLE, COMPRESSION_LZW, COMPRESSION_JPEG, COMPRESSION_ZIP};

	// R Statistical Analysis Software Package Server Connection
	public static RConnection rConnection = null;
	public static int numRetries = 0;
	public static int numRetriesLimit = 2;

	private static RConnection connectNew()
	{
		RConnection ret = null;
		try
		{
			ret = new RConnection(); // Throws an exception if it
			// doesn't connect
			return ret;

		}
		catch (Exception e)
		{
			// If we get here it means a previous RServer process was not open
			// and we should open one now
			try
			{
				ScriptRepository.startRServe();
				ret = new RConnection();
				if(isConnected(ret))
				{
					return ret;
				}
				else
				{
					return null;
				}
			}
			catch (Exception e2)
			{
				// If we are here we should give up because we tries to start a
				// new RServe process and failed
				e.printStackTrace();
				return null;
			}
		}
	}

	private static boolean connect()
	{
		rConnection = R.connectNew(); // Throws an exception if it
		if(rConnection == null)
		{
			return false;
		}
		return true;
	}

	private static boolean isConnected(RConnection c)
	{
		if(c != null && c.isConnected())
		{
			// the connection might still be compromised for some reason so check we can perform a simple operation
			try
			{
				c.eval("MyTempVariable12346ewefq2341et <- 0");
				return true;
			}
			catch(RserveException e)
			{
				// If this fails then try (as best we can) to shut it down and return false, instigating an attempt at establishing a new server.
				c.close();
				return false;
			}
		}
		return false;
	}

	public static boolean isConnected()
	{
		return isConnected(R.rConnection);
	}

	public static void close()
	{
		if(R.isConnected())
		{
			R.rConnection.close();
		}
		// Try to kill Rserve if possible.
		else if(OsVersion.IS_WINDOWS)
		{
			try {
				Runtime.getRuntime().exec("taskkill /F /IM Rserve.exe");
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}

	public static REXP eval(String command)
	{
		return evaluate(command, false, false);
	}

	public static REXP evalLineByLine(String command)
	{
		return evaluate(command, false, true);
	}

	public static REXP evalToConsole(String command)
	{
		return evaluate(command, true, false);
	}

	public static REXP evalToConsoleLineByLine(String command)
	{
		return evaluate(command, true, true);
	}

	/**
	 * @param command
	 * @param toConsole
	 * @param lineByLine
	 * @return
	 */
	private static REXP evaluate(String command, boolean toConsole, boolean lineByLine)
	{
		//		if(!safe && command.endsWith(";"))
		//		{
		//			command = command.substring(0, command.length() - 1);
		//		}
		//		if(!safe && command.contains(";"))
		//		{
		//			// Then it is dangerous to use the the other evaluation options.
		//			safe = true;
		//		}

		Logs.log("Attemping command: " + command, 0, "R");
		if(!R.isConnected()) // If not connected start the server and connect
		{
			if(!R.connect()) // If starting the server doesn't work return null
			{
				Logs.log("Couldn't evaluate R command because either couldn't start server or connect to server!", 0, "R");
				return null;
			}
		}
		REXP ret = null;
		LSVList commands = new LSVList(command);
		try
		{
			if(toConsole)
			{
				if(lineByLine)
				{
					for(String s : commands)
					{
						Logs.log(s, R.class);
						//						ret = rConnection.eval(s);
						rConnection.assign(".tmp.", s);
						ret = rConnection.eval("paste(capture.output(print(try(eval(parse(text=.tmp.)),silent=TRUE))),collapse='\\n')");
						if(ret.inherits("try-error"))
						{
							Logs.log("Error: "+ret.toDebugString(), Logs.ERROR, R.class);
						}
						else
						{
							Logs.log(ret.asString(), R.class);
						}
					}
				}
				else
				{
					rConnection.assign(".tmp.", command);
					ret = rConnection.eval("paste(capture.output(print(try(eval(parse(text=.tmp.)),silent=TRUE))),collapse='\\n')");
					if(ret.inherits("try-error"))
					{
						Logs.log("Error: "+ret.toDebugString(), Logs.ERROR, R.class);
					}
					else
					{
						Logs.log(ret.asString(), R.class);
					}
				}

			}
			else
			{
				if(lineByLine)
				{
					for(String s : commands)
					{
						Logs.log(s, R.class);
						rConnection.assign(".tmp.", s);
						ret = rConnection.eval("try(eval(parse(text=.tmp.)),silent=TRUE)");
						if(ret.inherits("try-error"))
						{
							Logs.log("Printing Error", Logs.ERROR, R.class);
							System.err.println("Error: "+ret.toDebugString());
						}
						else
						{
							// Do nothing, keep the console clean
						}
					}
				}
				else
				{
					rConnection.assign(".tmp.", command);
					ret = rConnection.eval("try(eval(parse(text=.tmp.)),silent=TRUE)");
					if(ret.inherits("try-error"))
					{
						Logs.log("Printing Error", Logs.ERROR, R.class);
						System.err.println("Error: "+ret.toDebugString());
					}
					else
					{
						// Do nothing, keep the console clean
					}
				}

			}
		}
		catch (RserveException e)
		{
			e.printStackTrace();
			Logs.log("Couldn't resolve issue with R evaluation of command '" + command + "'", 0, R.class.getSimpleName());
		}
		catch (REXPMismatchException e)
		{
			e.printStackTrace();
		}
		return ret;
	}

	public static REXP setwd(String path)
	{
		return R.eval("setwd(" + R.quotedPath(path) + ")");
	}

	/**
	 * Use this to source a .R file
	 * 
	 * @param path
	 * @return
	 */
	public static REXP source(String path)
	{
		return R.eval("source(" + R.quotedPath(path) + ")");
	}

	public static void serverSource(String path)
	{
		try
		{
			rConnection.serverSource(R.quotedPath(path));
		}
		catch (RserveException e)
		{
			e.printStackTrace();
		}
	}

	public static void serverEval(String commands)
	{
		try
		{
			rConnection.serverEval(commands);
		}
		catch (RserveException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Use this to load a library
	 * 
	 * @param library
	 * @return
	 */
	public static REXP load(String library)
	{
		return R.eval("library(" + R.sQuote(library) + ")");
	}

	/**
	 * Returns the full path where this is being plotted
	 * 
	 * @param extension
	 *            (file extension, e.g. png, tiff, bmp, pdf, or svg)
	 * @param width_inches
	 * @param height_inches
	 * @param res_ppi
	 *            (not used for svg or pdf)
	 * @param fontsize_pts
	 * @param optionalFont
	 * @param optionalTifCompression
	 *            (only used for tif and takes values of none, lzw, rle, jpeg, or zip)
	 * @return
	 */
	public static String startPlot(String extension, double width_inches, double height_inches, double res_ppi, double fontsize_pts, String optionalFont, String optionalTifCompression)
	{
		extension = extension.toLowerCase();
		String filePath = null;
		try
		{
			filePath = DirectoryManager.getUniqueAbsoluteTempPath(extension);
			if(filePath != null && R._startPlot(new File(filePath), width_inches, height_inches, res_ppi, fontsize_pts, optionalFont, optionalTifCompression))
			{
				return filePath;
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Returns the full path where this is being plotted
	 * 
	 * @param extension
	 *            (file extension, e.g. png, tiff, bmp, pdf, or svg)
	 * @param width_inches
	 * @param height_inches
	 * @param res_ppi
	 *            (not used for svg or pdf)
	 * @param fontsize_pts
	 * @param optionalFont
	 * @param optionalTifCompression
	 *            (only used for tif and takes values of none, lzw, rle, jpeg, or zip)
	 * @return
	 */
	public static boolean _startPlot(File file, double width_inches, double height_inches, double res_ppi, double fontsize_pts, String optionalFont, String optionalTifCompression)
	{
		R.eval("graphics.off()");
		String extension = FileUtility.getFileNameExtension(file.getAbsolutePath());
		extension = extension.toLowerCase();
		String commandStart = extension;
		if(extension.equals("tif"))
		{
			commandStart = "tiff";
		}
		if(extension.equals("tiff"))
		{
			extension = "tif";
		}
		// if(R.load("Cairo") == null)
		// {
		// Logs.log("Install Cairo Package for R to enable plotting (best package for plotting using server version of R).",
		// 0, "R");
		// return false;
		// }
		CSVList args = new CSVList();
		args.add(R.quotedPath(file.getAbsolutePath()));
		args.add("height=" + height_inches);
		args.add("width=" + width_inches);
		if(!extension.equals("svg") && !extension.equals("pdf"))
		{
			args.add("res=" + res_ppi);
			args.add("units='in'");
			args.add("type='cairo'");
		}
		args.add("pointsize=" + fontsize_pts);
		if(optionalFont != null)
		{
			if(extension.equals("pdf"))
			{
				if(R.isPostScriptFont(optionalFont))
				{
					args.add("family=" + R.sQuote(optionalFont));
				}
				else
				{
					args.add("family=" + R.sQuote(FONT_HELVETICA));
				}
			}

		}
		if(extension.equals("tif") && optionalTifCompression != null)
		{
			args.add("compression=" + R.sQuote(optionalTifCompression));
		}
		String command = commandStart + "(" + args.toString() + ")";
		if(R.eval(command) == null)
		{
			Logs.log("Couldn't start the " + extension + " plot in R", 0, "R");
			return false;
		}
		return true;
	}

	/**
	 * Calls 'graphics.off()'
	 */
	public static REXP endPlot()
	{
		return R.eval("graphics.off()");
	}

	/**
	 * Put parentheses around the String
	 * 
	 * @param s
	 * @return
	 */
	public static String parentheses(String s)
	{
		return "(" + s + ")";
	}

	/**
	 * Put single quotes around a string
	 * 
	 * @param s
	 * @return
	 */
	public static String sQuote(String s)
	{
		return "'" + s + "'";
	}

	public static CSVList sQuoteCSVList(String csvString)
	{
		CSVList csvl = StringUtility.getCSVListAndRemoveWhiteSpaceOnEnds(csvString);
		CSVList ret = new CSVList();
		for(String s : csvl)
		{
			ret.add(R.sQuote(s));
		}
		return(ret);
	}

	/**
	 * Put single quotes around a string
	 * 
	 * @param s
	 * @return
	 */
	public static String pathString(String path)
	{
		return Pattern.quote(path);
	}

	/**
	 * 
	 * 
	 * @param s
	 * @return
	 */
	public static String quotedPath(String path)
	{
		return "'" + path.replaceAll(Pattern.quote(File.separator), "/") + "'";
	}

	/**
	 * Put double quotes around a string
	 * 
	 * @param s
	 * @return
	 */
	public static String dQuote(String s)
	{
		return "\"" + s + "\"";
	}

	private static boolean isPostScriptFont(String font)
	{
		for (String fontName : fonts)
		{
			if(font.equals(fontName))
			{
				return true;
			}
		}
		return false;
	}

	public REXP get(String name)
	{
		REXP ret = null;
		try
		{
			R.rConnection.get(name, ret, true);
		}
		catch (REngineException e)
		{
			e.printStackTrace();
		}
		return ret;
	}

	public static <E> boolean makeVector(String vectorName, DimensionMap filter, Table<E> data)
	{
		if(data == null || data.data.size() == 0)
		{
			return false;
		}
		E temp = data.data.firstEntry().getValue();
		if(temp instanceof Double)
		{
			Vector<Double> vector = new Vector<Double>();
			for (DimensionMap map : data.dimTable.getMapIterator(filter))
			{
				Double temp2 = (Double) data.getData(map);
				if(temp == null)
				{
					temp2 = Double.NaN;
				}
				vector.add(temp2);
			}
			return makeVector(vectorName, vector);
		}
		else if(temp instanceof String)
		{
			Vector<String> vector = new Vector<String>();
			for (DimensionMap map : data.dimTable.getMapIterator(filter))
			{
				String temp2 = (String) data.getData(map);
				if(temp == null)
				{
					temp2 = "";
				}
				vector.add(temp2);
			}
			return makeVector(vectorName, vector);
		}
		else
		{
			return false;
		}
	}

	public static <E> boolean makeVector(String vectorName, Collection<E> data)
	{
		if(data == null || data.size() == 0)
		{
			return false;
		}
		E temp = data.iterator().next();
		if(temp instanceof Double)
		{
			double[] dNumbers = new double[data.size()];
			int i = 0;
			for (E num : data)
			{
				dNumbers[i] = (Double) num;
				i = i + 1;
			}
			return makeVector(vectorName, dNumbers);
		}
		else if(temp instanceof String)
		{
			String[] aStrings = data.toArray(new String[data.size()]);
			return makeVector(vectorName, aStrings);
		}
		else
		{
			return false;
		}
	}

	public static boolean makeVector(String vectorName, double[] numbers)
	{
		try
		{
			rConnection.assign(vectorName, numbers);
			return true;
		}
		catch (REngineException e)
		{
			e.printStackTrace();
			return false;
		}
	}

	public static boolean makeVector(String vectorName, int[] numbers)
	{
		try
		{
			rConnection.assign(vectorName, numbers);
			return true;
		}
		catch (REngineException e)
		{
			e.printStackTrace();
			return false;
		}
	}

	public static boolean makeVector(String vectorName, String[] strings)
	{
		try
		{
			rConnection.assign(vectorName, strings);
			return true;
		}
		catch (REngineException e)
		{
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Start Rsession, clear workspace variables, call library(foreign) for reading .arff files, create "jexTempRFolder" and "jexDBFolder"
	 */
	public static void initializeWorkspace()
	{
		R.eval("temp <- 0"); // Dummy command to get the R connection up an running.
		R.endPlot();
		R.eval("rm(list=ls())");
		R.load("foreign");
		String tempPath = JEXWriter.getDatabaseFolder() + File.separator + JEXWriter.getTempFolderName() + File.separator + "RScriptTempFolder";
		File tempFolder = new File(tempPath);
		if(!tempFolder.exists())
		{
			tempFolder.mkdirs();
		}
		R.eval("jexTempRFolder <- " + R.quotedPath(tempPath));

		String dbPath = JEXWriter.getDatabaseFolder();
		R.eval("jexDBFolder <- " + R.quotedPath(dbPath));
	}

	/**
	 * Save the information in a JEXData to a variable in the R workspace called 'name'.
	 * 
	 * The object is a list with $type, $name, $value (which is populated with read.arff(detachedpath), i.e., the .jxd file, not the files it may refer to)
	 * @param data
	 * @param name
	 */
	public static void initializeData(JEXData data, String name)
	{
		if(data == null)
		{
			return;
		}
		String path = JEXWriter.getDatabaseFolder() + File.separator + data.getDetachedRelativePath();
		R.eval(name + " <- list()");
		R.eval(name + "$type <- " + R.sQuote(data.getTypeName().getType().toString()));
		R.eval(name + "$name <- " + R.sQuote(data.getTypeName().getName()));
		R.eval(name + "$value <- read.arff(" + R.quotedPath(path) + ")");
	}

	public static JEXData getCharacterVectorAsJEXDataFileObject(String expression, boolean createImageObject)
	{
		TreeMap<DimensionMap,String> files = new TreeMap<DimensionMap,String>();
		REXP fileObject = R.eval(expression);
		String[] fileStrings = null;
		try
		{
			fileStrings = fileObject.asStrings();
			int i = 0;
			for(String s : fileStrings)
			{
				String fixedString = s; //s.replaceAll("/", File.separator); // Might have to figure out Pattern.quote(File.separator) stuff for windows.
				files.put(new DimensionMap("i=" + i), fixedString);
				i++;
			}
			JEXData ret = null;
			if(createImageObject)
			{
				ret = ImageWriter.makeImageStackFromPaths("dummy", files);
			}
			else
			{
				ret = FileWriter.makeFileObject("dummy", null, files);
			}

			return ret;
		}
		catch (REXPMismatchException e)
		{
			Logs.log("Couldn't convert " + expression + " to String[]", R.class);
			e.printStackTrace();
		}
		return null;
	}

	public static JEXData getCharacterVariableAsJEXDataFileObject(String expression, boolean createImageObject)
	{
		REXP fileObject = R.eval(expression);
		String fileString = null;
		try
		{
			fileString = fileObject.asString();
			String fixedString = fileString; //fileString.replaceAll("/", File.separator); // Might have to figure out Pattern.quote(File.separator) stuff for windows.
			JEXData ret = null;
			if(createImageObject)
			{
				ret = ImageWriter.makeImageObject("dummy", fixedString);
			}
			else
			{
				ret = FileWriter.makeFileObject("dummy", null, fixedString);
			}

			return ret;
		}
		catch (REXPMismatchException e)
		{
			Logs.log("Couldn't convert " + expression + " to String", R.class);
			e.printStackTrace();
		}
		return null;
	}

	public static String getExpressionResultAsString(String expression)
	{
		REXP fileObject = R.eval(expression);
		String[] fileString = null;
		try
		{
			fileString = fileObject.asStrings();
			if(fileString.length==0)
			{
				return null;
			}
			if(fileString.length > 1)
			{
				Logs.log("WARNING: Object has length greater than 1. Returning just first element.", R.class);
			}
			String fixedString = fileString[0]; //fileString.replaceAll("/", File.separator); // Might have to figure out Pattern.quote(File.separator) stuff for windows.
			return fixedString;
		}
		catch (Exception e)
		{
			Logs.log("Couldn't convert " + expression + " to String", R.class);
			e.printStackTrace();
		}
		return null;
	}

	public static Boolean getExpressionResultAsBoolean(String expression)
	{
		String s = getExpressionResultAsString(expression);
		if(s == null)
		{
			return null;
		}
		return Boolean.parseBoolean(s);
	}

	public static Double getExpressionResultAsDouble(String expression)
	{
		String s = getExpressionResultAsString(expression);
		if(s == null)
		{
			return null;
		}
		return Double.parseDouble(s);
	}

	public static Integer getExpressionResultAsInteger(String expression)
	{
		String s = getExpressionResultAsString(expression);
		if(s == null)
		{
			return null;
		}
		return Integer.parseInt(s);
	}

	public static TreeMap<DimensionMap,String> getExpressionResultAsStringTable(String expression, String valueCol)
	{
		// Get the expression as a SSVList of CSVLists (semicolons for rows and commas for columns) with first as header
		R.eval("getTableAsSVString <- function(x){paste(x[, do.call(paste, c(.SD, sep = ',')), .SDcols = names(duh)], collapse=';')}");
		String s = getExpressionResultAsString("getTableAsSVString(" + expression + ")");
		SSVList ssv = new SSVList(s);

		// Grab/remove the header
		CSVList header = new CSVList(ssv.remove(0));

		// Get the index of the value column
		int valCol = header.indexOf(valueCol);
		if(valCol < 0)
		{
			JEXDialog.messageDialog("Couldn't find the column '" + valueCol + "' in the table. returning null", R.class);
			return null;
		}

		// Gather the table of information
		TreeMap<DimensionMap,String> ret = new TreeMap<>();
		for(String row : ssv)
		{
			CSVList csv = new CSVList(row);
			String val = null;
			DimensionMap map = new DimensionMap();
			for(int i = 0; i < csv.size(); i++)
			{
				if(i == valCol)
				{
					val = csv.get(i);
				}
				else
				{
					map.put(header.get(i), csv.get(i));
				}
			}
			ret.put(map, val);
		}

		return ret;
	}

	public static TreeMap<DimensionMap,Double> getExpressionResultAsDoubleTable(String expression, String valueCol)
	{
		// Get the expression as a SSVList of CSVLists (semicolons for rows and commas for columns) with first as header
		R.eval("getTableAsSVString <- function(x){paste(x[, do.call(paste, c(.SD, sep = ',')), .SDcols = names(duh)], collapse=';')}");
		String s = getExpressionResultAsString("getTableAsSVString(" + expression + ")");
		SSVList ssv = new SSVList(s);

		// Grab/remove the header
		CSVList header = new CSVList(ssv.remove(0));

		// Get the index of the value column
		int valCol = header.indexOf(valueCol);
		if(valCol < 0)
		{
			JEXDialog.messageDialog("Couldn't find the column '" + valueCol + "' in the table. returning null", R.class);
			return null;
		}

		// Gather the table of information
		TreeMap<DimensionMap,Double> ret = new TreeMap<>();
		for(String row : ssv)
		{
			CSVList csv = new CSVList(row);
			Double val = null;
			DimensionMap map = new DimensionMap();
			for(int i = 0; i < csv.size(); i++)
			{
				if(i == valCol)
				{
					val = Double.parseDouble(csv.get(i));
				}
				else
				{
					map.put(header.get(i), csv.get(i));
				}
			}
			ret.put(map, val);
		}

		return ret;
	}

	public static TreeMap<DimensionMap,Boolean> getExpressionResultAsBooleanTable(String expression, String valueCol)
	{
		// Get the expression as a SSVList of CSVLists (semicolons for rows and commas for columns) with first as header
		R.eval("getTableAsSVString <- function(x){paste(x[, do.call(paste, c(.SD, sep = ',')), .SDcols = names(duh)], collapse=';')}");
		String s = getExpressionResultAsString("getTableAsSVString(" + expression + ")");
		SSVList ssv = new SSVList(s);

		// Grab/remove the header
		CSVList header = new CSVList(ssv.remove(0));

		// Get the index of the value column
		int valCol = header.indexOf(valueCol);
		if(valCol < 0)
		{
			JEXDialog.messageDialog("Couldn't find the column '" + valueCol + "' in the table. returning null", R.class);
			return null;
		}

		// Gather the table of information
		TreeMap<DimensionMap,Boolean> ret = new TreeMap<>();
		for(String row : ssv)
		{
			CSVList csv = new CSVList(row);
			Boolean val = null;
			DimensionMap map = new DimensionMap();
			for(int i = 0; i < csv.size(); i++)
			{
				if(i == valCol)
				{
					val = Boolean.parseBoolean(csv.get(i));
				}
				else
				{
					map.put(header.get(i), csv.get(i));
				}
			}
			ret.put(map, val);
		}

		return ret;
	}

	public static TreeMap<DimensionMap,Integer> getExpressionResultAsIntegerTable(String expression, String valueCol)
	{
		// Get the expression as a SSVList of CSVLists (semicolons for rows and commas for columns) with first as header
		R.eval("getTableAsSVString <- function(x){paste(x[, do.call(paste, c(.SD, sep = ',')), .SDcols = names(duh)], collapse=';')}");
		String s = getExpressionResultAsString("getTableAsSVString(" + expression + ")");
		SSVList ssv = new SSVList(s);

		// Grab/remove the header
		CSVList header = new CSVList(ssv.remove(0));

		// Get the index of the value column
		int valCol = header.indexOf(valueCol);
		if(valCol < 0)
		{
			JEXDialog.messageDialog("Couldn't find the column '" + valueCol + "' in the table. returning null", R.class);
			return null;
		}

		// Gather the table of information
		TreeMap<DimensionMap,Integer> ret = new TreeMap<>();
		for(String row : ssv)
		{
			CSVList csv = new CSVList(row);
			Integer val = null;
			DimensionMap map = new DimensionMap();
			for(int i = 0; i < csv.size(); i++)
			{
				if(i == valCol)
				{
					val = Integer.parseInt(csv.get(i));
				}
				else
				{
					map.put(header.get(i), csv.get(i));
				}
			}
			ret.put(map, val);
		}

		return ret;
	}

	public static boolean reorganize(String dataName, String idCols, String measurementCols, String valueCols, String dcastArgs)
	{

		if(measurementCols == null || measurementCols.equals(""))
		{
			JEXDialog.messageDialog("This function requires at least one 'Measurement' column name. (see ?dcast of data.table in R, formula = Id ~ Measurment and value.var= Value). Aborting.");
			return false;
		}
		if(valueCols == null || valueCols.equals(""))
		{
			JEXDialog.messageDialog("This function requires at least one 'Value' column name. (see ?dcast of data.table in R, formula = Id ~ Measurment and value.var= Value). Aborting.");
			return false;
		}
		if(idCols == null || idCols.equals(""))
		{
			idCols = "NULL"; // this causes R to guess what the value column is.
		}
		else
		{
			idCols = R.sQuote(idCols);
		}

		ScriptRepository.sourceGitHubFile("jaywarrick", "R-General", "master", ".Rprofile");
		R.load("data.table");

		if(dcastArgs == null)
		{
			R.eval(dataName + " <- reorganize(data=" + dataName + ", idCols=" + idCols + ", measurementCols=" + R.sQuote(measurementCols) + ", valueCols=" + R.sQuote(valueCols) + ")");
		}
		else
		{
			R.eval(dataName + " <- reorganize(data=" + dataName + ", idCols=" + idCols + ", measurementCols=" + R.sQuote(measurementCols) + ", valueCols=" + R.sQuote(valueCols) + ", " + dcastArgs + ")");
		}
		return true;
	}
	
	public static Boolean hasColumn(String tableVarName, String colName)
	{
		return getExpressionResultAsBoolean(colName + " %in% names(" + tableVarName + ")");
	}
	
	public static Boolean hasColumns(String tableVarName, Collection<String> colNames)
	{
		for(String col : colNames)
		{
			if(!hasColumn(tableVarName, col))
			{
				return false;
			}
		}
		return true;
	}

	//	private static void defineReorganize()
	//	{
	//		LSVList func = new LSVList();
	//		R.load("data.table");
	//		func.add("reorganize <- function(data, idCols=NULL, measurementCols='Measurement', valueCols='Value', ...)");
	//		func.add("{");
	//		func.add("library(data.table)");
	//		func.add("isDataTable <- FALSE");
	//		func.add("if(is.data.table(data))");
	//		func.add("{");
	//		func.add("isDataTable <- TRUE");
	//		func.add("}");
	//		func.add("else");
	//		func.add("{");
	//		func.add("data <- data.table(data)");
	//		func.add("}");
	//		func.add("measurementCols <- strsplit(measurementCols, ',', fixed=T)");
	//		func.add("measurementCols <- mapply(gsub, '^\\s+|\\s+$', '', measurementCols)");
	//		func.add("if(is.null(idCols))");
	//		func.add("{");
	//		func.add("idCols <- names(data)[!(names(data) %in% c(measurementCols, valueCols))]");
	//		func.add("}");
	//		func.add("formula <- as.formula(paste(paste(idCols, collapse='+'), ' ~ ', paste(measurementCols, collapse='+')))");
	//		func.add("data <- dcast(data, as.formula(paste(paste(idCols, collapse='+'), ' ~ ', paste(measurementCols, collapse='+'))), value.var = valueCols, ...)");
	//		func.add("if(isDataTable)");
	//		func.add("{");
	//		func.add("return(data)");
	//		func.add("}else{");
	//		func.add("else");
	//		func.add("return(data.frame(data))}}");
	//	}
}
