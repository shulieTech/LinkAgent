package com.pamirs.pradar;

/**
 * @author Licey
 * @date 2022/8/3
 */
public interface IBizClassLoaderService {

    void setBizClassLoader(ClassLoader classLoader);

    void clearBizClassLoader();

}
