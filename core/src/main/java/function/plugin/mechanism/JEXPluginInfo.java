package function.plugin.mechanism;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import logs.Logs;

import org.scijava.Context;
import org.scijava.InstantiableException;
import org.scijava.plugin.PluginInfo;
import org.scijava.service.Service;
import org.scijava.util.ClassUtils;

import miscellaneous.StringUtility;
import Database.Definition.Parameter;
import Database.Definition.Type;
import Database.Definition.TypeName;


public class JEXPluginInfo {
	
	private Class<? extends JEXPlugin> theClass;
	private PluginInfo<JEXPlugin> info;
	
	public SortedMap<String,TypeName> inputs;
	public SortedMap<String,Field> iFields;
	public SortedMap<String,String> iDescriptions;
	public SortedMap<String,Boolean> iOptionals;
	
	public SortedMap<String,Parameter> parameters;
	public SortedMap<String,Field> pField;
	public SortedMap<String,String> pOrder;
	public SortedMap<String,Boolean> pOptional;
	
	public SortedMap<String,TypeName> outputs;
	public SortedMap<String,Field> oField;
	public SortedMap<String,String> oDescription;
	public SortedMap<String,Boolean> oEnabled;
	
	
	/** Processes the given class's @{@link InputMarker}, @{@link ParameterMarker}, and @{@link OutputMarker}-annotated fields to make a JEXPluginInfo Object. */
	public JEXPluginInfo(PluginInfo<JEXPlugin> info)
	{
		this.info = info;
		try
		{
			this.theClass = info.loadClass();
		}
		catch (InstantiableException e)
		{
			Logs.log("Couldn't instantiate the scijava-based JEXPlugin.", Logs.ERROR, this);
			e.printStackTrace();
			return;
		}
		
		if (this.theClass == null) return;
		final List<Field> inputFields =
				ClassUtils.getAnnotatedFields(this.theClass, InputMarker.class);
		final List<Field> parameterFields =
				ClassUtils.getAnnotatedFields(this.theClass, ParameterMarker.class);
		final List<Field> outputFields =
				ClassUtils.getAnnotatedFields(this.theClass, OutputMarker.class);
		
		this.setInputs(inputFields);
		this.setParameters(parameterFields);
		this.setOutputs(outputFields);
	}
	
	public PluginInfo<JEXPlugin> getInfo()
	{
		return this.info;
	}
	
	public JEXPlugin createJEXPlugin() throws InstantiableException
	{
		final Class<? extends JEXPlugin> c = this.theClass;

		// instantiate plugin
		final JEXPlugin instance;
		try
		{
			instance = c.newInstance();
		}
		catch (final InstantiationException e)
		{
			throw new InstantiableException(e);
		}
		catch (final IllegalAccessException e)
		{
			throw new InstantiableException(e);
		}
		return instance;
	}
	
	private void setInputs(List<Field> inputFields)
	{
		this.inputs = new TreeMap<String,TypeName>(new StringUtility());
		this.iFields = new TreeMap<String,Field>(new StringUtility());
		this.iDescriptions = new TreeMap<String,String>(new StringUtility());
		this.iOptionals = new TreeMap<String,Boolean>(new StringUtility());
		for (final Field f : inputFields)
		{
			f.setAccessible(true); // expose private fields
			
			Boolean valid = this.isValid(f);
			if(!valid) continue; // skip invalid fields
			
			// Get the marker
			final InputMarker input = f.getAnnotation(InputMarker.class);
			
			// add items to the relevant lists
			String type = input.type();
			String flavor = input.flavor();
			if(flavor.equals(""))
			{
				flavor = null;
			}
			String name = input.name();
			boolean optional = input.optional();
			String description = input.description();
			
			this.inputs.put(name, new TypeName(new Type(type, flavor), name));
			this.iFields.put(name, f);
			this.iDescriptions.put(name, description);
			this.iOptionals.put(name, optional);
		}
		this.inputs = Collections.unmodifiableSortedMap(this.inputs);
		this.iFields = Collections.unmodifiableSortedMap(this.iFields);
		this.iDescriptions = Collections.unmodifiableSortedMap(this.iDescriptions);
		this.iOptionals = Collections.unmodifiableSortedMap(this.iOptionals);
	}
	
