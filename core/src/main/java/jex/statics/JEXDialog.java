package jex.statics;

import java.awt.SystemColor;
import java.io.File;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import Database.SingleUserDatabase.JEXWriter;
import logs.Logs;
import net.miginfocom.swing.MigLayout;


public class JEXDialog {
	
	public synchronized static void messageDialog(String msg)
	{
		//default icon, custom title
		Thread t = new Thread(new Runnable()
		{
			public void run()
			{
				JOptionPane.showMessageDialog(JEXStatics.main, msg);
				Logs.log(msg, JEXDialog.class);
			}
		});
		t.start();
	}
	
	public synchronized static void messageDialog(String msg, Class<?> source)
	{
		Thread t = new Thread(new Runnable()
		{
			public void run()
			{
				//default icon, custom title
				JOptionPane.showMessageDialog(JEXStatics.main, msg);
				Logs.log(msg, source);
			}
		});
		t.start();
	}
	
	public synchronized static void messageDialog(String msg, Object source)
	{
		Thread t = new Thread(new Runnable()
		{
			public void run()
			{
				//default icon, custom title
				JOptionPane.showMessageDialog(JEXStatics.main, msg);
				Logs.log(msg, source);
			}
		});
		t.start();
	}
	
	/**
	 * Get the index of the choice made by the user from the supplied list.
	 * @param title
	 * @param question Question to prompt user with for making the choice.
	 * @param choices
	 * @param defaultChoice
	 * @returnthe index of the choice preferred by the user otherwise cancel returns -1
	 */
	public synchronized static Integer getChoice(String title, String question, String[] choices, int defaultChoice)
	{
		JList<String> list = new JList<String>(choices);
		list.setSelectedIndex(defaultChoice);
		JTextArea questionLabel = new JTextArea(question);
		questionLabel.setLineWrap(true);
		questionLabel.setWrapStyleWord(true);
		questionLabel.setBackground(SystemColor.window);
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("flowy","[grow, center]","[grow]5"));
		panel.add(questionLabel, "grow, width 300!, height pref!");
		panel.add(list);
		
		int option = JOptionPane.showConfirmDialog(JEXStatics.main, panel, title, JOptionPane.OK_CANCEL_OPTION);
		if(option == 0)
		{
			return list.getSelectedIndex();
		}
		else
		{
			return -1;
		}
	}
	
	public synchronized static String fileSaveDialog()
	{
		return fileDialog(true, false);
	}
	
	public synchronized static String fileChooseDialog(boolean directoriesOnly)
	{
		return fileDialog(false, directoriesOnly);
	}
	
	private synchronized static String fileDialog(boolean save, boolean directoriesOnly)
	{
		// Creating file chooser or save dialog (but both using the save dialog because those allow you to create new folders along during the dialog)
		JFileChooser fc = new JFileChooser();
		fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		if(save)
		{
			fc.setDialogType(JFileChooser.SAVE_DIALOG);
		}
		else
		{
			fc.setDialogType(JFileChooser.OPEN_DIALOG);
			if(directoriesOnly)
			{
				fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			}
			
			// Try to allow creation of new folders while on mac
			try
			{
				JButton newFolderButton = (JButton) ((JPanel) ((JPanel) ((JPanel) ((JPanel) fc.getComponent(4)).getComponent(2)).getComponent(1)).getComponent(0)).getComponent(1);
				newFolderButton.setVisible(true);
			}
			catch(Exception e)
			{
				// At least we tried to enable the new folder button on macs
			}
			fc.revalidate();
		}
		
		// Set the current directory
		String lastPath = PrefsUtility.getLastPath();
		File filepath = new File(lastPath);
		if(filepath.isDirectory())
		{
			fc.setCurrentDirectory(filepath);
		}
		else
		{
			File filefolder = filepath.getParentFile();
			fc.setCurrentDirectory(filefolder);
		}
		
		// Open dialog box
		Integer returnVal = null;
		if(save)
		{
			returnVal = fc.showSaveDialog(JEXStatics.main);
		}
		else
		{
			returnVal = fc.showOpenDialog(JEXStatics.main);
		}
		
		// Get the return value
		if(returnVal == JFileChooser.APPROVE_OPTION)
		{
			File f = fc.getSelectedFile();
			
			// Set the last path opened to the path selected and return the path
			try
			{
				if(f.isDirectory())
				{
					File parent = f.getParentFile();
					String ret = parent.getCanonicalPath();
					PrefsUtility.setLastPath(ret);
					ret = f.getCanonicalPath();
					return ret;
				}
				else
				{
					String ret = f.getCanonicalPath();
					PrefsUtility.setLastPath(ret);
					return ret;
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
				return null;
			}
		}
		else
		{
			Logs.log("File saver/chooser dialog canceled.", 0, JEXWriter.class);
			return null;
		}	
	}
	
	
}
