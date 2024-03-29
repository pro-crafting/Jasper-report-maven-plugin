package com.pro_crafting.tools.jasperreport;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread factory the compile threads. Sets the thread name and marks it as a daemon thread.
 */
public class JasperReporterThreadFactory implements ThreadFactory {

    static final String THREAD_PREFIX = "jasper-compiler-";

    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger();

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r, THREAD_PREFIX + THREAD_COUNTER.incrementAndGet());
        thread.setDaemon(true);
        return thread;
    }

}
