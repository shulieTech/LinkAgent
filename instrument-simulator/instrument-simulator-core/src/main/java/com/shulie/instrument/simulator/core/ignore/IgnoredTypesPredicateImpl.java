package com.shulie.instrument.simulator.core.ignore;

import com.shulie.instrument.simulator.api.ignore.IgnoreAllow;
import com.shulie.instrument.simulator.api.ignore.IgnoredTypesBuilder;
import com.shulie.instrument.simulator.api.ignore.IgnoredTypesPredicate;
import com.shulie.instrument.simulator.api.ignore.Trie;

public class IgnoredTypesPredicateImpl implements IgnoredTypesPredicate {

    private IgnoredTypesBuilder typesBuilder;
    private Trie<IgnoreAllow> ignoredTypesTrie;
    private Trie<IgnoreAllow> ignoredClassLoadersTrie;

    private final ThreadLocal<String> latestClassName = new ThreadLocal<String>();
    private final ThreadLocal<ClassLoader> latestClassLoader = new ThreadLocal<ClassLoader>();
    private final ThreadLocal<Boolean> typesMatched = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return true;
        }
    };

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

        // 缓存上一次执行结果，加速匹配过程
        if (internalClassName.equals(latestClassName.get()) && loader == latestClassLoader.get()) {
            return typesMatched.get();
        }

        boolean matched = true;
        if (loader != null && ignoredClassLoadersTrie.getOrNull(loader.getClass().getName()) == IgnoreAllow.IGNORE) {
            matched = false;
        }
        if (internalClassName != null && ignoredTypesTrie.getOrNull(internalClassName) == IgnoreAllow.IGNORE) {
            matched = false;
        }

        latestClassName.set(internalClassName);
        latestClassLoader.set(loader);
        typesMatched.set(matched);

        return matched;
    }
}
