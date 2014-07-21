package jex;

import net.imagej.patcher.LegacyInjector;

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
