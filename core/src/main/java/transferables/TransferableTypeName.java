//
//  TransferableButtonObject.java
//  MicroFluidicHT_Tools
//
//  Created by erwin berthier on 8/26/08.
//  Copyright 2008 __MyCompanyName__. All rights reserved.
//
package transferables;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

import logs.Logs;
import Database.Definition.TypeName;

public class TransferableTypeName implements Transferable {
	
	public static DataFlavor jexDataFlavor = new DataFlavor(TypeName.class, "A TypeName object");
	
	protected static DataFlavor[] supportedFlavors = { jexDataFlavor, DataFlavor.stringFlavor };
	
	TypeName tn;
	
	public TransferableTypeName(TypeName tn)
	{
		this.tn = tn;
	}
	
	public DataFlavor[] getTransferDataFlavors()
	{
		return supportedFlavors;
	}
	
	public boolean isDataFlavorSupported(DataFlavor flavor)
	{
		if(flavor.equals(jexDataFlavor) || flavor.equals(DataFlavor.stringFlavor))
			return true;
		return false;
	}
	
	public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException
	{
		if(flavor.equals(jexDataFlavor))
		{
			Logs.log("Transfering typename", 1, this);
			return tn;
		}
		else if(flavor.equals(DataFlavor.stringFlavor))
		{
			Logs.log("Transfering string value " + tn.toString(), 1, this);
			String result = tn.toString();
			return result;
		}
		
		else
			throw new UnsupportedFlavorException(flavor);
	}
}