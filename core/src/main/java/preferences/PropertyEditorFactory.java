package preferences;

import java.beans.PropertyEditor;

/**
 * Creates editors for Property object.<br>
 * 
 */
public interface PropertyEditorFactory {
	
	PropertyEditor createPropertyEditor(Property property);
	
}
