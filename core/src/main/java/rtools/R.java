package rtools;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Vector;
import java.util.regex.Pattern;

import logs.Logs;
import miscellaneous.CSVList;
import miscellaneous.DirectoryManager;
import miscellaneous.FileUtility;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

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
	
	private static boolean connect()
	{
		try
		{
			if(!R.isConnected())
			{
				// If this is a restart of JEX and RServe is already started\
				// We should try to reconnect to the RServe that is already
				// going if possible
				// We do this by creating a new RConnection object which
				// searches for the already running process
				// We then check to see if it is connected. If not, start RServe
				// and try again.
				// If that doesn't work give up.
				rConnection = new RConnection(); // Throws an exception if it
				// doesn't connect
				return true;
			}
			else
			{
				return true;
			}
			
		}
		catch (Exception e)
		{
			// If we get here it means a previous RServer process was not open
			// and we should open one now
			try
			{
				ScriptRepository.startRServe();
				rConnection = new RConnection();
				return (R.isConnected());
			}
			catch (Exception e2)
			{
				// If we are here we should give up because we tries to start a
				// new RServe process and failed
				e.printStackTrace();
				return false;
			}
		}
	}
	
	public static boolean isConnected()
	{
		return (R.rConnection != null && R.rConnection.isConnected());
	}
	
	public static void close()
	{
		R.rConnection.close();
	}
	
	public static REXP eval(String command)
	{
		return evaluate(command, false, true);
	}
	
	public static REXP evalAsString(String command)
	{
		return evaluate(command, true, false);
	}
	
	public static REXP evalTry(String command)
	{
		return evaluate(command, false, false);
	}
	
	/**
	 * Be careful because ";" is not allowed at the end of the command when
	 * using as evalAsString or evalTry (safe avoids the use of the paste and
	 * try command to avoid this pitfall generally).
	 * @param command
	 * @param asString
	 * @param safe
	 * @return
	 */
	private static REXP evaluate(String command, boolean asString, boolean safe)
	{
		if(!safe && command.endsWith(";"))
		{
			command = command.substring(0, command.length() - 1);
		}
		if(!safe && command.contains(";"))
		{
			// Then it is dangerous to use the the other evaluation options.
			safe = true;
		}
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
		try
		{
			if(!safe && asString)
			{
				ret = rConnection.eval("paste(capture.output(print(" + command + ")),collapse='\\n')");
				Logs.log(ret.asString(), R.class);
			}
			else if(!safe)
			{
				ret = rConnection.parseAndEval("try(" + command + ",silent=TRUE)");
				if(ret.inherits("try-error"))
				{
					Logs.log(ret.asString(), Logs.ERROR, R.class);
				}
				else
				{}
			}
			else //safe
			{
				ret = rConnection.eval(command);
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
		catch (REngineException e)
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
		R.evalTry("graphics.off()");
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
		return R.evalTry("graphics.off()");
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
	 * Put single quotes around a string
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
		E temp = data.data.get(0);
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
	
}
