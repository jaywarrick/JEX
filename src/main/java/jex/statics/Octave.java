package jex.statics;

import java.io.File;
import java.io.IOException;
import java.io.Writer;

import logs.Logs;
import signals.SSCenter;
import dk.ange.octave.OctaveEngine;
import dk.ange.octave.OctaveEngineFactory;
import dk.ange.octave.exception.OctaveEvalException;
import dk.ange.octave.exception.OctaveIOException;

public class Octave {
	
	private OctaveEngine octave;
	private String workingDirectory;
	
	public static final String SIG_EvalErrorMessage_StringMessage = "eval exception";
	
	public Octave(String workingDirectory)
	{
		this.workingDirectory = workingDirectory;
		this.makeEngine(this.workingDirectory);
	}
	
	private void makeEngine(String workingDirectory)
	{
		// if an existing engine is open, then close it
		if(octave != null)
		{
			octave.close();
			octave = null;
		}
		
		// start the engine for later use during communication
		OctaveEngineFactory factory = new OctaveEngineFactory();
		String binaryPath = PrefsUtility.getOctaveBinary();
		factory.setOctaveProgram(new File(binaryPath));
		this.setWorkingDirectory(workingDirectory);
		JEXStatics.statusBar.setStatusText("Idle");
		
		try
		{
			octave = factory.getScriptEngine();
			// OctaveLiteStatics.octave = this.octave;
			Logs.log("Octave engine created !", 0, this);
			JEXStatics.statusBar.setStatusText("Idle");
		}
		catch (Exception e)
		{
			Logs.log("Error ! Octave engine was not created !", 0, this);
			JEXStatics.statusBar.setStatusText("Octave error");
			octave = null;
			// OctaveLiteStatics.octave = null;
		}
		
	}
	
	public void setWorkingDirectory(String path)
	{
		if(this.octave == null)
			return;
		this.runCode("cd(\'" + path + "\')");
		this.runCode("pwd");
	}
	
	public void runCommand(String cmd)
	{
		this.runCode(cmd);
	}
	
	private void runCode(String code)
	{
		JEXStatics.statusBar.setProgressPercentage(0);
		JEXStatics.statusBar.setStatusText("Idle");
		String[] breaks = code.split("[\\s]*debug[\\s]*\\n");
		String[] lines = code.split("\n");
		int totalbreaks = breaks.length;
		try
		{
			boolean drawnow = PrefsUtility.getOctaveAutoDrawNow();
			
			// octave.eval(code);
			int curLine = 0;
			for (int i = 0; i < breaks.length; i++)
			{
				System.out.println(breaks[i]);
				try
				{
					octave.eval(breaks[i]);
				}
				catch (OctaveIOException e)
				{
					Logs.log("Octave died, trying to bring back to life", 0, this);
					this.makeEngine(this.workingDirectory);
				}
				if(drawnow)
				{
					octave.eval("drawnow()");
				}
				lines = breaks[i].split("\n");
				curLine = curLine + lines.length + 1;
				if(i == 0 && breaks[i].equals(""))
					curLine--;
				JEXStatics.statusBar.setProgressPercentage(100 * (i + 1) / totalbreaks);
				JEXStatics.statusBar.setStatusText("Finished debug at line " + (curLine));
				JEXStatics.statusBar.labelBar.repaint();
			}
			
			JEXStatics.statusBar.setProgressPercentage(0);
			JEXStatics.statusBar.setStatusText("Octave Idle");
			
		}
		catch (OctaveEvalException e)
		{
			SSCenter.defaultCenter().emit(this, SIG_EvalErrorMessage_StringMessage, e.getMessage());
		}
	}
	
	public void runFile(File mFile)
	{
		JEXStatics.statusBar.setProgressPercentage(0);
		JEXStatics.statusBar.setStatusText("Idle");
		try
		{
			boolean drawnow = PrefsUtility.getOctaveAutoDrawNow();
			
			JEXStatics.statusBar.setProgressPercentage(50);
			JEXStatics.statusBar.labelBar.repaint();
			
			String mFilePath;
			try
			{
				mFilePath = mFile.getCanonicalPath();
				octave.eval("run(\'" + mFilePath + "\');");
				System.out.println("run(\'" + mFilePath + "\')");
				if(drawnow)
				{
					octave.eval("drawnow();");
					System.out.println("drawnow();");
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			
		}
		catch (OctaveEvalException e)
		{
			SSCenter.defaultCenter().emit(this, SIG_EvalErrorMessage_StringMessage, e.getMessage());
		}
		JEXStatics.statusBar.setProgressPercentage(100);
		JEXStatics.statusBar.labelBar.repaint();
		
		JEXStatics.statusBar.setProgressPercentage(0);
		JEXStatics.statusBar.labelBar.repaint();
		
		JEXStatics.statusBar.setStatusText("Idle");
	}
	
	public void sourceFile(File mFile)
	{
		JEXStatics.statusBar.setProgressPercentage(0);
		JEXStatics.statusBar.setStatusText("Idle");
		try
		{
			boolean drawnow = PrefsUtility.getOctaveAutoDrawNow();
			
			JEXStatics.statusBar.setProgressPercentage(50);
			JEXStatics.statusBar.labelBar.repaint();
			
			String mFilePath;
			try
			{
				mFilePath = mFile.getCanonicalPath();
				octave.eval("source(\'" + mFilePath + "\');");
				System.out.println("source(\'" + mFilePath + "\')");
				if(drawnow)
				{
					octave.eval("drawnow();");
					System.out.println("drawnow();");
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			
		}
		catch (OctaveEvalException e)
		{
			SSCenter.defaultCenter().emit(this, SIG_EvalErrorMessage_StringMessage, e.getMessage());
		}
		JEXStatics.statusBar.setProgressPercentage(100);
		JEXStatics.statusBar.labelBar.repaint();
		
		JEXStatics.statusBar.setProgressPercentage(0);
		JEXStatics.statusBar.labelBar.repaint();
		
		JEXStatics.statusBar.setStatusText("Idle");
	}
	
	public void setWriter(Writer writer)
	{
		if(octave == null)
		{
			Logs.log("Error ! Octave engine is null cannot connect terminal !", 0, this);
		}
		else
		{
			octave.setWriter(writer);
			octave.setErrorWriter(writer);
		}
	}
	
	public void close()
	{
		if(octave != null)
		{
			octave.close();
		}
	}
	
}