package jex;

import net.imagej.patcher.LegacyInjector;
import jex.JEXperiment;

/** Main entry point into JEX. */
public class Main {
	
	static
	{
		LegacyInjector.preinit();
	}
	
	public static void main(final String[] args) {
		JEXperiment.main(args);
	}
}
