package cruncher;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import logs.Logs;
import tables.DimTable;

public class DirectoryWatcher implements Runnable {

	public WatchService watcher = null;
	private WatchKey key = null;
	private Thread watchThread = null;
	private Cruncher parent = null;
	private String hotPath = null;
	private DimTable template = null;
	private String timeToken = null;

	public DirectoryWatcher(Cruncher parent)
	{
		this.parent = parent;
		try
		{
			watcher = FileSystems.getDefault().newWatchService();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public void close() throws IOException {
        try {
            stop();
        }
        catch ( InterruptedException e ) {
            Logs.log("request to stop failed, guess its time to stop being polite!", this);
        }
    }

	public void start(String dirString, DimTable template, String timeToken) throws InterruptedException
	{
		this.hotPath = dirString;
		this.template = template;
		this.timeToken = timeToken;

		this.watchThread = new Thread(this);
		watchThread.start();
		synchronized( this )
		{
			this.wait();
		}
	}
	
	public void join() throws InterruptedException
	{
		watchThread.join();
	}

	public void stop() throws InterruptedException
	{
		this.hotPath = null;
		this.template = null;
		this.timeToken = null;
		this.watchThread.interrupt();
		this.watchThread.join();
		this.watchThread = null;
		Logs.log("Updater stopped", this);
	}

	@SuppressWarnings("unchecked")
	public void run()
	{

		try {
			Path dirPath = Paths.get(this.hotPath);
			key = dirPath.register(watcher, StandardWatchEventKinds.ENTRY_CREATE);

			for (;;)
			{

				// wait for key to be signaled
				try
				{
					key = watcher.take();
				}
				catch (InterruptedException x)
				{
					Logs.log("Auto-updater interrupted.", this);
					watcher.close();
					key = null;
					return;
				}
				catch (ClosedWatchServiceException c)
				{
					Logs.log("Auto-updater interrupted.", this);
					key = null;
					return;
				}

				for (WatchEvent<?> event: key.pollEvents())
				{
					WatchEvent.Kind<?> kind = event.kind();

					// This key is registered only
					// for ENTRY_CREATE events,
					// but an OVERFLOW event can
					// occur regardless if events
					// are lost or discarded.
					if (kind == StandardWatchEventKinds.OVERFLOW) {
						continue;
					}

					// The filename is the
					// context of the event.
					WatchEvent<Path> ev = (WatchEvent<Path>)event;
					Path filename = ev.context();
					Logs.log("Found new file :" + filename, this);

					// Compare the provided DT with the number of files and see
					// if another timepoint worth of files as been created.
					this.parent.runUpdate();
				}

				// Reset the key -- this step is critical if you want to
				// receive further watch events.  If the key is no longer valid,
				// the directory is inaccessible so exit the loop.
				boolean valid = key.reset();
				if (!valid)
				{
					break;
				}
			}
		}
		catch (IOException x)
		{
			x.printStackTrace();
		}
	}

}
