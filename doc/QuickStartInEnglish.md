# QuickStart

## Get AgentX package
Download installation package : 
- [download](http://xxxxx)
<br/>Or build your own package：
- see [HowToBuild](HowToBuild.md)

### 二、配置修改

#### agent.properties

- `simulator.zk.servers=127.0.0.1:2181` Zookeeper address configuration, change to your zookeeper address.
- `simulator.log.path=/log/pradar/`Output address of log.

agent.properties, Configuration item meaning

```
agent.use.local - agent 是否使用本地配置，agent 设计探针包支持从远程拉取加载，如果此选项为 true 时则默认从本地加载，不再从远程拉取探针包
agent.config.url - 配置当前 agent 加载内容配置的 url, 如当前 agent 加载的版本号，需要加载的模块列表，当 agent.use.local 配置为 false 时此值必填
agent.processlist.url - 上传扫描的所有进程列表结果的 url，这个是在 agent 使用独立进程模式时需要知道当前所有的 java 进程列表
agent.load.url - 探针包的下载url，只有当 agent.use.local 为 false 是必填
simulator.config.url - 获取探针配置项值的地址，如果所有的探针配置项全部移到远程时通过此配置项配置拉取配置的 url
register.name -  注册中心名称，默认为 zookeeper，目前还仅支持 zookeeper 的注册中心，暂时不用配置
simulator.client.zk.path - zookeeper 注册中心的注册路径，默认路径为/config/log/pradar/client,可以不用配置
simulator.zk.servers - zookeeper 注册中心地址，此值必须要配置
simulator.zk.connection.timeout.ms - zookeeper 注册中心连接超时时间，单位毫秒，默认为30000，可以不用配置
simulator.zk.session.timeout.ms - zookeeper 注册中心 session 超时时间,单位毫秒，默认为60000，可以不用配置
simulator.log.path - 所有的日志路径，包括探针的日志存储路径和 trace 日志的路径，此值必须要配置
simulator.log.level - 日志的级别，默认为 info，可以不用配置

agent.use.local -  Whether the agent uses local configuration, the agent supports remote pull and load, if this option is true, it will load locally by default, and no longer pull the agent package from remote.
agent.config.url - Configure the url of the current agent loading content configuration, such as the version number of the current agent loading, the list of modules that need to be loaded, this value is required when the agent.use.local configuration is false
agent.processlist.url - Upload the url of all scanned process list results. This is the list of all java processes that need to be known when the agent uses the independent process mode
agent.load.url - The download url of the probe package, it is required only when agent.use.local is false
simulator.config.url - The address to obtain the value of the probe configuration item, if all the probe configuration items are moved to the remote, the configuration url is pulled through this configuration item configuration
register.name - The name of the registration center, the default is zookeeper, currently only supports the zookeeper registration center, no configuration is needed for the time being
simulator.client.zk.path - The registration path of the zookeeper registration center, the default path is /config/log/pradar/client, no need to configure
simulator.zk.servers - Zookeeper registration center address, this value must be configured
simulator.zk.connection.timeout.ms - Zookeeper registration center connection timeout time, in milliseconds, the default is 30000, no need to configure
simulator.zk.session.timeout.ms - Zookeeper registration center session timeout time, in milliseconds, the default is 60000, no need to configure
simulator.log.path - all log paths, including probe log storage path and trace log path, this value must be configured
simulator.log.level - the level of the log, the default is info, you don’t need to configure it
```

#### simulator.properties

Modify `tro.web.url` only.

simulator.properties configuration meaning：

```
user_module - 用户模块的加载地址,（不需要改）
provider - provider模块的加载路径，（不需要配置）
server.ip - 指定开放管理接口的 ip，对应框架的命令 url 地址ip
server.port - 指定开放管理接口的端口，对应框架的命令 url 地址端口
server.charset - 指定开放管理接口返回响应的编码方式
unsafe.enable - 是否允许开启 unsafe ，影响的结果是是否可以允许增强 jdk 本身的类,默认开启(不要修改)
simulator.dump.bytecode - 是否开启 dump 字节码，这个参数只允许在调试中使用
simulator.dump.bytecode.path - dump 字节码的路径，只有simulator.dump.bytecode参数为 true 时才有作用
simulator.dump.verify - 是否开启字节码 dump 时的字节码校验，如果开启校验会在日志中打印出校验的结果
simulator.dump.asm - 是否开启dump asm
simulator.dump.class.list - dump class 的白名单，多个以逗号分隔，默认为空则会 dump 所有增强的 class
simulator.cost.dump - 是否输出每个增强拦截器的增强耗时，开启后会在日志中输出每一个增强的耗时
module.repository.mode - 模块仓库模式，默认是本地，这个保留配置（不需要指定）
module.remote.repository.addr - 远程扩展仓库地址,这个不需要配置，属性保留配置 (不需要指定)
tro.web.url - tro 控制台的地址，根本这个地址来拉取 agent的各种配置如数据源配置、白名单等等 (这个是一定要配置的)
pradar.charset - 配置指定输出的trace 等日志文件的编码格式,默认 UTF-8
pradar.data.pusher - 日志推送类型，默认为 tcp，目前也仅支持这一种类型
pradar.server.zk.path - 远程日志服务的zk 注册路径,zk 地址为 agent.properties 中配置的地址
pradar.data.pusher.timeout - 日志推送时的超时时间，默认3000ms
pradar.push.serialize.protocol.code - 推送日志的序列化方式，默认 2为 java，可选项有1：kryo 2:java
max.push.log.failure.sleep.interval - 日志连续推送失败时的最大休眠时长，单位毫秒，默认10000
pradar.trace.log.version - trace 日志版本号， 每个 agent 版本都会对应一个固定的版本号，这个不要改
pradar.monitor.log.version - monitor 日志版本号， 每个 agent 版本都会对应一个固定的版本号，这个不要改
pradar.metrics.log.version - metrics 日志版本号， 每个 agent 版本都会对应一个固定的版本号，这个不要改
pradar.switcher.trace - 是否开启 trace的开关，默认开启
pradar.switcher.press - 是否开启压测，默认开启，如果关闭则不接受任何压测流量，这个参数不需要设置，除非在一些只需要日志的场景
pradar.trace.max.file.size - 获取 trace 日志单个文件的大小，默认为200*1024*1024，即200M
pradar.monitor.max.file.size - 获取 monitor 日志单个文件的大小，默认为200*1024*1024，即200M
auto.create.queue.rabbitmq - rabbitmq 是否开启自动创建 queue，如果开启则业务创建 queue 时则会创建对应的影子 queue
user.app.key - 当前 agent 使用的 user 的 appKey，这个值是压测控制台用户的 user key，是必填的
sf-kafka-plugin-param - sf-kafka 插件里面的配置，后面需要等待恒宇补全定义
is.kafka.message.headers - 是否开启 kafka 头信息写入，因为可能会有客户端支持消息头但是服务端不支持，如果写入 kafka 消息头时则会报错，则此时需要将这个值关闭（false），默认为 true
is.rabbitmq.routingkey - 是否开启 rabbitmq 的 routingkey 模式, 开启时会根据 routingkey 来进行判断是否是压测流量，并且也会对 routingkey 进行影子标的修改
pradar.config.fetch.type - agent 配置值获取的方式，默认为 http (不需要修改)
pradar.config.fetch.interval - agent 配置获取的间隔时间,默认为60(可以根据需要进行修改)
pradar.config.fetch.unit - agent 配置获取的间隔时间单位，默认 SECONDS,具体值可以参考TimeUnit 的枚举定义
pradar.user.id - 压测控制台的用户 ID,这个需要与压测控制台确定当前用户的 ID 是什么
pradar.biz.log.divider - true/false 是否开启业务日志的正常流量与压测流量的日志分流,如果开启则压测流量产生的业务日志会输出到不同的业务日志文件中，默认为 false
pradar.biz.log.divider.path - 压测流量产生的业务日志输出的目录，如果 pradar.biz.log.divider配置为 true 而此参数没有配置，则默认会输出到 pradar 日志目录
pradar.trace.sampling.path - pradar trace 的全局采样率在 zookeeper 上的路径，默认为/config/log/trace/simpling
pradar.trace.app.sampling.path - pradar  trace 的应用采用率在 zookeeper 上的路径，默认为/config/log/trace/%s/simpling ,%s 会自动替换成当前应用的应用名称
pradar.perf.push.enabled - 是否开启性能分析数据推送，默认不推送
pradar.trace.queue.size - pradar trace 日志的队列大小，默认为1024
pradar.monitor.queue.size - monitor 日志的队列大小，默认为512
pradar.cluster.test.prefix - pradar 压测标前缀，默认为 PT_
pradar.cluster.test.prefix.rod - pradar压测标中前缀，默认为 PT-
pradar.cluster.test.suffix - pradar 压测标后缀，默认为_PT
pradar.cluster.test.suffix.rod - pradar 压测标中后缀，默认为-PT
plugin.request.size - trace 日志记录的参数长度，默认为5000
plugin.response.size - trace 日志记录的响应长度，默认为5000
plugin.request.on - trace 日志是否开启参数的记录，默认为 true
plugin.response.on - trace 日志是否开启响应的记录，默认为 true
plugin.exception.on - trace 日志是否开启异常记录，默认为 true
mongodb322.enabled - mongdb 3.2.2版本插件是否启用，默认为 false


user_module - The load address of the user module, (no need to change)
provider - The loading path of the provider module, (no configuration required)
server.ip - Specifies the ip of the open management interface, corresponding to the command url address ip of the framework
server.port - Specifies the port of the open management interface, corresponding to the command url address port of the framework
server.charset - Specify the encoding method of the response returned by the open management interface
unsafe.enable - Whether to allow unsafe to be turned on, the result of the impact is whether to allow enhancement of jdk classes, which is enabled by default (do not modify)
simulator.dump.bytecode - Whether to enable dump bytecode, this parameter is only allowed to be used in debugging
simulator.dump.bytecode.path-the path to dump the bytecode, it only works when the parameter of simulator.dump.bytecode is true
Dump.verify-whether to enable bytecode verification during bytecode dump, if verification is enabled, the verification result will be printed in the log
simulator.dump.asm-whether to enable dump asm
simulator.dump.class.list-whitelist of dump classes, separated by commas, the default is empty to dump all enhanced classes
simulator.cost.dump-Whether to output the enhancement time of each enhancement interceptor, after enabling it, the time consumption of each enhancement will be output in the log
module.repository.mode-module repository mode, the default is local, this reserved configuration (no need to specify)
module.remote.repository.addr-remote extension warehouse address, this does not need to be configured, the attribute reservation configuration (does not need to be specified)
tro.web.url-The address of the tro console. This address is used to pull various configurations of the agent, such as data source configuration, whitelist, etc. (this must be configured)
pradar.charset-configure the encoding format of the specified output trace and other log files, the default is UTF-8
pradar.data.pusher-log push type, the default is tcp, currently only this type is supported
pradar.server.zk.path-the zk registration path of the remote log service, the zk address is the address configured in agent.properties
pradar.data.pusher.timeout-the timeout period for log push, the default is 3000ms
pradar.push.serialize.protocol.code-the serialization method of the push log, the default 2 is java, and the options are 1: kryo 2: java
max.push.log.failure.sleep.interval-Maximum sleep duration when continuous log push fails, in milliseconds, default 10000
pradar.trace.log.version-trace log version number, each agent version will correspond to a fixed version number, do not change this
pradar.monitor.log.version-monitor log version number, each agent version will correspond to a fixed version number, do not change this
pradar.metrics.log.version-metrics log version number, each agent version will correspond to a fixed version number, don’t change this
pradar.switcher.trace-Whether to enable the trace switch, it is enabled by default
pradar.switcher.press-Whether to enable pressure measurement, it is enabled by default, if it is turned off, no pressure measurement flow will be accepted. This parameter does not need to be set, except in some scenarios where only logs are required
pradar.trace.max.file.size-Get the size of a single file of the trace log, the default is 200*1024*1024, which is 200M
pradar.monitor.max.file.size-Get the size of a single file of the monitor log, the default is 200*1024*1024, which is 200M
auto.create.queue.rabbitmq-whether rabbitmq enables automatic queue creation. If enabled, the corresponding shadow queue will be created when the business creates a queue
user.app.key-the appKey of the user currently used by the agent. This value is the user key of the stress test console user and is required
sf-kafka-plugin-param-The configuration in the sf-kafka plugin, you need to wait for Hengyu to complete the definition later
is.kafka.message.headers-whether to enable the writing of Kafka header information, because there may be clients that support the message header but the server does not support it. If an error is reported when writing the Kafka message header, you need to turn this value off at this time (False), the default is true
is.rabbitmq.routingkey-Whether to enable the routingkey mode of rabbitmq, when it is enabled, it will judge whether it is a pressure measurement flow according to the routingkey, and the routingkey will also be shadowed.
pradar.config.fetch.type-the way to get the agent configuration value, the default is http (no need to modify)
pradar.config.fetch.interval-the interval for fetching the agent configuration, the default is 60 (can be modified as needed)
pradar.config.fetch.unit-the interval time unit for agent configuration acquisition, the default SECONDS, the specific value can refer to the enumeration definition of TimeUnit
pradar.user.id-the user ID of the stress test console, this needs to be determined with the stress test console to determine what the current user ID is
pradar.biz.log.divider-true/false Whether to enable the normal flow of business logs and the log diversion of stress test traffic, if enabled, the business logs generated by stress test traffic will be output to different business log files, the default is false
pradar.biz.log.divider.path-The output directory of the business log generated by the pressure measurement traffic. If pradar.biz.log.divider is configured as true and this parameter is not configured, it will be output to the pradar log directory by default
pradar.trace.sampling.path-The path of the global sampling rate of pradar trace on zookeeper, the default is /config/log/trace/simpling
pradar.trace.app.sampling.path-the path of pradar trace application adoption rate on zookeeper, the default is /config/log/trace/%s/simpling, %s will be automatically replaced with the application name of the current application
pradar.perf.push.enabled-whether to enable performance analysis data push, not push by default
pradar.trace.queue.size-pradar trace log queue size, the default is 1024
pradar.monitor.queue.size-monitor log queue size, the default is 512
pradar.cluster.test.prefix-pradar pressure test prefix, the default is PT_
pradar.cluster.test.prefix.rod-the prefix in the pradar pressure test standard, the default is PT-
pradar.cluster.test.suffix-pradar pressure test standard suffix, the default is _PT
pradar.cluster.test.suffix.rod-suffix in pradar pressure test standard, default is -PT
plugin.request.size-the parameter length of the trace log record, the default is 5000
plugin.response.size-the response length of the trace log record, the default is 5000
plugin.request.on-Whether to enable parameter recording in the trace log, the default is true
plugin.response.on-Whether to enable response recording in the trace log, the default is true
plugin.exception.on-Whether to enable exception logging in the trace log, the default is true
mongodb322.enabled-Whether the mongdb 3.2.2 version plug-in is enabled, the default is false
```

> All configuration items in this configuration file support remote pull. Please refer to the agent.properties configuration item.

### Application Access

Application access needs to add the following jvm parameters when the application starts:

```aidl
-Xbootclasspath/a:$JAVA_HOME/lib/tools.jar
-javaagent:/Users/xiaobin/workspace/simulator-agent/target/simulator-agent/simulator-launcher-instrument.jar
-Dpradar.project.name=xxx-test
-Dsimulator.agentId=xxxx
-Djdk.attach.allowAttachSelf=true
```
The agent starts loading after a default delay, the delay defaults to 300, and the time unit defaults to SECONDS.
If you need to customize the startup delay time, you can specify the delay time through `-Dsimulator.delay`,
Use `-Dsimulator.unit` to specify the delay time unit, currently the following units are supported: 
```aidl
DAYS
HOURS
MINUTES
SECONDS
MILLISECONDS
MICROSECONDS
NANOSECONDS
```
这两个参数只供开发在调试时使用，在实际的环境不建议去配置这两个参数
虽然不指定 `simulator.agentId` 系统也会生成一个 agentId，但是为了保证唯一性和可读性，还是推荐显示指定一个 agentId(`-Dsimulator.agentId`)

> 注 
> 1. -Djdk.attach.allowAttachSelf=true必须项，并且值必须为true 
> 2. -Xbootclasspath/a:$JAVA_HOME/lib/tools.jar 必须添加，防止当前环境的CLASSPATH变量有问题导致异常

### 四、验证

应用接入agent启动成功后，agent会在日志目录下输出一下日志文件

![log.png](./imgs/loglist.jpg)

`simulator.log` 会输出agent加载过程中的详细日志

![log.png](./imgs/img.png)

