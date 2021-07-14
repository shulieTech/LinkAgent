package com.pamirs.attach.plugin.common.datasource;

import com.shulie.instrument.simulator.api.ExtensionModule;
import com.shulie.instrument.simulator.api.ModuleInfo;
import org.kohsuke.MetaInfServices;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/10/11 7:17 下午
 */
@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = "datasource-common", version = "1.0.0", author = "xiaobin@shulie.io",description = "数据源通用依赖模块,提供给各个数据源模块依赖")
public class DatasourceCommonModule implements ExtensionModule {

}