	private void setParameters(List<Field> parameterFields)
	{
		this.parameters = new TreeMap<String,Parameter>(new StringUtility());
		this.pField = new TreeMap<String,Field>(new StringUtility());
		this.pOrder = new TreeMap<String,String>(new StringUtility());
		this.pOptional = new TreeMap<String,Boolean>(new StringUtility());
		for (final Field f : parameterFields)
		{
			f.setAccessible(true); // expose private fields
			
			Boolean valid = this.isValid(f);
			if(!valid) continue; // skip invalid fields
			
			// Get the marker
			final ParameterMarker parameter = f.getAnnotation(ParameterMarker.class);
			
			// add items to the relevant lists
			String name = parameter.name();
			int order = parameter.uiOrder();
			String description = parameter.description();
			int ui = parameter.ui();
			String defaultText = parameter.defaultText();
			Boolean defaultBoolean = parameter.defaultBoolean();
			int defaultChoice = parameter.defaultChoice();
			String[] choices = parameter.choices();
			Boolean optional = parameter.optional();
			
			Parameter p = null;
			if(ui == Parameter.CHECKBOX)
			{
				p = new Parameter(name, description, ui, defaultBoolean);
			}
			else if(ui == Parameter.DROPDOWN)
			{
				p = new Parameter(name, description, ui, choices, defaultChoice);
			}
			else
			{ // Filechooser, Password, or Textfield
				p = new Parameter(name, description, ui, defaultText);
			}
			
			this.parameters.put(name,p);
			this.pField.put(name, f);
			this.pOrder.put("" + order + "_" + name, name);
			this.pOptional.put(name, optional);
		}
		this.parameters = Collections.unmodifiableSortedMap(this.parameters);
		this.pField = Collections.unmodifiableSortedMap(this.pField);
		this.pOrder = Collections.unmodifiableSortedMap(this.pOrder);
		this.pOptional = Collections.unmodifiableSortedMap(this.pOptional);
	}
	
	private void setOutputs(List<Field> outputFields)
	{
		this.outputs = new TreeMap<String,TypeName>(new StringUtility());
		this.oField = new TreeMap<String,Field>(new StringUtility());
		this.oDescription = new TreeMap<String,String>(new StringUtility());
		this.oEnabled = new TreeMap<String,Boolean>(new StringUtility());
		for (final Field f : outputFields)
		{
			f.setAccessible(true); // expose private fields
			
			Boolean valid = this.isValid(f);
			if(!valid) continue; // skip invalid fields
			
			// Get the marker
			final OutputMarker output = f.getAnnotation(OutputMarker.class);
			
			// add items to the relevant lists
			String type = output.type();
			String flavor = output.flavor();
			if(flavor.equals(""))
			{
				flavor = null;
			}
			String name = output.name();
			boolean enabled = output.enabled();
			String description = output.description();
			
			this.outputs.put(name, new TypeName(new Type(type, flavor), name));
			this.oField.put(name, f);
			this.oDescription.put(name, description);
			this.oEnabled.put(name, enabled);
		}
		this.inputs = Collections.unmodifiableSortedMap(this.inputs);
		this.iFields = Collections.unmodifiableSortedMap(this.iFields);
		this.iDescriptions = Collections.unmodifiableSortedMap(this.iDescriptions);
		this.iOptionals = Collections.unmodifiableSortedMap(this.iOptionals);
	}
	
	private Boolean isValid(Field f)
	{
		// NB: Skip types handled by the application framework itself.
		// I.e., these parameters get injected by Context#inject(Object).
		if (Service.class.isAssignableFrom(f.getType())) return false;
		if (Context.class.isAssignableFrom(f.getType())) return false;
		
		boolean valid = true;
		
		final boolean isFinal = Modifier.isFinal(f.getModifiers());
		if (isFinal) {
			// NB: Final parameters are bad because they cannot be modified.
			final String error = "Invalid final parameter: " + f;
			Logs.log(error, Logs.ERROR, this);
			//problems.add(new ValidityProblem(error));
			valid = false;
		}
		
		final String field = f.getName();
		if (iFields.containsKey(field)) {
			// NB: Shadowed parameters are bad because they are ambiguous.
			final String error = "Invalid duplicate parameter: " + f;
			Logs.log(error, Logs.ERROR, this);
			//				problems.add(new ValidityProblem(error));
			valid = false;
		}
		
		return valid;
	}
	
}
