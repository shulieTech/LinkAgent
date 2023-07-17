package com.shulie.instrument.simulator.core.ignore;

import com.shulie.instrument.simulator.api.ignore.*;

public class IgnoredTypesPredicateImpl implements IgnoredTypesPredicate {

    private IgnoredTypesBuilder typesBuilder;
    private static Trie<IgnoreAllow> ignoredTypesTrie;
    private static Trie<IgnoreAllow> ignoredClassLoadersTrie;

    static {
        IgnoredTypesBuilder builder = new IgnoredTypesBuilderImpl();
        new InstrumentSimulatorTypesConfigurer().configure(builder);
        builder.freezeConfigurer();
        ignoredTypesTrie = builder.buildIgnoredTypesTrie();
        ignoredClassLoadersTrie = builder.buildIgnoredClassloaderTrie();
    }

    public IgnoredTypesPredicateImpl(IgnoredTypesBuilder typesBuilder) {
        this.typesBuilder = typesBuilder;
    }

    @Override
    public boolean test(ClassLoader loader, String internalClassName) {
        if (typesBuilder.isConfigurerFrozen()) {
            if (ignoredTypesTrie == null || ignoredClassLoadersTrie == null) {
                ignoredTypesTrie = typesBuilder.buildIgnoredTypesTrie();
                ignoredClassLoadersTrie = typesBuilder.buildIgnoredClassloaderTrie();
            }
        }
        if (loader != null && ignoredClassLoadersTrie.getOrNull(loader.getClass().getName()) == IgnoreAllow.IGNORE) {
            return false;
        }
        if (internalClassName != null && ignoredTypesTrie.getOrNull(internalClassName) == IgnoreAllow.IGNORE) {
            return false;
        }
        return true;
    }

}
