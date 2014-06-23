package jex.utilities;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import jex.statics.PrefsUtility;
import logs.Logs;
import signals.SSCenter;
import dk.ange.octave.OctaveEngine;
import dk.ange.octave.OctaveEngineFactory;
import dk.ange.octave.exception.OctaveEvalException;

public class OctaveController {
	
	private OctaveEngine octave;
	
	public static final String SIG_EvalErrorMessage_StringMessage = "eval exception";
	public static boolean drawnow = true;
	
	public OctaveController()
	{}
	
	public void makeEngine(String workingDirectory)
	{
		// if an existing engine is open, then close it
		if(octave != null)
		{
			octave.close();
			octave = null;
		}
		
		// start the engine for later use during communication
		OctaveEngineFactory factory = new OctaveEngineFactory();
		factory.setOctaveProgram(new File(PrefsUtility.getOctaveBinary()));
		factory.setWorkingDir(new File(workingDirectory));
		factory.setErrorWriter(new OutputStreamWriter(System.err));
		try
		{
			octave = factory.getScriptEngine();
			Logs.log("Octave engine created !", 0, this);
		}
		catch (Exception e)
		{
			// Logs.log("Error ! Octave engine was not created !",
			// 0, this);
			octave = null;
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
	
	public void runCode(String code)
	{
		String[] breaks = code.split("[\\s]*debug[\\s]*\\n");
		String[] lines = code.split("\n");
		try
		{
			// octave.eval(code);
			int curLine = 0;
			for (int i = 0; i < breaks.length; i++)
			{
				System.out.println(breaks[i]);
				octave.eval(breaks[i]);
				if(drawnow)
				{
					octave.eval("drawnow()");
				}
				lines = breaks[i].split("\n");
				curLine = curLine + lines.length + 1;
				if(i == 0 && breaks[i].equals(""))
					curLine--;
			}
		}
		catch (OctaveEvalException e)
		{
			Logs.log(e.getMessage(), 0, this);
			SSCenter.defaultCenter().emit(this, SIG_EvalErrorMessage_StringMessage, e.getMessage());
		}
	}
	
	public void runCode(File mFile)
	{
		try
		{
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
