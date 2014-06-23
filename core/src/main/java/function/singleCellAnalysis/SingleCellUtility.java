package function.singleCellAnalysis;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.TreeMap;

import logs.Logs;
import miscellaneous.CSVList;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;

import rtools.R;
import tables.DimensionMap;

public class SingleCellUtility {

	public static String r = "R", g = "G", b = "B", fr = "FR", rN = "RN", gN = "GN", bN = "BN", frN = "FRN", x = "X", y = "Y";

	public static double pointsize = 0.6; // #
	// cex
	public static double meanindicatorsize = 2; // #
	// cex
	public static String meanindicatorcolor = R.sQuote("dodgerblue");
	public static String threshlinecolor = R.sQuote("blue");
	public static double axislabelsize = 1; // #
	// cex
	public static double ticklabelsize = 0.8; // #
	// cex
	public static double annotationsize = 0.6; // #

	// cex

	public static TreeMap<DimensionMap,Double> applyLogicleScale(TreeMap<DimensionMap,Double> data, double transitionPoint, double linearUnitsPerOrder)
	{
		TreeMap<DimensionMap,Double> ret = new TreeMap<DimensionMap,Double>();
		for (Entry<DimensionMap,Double> e : data.entrySet())
		{
			ret.put(e.getKey().copy(), calculateLogicleScaleValue(e.getValue(), transitionPoint, linearUnitsPerOrder));
		}
		return ret;
	}

	public static void drawLogicleAxis(boolean isXAxis, double transitionPoint, double linearUnitsPerOrder, String desiredTicks)
	{
		R.eval("par(cex = " + ticklabelsize + "); #ticklabelsize");

		// Set the axis value
		String axis = "1";
		if(!isXAxis)
		{
			axis = "2";
		}

		// Create the tick labels and tick locations
		CSVList tickLabels = new CSVList(desiredTicks);
		CSVList newTickLabels = new CSVList();
		CSVList tickLocations = new CSVList();
		int i = 0;
		int indexOfTransitionTick = -1;
		for (String tick : tickLabels)
		{
			if(Double.parseDouble(tick) == transitionPoint)
			{
				newTickLabels.add(R.sQuote(tick + "*"));
			}
			else
			{
				newTickLabels.add(R.sQuote(tick));
			}
			double tickLocation = Double.parseDouble(tick);
			tickLocations.add("" + SingleCellUtility.calculateLogicleScaleValue(tickLocation, transitionPoint, linearUnitsPerOrder));
			i = i + 1;
		}

		if(indexOfTransitionTick >= 0)
		{
			tickLabels.set(indexOfTransitionTick, tickLabels.get(indexOfTransitionTick) + "*");
		}

		// Create the final axis command
		String command = "axis(" + axis + ", at=c(" + tickLocations.toString() + "), labels=c(" + newTickLabels.toString() + "));";
		R.eval(command);
	}

	/**
	 * transitionPoint should be above 0.
	 * 
	 * @param val
	 * @param transitionPoint
	 * @param linearUnitsPerOrder
	 * @return
	 */
	public static double calculateLogicleScaleValue(double val, double transitionPoint, double linearUnitsPerOrder)
	{
		double linearDifference = transitionPoint - val;
		if(linearDifference >= 0)
		{
			double ordersDifferenceOnDisplay = linearDifference / linearUnitsPerOrder;
			return transitionPoint * Math.pow(10, -1 * ordersDifferenceOnDisplay);
		}
		else
		{
			return val;
		}
	}

	/**
	 * transitionPoint should be above 0.
	 * 
	 * @param val
	 * @param transitionPoint
	 * @param linearUnitsPerOrder
	 * @return
	 */
	public static void calculateLogicleScaleValues(String srcVariable, String optionalSrcColumnName, String dstVariable, double transition, double linLogRatio)
	{
		if(optionalSrcColumnName == null)
		{
			R.eval(dstVariable + " <- " + srcVariable + ";");
			R.eval(dstVariable + "[" + dstVariable + "<" + transition + "] <- " + transition + "*10^(-1*((" + transition + "-" + dstVariable + "[" + dstVariable + "<" + transition + "])/" + linLogRatio + "));");
		}
		else
		{
			R.eval(dstVariable + " <- " + srcVariable + "$" + optionalSrcColumnName + ";");
			R.eval(dstVariable + "[" + dstVariable + "<" + transition + "] <- " + transition + "*10^(-1*((" + transition + "-" + dstVariable + "[" + dstVariable + "<" + transition + "])/" + linLogRatio + "));");
		}

	}

