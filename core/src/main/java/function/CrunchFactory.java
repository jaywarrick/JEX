package function;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
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
import miscellaneous.SSVList;
import miscellaneous.StringUtility;

import org.scijava.command.Command;
import org.scijava.command.CommandInfo;
import org.scijava.plugin.DefaultPluginFinder;
import org.scijava.plugin.PluginInfo;

import updates.Updater;
import function.plugin.IJ2.IJ2CrunchablePlugin;
import function.plugin.IJ2.IJ2PluginUtility;
import function.plugin.mechanism.JEXCrunchablePlugin;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.JEXPluginInfo;

public class CrunchFactory extends URLClassLoader {
	
	static TreeMap<String,JEXCrunchable> jexCrunchables = new TreeMap<String,JEXCrunchable>();
	
	private Vector<String> internalOldJEXCrunchableNames = new Vector<String>();
	private String jarPath = null;
	private List<PluginInfo<?>> allSciJavaPlugins = new Vector<PluginInfo<?>>();
	
	static
	{
		loadJEXCrunchables();
	}
	
	public CrunchFactory()
	{
		super(new URL[0], JEXperiment.class.getClassLoader());
	}
	
	/**
	 * Return a map of all loadable functions
	 * 
	 * @return
	 */
	public static void loadJEXCrunchables()
	{
		// Create and instance of CrunchFactory to keep track of things and for loading classes.
		CrunchFactory loader = new CrunchFactory();
		
		loader.findOldJEXCrunchables();
		loader.loadOldJEXCrunchables();
		loader.findSciJavaPlugins();
		loader.loadSciJavaPlugins();
		
		try
		{
			loader.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Find internally defined JEXCrunchables (old-style) determining whether or not we are running from
	 * a jar file along the way. Return the jar file path if found. Else null.
	 */
	private void findOldJEXCrunchables()
	{
		Logs.log("Finding old internally defined JEXCrunchables.", CrunchFactory.class);
		URL classLoaderURL = JEXCrunchable.class.getResource("JEXCrunchable.class");
		if(classLoaderURL == null)
		{
			Logs.log("ClassLoaderURL was null", CrunchFactory.class);
		}
		else
		{
			Logs.log("Source code URL for finding internally defined JEX functions: " + classLoaderURL.toString(), CrunchFactory.class);
		}
		
		if(Updater.runningFromJar())
		{
			// We are running from a jar
			try
			{
				URL jarFile = new URL(classLoaderURL.toString().substring(0, classLoaderURL.toString().indexOf("!") + 2)); // jar file that JEX is packaged in
				jarPath = classLoaderURL.toString().substring(0, classLoaderURL.toString().indexOf("!") + 2);
				Logs.log("JEX Jar URL: " + jarPath, CrunchFactory.class);
				this.findOldJEXCrunchablePluginNamesInJar(jarFile);
			}
			catch (MalformedURLException e)
			{
				e.printStackTrace();
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
						Logs.log("Found JEXCrunchable: " + name, CrunchFactory.class);
						this.internalOldJEXCrunchableNames.add(name.substring(0, name.length() - 6));
					}
				}
			}
			catch (URISyntaxException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	private void loadOldJEXCrunchables()
	{
		// Create and store internally defined ExperimentalDataCrunch Objects
		for (String pluginName : this.internalOldJEXCrunchableNames)
		{
			JEXCrunchable c = getInstanceOfJEXCrunchable(pluginName);
			if(c == null)
			{
				continue;
			}
			if(c.showInList())
			{
				jexCrunchables.put(c.getName(), c);
			}
		}
	}
	
	/**
	 * Plugins are required to be in jar form in the folder or subfolders described by path.
	 * This puts all jars in the folder (and subfolder) on the path of the classloader (i.e., the CrunchFactory instance).
	 * @param path
	 */
	private void findJarsInFolder(String path)
	{
		Logs.log("Looking for jar files in: " + path, this);
		if(path != null && !path.equals(""))
		{
			File f = new File(path);
			if(f.exists() && f.isDirectory())
			{
				File[] list = f.listFiles();
				if(list == null)
				{
					return;
				}
				else
				{
					for(File f2 : list)
					{
						if(f2.getName().endsWith(".jar"))
						{
							try
							{	
								Logs.log("Found jar file that potentially contains JEXPlugins: " + f2, this);
								this.addURL(f2.toURI().toURL());
								ClassPathHack.addURL(f2.toURI().toURL()); // Add the jar to the system class path so we can find any other classes this jar requires.
							}
							catch (MalformedURLException e1)
							{
								Logs.log("Couldn't convert file path to URI to URL: " + f2, this);
								e1.printStackTrace();
							}
							catch (IOException e)
							{
								Logs.log("Couldn't add the jar to the general class path of the application: " + f2, this);
								e.printStackTrace();
							}
						}
					}
				}
			}
			else
			{
				Logs.log("Couldn't find external plugins folder at " + path + ". Check location and/or fix JEX preferences to point to the external plugins folder (button in top right of window).", CrunchFactory.class);
			}
		}
		else
		{
			Logs.log("Couldn't find the external plugins folder using the null or empty path given. Set the external plugins folder path in the JEX preferences (button in top right of window).", CrunchFactory.class);
		}
	}
	
	/**
	 * Use the DefaultPluginFinder from the scijava framework to find all scijava plugins.
	 * Thus, this includes new JEX plugins and ImageJ Plugins
	 */
	private void findSciJavaPlugins()
	{		
		String pathsToLoad = PrefsUtility.getExternalPluginsFolder();
		
		// Add paths from the preferences for finding JEXPlugins in externally defined jar files
		SSVList pathSVList = new SSVList(pathsToLoad);
		// Add the default plugins folder that is distributed with the jar'd JEX software
		if(this.jarPath != null)
		{
			pathSVList.insertElementAt(this.jarPath + File.separator + "plugins", 0);
		}
		for(String pathToLoad : pathSVList)
		{
			File folderToLoad = new File(pathToLoad);
			if(folderToLoad.exists())
			{
				// Add any jars we find on these paths to the class path of loader
				this.findJarsInFolder(pathToLoad);
			}
			else
			{
				Logs.log("Couldn't locate folder '" + pathToLoad + "' specified as an 'External Plugins Folder' in the JEX preferences.", this);
			}
		}
		
		DefaultPluginFinder pf = new DefaultPluginFinder(this);
		allSciJavaPlugins.clear();
		pf.findPlugins(allSciJavaPlugins);
	}
	
	private void loadSciJavaPlugins()
	{
		// First load the ImageJ plugins (just commands)
		for(PluginInfo<?> pi : allSciJavaPlugins)
		{
			if(pi.getPluginType() == Command.class)
			{
				@SuppressWarnings("unchecked")
				CommandInfo command = new CommandInfo((PluginInfo<Command>) pi);
				if(IJ2PluginUtility.isValidForJEX(command))
				{
					IJ2CrunchablePlugin p = new IJ2CrunchablePlugin(command);
					if(p != null)
					{
						Logs.log("Loaded ImageJ Plugin: " + command.getTitle(), CrunchFactory.class);
						jexCrunchables.put(command.getTitle(), p);
					}
				}
			}
			
		}
		
		// Then try to set all the JEXPlugins that are internal (best guess is that their package is functions.plugin.plugins)
		for(PluginInfo<?> pi : allSciJavaPlugins)
		{
			if(pi.getPluginType() == JEXPlugin.class && pi.getClassName().startsWith("function.plugin.plugins"))
			{
				@SuppressWarnings("unchecked")
				JEXPluginInfo fullInfo = new JEXPluginInfo((PluginInfo<JEXPlugin>) pi);
				JEXCrunchablePlugin crunchable = new JEXCrunchablePlugin(fullInfo);
				jexCrunchables.put(crunchable.getName(), crunchable);
				Logs.log("Loaded internal JEXPlugin: " + pi.getName() + " - "+ pi.getClassName(), CrunchFactory.class);
			}
		}
		
		// Then finally load the externally defined plugins so they "overwrite" the ones previously defined of the same name
		for(PluginInfo<?> pi : allSciJavaPlugins)
		{
			if(pi.getPluginType() == JEXPlugin.class && !pi.getClassName().startsWith("function.plugin.plugins"))
			{
				@SuppressWarnings("unchecked")
				JEXPluginInfo fullInfo = new JEXPluginInfo((PluginInfo<JEXPlugin>) pi);
				try
				{
					JEXCrunchablePlugin crunchable = new JEXCrunchablePlugin(fullInfo);
					jexCrunchables.put(crunchable.getName(), crunchable);
					Logs.log("Loaded external JEXPlugin: " + pi.getName() + " - "+ pi.getClassName(), CrunchFactory.class);
				}
				catch (Error e2)
				{
					// If the plugin is malformed, don't hinder the rest of the plugins
					Logs.log("(Error not Exception!) Couldn't instantiate plugin: " + pi.getName() + " - " + pi.getClassName(), Logs.ERROR, this);
					e2.printStackTrace();
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
	public static JEXCrunchable getJEXCrunchable(String functionName)
	{
		try
		{
			// Get the native ExperimentalDataCrunch
			JEXCrunchable result = jexCrunchables.get(functionName);
			
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
	
	private void findOldJEXCrunchablePluginNamesInJar(URL jarFile)
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
						Logs.log("Found JEXCrunchable: " + name, CrunchFactory.class);
						this.internalOldJEXCrunchableNames.add(name.substring(0, name.length() - 6));
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
	public static JEXCrunchable getInstanceOfJEXCrunchable(String name)
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
		
		for (JEXCrunchable c : jexCrunchables.values())
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
	public static TreeMap<String,JEXCrunchable> getJEXCrunchablesInToolbox(String toolbox)
	{
		// HashMap<String,ExperimentalDataCrunch> result = new
		// HashMap<String,ExperimentalDataCrunch>();
		TreeMap<String,JEXCrunchable> result = new TreeMap<String,JEXCrunchable>();
		
		for (JEXCrunchable c : jexCrunchables.values())
		{
			if(c.getToolbox().equals(toolbox))
			{
				result.put(c.getName(), c);
			}
		}
		
		return result;
	}	
}

class ClassPathHack {
    private static final Class<?>[] parameters = new Class[] {URL.class};

    public static void addFile(String s) throws IOException
    {
        File f = new File(s);
        addFile(f);
    }

    public static void addFile(File f) throws IOException
    {
        addURL(f.toURI().toURL());
    }

    public static void addURL(URL u) throws IOException
    {
        URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        Class<?> sysclass = URLClassLoader.class;

        try {
            Method method = sysclass.getDeclaredMethod("addURL", parameters);
            method.setAccessible(true);
            method.invoke(sysloader, new Object[] {u});
        } catch (Throwable t) {
            t.printStackTrace();
            throw new IOException("Error, could not add URL to system classloader");
        }

    }
}
