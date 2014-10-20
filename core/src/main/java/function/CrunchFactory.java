package function;

import function.plugin.IJ2.IJ2CrunchablePlugin;
import function.plugin.IJ2.IJ2PluginUtility;
import function.plugin.mechanism.JEXCrunchablePlugin;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.JEXPluginInfo;
import function.plugin.plugins.imageProcessing.AdjustImage;
import function.plugin.plugins.imageTools.ImageStitcher;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import jex.JEXperiment;
import jex.statics.PrefsUtility;
import logs.Logs;
import miscellaneous.StringUtility;

import org.scijava.plugin.PluginInfo;

public class CrunchFactory extends URLClassLoader {
	
	public Vector<String> internalPluginNames = new Vector<String>();
	public Vector<String> externalPluginNames = new Vector<String>();
	static TreeMap<String,JEXCrunchable> listOfCrunchers = getExperimentalDataCrunchers();
	
	public CrunchFactory()
	{
		super(new URL[0], JEXperiment.class.getClassLoader());
	}
	
	public void loadPluginsFromFolder(String path)
	{
		// find all JAR files on the path and subdirectories
		if(path != null && !path.equals(""))
		{
			File f = new File(path);
			if(f.exists() && f.isDirectory())
			{
				try
				{
					// Add plugin directory to search path
					this.addURL(f.toURI().toURL());
					String[] list = f.list();
					if(list == null)
					{
						return;
					}
					for (int i = 0; i < list.length; i++)
					{
						if(list[i].equals(".rsrc"))
						{
							continue;
						}
						f = new File(path, list[i]);
						if(f.isDirectory())
						{
							try
							{
								// Add first level subdirectories to search path
								this.addURL(f.toURI().toURL());
							}
							catch (MalformedURLException e)
							{
								Logs.log("PluginClassLoader: Error", this);
								e.printStackTrace();
							}
							String[] innerlist = f.list();
							if(innerlist == null)
							{
								continue;
							}
							for (int j = 0; j < innerlist.length; j++)
							{
								File g = new File(f, innerlist[j]);
								if(g.isFile())
								{
									this.add(g, f.getName());
								}
							}
						}
						else
						{
							this.add(f, null);
						}
					}
				}
				catch (MalformedURLException e)
				{
					Logs.log("Couldn't establish a URL from the given path: " + path + ". Check location and/or fix JEX preferences to point to the external plugins folder (button in top right of window).", CrunchFactory.class);
				}
			}
			else
			{
				Logs.log("Couldn't find the external plugins folder at " + path + ". Check location and/or fix JEX preferences to point to the external plugins folder (button in top right of window).", CrunchFactory.class);
			}
		}
		else
		{
			Logs.log("Couldn't find the external plugins folder using the null or empty path given. Set the external plugins folder path in the JEX preferences (button in top right of window).", CrunchFactory.class);
		}
	}
	
	private static TreeMap<String,JEXCrunchable> loadJEXCrunchablePlugins()
	{
		TreeMap<String,JEXCrunchable> ret = new TreeMap<String,JEXCrunchable>();
		List<PluginInfo<JEXPlugin>> jexPlugins = IJ2PluginUtility.ij.plugin().getPluginsOfType(JEXPlugin.class);
		Logs.log("Number of JEXPlugins: " + jexPlugins.size(), CrunchFactory.class);
		for(PluginInfo<JEXPlugin> info : jexPlugins)
		{
			Logs.log("Found new JEXPlugin: " + info.getName() + " - "+ info.getClassName(), CrunchFactory.class);
			try
			{
				JEXPluginInfo fullInfo = new JEXPluginInfo(info);
				JEXCrunchablePlugin crunchable = new JEXCrunchablePlugin(fullInfo);
				ret.put(crunchable.getName(), crunchable);
			}
			catch(java.lang.NoClassDefFoundError e1)
			{
				// Just skip this because I think the EclipseHelper is messing up the accumulation of the annotation index
				Logs.log("Remember to figure out why annotation index in Eclipse helper gets called sometimes (when legacy called first) and not others (when ImageJ2 called first).", CrunchFactory.class);
			}
			catch(Exception e)
			{
				e.printStackTrace();
				// Just skip this because I think the EclipseHelper is messing up the accumulation of the annotation index
				Logs.log("Couldn't load " + info.getName() + ". Check error and see if an @Parameter, @Input, or @Output annotation isn't perfectly correct.", CrunchFactory.class);
			}
		}
		return ret;
	}
	
