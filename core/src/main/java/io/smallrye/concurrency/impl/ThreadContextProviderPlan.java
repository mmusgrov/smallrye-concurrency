package io.smallrye.concurrency.impl;

import java.util.Set;

import org.eclipse.microprofile.concurrent.spi.ThreadContextProvider;

public class ThreadContextProviderPlan {

    public final Set<ThreadContextProvider> propagatedProviders;
    public final Set<ThreadContextProvider> unchangedProviders;
    public final Set<ThreadContextProvider> clearedProviders;

    public ThreadContextProviderPlan(Set<ThreadContextProvider> propagatedSet, Set<ThreadContextProvider> unchangedSet,
            Set<ThreadContextProvider> clearedSet) {
        this.propagatedProviders = propagatedSet;
        this.unchangedProviders = unchangedSet;
        this.clearedProviders = clearedSet;
    }
}
