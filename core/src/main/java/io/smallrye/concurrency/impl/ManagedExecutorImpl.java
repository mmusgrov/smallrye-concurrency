package io.smallrye.concurrency.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import org.eclipse.microprofile.concurrent.ManagedExecutor;

public class ManagedExecutorImpl extends ThreadPoolExecutor implements ManagedExecutor {

    private final ThreadContextImpl threadContext;
    private final int maxAsync;
    private final int maxQueued;
    private final String injectionPointName;

    public ManagedExecutorImpl(int maxAsync, int maxQueued, ThreadContextImpl threadContext) {
        this(maxAsync, maxQueued, threadContext, null);
    }

    public ManagedExecutorImpl(int maxAsync, int maxQueued, ThreadContextImpl threadContext, String injectionPointName) {
        super(maxAsync == -1 ? Runtime.getRuntime().availableProcessors() : maxAsync,
                maxAsync == -1 ? Runtime.getRuntime().availableProcessors() : maxAsync, 5000l, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(maxQueued == -1 ? Integer.MAX_VALUE : maxQueued),
                new ThreadPoolExecutor.AbortPolicy());
        // we set core thread == max threads but allow for core thread timeout
        // this prevents delaying spawning of new thread to when the queue is full
        this.allowCoreThreadTimeOut(true);
        this.threadContext = threadContext;
        this.maxAsync = maxAsync;
        this.maxQueued = maxQueued;
        this.injectionPointName = injectionPointName;
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return super.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return super.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return super.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return super.awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return super.submit(threadContext.contextualCallableUnlessContextualized(task));
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return super.submit(threadContext.contextualRunnableUnlessContextualized(task), result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return super.submit(threadContext.contextualRunnableUnlessContextualized(task));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        List<Callable<T>> wrappedTasks = new ArrayList<>(tasks.size());
        for (Callable<T> task : tasks) {
            wrappedTasks.add(threadContext.contextualCallableUnlessContextualized(task));
        }
        return super.invokeAll(wrappedTasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException {
        List<Callable<T>> wrappedTasks = new ArrayList<>(tasks.size());
        for (Callable<T> task : tasks) {
            wrappedTasks.add(threadContext.contextualCallableUnlessContextualized(task));
        }
        return super.invokeAll(wrappedTasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        List<Callable<T>> wrappedTasks = new ArrayList<>(tasks.size());
        for (Callable<T> task : tasks) {
            wrappedTasks.add(threadContext.contextualCallableUnlessContextualized(task));
        }
        return super.invokeAny(wrappedTasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        List<Callable<T>> wrappedTasks = new ArrayList<>(tasks.size());
        for (Callable<T> task : tasks) {
            wrappedTasks.add(threadContext.contextualCallableUnlessContextualized(task));
        }
        return super.invokeAny(wrappedTasks, timeout, unit);
    }

    @Override
    public void execute(Runnable command) {
        super.execute(threadContext.contextualRunnableUnlessContextualized(command));
    }

    @Override
    public <U> CompletableFuture<U> completedFuture(U value) {
        return threadContext.withContextCapture(CompletableFuture.completedFuture(value), this);
    }

    @Override
    public <U> CompletionStage<U> completedStage(U value) {
        return completedFuture(value);
    }

    @Override
    public <U> CompletableFuture<U> failedFuture(Throwable ex) {
        CompletableFuture<U> ret = new CompletableFuture<>();
        ret.completeExceptionally(ex);
        return threadContext.withContextCapture(ret, this);
    }

    @Override
    public <U> CompletionStage<U> failedStage(Throwable ex) {
        return failedFuture(ex);
    }

    @Override
    public CompletableFuture<Void> runAsync(Runnable runnable) {
        // I don't need to wrap runnable because this executor will be used to submit
        // the task immediately with
        // a Runnable that will capture the context before calling my Runnable.run
        return threadContext.withContextCapture(CompletableFuture.runAsync(runnable, this), this);
    }

    @Override
    public <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier) {
        // I don't need to wrap supplier because this executor will be used to submit
        // the task immediately with
        // a Runnable that will capture the context before calling my Supplier.run
        return threadContext.withContextCapture(CompletableFuture.supplyAsync(supplier, this), this);
    }

    @Override
    public <U> CompletableFuture<U> newIncompleteFuture() {
        CompletableFuture<U> ret = new CompletableFuture<>();
        return threadContext.withContextCapture(ret, this);
    }

    @Override
    public String toString() {
        final String DELIMITER = ", ";
        StringBuilder builder = new StringBuilder();
        builder.append(ManagedExecutorImpl.class.getName()).append(DELIMITER);
        builder.append("with maxAsync: ").append(maxAsync).append(DELIMITER);
        builder.append("with maxQueued: ").append(maxQueued).append(DELIMITER);
        builder.append("with cleared contexts: ").append(threadContext.getPlan().clearedProviders).append(DELIMITER);
        builder.append("with propagated contexts: ").append(threadContext.getPlan().propagatedProviders).append(DELIMITER);
        builder.append("with unchanged contexts: ").append(threadContext.getPlan().unchangedProviders);
        if (injectionPointName != null) {
            builder.append(DELIMITER).append(" with injection point name: ").append(injectionPointName);
        }
        return builder.toString();
    }

}
