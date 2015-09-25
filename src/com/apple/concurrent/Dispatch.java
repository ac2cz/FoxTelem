package com.apple.concurrent;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/** @since Java for Mac OS X 10.6 Update 2 */
public final class Dispatch {

    public static enum Priority {HIGH, LOW, NORMAL}

    public ExecutorService createExecutor(String label) {return null;}
    public Executor getAsyncExecutor(Priority priority) {return null;}
    public Executor getBlockingMainQueueExecutor() {return null;}
    public Executor getNonBlockingMainQueueExecutor() {return null;}
    public static Dispatch getInstance() {return null;}
}
