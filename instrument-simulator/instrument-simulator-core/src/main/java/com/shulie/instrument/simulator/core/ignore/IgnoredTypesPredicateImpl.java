package com.shulie.instrument.simulator.core.ignore;

import com.google.common.collect.HashBasedTable;
import com.shulie.instrument.simulator.api.ignore.IgnoreAllow;
import com.shulie.instrument.simulator.api.ignore.IgnoredTypesBuilder;
import com.shulie.instrument.simulator.api.ignore.IgnoredTypesPredicate;
import com.shulie.instrument.simulator.api.ignore.Trie;

public class IgnoredTypesPredicateImpl implements IgnoredTypesPredicate {

    private IgnoredTypesBuilder typesBuilder;
    private static Trie<IgnoreAllow> ignoredTypesTrie;
    private static Trie<IgnoreAllow> ignoredClassLoadersTrie;

    /**
     * 探针启动期间和启动完成后用的是不同的trie
     * 所有模块加载完成后需要刷新trie
     */
    private static boolean refreshed;

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

    static {
        IgnoredTypesBuilder builder = new IgnoredTypesBuilderImpl();
        new InstrumentSimulatorIgnoredTypesConfigurer(null).configure(builder);
        builder.freezeConfigurer();
        ignoredTypesTrie = builder.buildIgnoredTypesTrie();
        ignoredClassLoadersTrie = builder.buildIgnoredClassloaderTrie();
    }

    public IgnoredTypesPredicateImpl(IgnoredTypesBuilder typesBuilder) {
        this.typesBuilder = typesBuilder;
    }

    @Override
    public boolean test(ClassLoader loader, String internalClassName) {
        if (typesBuilder.isConfigurerFrozen() && !refreshed) {
            ignoredTypesTrie = typesBuilder.buildIgnoredTypesTrie();
            ignoredClassLoadersTrie = typesBuilder.buildIgnoredClassloaderTrie();
            refreshed = true;
        }
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