	public static void initializeFACSPlot(String xLabel, String yLabel, double xmin, double xmax, double ymin, double ymax)
	{
		R.eval("par(lwd = 1)");
		R.eval("par(mar = c(3,3,0,0)); # beyond plot frame [b,l,t,r]");
		R.eval("par(mgp = c(2,1,0)); # placement of axis labels [1], numbers [2] and symbols[3]");
		R.eval("par(oma = c(1,0,1.5,1)); # cutoff beyond other measures");
		R.eval("par(cex = " + axislabelsize + "); # axislabelsize");
		CSVList args = new CSVList();
		args.add("c(), c(), axes='F', frame='F', type='n', log='xy'");
		args.add("xlab=" + R.sQuote(xLabel));
		args.add("xlim=c(" + xmin + "," + xmax + ")");
		args.add("ylim=c(" + ymin + "," + ymax + ")");
		args.add("ylab=" + R.sQuote(yLabel));
		R.eval("plot(" + args.toString() + ");");
	}

	public static void readNumericData(String variableName, String dataFilePath)
	{
		R.load("RWeka");
		R.eval(variableName + " <- read.arff(" + R.quotedPath(dataFilePath) + ");");
		R.eval("isNumeric <- !(typeof(" + variableName + "$Value)=='character');");
		R.eval("if(!isNumeric)\n{values <- as.numeric(unlist(" + variableName + "$Value));\n" + variableName + "$Value <- values;}");
		R.eval(variableName + " <- " + variableName + "[(!is.na(" + variableName + "$Value)),];");
	}

	public static void plotXYData(boolean plotAsPoints, String dataFilePath, String xMeasurement, String yMeasurement, double cexPointSize, double xTransition, double xLinLogRatio, double yTransition, double yLinLogRatio, String bgColorMeasurementName, double bgMin, double bgMax, String backgroundColor, Double bgColor_0to1)
	{
		readNumericData("data", dataFilePath);
		filterData("x", "data", "Measurement", "==", xMeasurement, true);
		filterData("y", "data", "Measurement", "==", yMeasurement, true);
		calculateLogicleScaleValues("x", "Value", "xLogicle", xTransition, xLinLogRatio);
		calculateLogicleScaleValues("y", "Value", "yLogicle", yTransition, yLinLogRatio);

		// Never draw the circle around points
		R.eval("par(col = rgb(0,0,0,0))");

		String bg = "";
		// Set the color of points
		if(bgColor_0to1 != null)
		{
			bg = ("bg = gray(" + bgColor_0to1 + ")");
		}
		else if(backgroundColor == null || backgroundColor.equals(""))
		{
			bg = ("bg = rgb(0,0,0,0.2)");
		}
		else
		{
			bg = ("bg = " + R.sQuote(backgroundColor));
		}

		// Draw the points
		if(bgColorMeasurementName == null)
		{
			//			R.evalAsString("xLogicle");
			//			R.evalAsString("yLogicle");
			R.evalTry("points(xLogicle,yLogicle,pch=21,cex=" + cexPointSize + "," + bg + ")");
		}
		else
			// Draw a custom background based on measurement values
		{
			makeBackgroundColorVector("bgColors", bgColorMeasurementName, bgMin, bgMax, true);
			//			R.eval("print(bgValues);");
			R.evalTry("points(xLogicle,yLogicle,pch=21,cex=" + cexPointSize + ", bg=bgColors)");
		}
	}

	public static void makeLogSequence(String newVariableName, double from, double to, int n)
	{
		R.eval(newVariableName + "<- exp(seq(log(" + from + "), log(" + to + "), length.out = " + n + "));");
	}

	public static void makeLinearSequence(String newVariableName, double from, double to, int n)
	{
		R.eval(newVariableName + "<- seq(" + from + ", " + to + ", length.out = " + n + ");");
	}

	public static void initializeLogHistogram(String xLabel, String yLabel, int nBins, double xmin, double xmax, double ymaxNorm)
	{
		R.eval("par(lwd = 1)");
		R.eval("par(mar = c(3,3,0,0)); # beyond plot frame [b,l,t,r]");
		R.eval("par(mgp = c(2,1,0)); # placement of axis labels [1], numbers [2] and symbols[3]");
		R.eval("par(oma = c(1,0,1.5,1)); # cutoff beyond other measures");
		R.eval("par(cex = " + axislabelsize + "); # axislabelsize");
		CSVList args = new CSVList();
		args.add("c(), c(), xaxt='n', frame='F', log='x'");
		args.add("xlab=" + R.sQuote(xLabel));
		args.add("xlim=c(log10(" + xmin + "), log10(" + xmax + "))");
		args.add("ylim=c(0," + ymaxNorm + ")");
		args.add("ylab=" + R.sQuote(yLabel));
		SingleCellUtility.makeLogSequence("bins", xmin, xmax, nBins + 1);
		R.eval("plot(" + args.toString() + ");");
	}

