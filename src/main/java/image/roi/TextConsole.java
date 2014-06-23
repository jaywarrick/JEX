package image.roi;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.rosuda.REngine.REngineCallbacks;
import org.rosuda.REngine.REngine;

public class TextConsole implements REngineCallbacks {
	
	public void rWriteConsole(REngine re, String text, int oType)
	{
		System.out.print(text);
	}
	
	public void rBusy(REngine re, int which)
	{
		System.out.println("rBusy(" + which + ")");
	}
	
	public String rReadConsole(REngine re, String prompt, int addToHistory)
	{
		System.out.print(prompt);
		try
		{
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			String s = br.readLine();
			return (s == null || s.length() == 0) ? s : s + "\n";
		}
		catch (Exception e)
		{
			System.out.println("jriReadConsole exception: " + e.getMessage());
		}
		return null;
	}
	
	public void rShowMessage(REngine re, String message)
	{
		System.out.println("rShowMessage \"" + message + "\"");
	}
	
	@SuppressWarnings("deprecation")
	public String rChooseFile(REngine re, int newFile)
	{
		FileDialog fd = new FileDialog(new Frame(), (newFile == 0) ? "Select a file" : "Select a new file", (newFile == 0) ? FileDialog.LOAD : FileDialog.SAVE);
		fd.show();
		String res = null;
		if(fd.getDirectory() != null)
			res = fd.getDirectory();
		if(fd.getFile() != null)
			res = (res == null) ? fd.getFile() : (res + fd.getFile());
		return res;
	}
	
	public void rFlushConsole(REngine re)
	{}
	
	public void rLoadHistory(REngine re, String filename)
	{}
	
	public void rSaveHistory(REngine re, String filename)
	{}
}
