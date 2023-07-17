/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.shulie.instrument.simulator.core.ignore;

import com.shulie.instrument.simulator.api.ignore.IgnoreAllow;
import com.shulie.instrument.simulator.api.ignore.IgnoredTypesBuilder;
import com.shulie.instrument.simulator.api.ignore.IgnoredTypesPredicate;
import com.shulie.instrument.simulator.api.ignore.Trie;

public class IgnoredTypesBuilderImpl implements IgnoredTypesBuilder {

    private static final Trie.Builder<IgnoreAllow> ignoredTypesTrieBuilder = Trie.builder();
    private static final Trie.Builder<IgnoreAllow> ignoredClassLoadersTrieBuilder = Trie.builder();

    private static Trie<IgnoreAllow> ignoredTypesTrie;
    private static Trie<IgnoreAllow> ignoredClassLoadersTrie;

    private boolean isConfigurerFrozen;

    @Override
    public synchronized IgnoredTypesBuilder ignoreClass(String classNameOrPrefix) {
        checkConfigEnable();
        ignoredTypesTrieBuilder.put(classNameOrPrefix.replace('.', '/'), IgnoreAllow.IGNORE);
        return this;
    }

    @Override
    public synchronized IgnoredTypesBuilder allowClass(String classNameOrPrefix) {
        checkConfigEnable();
        ignoredTypesTrieBuilder.put(classNameOrPrefix.replace('.', '/'), IgnoreAllow.ALLOW);
        return this;
    }


    @Override
    public IgnoredTypesBuilder ignoreClassLoader(String classNameOrPrefix) {
        checkConfigEnable();
        ignoredClassLoadersTrieBuilder.put(classNameOrPrefix.replace('.', '/'), IgnoreAllow.IGNORE);
        return this;
    }

    @Override
    public IgnoredTypesBuilder allowClassLoader(String classNameOrPrefix) {
        checkConfigEnable();
        ignoredClassLoadersTrieBuilder.put(classNameOrPrefix.replace('.', '/'), IgnoreAllow.ALLOW);
        return this;
    }

    @Override
    public void freezeConfigurer() {
        ignoredTypesTrie = ignoredTypesTrieBuilder.build();
        ignoredClassLoadersTrie = ignoredClassLoadersTrieBuilder.build();
        isConfigurerFrozen = true;
    }

    @Override
    public Trie<IgnoreAllow> buildIgnoredClassloaderTrie() {
        return ignoredClassLoadersTrie;
    }

    @Override
    public Trie<IgnoreAllow> buildIgnoredTypesTrie() {
        return ignoredTypesTrie;
    }

    @Override
    public boolean isConfigurerFrozen() {
        return isConfigurerFrozen;
    }

    @Override
    public IgnoredTypesPredicate buildTransformIgnoredFilter() {
        return new IgnoredTypesPredicateImpl(this);
    }

    private void checkConfigEnable() {
        if (isConfigurerFrozen) {
            throw new IllegalStateException("[instrument-simulator]: ignore types/classloaders configurer is frozen!!!");
        }
    }
}