	public static void initializeLinearHistogram(String xLabel, String yLabel, double xmin, double xmax, double ymaxNorm)
	{
		R.eval("par(lwd = 1)");
		R.eval("par(mar = c(3,3,0,0)); # beyond plot frame [b,l,t,r]");
		R.eval("par(mgp = c(2,1,0)); # placement of axis labels [1], numbers [2] and symbols[3]");
		R.eval("par(oma = c(1,0,1.5,1)); # cutoff beyond other measures");
		R.eval("par(cex = " + axislabelsize + "); # axislabelsize");
		CSVList args = new CSVList();
		args.add("c(), c(), frame='F'");
		args.add("xlab=" + R.sQuote(xLabel));
		args.add("xlim=c(" + xmin + ", " + xmax + ")");
		args.add("ylim=c(0," + ymaxNorm + ")");
		args.add("ylab=" + R.sQuote(yLabel));
		R.eval("plot(" + args.toString() + ");");
	}

	public static void initializeTrajectoryPlot(String xLabel, String yLabel, double xmin, double xmax, double ymin, double ymax, boolean logX, boolean logY)
	{
		R.eval("par(lwd = 1)");
		R.eval("par(mar = c(3,3,0,0)); # beyond plot frame [b,l,t,r]");
		R.eval("par(mgp = c(2,1,0)); # placement of axis labels [1], numbers [2] and symbols[3]");
		R.eval("par(oma = c(1,0,1.5,1)); # cutoff beyond other measures");
		R.eval("par(cex = " + axislabelsize + "); # axislabelsize");
		CSVList args = new CSVList();
		args.add("c(), c()");
		args.add("xlab=" + R.sQuote(xLabel));
		args.add("xlim=c(" + xmin + ", " + xmax + ")");
		args.add("ylim=c(" + ymin + "," + ymax + ")");
		args.add("ylab=" + R.sQuote(yLabel));
		if(logX && logY)
		{
			args.add("log='xy'");
		}
		else if(logX)
		{
			args.add("log='x'");
		}
		else if(logY)
		{
			args.add("log='y'");
		}
		R.eval("plot(" + args.toString() + ");");
	}

	public static void calculateHistogram(String dataName, String histCalcsName, Collection<Double> nums, double histMin, double histMax, double ymaxNorm, int nBins)
	{
		R.makeVector(dataName, nums);
		SingleCellUtility.makeLinearSequence("bins", histMin, histMax, nBins + 1);
		R.eval(dataName + "[" + dataName + " > max(bins)] <- max(bins);");
		R.eval(dataName + "[" + dataName + " < min(bins)] <- min(bins);");
		R.eval(histCalcsName + " <- hist(" + dataName + ", breaks=bins, freq=FALSE, plot=FALSE);");
	}

	public static void plotLogHistogram(String dataFilePath, String Measurement, double color_0to1, double alpha_0to1, double offset)
	{
		readNumericData("data", dataFilePath);
		filterData("x", "data", "Measurement", "==", Measurement, true);
		R.eval("x <- x$Value +" + offset);
		//		R.evalAsString("x");
		R.eval("x[x > max(bins)] <- max(bins);");
		R.eval("x[x < min(bins)] <- min(bins);");
		R.eval("temp <- hist(log10(x), breaks=log10(bins), freq=FALSE, plot=FALSE);");
		//		R.evalAsString("temp");
		// R.eval("print(temp$mids);");
		R.eval("par(col = rgb(" + color_0to1 + "," + color_0to1 + "," + color_0to1 + "," + alpha_0to1 + "));");
		R.eval("lines(temp$mids, temp$counts/(sum(temp$counts)), lwd=3);");
	}

	public static void plotLinearHistogram(String histogramCalcsName, String xLabel, String yLabel, double xmin, double xmax, double ymaxNorm, int nBins, Double optionalMedian)
	{
		SingleCellUtility.initializeLinearHistogram(xLabel, yLabel, xmin, xmax, ymaxNorm);
		R.eval("lines(" + histogramCalcsName + "$mids, " + histogramCalcsName + "$counts/(sum(" + histogramCalcsName + "$counts)), lwd=3, col='black');");
		if(optionalMedian != null)
		{
			DecimalFormat formatter = new DecimalFormat("0.00");
			R.eval("mtext(" + R.sQuote("  Median: " + formatter.format(optionalMedian)) + ", side=1, adj=0, cex=" + annotationsize + ", outer=TRUE);");
		}
	}

	public static void drawLogScaleAxis(boolean isXaxis, String ticks)
	{
		R.eval("ticks <- c(" + ticks + ");");
		int axis = 1;
		if(!isXaxis)
		{
			axis = 2;
		}
		R.eval("axis(" + axis + ", at=log10(c(" + ticks + ")), labels=c(" + ticks + "));");
	}

