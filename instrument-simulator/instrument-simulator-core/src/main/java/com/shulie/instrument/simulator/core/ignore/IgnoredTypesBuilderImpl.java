/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.shulie.instrument.simulator.core.ignore;

import com.shulie.instrument.simulator.api.ignore.IgnoreAllow;
import com.shulie.instrument.simulator.api.ignore.IgnoredTypesBuilder;
import com.shulie.instrument.simulator.api.ignore.IgnoredTypesPredicate;
import com.shulie.instrument.simulator.api.ignore.Trie;
import com.shulie.instrument.simulator.api.resource.SimulatorConfig;

public class IgnoredTypesBuilderImpl implements IgnoredTypesBuilder {

    private static final Trie.Builder<IgnoreAllow> ignoredTypesTrieBuilder = Trie.builder();
    private static final Trie.Builder<IgnoreAllow> ignoredClassLoadersTrieBuilder = Trie.builder();

    private static Trie<IgnoreAllow> ignoredTypesTrie;
    private static Trie<IgnoreAllow> ignoredClassLoadersTrie;

    @Override
    public IgnoredTypesBuilder ignoreClass(String classNameOrPrefix) {
        ignoredTypesTrieBuilder.put(classNameOrPrefix, IgnoreAllow.IGNORE);
        ignoredTypesTrieBuilder.put(classNameOrPrefix.replace('.', '/'), IgnoreAllow.IGNORE);
        return this;
    }

    @Override
    public IgnoredTypesBuilder allowClass(String classNameOrPrefix) {
        ignoredTypesTrieBuilder.put(classNameOrPrefix, IgnoreAllow.ALLOW);
        ignoredTypesTrieBuilder.put(classNameOrPrefix.replace('.', '/'), IgnoreAllow.ALLOW);
        return this;
    }


    @Override
    public IgnoredTypesBuilder ignoreClassLoader(String classNameOrPrefix) {
        ignoredClassLoadersTrieBuilder.put(classNameOrPrefix, IgnoreAllow.IGNORE);
        ignoredClassLoadersTrieBuilder.put(classNameOrPrefix.replace('.', '/'), IgnoreAllow.IGNORE);
        return this;
    }

    @Override
    public IgnoredTypesBuilder allowClassLoader(String classNameOrPrefix) {
        ignoredClassLoadersTrieBuilder.put(classNameOrPrefix, IgnoreAllow.ALLOW);
        ignoredClassLoadersTrieBuilder.put(classNameOrPrefix.replace('.', '/'), IgnoreAllow.ALLOW);
        return this;
    }

    @Override
    public Trie<IgnoreAllow> buildIgnoredClassloaderTrie() {
        if (ignoredClassLoadersTrie == null) {
            ignoredClassLoadersTrie = ignoredClassLoadersTrieBuilder.build();
        }
        return ignoredClassLoadersTrie;
    }

    @Override
    public Trie<IgnoreAllow> buildIgnoredTypesTrie() {
        if (ignoredTypesTrie == null) {
            ignoredTypesTrie = ignoredTypesTrieBuilder.build();
        }
        return ignoredTypesTrie;
    }


    @Override
    public IgnoredTypesPredicate buildTransformIgnoredPredicate() {
        return new IgnoredTypesPredicateImpl(buildIgnoredTypesTrie(), buildIgnoredClassloaderTrie());
    }

}
