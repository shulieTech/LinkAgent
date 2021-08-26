/**
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.quartz.utils;

import java.io.Serializable;

/**
 * @Description
 * @Author xiaobin.zfb
 * @mail xiaobin@shulie.io
 * @Date 2020/7/23 8:23 下午
 */
public class Key<T> implements Serializable, Comparable<Key<T>> {

    private static final long serialVersionUID = -7141167957642391350L;

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *
     * Constructors.
     *
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * Construct a new key with the given name and group.
     *
     * @param name  the name
     * @param group the group
     */
    public Key(String name, String group) {
    }

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *
     * Interface.
     *
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * <p>
     * Get the name portion of the key.
     * </p>
     *
     * @return the name
     */
    public String getName() {
        return null;
    }

    /**
     * <p>
     * Get the group portion of the key.
     * </p>
     *
     * @return the group
     */
    public String getGroup() {
        return null;
    }

    /**
     * <p>
     * Return the string representation of the key. The format will be:
     * &lt;group&gt;.&lt;name&gt;.
     * </p>
     *
     * @return the string representation of the key
     */
    @Override
    public String toString() {
        return getGroup() + '.' + getName();
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        return true;
    }

    @Override
    public int compareTo(Key<T> o) {

        return 0;
    }

    public static String createUniqueName(String group) {
        return null;
    }
}