	public static void makeBackgroundColorVector(String dstVariableName, String bgColorMeasurementName, double min, double max, boolean logScale)
	{
		double rmax = 1, gmax = 0.3, bmax = 0.3;
		filterData("bgValues", "data", "Measurement", "==", bgColorMeasurementName, true);
		R.eval("bgValues <- bgValues$Value");
		if(logScale)
		{
			if(min == 0)
			{
				Logs.log("Minimum for point background color range can't be 0 for log scale representation. Setting to 1.", 0, SingleCellUtility.class.getSimpleName());
				min = 1;
			}
			R.eval("bgValues <- log10(bgValues + 100);");
			R.eval("bgValues <- (bgValues-log10(" + min + "))/(log10(" + max + ")-log10(" + min + "));");
		}
		else
		{
			R.eval("bgValues <- (bgValues-" + min + ")/(" + max + "-" + min + ");");
		}

		R.eval("bgValues[bgValues < 0] <- 0;");
		R.eval("bgValues[bgValues > 1] <- 1;");
		R.eval(dstVariableName + " <- bgValues;");
		// R.eval("print(min(bgColors))");
		R.eval("for(i in 1:length(bgValues)){" + dstVariableName + "[i] <- rgb(bgValues[i]*" + rmax + ",bgValues[i]*" + gmax + ",bgValues[i]*" + bmax + ");}");
	}

	/**
	 * "xMean" and "yMean" are returned in the TreeMap
	 * 
	 * @return
	 */
	public static void addMeanInfo(double xMean, double yMean, double xTransition, double xLinLogRatio, double yTransition, double yLinLogRatio)
	{
		DecimalFormat formatter = new DecimalFormat("#0.00");
		double xMeanLogicle = SingleCellUtility.calculateLogicleScaleValue(xMean, xTransition, xLinLogRatio);
		double yMeanLogicle = SingleCellUtility.calculateLogicleScaleValue(yMean, yTransition, yLinLogRatio);
		R.eval("points(" + xMeanLogicle + ", " + yMeanLogicle + ", pch=10, cex=" + meanindicatorsize + ", col=" + meanindicatorcolor + ", lwd=1.5);");
		R.eval("par(col = 'black');");
		R.eval("mtext(" + R.sQuote("  Mean: X(green) " + formatter.format(xMean) + ", Y(red) " + formatter.format(yMean)) + ", side=1, adj=0, cex=" + annotationsize + ", outer=TRUE);");
	}

	public static void plotTimeStamp(String timeStamp)
	{
		R.eval("par(col = 'black');");
		R.eval("mtext(" + R.sQuote(timeStamp) + ", side=1, adj=1, cex=" + annotationsize + ", outer=TRUE);");
	}

	/**
	 * Use the calculateDoubleThresholdPercentages function to get values for the 'percentages' variable
	 * 
	 * @param percentages
	 * @param xmin
	 * @param xmax
	 * @param ymin
	 * @param ymax
	 * @param transitionPoint
	 * @param linearUnitsPerOrder
	 */
	public static void plotDoubleThresholdText(TreeMap<String,Double> percentages, double xmin, double xmax, double ymin, double ymax, double xTransition, double xLinLogRatio, double yTransition, double yLinLogRatio)
	{
		DecimalFormat formatter = new DecimalFormat("#0.00");

		xmin = calculateLogicleScaleValue(xmin, xTransition, xLinLogRatio) * 1.1;
		ymin = calculateLogicleScaleValue(ymin, yTransition, yLinLogRatio) * 1.1;
		xmax = calculateLogicleScaleValue(xmax, xTransition, xLinLogRatio) * 0.9;
		ymax = calculateLogicleScaleValue(ymax, yTransition, yLinLogRatio) * 0.9;

		String plusplus = null, minusminus = null, plusminus = null, minusplus = null;

		Double mean = percentages.get("XY meanX ++");
		if(mean == null || mean.equals(Double.NaN))
		{
			plusplus = "";
		}
		else
		{
			plusplus = "avg=(" + formatter.format(mean) + "," + formatter.format(percentages.get("XY meanY ++")) + "), ";
		}
		mean = percentages.get("XY meanX --");
		if(mean == null || mean.equals(Double.NaN))
		{
			minusminus = "";
		}
		else
		{
			minusminus = ", avg=(" + formatter.format(mean) + "," + formatter.format(percentages.get("XY meanY --")) + ")";
		}
		mean = percentages.get("XY meanX +-");
		if(mean == null || mean.equals(Double.NaN))
		{
			plusminus = "";
		}
		else
		{
			plusminus = "avg=(" + formatter.format(mean) + "," + formatter.format(percentages.get("XY meanY +-")) + "), ";
		}
		mean = percentages.get("XY meanX -+");
		if(mean == null || mean.equals(Double.NaN))
		{
			minusplus = "";
		}
		else
		{
			minusplus = ", avg=(" + formatter.format(mean) + "," + formatter.format(percentages.get("XY meanY -+")) + ")";
		}

		R.eval("text(" + xmax + ", " + ymax + ", " + R.sQuote(plusplus + "[+,+] " + formatter.format(percentages.get("XY % ++")) + "%") + ", col = " + threshlinecolor + ", cex = " + annotationsize + ", adj = c(1));");
		R.eval("text(" + xmin + ", " + ymin + ", " + R.sQuote("[-,-] " + formatter.format(percentages.get("XY % --")) + "%" + minusminus) + ", col = " + threshlinecolor + ", cex = " + annotationsize + ", adj= c(0));");
		R.eval("text(" + xmax + ", " + ymin + ", " + R.sQuote(plusminus + "[+,-] " + formatter.format(percentages.get("XY % +-")) + "%") + ", col = " + threshlinecolor + ", cex = " + annotationsize + ", adj = c(1));");
		R.eval("text(" + xmin + ", " + ymax + ", " + R.sQuote("[-,+] " + formatter.format(percentages.get("XY % -+")) + "%" + minusplus) + ", col = " + threshlinecolor + ", cex = " + annotationsize + ", adj= c(0));");
	}

