package progistar.scan.data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMTag;
import progistar.scan.run.Scan;

public class BarcodeTable {

	public static Hashtable<String, Boolean> hasBarcode = new Hashtable<String, Boolean>();
	public static ArrayList<String> barcodeIds = new ArrayList<String>();
	
	public static void load() {
		assert Scan.whitelistFile != null;
		assert Scan.isSingleCellMode;
		
		try {
			BufferedReader BR = new BufferedReader(new FileReader(Scan.whitelistFile));
			String line = null;
			barcodeIds.add(Constants.NULL_BARCODE_ID);
			barcodeIds.add(Constants.OTHER_BARCODE_ID);
			
			BR.readLine(); // skip header
			while((line = BR.readLine()) != null) {
				String[] fields = line.split("\t");
				
				if(hasBarcode.get(fields[0]) == null) {
					hasBarcode.put(fields[0], true);
					barcodeIds.add(fields[0]);
				}
			}
			
			BR.close();
			
			System.out.println("A total of "+hasBarcode.size()+" barcodes were saved.");
		}catch(IOException ioe) {
			System.out.println("Fail to load white list: "+Scan.whitelistFile.getName());
		}
	}
	
	/**
	 * If Scan.isSingleCellMode is:
	 *  - turned on => find barcode id and return the id. 
	 *  - turned off => return default barcode id.
	 * 
	 * @param samRecord
	 * @return
	 */
	public static String getBarcodeFromBam (SAMRecord samRecord) {
		String barcodeId = Constants.DEFAULT_BARCODE_ID;
		
		// single cell mode?
		if(Scan.isSingleCellMode) {
			Object cbTag = samRecord.getAttribute(SAMTag.CB);
			if(cbTag == null) {
				barcodeId = Constants.NULL_BARCODE_ID;
			} else {
				barcodeId = (String) cbTag;
				// check whitelist
				if(BarcodeTable.hasBarcode.get(barcodeId) == null) {
					barcodeId = Constants.OTHER_BARCODE_ID;
				}
			}
 		}
		
		return barcodeId;
	}
}
