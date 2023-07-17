/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.shulie.instrument.simulator.api.ignore;

/**
 * This interface defines different ways to ignore/allow instrumenting classes or packages.
 *
 * <p>This interface should not be implemented by the javaagent extension developer - the javaagent
 * will provide the implementation.
 */
public interface IgnoredTypesBuilder {

    /**
     * Ignore the class or package specified by {@code classNameOrPrefix} and exclude it from being
     * instrumented. Calling this will overwrite any previous settings for passed prefix.
     *
     * <p>{@code classNameOrPrefix} can be the full class name (ex. {@code com.example.MyClass}),
     * package name (ex. {@code com.example.mypackage.}), or outer class name (ex {@code
     * com.example.OuterClass$})
     *
     * @return {@code this}
     */
    IgnoredTypesBuilder ignoreClass(String classNameOrPrefix);

    /**
     * Allow the class or package specified by {@code classNameOrPrefix} to be instrumented. Calling
     * this will overwrite any previous settings for passed prefix; in particular, calling this method
     * will override any previous {@link #ignoreClass(String)} setting.
     *
     * <p>{@code classNameOrPrefix} can be the full class name (ex. {@code com.example.MyClass}),
     * package name (ex. {@code com.example.mypackage.}), or outer class name (ex {@code
     * com.example.OuterClass$})
     *
     * @return {@code this}
     */
    IgnoredTypesBuilder allowClass(String classNameOrPrefix);

    /**
     * Ignore the class loader specified by {@code classNameOrPrefix} and exclude it from being
     * instrumented. Calling this will overwrite any previous settings for passed prefix.
     *
     * <p>{@code classNameOrPrefix} can be the full class name (ex. {@code com.example.MyClass}),
     * package name (ex. {@code com.example.mypackage.}), or outer class name (ex {@code
     * com.example.OuterClass$})
     *
     * @return {@code this}
     */
    IgnoredTypesBuilder ignoreClassLoader(String classNameOrPrefix);

    /**
     * Allow the class loader specified by {@code classNameOrPrefix} to be instrumented. Calling this
     * will overwrite any previous settings for passed prefix; in particular, calling this method will
     * override any previous {@link #ignoreClassLoader(String)} setting.
     *
     * <p>{@code classNameOrPrefix} can be the full class name (ex. {@code com.example.MyClass}),
     * package name (ex. {@code com.example.mypackage.}), or outer class name (ex {@code
     * com.example.OuterClass$})
     *
     * @return {@code this}
     */
    IgnoredTypesBuilder allowClassLoader(String classNameOrPrefix);


    /**
     * 冻结config
     *
     * @return
     */
    void freezeConfigurer();

    /**
     * 构建types trie
     *
     * @return
     */
    Trie<IgnoreAllow> buildIgnoredTypesTrie();

    /**
     * 构建classloaders trie
     *
     * @return
     */
    Trie<IgnoreAllow> buildIgnoredClassloaderTrie();

    /**
     * 是否冻结配置,即模块都是否加载完成
     *
     * @return
     */
    boolean isConfigurerFrozen();

    /**
     * 构建transform class filter
     *
     * @return
     */
    IgnoredTypesPredicate buildTransformIgnoredFilter();


}