	public static void plotSingleThresholdText(TreeMap<String,Double> percentages, boolean isVerticalThresh, double xmin, double xmax, double ymin, double ymax, double xTransition, double xLinLogRatio, double yTransition, double yLinLogRatio)
	{
		DecimalFormat formatter = new DecimalFormat("#0.00");
		xmin = calculateLogicleScaleValue(xmin, xTransition, xLinLogRatio) * 1.1;
		ymin = calculateLogicleScaleValue(ymin, yTransition, yLinLogRatio) * 1.1;
		xmax = calculateLogicleScaleValue(xmax, xTransition, xLinLogRatio) * 0.9;
		ymax = calculateLogicleScaleValue(ymax, yTransition, yLinLogRatio) * 0.9;
		String plus = null, minus = null;
		if(isVerticalThresh)
		{
			Double mean = percentages.get("X meanX +");
			if(mean == null || mean.equals(Double.NaN))
			{
				plus = "";
			}
			else
			{
				plus = "avg=(" + formatter.format(mean) + "," + formatter.format(percentages.get("X meanY +")) + "), ";
			}
			mean = percentages.get("X meanX -");
			if(mean == null || mean.equals(Double.NaN))
			{
				minus = "";
			}
			else
			{
				minus = ", avg=(" + formatter.format(mean) + "," + formatter.format(percentages.get("X meanY -")) + ")";
			}
			R.eval("text(" + xmin + ", " + ymax + ", " + R.sQuote("[-] " + formatter.format(percentages.get("X % -")) + "%" + minus) + ", col = " + threshlinecolor + ", cex = " + annotationsize + ", adj= c(0));");
			R.eval("text(" + xmax + ", " + ymax + ", " + R.sQuote(plus + "[+] " + formatter.format(percentages.get("X % +")) + "%") + ", col = " + threshlinecolor + ", cex = " + annotationsize + ", adj = c(1));");
		}
		else
		{
			Double mean = percentages.get("Y meanX +");
			if(mean == null || mean.equals(Double.NaN))
			{
				plus = "";
			}
			else
			{
				plus = ", avg=(" + formatter.format(mean) + "," + formatter.format(percentages.get("Y meanY +")) + ")";
			}
			mean = percentages.get("Y meanX -");
			if(mean == null || mean.equals(Double.NaN))
			{
				minus = "";
			}
			else
			{
				minus = ", avg=(" + formatter.format(mean) + "," + formatter.format(percentages.get("Y meanY -")) + ")";
			}
			R.eval("text(" + xmin + ", " + ymin + ", " + R.sQuote("[-] " + formatter.format(percentages.get("Y % -")) + "%" + minus) + ", col = " + threshlinecolor + ", cex = " + annotationsize + ", adj = c(0));");
			R.eval("text(" + xmin + ", " + ymax + ", " + R.sQuote("[+] " + formatter.format(percentages.get("Y % +")) + "%" + plus) + ", col = " + threshlinecolor + ", cex = " + annotationsize + ", adj = c(0));");
		}
	}

