package cruncher;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import jex.statics.JEXDialog;
import jex.statics.JEXStatics;
import logs.Logs;

public class DirectoryWatcher implements Runnable {

	private WatchService watcher = null;
	private WatchKey key = null;
	
	@SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event)
	{
        return (WatchEvent<T>) event;
    }
	
	public DirectoryWatcher()
	{
		try
		{
			watcher = FileSystems.getDefault().newWatchService();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	/**
     * Creates a WatchService and registers the given directory
     */
    public synchronized void watch(String path) throws IOException
    {
    		if(this.watcher == null)
    		{
    			this.watcher = FileSystems.getDefault().newWatchService();
    		}
    		if(this.key == null)
    		{
    	        Path dir = Paths.get(path);
    	        this.key = dir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE);
    		}
    		else
    		{
    			JEXDialog.messageDialog("Warning! A directory is already being watched for updates. Ignorning request to watch new directory.", this);
    		}
    }
    
    @Override
    public void run()
    {
        try {
            for (;;) {
                // wait for key to be signalled
                WatchKey key = watcher.take();

                if (this.key != key) {
                    System.err.println("WatchKey not recognized!");
                    continue;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent<Path> ev = cast(event);
                    WatchEvent.Kind<?> kind = event.kind();

					// This key is registered only
					// for ENTRY_CREATE events,
					// but an OVERFLOW event can
					// occur regardless if events
					// are lost or discarded.
					if (kind == StandardWatchEventKinds.OVERFLOW) {
						continue;
					}

					// Get the path of the created file.
					Path filename = ev.context();
					Logs.log("Found new file :" + filename, this);

					// Compare the provided DT with the number of files and see
					// if another timepoint worth of files as been created.
					JEXStatics.cruncher.runUpdate();
                }

                // reset key
                if (!key.reset()) {
                		JEXDialog.messageDialog("The Auto-Updater status is in valid. Resetting.", this);
                		this.reset();
                    break;
                }
            }
        }
        catch (InterruptedException x)
        {	
        		this.reset();
            return;
        }
    }
    
    public void reset()
    {
    		if(this.key != null)
    		{
    			this.key.reset();
    			this.key.cancel();
    		}
		this.key = null;
    }
}
