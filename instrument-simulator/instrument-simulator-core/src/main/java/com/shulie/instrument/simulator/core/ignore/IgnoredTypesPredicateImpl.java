package com.shulie.instrument.simulator.core.ignore;

import com.google.common.collect.HashBasedTable;
import com.shulie.instrument.simulator.api.ignore.IgnoreAllow;
import com.shulie.instrument.simulator.api.ignore.IgnoredTypesBuilder;
import com.shulie.instrument.simulator.api.ignore.IgnoredTypesPredicate;
import com.shulie.instrument.simulator.api.ignore.Trie;
import com.shulie.instrument.simulator.api.resource.SimulatorConfig;

public class IgnoredTypesPredicateImpl implements IgnoredTypesPredicate {

    private Trie<IgnoreAllow> ignoredTypesTrie;
    private Trie<IgnoreAllow> ignoredClassLoadersTrie;

    private static HashBasedTable<String, String, IgnoreAllow> ignoreCaches = HashBasedTable.create(256, 2 << 13);

    public IgnoredTypesPredicateImpl(Trie<IgnoreAllow> ignoredTypesTrie, Trie<IgnoreAllow> ignoredClassLoadersTrie) {
        this.ignoredTypesTrie = ignoredTypesTrie;
        this.ignoredClassLoadersTrie = ignoredClassLoadersTrie;
    }

    @Override
    public boolean test(ClassLoader loader, String internalClassName) {
        String classLoaderKey = loader == null ? "bootstrapClassLoader" : loader.getClass().getName();
        IgnoreAllow ignoreAllow = ignoreCaches.get(classLoaderKey, internalClassName);
        if (ignoreAllow != null) {
            return ignoreAllow == IgnoreAllow.ALLOW;
        }
        boolean allow = true;
        if (ignoredClassLoadersTrie.getOrNull(classLoaderKey) == IgnoreAllow.IGNORE) {
            allow = false;
        }
        if (allow && (ignoredTypesTrie.getOrNull(internalClassName) == IgnoreAllow.IGNORE)) {
            allow = false;
        }
        ignoreCaches.put(classLoaderKey, internalClassName, allow ? IgnoreAllow.ALLOW : IgnoreAllow.IGNORE);
        return allow;
    }

    public static void clearIgnoredTypesCache() {
        ignoreCaches.clear();
    }

}