	// /**
	// * "Total", "x+", "y+", "++", "+-", "-+", and "--" are the keys to the
	// data.
	// * @param xThresh
	// * @param xCross
	// * @param yThresh
	// * @param yCross
	// * @return
	// */
	// public static TreeMap<String,Double>
	// calculateDoubleThresholdPercentages(TreeMap<DimensionMap,Double>
	// linearData, double xThresh, double xCross, double yThresh, double yCross)
	// {
	// try
	// {
	// REXP result;
	// TreeMap<String,Double> ret = new TreeMap<String,Double>();
	// result = R.eval("total <- nrow(x);");
	// ret.put("Total", result.asDouble());
	//
	// result = R.eval("xPlus <- 100*total / nrow(x[x['Value'] > y['Value']*" +
	// xCross + " + " + xThresh + ",]);");
	// ret.put("x+", result.asDouble());
	//
	// result = R.eval("yPlus <- 100*total / nrow(y[y['Value'] > x['Value']*" +
	// yCross + " + " + yThresh + ",]);");
	// ret.put("y+", result.asDouble());
	//
	// result =
	// R.eval("xPlusyPlus <- 100*total / nrow(y[y['Value'] > x['Value']*" +
	// yCross + " + " + yThresh + " & x['Value'] > y2['Value']*" + xCross +
	// " + " + xThresh + ",]);");
	// ret.put("++", result.asDouble());
	//
	// result =
	// R.eval("xMinusyMinus <- 100-((yPlus-xPlusyPlus) + xPlusyPlus + (xPlus-xPlusyPlus));");
	// ret.put("--", result.asDouble());
	//
	// result = R.eval("xMinusyPlus <- yPlus - xMinusyMinus");
	// ret.put("-+", result.asDouble());
	//
	// result = R.eval("xPlusyMinus <- xPlus - xMinusyMinus");
	// ret.put("+-", result.asDouble());
	//
	// return ret;
	// }
	// catch (REXPMismatchException e)
	// {
	// e.printStackTrace();
	// return null;
	// }
	// }

