package logs;

public class Logs {
	
	// Statics
	public static int STANDARD = 0;
	public static int ERROR = -1;
	
	public static String status = "";
	public static double progress = 0.0; // number between 0 and
	// 1;
	public static StatusListener listener = null;
	
	public static void log(String str, Object source)
	{
		log(str, 0, source);
	}
	
	@SuppressWarnings("rawtypes")
	public static void log(String str, Class source)
	{
		log(str, 0, source);
	}
	
	public static void log(String message, int logType, Object source)
	{
		if(message == null)
		{
			message = "null";
		}
		if(source == null)
		{
			source = "Unknown";
		}
		if(logType >= STANDARD)
		{
			if(source instanceof String)
			{
				System.out.println(source + "   ------>   " + message);
			}
			else
			{
				System.out.println(source.getClass().getSimpleName() + "   ------>   " + message);
			}
		}
		else
		// ERROR
		{
			if(source instanceof String)
			{
				System.out.println("!!!!! ERROR !!!!!  " + source + "   ------>   " + message);
			}
			else
			{
				System.out.println("!!!!! ERROR !!!!!  " + source.getClass().getSimpleName() + "   ------>   " + message);
			}
		}
	}
	
	@SuppressWarnings("rawtypes")
	public static void log(String message, int logType, Class source)
	{
		if(source == null)
		{
			log(message, logType, "Unknown");
			return;
		}
		if(logType >= STANDARD)
		{
			System.out.println(source.getSimpleName() + "   ------>   " + message);
		}
		else
		// ERROR
		{
			System.out.println("!!!!! ERROR !!!!!  " + source.getSimpleName() + "   ------>   " + message);
		}
	}
	
	public static void setStatusText(String statusMessage)
	{
		status = statusMessage;
		if(listener != null)
		{
			listener.setStatus(statusMessage);
		}
	}
	
	public static void setProgressPercentage(double progressPercentage)
	{
		progress = progressPercentage;
		if(listener != null)
		{
			listener.setProgress(progressPercentage);
		}
	}
	
	public void setStatusListener(StatusListener statusListener)
	{
		listener = statusListener;
		listener.setStatus(status);
		listener.setProgress(progress);
	}
}
