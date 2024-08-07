package progistar.scan.function;

import java.io.File;
import java.util.ArrayList;

import org.ahocorasick.trie.Trie;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import progistar.scan.data.Constants;
import progistar.scan.data.LocationInformation;
import progistar.scan.data.SequenceRecord;
import progistar.scan.run.Scan;
import progistar.scan.run.Task;

public class TargetModeRun {
	
	public static void runTargetMode (Task task) {
		if(task.type == Constants.TYPE_TARGET_MODE_MAPPED_TASK) {
			countMappedReads(task);
		} else if(task.type == Constants.TYPE_TARGET_MODE_UNMAPPED_TASK) {
			countUnmappedReads(task);
		} else if(task.type == Constants.TYPE_TARGET_MODE_LIBRARY_ESTIMATION_TASK) {
			estimateLibSize(task);
		}
	}

	
	private static void countUnmappedReads(Task task) {
		long startTime = System.currentTimeMillis();
		// to prevent racing
		File file = new File(Scan.bamFile.getAbsolutePath());
		try (SamReader samReader = SamReaderFactory.makeDefault().open(file)) {
			// for unmapped reads
			Trie trie = SequenceRecord.getTrie(task.records);
			SAMRecordIterator iterator = samReader.queryUnmapped();
			ScanModeRun.find(iterator, trie, task);
            
		} catch(Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		long endTime = System.currentTimeMillis();
		
		if(Scan.verbose) {
			System.out.println("Task"+task.taskIdx+" "+(endTime-startTime)/1000+" sec");
		}
	}
	
	
	private static void countMappedReads (Task task) {
		long startTime = System.currentTimeMillis();
		// to prevent racing
		File file = new File(Scan.bamFile.getAbsolutePath());
		try (SamReader samReader = SamReaderFactory.makeDefault().open(file)) {
			double size = task.records.size();
			for(int i=0; i<size; i++) {
				task.currentRecordIdx = i;
				SequenceRecord record = task.records.get(i);
				
				Trie trie = Trie.builder().addKeyword(record.sequence).build();
				
	            // in case of soft-clip, it can be zero because of unstable record range.
				SAMRecordIterator iterator = samReader.queryOverlapping(record.chr, record.start-100, record.end+100);
				ScanModeRun.find(iterator, trie, task);
			}
            
		} catch(Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		long endTime = System.currentTimeMillis();
		
		if(Scan.verbose) {
			System.out.println(task.taskIdx+" "+(endTime-startTime)/1000+" sec");
		}
	}
	
	private static void estimateLibSize (Task task) {
		long startTime = System.currentTimeMillis();
		// to prevent racing
		File file = new File(Scan.bamFile.getAbsolutePath());
		try (SamReader samReader = SamReaderFactory.makeDefault().open(file)) {
			SAMRecordIterator iterator = null;
			if(task.readType == Constants.MAPPED_READS) {
				iterator = samReader.query(task.chrName, task.start, task.end, false);
				estimate(iterator, task);
			}
		} catch(Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		long endTime = System.currentTimeMillis();
		
		if(Scan.verbose) {
			System.out.println("Task"+task.taskIdx+" "+(endTime-startTime)/1000+" sec");
		}
	}
	

	private static void estimate (SAMRecordIterator iterator, Task task) {
		while (iterator.hasNext()) {
            SAMRecord samRecord = iterator.next();
            boolean isPass = false;

            // if the task is for mapped reads
            // only reads with below that genomic start are retrieved
            if(task.readType == Constants.MAPPED_READS) {
            	if( !(samRecord.getAlignmentStart() >= task.start && 
            			samRecord.getAlignmentStart() < task.end) ) {
            		isPass = true;
            	}
            	
            	if(samRecord.isSecondaryAlignment()) {
            		isPass = true;
            	}
            	
            } else {
            	isPass = true;
            }
            
            if(isPass) {
            	continue;
            }
            
            task.processedReads++;
            
        }
        iterator.close();
	}
}