	private void add(File f, String packageName)
	{
		if(f.getName().endsWith(".jar") || f.getName().endsWith(".zip"))
		{
			try
			{
				this.addURL(f.toURI().toURL());
			}
			catch (MalformedURLException e)
			{
				ij.IJ.log("PluginClassLoader: " + e);
			}
		}
		else if(f.getName().endsWith(".class") && !f.getName().contains("$"))
		{
			String temp = f.getName().substring(0, f.getName().length() - 6);
			if(!this.externalPluginNames.contains(temp))
			{
				if(packageName != null)
				{
					this.externalPluginNames.add(packageName + "." + temp);
				}
				else
				{
					this.externalPluginNames.add(temp);
				}
			}
		}
	}
	
	/**
	 * Return the experimental data cruncher of name FUNCTIONNAME
	 * 
	 * @param functionName
	 * @return An experimental data cruncher
	 */
	public static JEXCrunchable getExperimentalDataCrunch(String functionName)
	{
		try
		{
			// Get the native ExperimentalDataCrunch
			JEXCrunchable result = listOfCrunchers.get(functionName);
			
			if(result == null)
			{
				return null;
			}
			
			if(result instanceof IJ2CrunchablePlugin)
			{
				return new IJ2CrunchablePlugin(((IJ2CrunchablePlugin) result).command);
			}
			if(result instanceof JEXCrunchablePlugin)
			{
				return new JEXCrunchablePlugin(((JEXCrunchablePlugin) result).info);
			}
			else
			{ // Old JEXCrunchable
				return result.getClass().newInstance();
			}
		}
		catch (InstantiationException e)
		{
			e.printStackTrace();
		}
		catch (IllegalAccessException e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Return a map of all loadable functions
	 * 
	 * @return
	 */
	public static TreeMap<String,JEXCrunchable> getExperimentalDataCrunchers()
	{
		// Create a structure to store all the ExperimentalDataCrunch Objects
		TreeMap<String,JEXCrunchable> result = new TreeMap<String,JEXCrunchable>();
		
		Logs.log("Getting ImageJ Plugins.", CrunchFactory.class);
		TreeMap<String,IJ2CrunchablePlugin> ij2Plugins = IJ2PluginUtility.ijCommands;
		result.putAll(ij2Plugins);
		
		Logs.log("Getting external JEXCrunchables.", CrunchFactory.class);
		// Find externally defined plugin class names
		String prefsPluginsPath = PrefsUtility.getExternalPluginsFolder();
		String jarPath = null;
		CrunchFactory loader = new CrunchFactory(); // constructor does the storing of the class names in the "externalPluginNames" vector field
		
		Logs.log("Getting internal JEXCrunchables.", CrunchFactory.class);
		// Find internally defined plugin class names
		URL classLoaderURL = JEXCrunchable.class.getResource("JEXCrunchable.class");		
		if(classLoaderURL == null)
		{
			Logs.log("ClassLoaderURL was null", CrunchFactory.class);
		}
		else
		{
			Logs.log("Source code URL for finding internally defined JEX functions: " + classLoaderURL.toString(), CrunchFactory.class);
		}
		
		if(!classLoaderURL.toString().startsWith("file:"))
		{
			if(classLoaderURL.toString().startsWith("rsrc:"))
			{
				// Then we are running JEX from the JEX.jar created using the Eclipse export Runnable Jar plugin (jarinjar rsrc URL) and we need to find the function class names this way...
				try
				{
					URI jarFolderURI = ClassLoader.getSystemClassLoader().getResource(".").toURI();
					jarPath = jarFolderURI.getPath();
					// Logs.log("Normalizing jar path... " + systemJarPath.toString(), CrunchFactory.class);
					// File jarPath = new File(systemJarPath); // Need to do this because Windows might put a leading slash but...
					// systemJarPath = jarPath.getAbsolutePath(); // the File class knows how to deal with this and remove it.
					URL jarURL = new URL("jar:" + jarFolderURI + "JEX.jar!/");
					Logs.log("System JEX.jar path... " + jarURL.toString(), CrunchFactory.class);
					loader.getInternalPluginNamesFromJar(jarURL);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
			else if(classLoaderURL.toString().startsWith("jar:"))
			{
				// We are running from JEX created using the export App Bundle plugin of eclipse
				try
				{
					URL jarFile = new URL(classLoaderURL.toString().substring(0, classLoaderURL.toString().indexOf("!") + 2)); // jar file that JEX is packaged in
					String jarURLPath = classLoaderURL.toString().substring(0, classLoaderURL.toString().indexOf("!") + 2);
					Logs.log("JEX Jar URL: " + jarURLPath, CrunchFactory.class);
					loader.getInternalPluginNamesFromJar(jarFile);
				}
				catch (MalformedURLException e)
				{
					e.printStackTrace();
				}
			}
		}
		else
		{
			// We are running JEX from Eclipse, in which case the classes are not stored in a jar and so we need to get the function class names using File-based methods...
			File root;
			try
			{
				root = new File(classLoaderURL.toURI().getPath());
				root = new File(root.getParentFile() + File.separator + "plugin" + File.separator + "old");
				File[] l = root.listFiles();
				
				for (int i = 0; i < l.length; i++)
				{
					String name = l[i].getName();
					if(name.length() > 4 && name.startsWith("JEX_") && name.endsWith(".class") && !name.contains("$")) // Need to check for $ because those represent class files that are extra created by Eclipse that won't work
					{
						Logs.log("Found function definition: " + name, CrunchFactory.class);
						loader.internalPluginNames.add(name.substring(0, name.length() - 6));
					}
				}
			}
			catch (URISyntaxException e)
			{
				e.printStackTrace();
			}
		}
		
		// Create and store internally defined ExperimentalDataCrunch Objects
		for (String pluginName : loader.internalPluginNames)
		{
			JEXCrunchable c = getInstanceOfExperimentalDataCrunch(pluginName);
			if(c == null)
			{
				continue;
			}
			if(c.showInList())
			{
				result.put(c.getName(), c);
			}
		}
		
		// Create and store externally defined ExperimentalDataCruch Objects
		// Do external ones second so that external functions with the same name as an internal function will be kept only (i.e. external overrides internal)
		String pathToLoad = null;
		if(jarPath != null)
		{
			pathToLoad = jarPath + File.separator + "plugins";
		}
		else
		{
			pathToLoad = prefsPluginsPath;
		}
		File folderToLoad = new File(pathToLoad);
		if(folderToLoad.exists())
		{
			/* This the load.externalPluginNames list of the CrunchFactory and also sets the internal URLs inside which to search for the class objects and now we can ask loader to load a class for us */
			loader.loadPluginsFromFolder(pathToLoad);
			
			// For each function class name we found, load it
			for (String pluginName : loader.externalPluginNames)
			{
				String[] names = pluginName.split("\\.");
				String name = names[names.length - 1];
				if(name.startsWith("JEX_"))
				{
					try
					{
						Class<?> functionClass = loader.loadClass(pluginName);
						Object function = functionClass.newInstance();
						if(function instanceof JEXCrunchable)
						{
							JEXCrunchable temp = (JEXCrunchable) function;
							if(temp.showInList())
							{
								result.put(temp.getName(), temp);
							}
						}
					}
					catch (Exception e)
					{
						Logs.log("Couldn't load plugin: " + pluginName, CrunchFactory.class);
						e.printStackTrace();
					}
				}
			}
		}
		
		try
		{
			loader.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		// Load these last in case there is a name conflict, we take the newer version instead as JEXPlugin style is the newer style
		Logs.log("Getting new JEXPlugins.", CrunchFactory.class);
		//		loadMissing();
		TreeMap<String,JEXCrunchable> jexPlugins = loadJEXCrunchablePlugins();
		result.putAll(jexPlugins);
		
		return result;
	}
	
	public void getInternalPluginNamesFromJar(URL jarFile)
	{
		if(jarFile == null)
		{
			return;
		}
		try
		{
			JarFile jar = ((JarURLConnection) jarFile.openConnection()).getJarFile();
			Enumeration<JarEntry> e = jar.entries();
			while (e.hasMoreElements())
			{
				JarEntry file = e.nextElement();
				// Logs.log(file.getName(), CrunchFactory.class);
				String[] names = file.getName().split("/"); // Always a forward slash because it is URL style.
				if(names.length > 1)
				{
					String packageName = names[names.length - 2];
					String name = names[names.length - 1];
					if(packageName.equals("old") && name.length() >= 4 && name.startsWith("JEX_") && name.endsWith(".class") && !name.contains("$"))
					{
						// Need to check for $ because those represent class files that are extra created by Eclipse that won't work
						Logs.log("Found function definition: " + name, CrunchFactory.class);
						this.internalPluginNames.add(name.substring(0, name.length() - 6));
					}
				}
			}
		}
		catch (IOException e1)
		{
			Logs.log("Couldn't retrieve function names from jar file: " + jarFile.toString(), this);
			e1.printStackTrace();
		}
	}
	
	/**
	 * Create a new instance of ExperimentalDataCrunch class
	 * 
	 * @param name
	 * @return instance of ExperimentalDataCrunch of name NAME
	 */
	public static JEXCrunchable getInstanceOfExperimentalDataCrunch(String name)
	{
		// Class toInstantiate;
		try
		{
			@SuppressWarnings("rawtypes")
			Class toInstantiate = Class.forName("function.plugin.old." + name);
			JEXCrunchable ret = (JEXCrunchable) toInstantiate.newInstance();
			return ret;
		}
		catch (ClassNotFoundException e)
		{
			e.printStackTrace();
		}
		catch (InstantiationException e)
		{
			e.printStackTrace();
		}
		catch (IllegalAccessException e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Return an ordered set of all the toolboxes
	 * 
	 * @return set of toolboxes
	 */
	public static TreeSet<String> getToolboxes()
	{
		TreeSet<String> result = new TreeSet<String>(new StringUtility());
		// listOfCrunchers = getExperimentalDataCrunchers();
		
		for (JEXCrunchable c : listOfCrunchers.values())
		{
			String tb = c.getToolbox();
			result.add(tb);
		}
		return result;
	}
	
	/**
	 * Get a subset of the functions in toolbox TOOBOX
	 * 
	 * @param toolbox
	 * @return Sub set of function matching a toolbox name
	 */
	public static TreeMap<String,JEXCrunchable> getFunctionsFromToolbox(String toolbox)
	{
		// HashMap<String,ExperimentalDataCrunch> result = new
		// HashMap<String,ExperimentalDataCrunch>();
		TreeMap<String,JEXCrunchable> result = new TreeMap<String,JEXCrunchable>();
		
		for (JEXCrunchable c : listOfCrunchers.values())
		{
			if(c.getToolbox().equals(toolbox))
			{
				result.put(c.getName(), c);
			}
		}
		
		return result;
	}
	
	public static void loadMissing()
	{
		Object o = new AdjustImage();
		Logs.log(o.toString(), CrunchFactory.class);
		o = new ImageStitcher();
		Logs.log(o.toString(), CrunchFactory.class);
		return;
		
	}
	
}
