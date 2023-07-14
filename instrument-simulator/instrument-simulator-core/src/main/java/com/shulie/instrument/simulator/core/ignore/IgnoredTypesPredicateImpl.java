package com.shulie.instrument.simulator.core.ignore;

import com.shulie.instrument.simulator.api.ignore.IgnoreAllow;
import com.shulie.instrument.simulator.api.ignore.IgnoredTypesBuilder;
import com.shulie.instrument.simulator.api.ignore.IgnoredTypesPredicate;
import com.shulie.instrument.simulator.api.ignore.Trie;

public class IgnoredTypesPredicateImpl implements IgnoredTypesPredicate {

    private IgnoredTypesBuilder typesBuilder;
    private Trie<IgnoreAllow> ignoredTypesTrie;
    private Trie<IgnoreAllow> ignoredClassLoadersTrie;

    public IgnoredTypesPredicateImpl(IgnoredTypesBuilder typesBuilder) {
        this.typesBuilder = typesBuilder;
    }

    @Override
    public boolean test(ClassLoader loader, String internalClassName) {
        if (!typesBuilder.isConfigurerFrozen()) {
            return true;
        }
        if (ignoredTypesTrie == null || ignoredClassLoadersTrie == null) {
            ignoredTypesTrie = typesBuilder.buildIgnoredTypesTrie();
            ignoredClassLoadersTrie = typesBuilder.buildIgnoredClassloaderTrie();
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
