package org.opendedup.util;

import java.util.concurrent.BlockingQueue;

import java.util.concurrent.locks.ReentrantLock;


import org.opendedup.collections.QuickList;
import org.opendedup.logging.SDFSLogger;
import org.opendedup.sdfs.io.WritableCacheBuffer;

public class PoolThread extends Thread {

	private BlockingQueue<WritableCacheBuffer> taskQueue = null;
	private boolean isStopped = false;
	private final QuickList<WritableCacheBuffer> tasks = new QuickList<WritableCacheBuffer>(
			120);

	public PoolThread(BlockingQueue<WritableCacheBuffer> queue) {
		taskQueue = queue;
	}

	@Override
	public void run() {
		while (!isStopped()) {
			try {
				tasks.clear();
				int ts = taskQueue.drainTo(tasks, 80);
				for (int i = 0; i < ts; i++) {
					WritableCacheBuffer runnable = tasks.get(i);
					try {
						runnable.close();
					} catch (Exception e) {
						SDFSLogger.getLog()
								.fatal("unable to execute thread", e);
					}
				}
				Thread.sleep(2);
			} catch (Exception e) {
				SDFSLogger.getLog().fatal("unable to execute thread", e);
			}
		}
	}

	private ReentrantLock exitLock = new ReentrantLock();

	public void exit() {
		exitLock.lock();
		try {
			isStopped = true;
			this.interrupt(); // break pool thread out of dequeue() call.
		} finally {
			exitLock.unlock();
		}
	}

	public boolean isStopped() {
		return isStopped;
	}
}