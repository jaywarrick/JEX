package preferences;

import java.io.File;
import java.io.IOException;

import javax.swing.JFileChooser;

/**
 * DirectoryPropertyEditor.<br>
 * 
 */
public class DirectoryPropertyEditor extends FilePropertyEditor {
	
	@Override
	protected void selectFile()
	{
		ResourceManager rm = ResourceManager.all(FilePropertyEditor.class);
		
		JFileChooser chooser = UserPreferences.getDefaultDirectoryChooser();
		
		chooser.setDialogTitle(rm.getString("DirectoryPropertyEditor.dialogTitle"));
		chooser.setApproveButtonText(rm.getString("DirectoryPropertyEditor.approveButtonText"));
		chooser.setApproveButtonMnemonic(rm.getChar("DirectoryPropertyEditor.approveButtonMnemonic"));
		
		File oldFile = (File) getValue();
		if(oldFile != null && oldFile.isDirectory())
		{
			try
			{
				chooser.setCurrentDirectory(oldFile.getCanonicalFile());
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		
		if(JFileChooser.APPROVE_OPTION == chooser.showOpenDialog(editor))
		{
			File newFile = chooser.getSelectedFile();
			String text = newFile.getAbsolutePath();
			textfield.setText(text);
			firePropertyChange(oldFile, newFile);
		}
	}
	
}
