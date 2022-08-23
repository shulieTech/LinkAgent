package com.pamirs.pradar;

/**
 * @author Licey
 * @date 2022/8/3
 */
public class BizClassLoaderService {

    private static IBizClassLoaderService service;

    public static void register(IBizClassLoaderService service){
        BizClassLoaderService.service = service;
    }

    public static void setBizClassLoader(ClassLoader classLoader) {
        service.setBizClassLoader(classLoader);
    }

    public static void clearBizClassLoader() {
        service.clearBizClassLoader();
    }

}