	/**
	 * "Total", "x+", "y+", "++", "+-", "-+", and "--" are the keys to the data.
	 * 
	 * @param xThresh
	 * @param xCross
	 * @param yThresh
	 * @param yCross
	 * @return
	 */
	public static TreeMap<String,Double> calculateStats_DoubleThreshold(double xThresh, double xCross, double yThresh, double yCross)
	{
		TreeMap<String,Double> ret = SingleCellUtility.calculateStats_SingleThreshold(true, xThresh, xCross);
		ret.putAll(SingleCellUtility.calculateStats_SingleThreshold(false, yThresh, yCross));
		REXP result;
		try
		{
			// Define subpopulations
			result = R.eval("plusplus <- (y$Value > x$Value*" + yCross + " + " + yThresh + " & x$Value > y$Value*" + xCross + " + " + xThresh + ");");
			result = R.eval("minusminus <- (y$Value <= x$Value*" + yCross + " + " + yThresh + " & x$Value <= y$Value*" + xCross + " + " + xThresh + ");");
			result = R.eval("plusminus <- (y$Value <= x$Value*" + yCross + " + " + yThresh + " & x$Value > y$Value*" + xCross + " + " + xThresh + ");");
			result = R.eval("minusplus <- (y$Value > x$Value*" + yCross + " + " + yThresh + " & x$Value <= y$Value*" + xCross + " + " + xThresh + ");");

			// Calculate stats for each subpopulation
			// ++ //
			result = R.eval("temp <- 100 * (sum(plusplus) / (total));");
			ret.put("XY % ++", result.asDouble());
			result = R.eval("temp <- sum(plusplus)");
			ret.put("XY n ++", result.asDouble());
			result = R.eval("temp <- mean(x[plusplus,]$Value);");
			ret.put("XY meanX ++", result.asDouble());
			result = R.eval("temp <- mean(y[plusplus,]$Value);");
			ret.put("XY meanY ++", result.asDouble());
			result = R.eval("temp <- sd(x[plusplus,]$Value);");
			ret.put("XY sdX ++", result.asDouble());
			result = R.eval("temp <- sd(y[plusplus,]$Value);");
			ret.put("XY sdY ++", result.asDouble());

			// -- //
			result = R.eval("temp <- 100 * (sum(minusminus) / (total));");
			ret.put("XY % --", result.asDouble());
			result = R.eval("temp <- sum(minusminus)");
			ret.put("XY n --", result.asDouble());
			result = R.eval("temp <- mean(x[minusminus,]$Value);");
			ret.put("XY meanX --", result.asDouble());
			result = R.eval("temp <- mean(y[minusminus,]$Value);");
			ret.put("XY meanY --", result.asDouble());
			result = R.eval("temp <- sd(x[minusminus,]$Value);");
			ret.put("XY sdX --", result.asDouble());
			result = R.eval("temp <- sd(y[minusminus,]$Value);");
			ret.put("XY sdY --", result.asDouble());

			// -+ //
			result = R.eval("temp <- 100 * (sum(minusplus) / (total));");
			ret.put("XY % -+", result.asDouble());
			result = R.eval("temp <- sum(minusplus)");
			ret.put("XY n -+", result.asDouble());
			result = R.eval("temp <- mean(x[plusminus,]$Value);");
			ret.put("XY meanX +-", result.asDouble());
			result = R.eval("temp <- mean(y[plusminus,]$Value);");
			ret.put("XY meanY +-", result.asDouble());
			result = R.eval("temp <- sd(x[plusminus,]$Value);");
			ret.put("XY sdX +-", result.asDouble());
			result = R.eval("temp <- sd(y[plusminus,]$Value);");
			ret.put("XY sdY +-", result.asDouble());

			// +- //
			result = R.eval("temp <- 100 * (sum(plusminus) / (total));");
			ret.put("XY % +-", result.asDouble());
			result = R.eval("temp <- sum(plusminus)");
			ret.put("XY n +-", result.asDouble());
			result = R.eval("temp <- mean(x[minusplus,]$Value);");
			ret.put("XY meanX -+", result.asDouble());
			result = R.eval("temp <- mean(y[minusplus,]$Value);");
			ret.put("XY meanY -+", result.asDouble());
			result = R.eval("temp <- sd(x[minusplus,]$Value);");
			ret.put("XY sdX -+", result.asDouble());
			result = R.eval("temp <- sd(y[minusplus,]$Value);");
			ret.put("XY sdY -+", result.asDouble());

			return ret;
		}
		catch (REXPMismatchException e)
		{
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * "Total", "+", and "-" are the keys to the data. This can be called after calling plotXYData which loads the variables 'x' and 'y'
	 * 
	 * @param isVerticalThresh
	 * @param thresh
	 * @param cross
	 * @return
	 */
	public static TreeMap<String,Double> calculateStats_SingleThreshold(boolean isVerticalThresh, double thresh, double cross)
	{
		TreeMap<String,Double> ret = calculateStats_NoThreshold();
		REXP result;
		try
		{
			if(isVerticalThresh)
			{
				// Define subpopulations
				result = R.eval("plus <- (x$Value > y$Value*" + cross + " + " + thresh + ");");
				result = R.eval("minus <- (x$Value <= y$Value*" + cross + " + " + thresh + ");");

				// Calculate stats for each subpopulation
				result = R.eval("temp <- 100 * (sum(plus) / (total));");
				ret.put("X % +", result.asDouble());
				result = R.eval("temp <- sum(plus)");
				ret.put("X n +", result.asDouble());
				result = R.eval("temp <- mean(x[plus,]$Value);");
				ret.put("X meanX +", result.asDouble());
				result = R.eval("temp <- sd(x[plus,]$Value);");
				ret.put("X sdX +", result.asDouble());
				result = R.eval("temp <- mean(y[plus,]$Value);");
				ret.put("X meanY +", result.asDouble());
				result = R.eval("temp <- sd(y[plus,]$Value);");
				ret.put("X sdY +", result.asDouble());

				result = R.eval("temp <- 100 * (sum(minus) / (total));");
				ret.put("X % -", result.asDouble());
				result = R.eval("temp <- sum(minus)");
				ret.put("X n -", result.asDouble());
				result = R.eval("temp <- mean(x[minus,]$Value);");
				ret.put("X meanX -", result.asDouble());
				result = R.eval("temp <- sd(x[minus,]$Value);");
				ret.put("X sdX -", result.asDouble());
				result = R.eval("temp <- mean(y[minus,]$Value);");
				ret.put("X meanY -", result.asDouble());
				result = R.eval("temp <- sd(y[minus,]$Value);");
				ret.put("X sdY -", result.asDouble());
			}
			else
			{
				// Define subpopulations
				result = R.eval("plus <- (y$Value > x$Value*" + cross + " + " + thresh + ");");
				result = R.eval("minus <- (y$Value <= x$Value*" + cross + " + " + thresh + ");");

				// Calculate stats for each subpopulation
				result = R.eval("temp <- 100 * (sum(plus) / (total));");
				ret.put("Y % +", result.asDouble());
				result = R.eval("temp <- sum(plus)");
				ret.put("Y n +", result.asDouble());
				result = R.eval("temp <- mean(x[plus,]$Value);");
				ret.put("Y meanX +", result.asDouble());
				result = R.eval("temp <- sd(x[plus,]$Value);");
				ret.put("Y sdX +", result.asDouble());
				result = R.eval("temp <- mean(y[plus,]$Value);");
				ret.put("Y meanY +", result.asDouble());
				result = R.eval("temp <- sd(y[plus,]$Value);");
				ret.put("Y sdY +", result.asDouble());

				result = R.eval("temp <- 100 * (sum(minus) / (total));");
				ret.put("Y % -", result.asDouble());
				result = R.eval("temp <- sum(minus)");
				ret.put("Y n -", result.asDouble());
				result = R.eval("temp <- mean(x[minus,]$Value);");
				ret.put("Y meanX -", result.asDouble());
				result = R.eval("temp <- sd(x[minus,]$Value);");
				ret.put("Y sdX -", result.asDouble());
				result = R.eval("temp <- mean(y[minus,]$Value);");
				ret.put("Y meanY -", result.asDouble());
				result = R.eval("temp <- sd(y[minus,]$Value);");
				ret.put("Y sdY -", result.asDouble());
			}

			return ret;
		}
		catch (REXPMismatchException e)
		{
			e.printStackTrace();
			return null;
		}

	}

	/**
	 * "Total", "x+", "y+", "++", "+-", "-+", and "--" are the keys to the data.
	 * 
	 * @param xThresh
	 * @param xCross
	 * @param yThresh
	 * @param yCross
	 * @return
	 */
	public static TreeMap<String,Double> calculateStats_NoThreshold()
	{
		try
		{
			REXP result;
			TreeMap<String,Double> ret = new TreeMap<String,Double>();
			result = R.eval("total <- nrow(x);");
			ret.put("n", result.asDouble());
			result = R.eval("temp <- mean(x$Value);");
			ret.put("meanX", result.asDouble());
			result = R.eval("temp <- mean(y$Value);");
			ret.put("meanY", result.asDouble());
			result = R.eval("temp <- sd(x$Value);");
			ret.put("sdX", result.asDouble());
			result = R.eval("temp <- sd(y$Value);");
			ret.put("sdY", result.asDouble());

			return ret;
		}
		catch (REXPMismatchException e)
		{
			e.printStackTrace();
			return null;
		}
	}

	public static void plotLine(boolean flipAxes, boolean logscale, double slope, double intercept, double xMin, double xMax, int numPoints, String color, double lineWeight, double xAxisTransition, double xAxisLinLogRatio, double yAxisTransition, double yAxisLinLogRatio)
	{
		if(logscale && xMin < 0.001)
		{
			if(!flipAxes)
			{
				R.eval("tempX <- c(seq(" + xMin + ", " + xAxisTransition + ", length.out = " + numPoints + "), exp(seq(log(" + xAxisTransition + "), log(" + xMax + "), length.out = " + numPoints + ")));");
				R.eval("tempY <- " + slope + "*tempX + " + intercept + ";");
			}
			else
			{
				R.eval("tempY <- c(seq(" + xMin + ", " + yAxisTransition + ", length.out = " + numPoints + "), exp(seq(log(" + yAxisTransition + "), log(" + xMax + "), length.out = " + numPoints + ")));");
				R.eval("tempX <- " + slope + "*tempX + " + intercept + ";");
			}
		}
		else if(logscale)
		{
			if(!flipAxes)
			{
				R.eval("tempX <- exp(seq(log(" + xMin + "), log(" + xMax + "), length.out = " + numPoints + "));");
				R.eval("tempY <- " + slope + "*tempX + " + intercept + ";");
			}
			else
			{
				R.eval("tempY <- exp(seq(log(" + xMin + "), log(" + xMax + "), length.out = " + numPoints + "));");
				R.eval("tempX <- " + slope + "*tempY + " + intercept + ";");
			}
		}
		else
		{
			if(!flipAxes)
			{
				R.eval("tempX <- seq(" + xMin + ", " + xMax + ", length.out = " + numPoints + ");");
				R.eval("tempY <- " + slope + "*tempX + " + intercept + ";");
			}
			else
			{
				R.eval("tempY <- seq(" + xMin + ", " + xMax + ", length.out = " + numPoints + ");");
				R.eval("tempX <- " + slope + "*tempY + " + intercept + ";");
			}
		}
		SingleCellUtility.calculateLogicleScaleValues("tempX", null, "tempX", xAxisTransition, xAxisLinLogRatio);
		SingleCellUtility.calculateLogicleScaleValues("tempY", null, "tempY", yAxisTransition, yAxisLinLogRatio);
		R.eval("lines(tempX, tempY, col=" + R.sQuote(color) + ", lwd=" + lineWeight + ");");

	}

	public static void plotSingleThresholdLine(boolean isVertical, double constant, double crossOver, String color, double xAxisTransition, double xAxisLinLogRatio, double yAxisTransition, double yAxisLinLogRatio)
	{
		if(isVertical)
		{
			plotLine(true, true, crossOver, constant, -100, 1000000, 100, "blue", 1.5, xAxisTransition, xAxisLinLogRatio, yAxisTransition, yAxisLinLogRatio);
		}
		else
		{
			plotLine(false, true, crossOver, constant, -100, 1000000, 100, "blue", 1.5, xAxisTransition, xAxisLinLogRatio, yAxisTransition, yAxisLinLogRatio);
		}
	}

	public static void filterData(String dstVariable, String srcVariable, String filterKey, String comparison, String filterValue, boolean filterValueIsString)
	{
		if(filterValueIsString)
		{
			R.eval(dstVariable + "<- " + srcVariable + "[" + srcVariable + "$" + filterKey + comparison + R.sQuote(filterValue) + ",];");
		}
		else
		{
			R.eval(dstVariable + "<- " + srcVariable + "[" + srcVariable + "$" + filterKey + comparison + filterValue + ",];");
		}
	}

}
