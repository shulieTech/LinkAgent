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
package com.pamirs.attach.plugin.neo4j.config;

import org.neo4j.ogm.MetaData;
import org.neo4j.ogm.driver.Driver;
import org.neo4j.ogm.session.Neo4jSession;

/**
 * @ClassName: Neo4JSessionExt
 * @author: wangjian
 * @Date: 2020/7/31 16:21
 * @Description:
 */
public class Neo4JSessionExt extends Neo4jSession {

    private Driver driver;

    public Neo4JSessionExt(MetaData metaData, Driver driver) {
        super(metaData, driver);
        this.driver = driver;
    }

    public Driver getDriver() {
        return driver;
    }

}
