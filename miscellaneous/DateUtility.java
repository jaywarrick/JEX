package miscellaneous;

//
//  Utility.java
//  MicroFluidicHT_Tools
//
//  Created by erwin berthier on 7/6/08.
//  Copyright 2008 __MyCompanyName__. All rights reserved.
//

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public final class DateUtility {
	
	public static String getDate()
	{
		Calendar cal = Calendar.getInstance(TimeZone.getDefault());
		String DATE_FORMAT = "yyyy-MM-dd";
		java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(DATE_FORMAT);
		sdf.setTimeZone(TimeZone.getDefault());
		String date = sdf.format(cal.getTime());
		return date;
	}
	
	public static Date makeDate(String date)
	{
		// if (date == null) return null;
		String DATE_FORMAT = "yyyy-MM-dd";
		java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(DATE_FORMAT);
		sdf.setTimeZone(TimeZone.getDefault());
		Date result = null;
		try
		{
			result = sdf.parse(date);
		}
		catch (ParseException e)
		{
			// e.printStackTrace();
		}
		return result;
	}
	
	public static int compareDates(Date date1, Date date2)
	{
		if(date1 == null && date2 == null)
			return 0;
		if(date2 == null)
			return 1;
		if(date1 == null)
			return -1;
		return date1.compareTo(date2);
	}
	
	public static int compareDates(String dateStr1, String dateStr2)
	{
		Date date1 = makeDate(dateStr1);
		Date date2 = makeDate(dateStr2);
		if(date1 == null && date2 == null)
			return 0;
		if(date2 == null)
			return 1;
		if(date1 == null)
			return -1;
		return date1.compareTo(date2);
	}
}
