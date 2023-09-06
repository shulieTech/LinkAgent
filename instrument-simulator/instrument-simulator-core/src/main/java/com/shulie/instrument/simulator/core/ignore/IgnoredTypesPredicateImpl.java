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


    private static ClassLoader nullClassloader = new ClassLoader() {
        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            return super.loadClass(name);
        }

        @Override
        public int hashCode() {
            return 1;
        }
    };

    private static HashBasedTable<ClassLoader, String, IgnoreAllow> ignoreCaches = HashBasedTable.create(256, 2 << 13);


    public IgnoredTypesPredicateImpl(Trie<IgnoreAllow> ignoredTypesTrie, Trie<IgnoreAllow> ignoredClassLoadersTrie) {
        this.ignoredTypesTrie = ignoredTypesTrie;
        this.ignoredClassLoadersTrie = ignoredClassLoadersTrie;
    }

    @Override
    public boolean test(ClassLoader loader, String internalClassName) {
        loader = loader == null ? nullClassloader : loader;
        IgnoreAllow ignoreAllow = ignoreCaches.get(loader, internalClassName);
        if (ignoreAllow != null) {
            return ignoreAllow == IgnoreAllow.ALLOW;
        }
        boolean allow = true;
        if (ignoredClassLoadersTrie.getOrNull(loader.getClass().getName()) == IgnoreAllow.IGNORE) {
            allow = false;
        }
        if (allow && (ignoredTypesTrie.getOrNull(internalClassName) == IgnoreAllow.IGNORE)) {
            allow = false;
        }
        ignoreCaches.put(loader, internalClassName, allow ? IgnoreAllow.ALLOW : IgnoreAllow.IGNORE);
        return allow;
    }

    public static void clearIgnoredTypesCache() {
        ignoreCaches.clear();
    }

}
