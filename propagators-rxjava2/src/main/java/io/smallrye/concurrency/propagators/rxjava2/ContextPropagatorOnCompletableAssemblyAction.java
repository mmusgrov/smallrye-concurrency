package io.smallrye.concurrency.propagators.rxjava2;

import java.util.concurrent.Executor;

import org.eclipse.microprofile.concurrent.ThreadContext;

import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.functions.Function;

public class ContextPropagatorOnCompletableAssemblyAction implements Function<Completable, Completable> {

    private ThreadContext threadContext;

    public ContextPropagatorOnCompletableAssemblyAction(ThreadContext threadContext) {
        this.threadContext = threadContext;
    }

    @Override
    public Completable apply(Completable t) throws Exception {
        return new ContextPropagatorCompletable(t, threadContext.currentContextExecutor());
    }

    public class ContextPropagatorCompletable extends Completable {

        private Completable source;

        private Executor contextExecutor;

        public ContextPropagatorCompletable(Completable t, Executor contextExecutor) {
            this.source = t;
            this.contextExecutor = contextExecutor;
        }

        @Override
        protected void subscribeActual(CompletableObserver observer) {
            contextExecutor.execute(() -> source.subscribe(observer));
        }

    }

}
