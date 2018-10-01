package cruncher;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import tables.DimTable;

public class DirectoryWatcher {
	
	WatchService watcher;
	
	public DirectoryWatcher(String dirString, DimTable template, String timeToken)
	{
		Path dirPath = Paths.get(dirString);
		try {
			WatchService watcher = FileSystems.getDefault().newWatchService();
		    WatchKey key = dirPath.register(watcher,
		                           StandardWatchEventKinds.ENTRY_CREATE,
		                           StandardWatchEventKinds.ENTRY_DELETE,
		                           StandardWatchEventKinds.ENTRY_MODIFY);
		    
		    for (;;) {

		        // wait for key to be signaled
		        try {
		            key = watcher.take();
		        } catch (InterruptedException x) {
		            return;
		        }

		        for (WatchEvent<?> event: key.pollEvents()) {
		            WatchEvent.Kind<?> kind = event.kind();

		            // This key is registered only
		            // for ENTRY_CREATE events,
		            // but an OVERFLOW event can
		            // occur regardless if events
		            // are lost or discarded.
		            if (kind == OVERFLOW) {
		                continue;
		            }

		            // The filename is the
		            // context of the event.
		            WatchEvent<Path> ev = (WatchEvent<Path>)event;
		            Path filename = ev.context();

		            // Verify that the new
		            //  file is a text file.
		            try {
		                // Resolve the filename against the directory.
		                // If the filename is "test" and the directory is "foo",
		                // the resolved name is "test/foo".
		                Path child = dir.resolve(filename);
		                if (!Files.probeContentType(child).equals("text/plain")) {
		                    System.err.format("New file '%s'" +
		                        " is not a plain text file.%n", filename);
		                    continue;
		                }
		            } catch (IOException x) {
		                System.err.println(x);
		                continue;
		            }

		            // Email the file to the
		            //  specified email alias.
		            System.out.format("Emailing file %s%n", filename);
		            //Details left to reader....
		        }

		        // Reset the key -- this step is critical if you want to
		        // receive further watch events.  If the key is no longer valid,
		        // the directory is inaccessible so exit the loop.
		        boolean valid = key.reset();
		        if (!valid) {
		            break;
		        }
		    }
		} catch (IOException x) {
		    System.err.println(x);
		}
		
		
	}

}
