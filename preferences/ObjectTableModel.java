package preferences;

import javax.swing.table.TableModel;

/**
 * Implemented by TableModel which are more a list of objects than table.<br>
 */
public interface ObjectTableModel extends TableModel {
	
	Object getObject(int p_Row);
	
}
