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
package com.pamirs.attach.plugin.hbase.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * @author HyunGil Jeong
 */
public final class SocketAddressUtils {


    // TODO JDK 7 InetSocketAddress.getHostString() - reflection not needed once we drop JDK 6 support.
    private static HostStringAccessor HOST_STRING_ACCESSOR;

    private static HostStringAccessor getHostStringAccessor() {
        if (HOST_STRING_ACCESSOR == null) {
            synchronized (SocketAddressUtils.class) {
                if (HOST_STRING_ACCESSOR == null) {
                    try {
                        final Method m = InetSocketAddress.class.getDeclaredMethod("getHostString");
                        m.setAccessible(true);
                        HOST_STRING_ACCESSOR = new ReflectiveHostStringAccessor(m);
                    } catch (NoSuchMethodException e) {
                        throw new IllegalStateException("Unexpected InetSocketAddress class", e);
                    }
                }
            }
        }
        return HOST_STRING_ACCESSOR;
    }

    public static void release() {
        HOST_STRING_ACCESSOR = null;
    }

    private interface HostStringAccessor {
        String getHostString(InetSocketAddress inetSocketAddress);
    }

    private static class ReflectiveHostStringAccessor implements HostStringAccessor {


        private final Method method;

        private ReflectiveHostStringAccessor(Method method) {
            if (method == null) {
                throw new NullPointerException("method must not be null");
            }
            this.method = method;
        }

        @Override
        public String getHostString(InetSocketAddress inetSocketAddress) {
            try {
                return (String) method.invoke(inetSocketAddress);
            } catch (IllegalAccessException e) {
            } catch (InvocationTargetException e) {
            } catch (Exception e) {
            }
            return null;
        }
    }

    private SocketAddressUtils() {
    }

    /**
     * Returns the hostname of the given {@link InetSocketAddress}, or the String form of the IP address
     * if it does not have one. Returns {@code null} if neither can be retrieved.
     *
     * <p> This <b>does not</b> trigger a reverse DNS lookup if was created using an address and the hostname
     * is not yet available.
     *
     * @param inetSocketAddress the com.pamirs.pradar.rds.socket address in which to retrieve the hostname from
     * @return the hostname or the String representation of the address, or {@code null} if neither
     * can be found.
     * @see InetSocketAddress#getHostString()
     */
    public static String getHostNameFirst(InetSocketAddress inetSocketAddress) {
        if (inetSocketAddress == null) {
            return null;
        }
        return getHostStringAccessor().getHostString(inetSocketAddress);
    }

    /**
     * Returns the hostname of the given {@link InetSocketAddress}, or the String form of the IP address
     * if it does not have one. Returns {@code defaultHostName} if neither can be retrieved.
     *
     * <p>This <b>does not</b> trigger a reverse DNS lookup if was created using an address and the hostname
     * is not yet available.
     *
     * @param inetSocketAddress the com.pamirs.pradar.rds.socket address in which to retrieve the hostname from
     * @param defaultHostName   the value to return if neither the hostname nor the string form of the
     *                          address can be found
     * @return the hostname or the String representation of the address, or the {@code defaultHostName}
     * if neither can be found.
     * @see InetSocketAddress#getHostString()
     */
    public static String getHostNameFirst(InetSocketAddress inetSocketAddress, String defaultHostName) {
        String hostName = getHostNameFirst(inetSocketAddress);
        if (hostName == null) {
            return defaultHostName;
        }
        return hostName;
    }

    /**
     * Returns the String form of the IP address of the given {@link InetSocketAddress}, or the hostname
     * if it has not been resolved. Returns {@code null} if neither can be retrieved.
     *
     * <p>This <b>does not</b> trigger a DNS lookup if the address has not been resolved yet by simply
     * returning the hostname.
     *
     * @param inetSocketAddress the com.pamirs.pradar.rds.socket address in which to retrieve the address from
     * @return the String representation of the address or the hostname, or {@code null} if neither
     * can be found.
     * @see InetSocketAddress#isUnresolved()
     */
    public static String getAddressFirst(InetSocketAddress inetSocketAddress) {
        if (inetSocketAddress == null) {
            return null;
        }
        InetAddress inetAddress = inetSocketAddress.getAddress();
        if (inetAddress != null) {
            return inetAddress.getHostAddress();
        }
        // This won't trigger a DNS lookup as if it got to here, the hostName should not be null on the basis
        // that InetSocketAddress does not allow both address and hostName fields to be null.
        return inetSocketAddress.getHostName();
    }

    /**
     * Returns the String form of the IP address of the given {@link InetSocketAddress}, or the hostname
     * if it has not been resolved. Returns {@code defaultAddress} if neither can be retrieved.
     *
     * <p>This <b>does not</b> trigger a DNS lookup if the address has not been resolved yet by simply
     * returning the hostname.
     *
     * @param inetSocketAddress the com.pamirs.pradar.rds.socket address in which to retrieve the address from
     * @param defaultAddress    the value to return if neither the string form of the address nor the
     *                          hostname can be found
     * @return the String representation of the address or the hostname, or {@code defaultAddress} if neither
     * can be found.
     * @see InetSocketAddress#isUnresolved()
     */
    public static String getAddressFirst(InetSocketAddress inetSocketAddress, String defaultAddress) {
        String address = getAddressFirst(inetSocketAddress);
        if (address == null) {
            return defaultAddress;
        }
        return address;
    }
}
