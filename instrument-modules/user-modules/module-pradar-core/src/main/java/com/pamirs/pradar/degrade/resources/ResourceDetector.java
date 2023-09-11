package com.pamirs.pradar.degrade.resources;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2022/10/18 11:09 AM
 */
public interface ResourceDetector {

    boolean hasResource();

    String name();

    double threshold();

    String configName();

    void refreshThreshold();
}
