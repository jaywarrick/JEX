JEX
===

#### Description

Java-based software and GUI to automatically manage/database large numbers of files (e.g., images) in a transparent file structure and perform batch processing with standard tools (ImageJ, R, Octave, Weka).

#### Downloading the JEX Executable Version

Executable jars and simple exectuable scripts to run those jars with specific java heap space arguments for increasing the amount of RAM that can be used are available for download at [sourceforge](http://sourceforge.net/projects/jextools/). Simply save and unzip the file anywhere you would like (except maybe your root directory of your hard drive such as 'C:/' as that is often weirdly write protected and can cause issues) and double-click the "JEX for Mac" or "JEX for Windows" exectubale script depending on your operating system (Linux users can double-click the jar directly or run the jar from the command line to change the memory settings). This will start a terminal window that will show information regarding any actions that JEX is taking as well as error information that would be very useful for debugging issues you might run into :-).

You can update your version of JEX to the most recent stable release or developmental version at any time by simply clicking the "Update" button in the top-left corner of JEX when viewing a database. However, to create and view a database, follow the mini-tutorial below titled "Getting Started".

#### Reporting Bugs/Issues

Please go to [https://github.com/jaywarrick/JEX/issues](https://github.com/jaywarrick/JEX/issues) to describe and submit the issue, which will be emailed to JEX's developers.

#### Getting started

Note: There is also a "Test Script" with screen shots created for a specific project, that you can also download and follow [here](https://sourceforge.net/projects/ctciap/files/TestScriptFinal.zip/download).

Upon starting JEX, you will be shown a screen for either...

1. Creating a user profile
2. Opening a user profile via a file browser

or

3. Opening a recently opened user profile

After creating your user profile, typically you just click the recently opened profile to use JEX. However, upon first use, you must create a user profile. Select to create a user profile and choose to save the user profile wherever you like (e.g., your "Documents" folder).

With the user profile file created, we can now store information regarding what disks or servers you would like to use for storing JEX databases (e.g., any folder on a NAS device, external hardrive, or your PC). These locations are remembered so you don't have to constantly relocate them each time you open JEX. These locations are termed JEX "Repositories". This is the highest level of organization of data. 

  * A small side note: The organization of data by JEX goes like this... Repositories > Databases > Datasets > Entries > Objects. Databases are simply folders of Datasets with some extra xml files that represent the database structure. Datasets are simply folders Entries. Entries are simply folders of Objects. Objects are simply folders of raw data files (e.g., tiff image files) and a single small "arff" format text file that JEX uses to keep track of how to organize the raw data files in that folder (e.g., many tiff files that represent timpoints from a timelapse). In this way, the files contained within an _Object_ folder can be treated as a N-dimensional dataset. For example, a group of tiff files could be images that represent a mosaic of images taken in multiple colors, at multiple z-planes, at multiple times. The small .arff file keeps track of which images are for which location in the mosaic as well as which color, plane, and time. The number and name of dimensions of any _Object_ is completely user defined in JEX.
  
With a respository location defined for your user profile, we can now create databases using the "+" button. Name the database. Single-click the **icon** to the left of the database name to open the newly created and empty database. Once you have opened your database the general rule of thumb is to procede through the "tabs" of JEX in order (see the icons numbered 1-7 in the top center of the window).

You are initially brought into JEX with tab \#1 active. At the bottom center of the window is a button labeled "Add Dataset". As the name implies, you can click this to create a _Dataset_. 

  * Another side note: A _Dataset_ is a 2D array of _Entries_ and is meant to represent something like a well-plate commonly used for cell culture (e.g., a 96-well plate consisting of 8 rows and 12 columns from which one could collect similar types of imaging data); however, feel free to organize your data as you see fit. Each _Entry_ (or well of the well plate) generally will have the same types of objects (e.g., an N-dimensional image object of the image data obtained from that well and a resulting table of data resulting from it's analysis) but each object is unique to that _Entry_ or well. Given each _Entry_ contains data that is independent of data from adjacent _Entries_ but data _Objects_ within each _Entry_ typically depend on one-another. Thus, multi-threading of data analysis is performed at the level of _Entries_ (i.e., analysis of _Entries_ is processed in parallel using the multiple processing cores of the host computer while operations within each _Entry_ are processed linearly/sequentially).

Long story short, you create a _Dataset_, which is a 2D array of _Entries_. Now we can import data into each _Entry_. You can see your array of entries by going to tab \#2. Each _Entry_ is displayed as a box. Click the boxes to select particular _Entries_ you would like to import data into (e.g., all of them or just particular row, column, or individual _Entry_). You can also use the "Quick Selector" on the left side to select locations in the _Dataset_ array.

To import data go to tab \#3. On the right-hand side of this tab there are selections to be made. Generally work from top to bottom. Choose what type of object you will be creating (e.g., and Image object). Then select the number of dimensions (e.g., 1D for a time-series of images or 2D for a timeseries of images taken in different colors, etc.). Type a name to give to this object. Choose the files that make up this object.

  * Side note: Within the "Choose files" dialog, click the "load" button, choose the files you would like using the file browser and click "OK".
  
Then below the list that shows the files you have chosen, indicate how the files are ordered in the list. Using the example of a 2D image object that has 12 image files from 3 different times in 4 different colors, if the first 3 files are images from different times of the same color while images 4-7 are from those same times but in the second color, you would make the following selections... Type a name for the "Time" dimension into the first drop-down (e.g., "T" or "time" or "Time", etc. it is your choice) and enter 3 for the "size" of the dimension. Then type a name for the "Color" dimension and a "size" of 4. Essentially you are indicating how to assign dimension values to each file in the list (the first dimension listed in the inner-most loop of a nested for-loop of dimensions used for assigning dimension indices and so on).

You must also indicate when the file list changes to a different "Array Row" and "Array Col", if at all. "Array Row" and "Array Col" are reserved dimension names for the _Dataset_ rows and columns. If you are importing into a single _Entry_, it doesn't matter where in the list "Array Row" and "Array Col" fall in the list as there is only one _Entry_ into which data is being imported so the index never changes. However, if you have multiple _Entries_, pay attention to where in the list these special dimension names fall. Although not completely obvious, it doesn't matter what you put for the "size" of the "Array Row" and "Array Col" dimensions as this is inferred based on your selection to reduce the potential for errors.

When you feel you have the correct selections made, click "Deal files". This displays within the _Entry_ boxes themselves, which files will be put where and what dimension values they will be assigned. If this doesn't look right, review your selections and adjust accordingly. When the result of "Deal files" looks right, click the "Save" or "Import" button **at the bottom of the window** to actually import these files to create an N-Dimensional object.

The newly imported object with the name you specified will now show up in the left-hand portion of the window that is everpresent in all tabs of JEX. If this is an image object and you have an _Entry_ selected into which you created such an object, you can **double-click the image icon** to open the N-dimensional image in JEX's built-in image viewer.

Other potential ways to specify files for importing is to drag files onto _Entry_ boxes when using tab \#3 or by dragging files into the list above the "Choose files" button of the same tab. Whatever is listed in the _Entry_ boxes is how things will be imported when the "Save" or "Import" button at the bottom of the window is clicked.

  * Side Note: In general, save the database frequently, in between steps, by clicking the "Save" button in the top-left of the JEX window.

A useful but not a required step would then be to use tab \#4 to create "Label" objects in entries to save information regarding experimental conditions. For example, you can create a label object with the name "Treatment Concentration" in each _Entry_. The value of the label object would then be the actual treatment concentration used to obtain the data for that _Entry_. These label objects can then be passed to batch processing algorithms that might benefit from such information.

Once you have an object in the database, you can batch process it using tab\#5. On the right-hand side of tab \#5 is a list of all the batch processing "Toolboxes" of JEX. Each "Toolbox" contains a list of batch processing functions. Double-clicking a funciton will insert the function into the JEX "Workflow". A "Workflow" is just a list of functions. These workflows can be seen in the top center panel and can be saved and re-imported later using the buttons in this panel. Each function has a very similar user interface consisting of "Inputs" and "Outputs". Clicking on the function name at the top of the function's user interface result in the "Parameters" of the function to show in the middle-lower-right :-) panel of the tab. 

Simply drag an appropriate database object onto the input icon of the function's user interface to set the input to that function. Likewise, outputs of functions can be dragged to inputs of other functions. In general, it really only makes sense to drag outputs from earlier in the "Workflow" (i.e., those that are farther left in the list) to inputs of functions that are later in the list. The functions will be processed from left to right.

Click the "Run" button on of a particular function to only run that function. Click the "Run" button to the right of the "Workflow" to run the entire sequence of functions. This also automatically results in the saving of a "Workflow" object into the database. This "Workflow" object is a saved version of the entire workflow, complete with selections made for inputs/outputs and parameters. Double-clicking the "Workflow" object in the left panel, "loads" the workflow into the upper-middle panel, appending the functions to the end of what already might exist in this panel. In this way, workflows can be easily re-run with exactly the same parameters if necessary. Likewise, with the ability to save and load "Workflows" using the "Save" and "Load" button of tab \#5 allows for easy sharing of batch processing workflows between individuals and databases.

 * Side note: There are three types of toolboxes, those for "Old" JEX functions that don't use java annotation processing and a different formation for defining function inputs, outputs, and parameters; those for "ImageJ" functions which are automatically converted versions of existing ImageJ plugins (potentially useful but not completely supported in every way yet); and those for "New" JEX functions which leverage java annotation processing to simplify the writing of new functions. The ImageJ toolboxes begin with "ImageJ". The new JEX toolboxes begin with "JEX" while the old JEX toolboxes do not have a common prefix.

Those are the basics. For now, try things out from there. Hopefully we'll get more documentation on all the other features of JEX including some of the useful "Plugins" on tab \#7.

#### Using Eclipse to run or edit/develop the JEX and JEX functions...

The project is structured as a Maven project which is automatically recognized by multiple java IDE's including Eclipse (at least starting with the "Kepler" version). To import the project into eclipse simply...

1. Clone the git repository to your computer (see below for possible ways to do so).
2. Within Eclipse, choose File > Import... > Maven > Existing Maven Project, and follow the resulting dialog.

  * Select the JEX/pom.xml (notice all caps) and the pom.xml files under this pom (jex/pom.xml, core/pom.xml, and broken/pom.xml).
3. Create a "Run Configuration"
  * Select Run > Run Configurations...
  * Set the project to the project you just created that relates to the JEX/pom.xml
  * Set the "Main Class" to jex.Main (this class essentially runs the main class within the core/pom.xml related project containing most of the classes of JEX).
  * Under the "Arguments" tab of this same dialog window, add (without the surrounding quotes) "-Xmx1024m" to the "VM arguments:" text box to allow JEX to use up to 1024 MB of memory. Change accordingly as desired or needed for your application.
  * Choose to run the configuration to run JEX.

#### Cloning the JEX git repository

For novices, here is one way to go about getting a copy of the JEX source code work with on your computer.

Option A: If you wish to just download the source-code without intending to make any edits or additions (e.g., creating a new JEX function for image processing), you can do the following. However, if you plan to just use JEX "as is", it is much easier to following the instructions for "Downloading the JEX Executable Version".

1. Go to [http://github.com/jaywarrick/JEX](http://github.com/jaywarrick/JEX) and click the "Clone in Desktop" icon.
2. Unzip the downloaded file wherever you desire and follow the instructions from Using Eclipse to run or edit/develop the JEX and JEX functions..." to run via Eclipse.

Option B: If you wish to potentially develop functions for JEX or develop JEX itself, do the following.

1. Go to www.github.com and create a username and password.
2. From the github website, download and install the GitHub.app (available for all major platforms)
3. Go to [http://github.com/jaywarrick/JEX](http://github.com/jaywarrick/JEX) and click the "Fork" button to create a copy of the source-code files stored on github as part of your new account and completely editable by yourself without impact on the original code at [http://github.com/jaywarrick/JEX](http://github.com/jaywarrick/JEX). You should now have been redirected to your new forked repository (i.e., a website with the an address similar to http://github.com/<your username>/JEX).
4. Now click the "Clone in Desktop" button to save your forked repository to your computer via the GitHub.app you installed.
5. Import this downloaded source-code following the instructions from "Using Eclipse to run or edit/develop the JEX and JEX functions..."

#### Contributing to JEX

After making a saving edits to your forked version of the JEX repository (see "Cloning the JEX git repository" "Option B") you can use the GitHub.app to "sync" your changes with the version of your source-code stored on GitHub. If you have made changes that you would like incorporated into the original JEX respository you can create and submit a pull request. This sends a message to us with your code changes in a way that your changes can be easily integrated into JEX. This way you don't have to worry about "messing up" any code.

To keep your forked repository up-to-date with changes to the original JEX, termed the "upstream" repository, you need to use the command line git tools. See [here](https://help.github.com/articles/syncing-a-fork/) for updating your fork.

If you are an experienced programmer that would like to be part of the team that develops JEX in a more direct manner, please contact us by creating and "issue" and we'll see if we can't set you up as a "developer" on this repository so you can sync changes directly to the source-code.

#### How to create a JEX Function

See [AdjustImage.java](https://github.com/jaywarrick/JEX/blob/master/core/src/main/java/function/plugin/plugins/imageProcessing/AdjustImage.java) for a current template on how to write a JEX funciton. In general, we use annotation processing to mark these classes as JEX functions or, more precisely, as classes that implement the JEXPlugin interface. Annotations are anything with the "@" symbol preceding them. The four types of annotations we use are @Plugin, @InputMarker, @ParameterMarker, @OutputMarker. @Plugin provides information about the function's name, description, and the toolbox is should be placed in. @InputMarker's and @OutputMarkers define the default names and types of database object that are used as inputs and outputs to the function. @ParameterMarker's defines the name, description, order in the user interface, and type of "widget" that should be used to allow the user to enter a value for the parameter. The type of the parameter (i.e., a String, Boolean, or Number) is defined by the type of variable (e.g., "String pathToFile;")that is listed in the line directly below the annotation. This way of doing things provides a very explicit, compact, and simple way to define function inputs, outputs, and parameters and enables automatic generation of user interfaces for each function. Search other functions in folders adjacent to the AdjustImage.java file for examples on different widgets that are available. Bascially, there are single-line text boxes, dropdowns, check boxes, file-choosers, and multi-line text boxes/editors intended for entering scripting commands to pass to functions (e.g., for functions that leverage R or Octave).

Typically to develop such functions, it is easiest to just clone the repository and run JEX from source-code using a java IDE such as eclipse. We would suggest creating a folder adjacent to the folder used for the AdjustImage.java function or to add to one of the existing adjacent folders.

##### Happy JEXing :-)
