# QuickStart

## Get AgentX package
Download installation package : 
- [download](https://shulie-main-pass.oss-cn-hangzhou.aliyuncs.com/open-source/simulator-agent.tar)
<br/>
Or build your own package：
- see [HowToBuild](HowToBuild.md)

### 二、配置修改

#### agent.properties

- `simulator.zk.servers=127.0.0.1:2181` Zookeeper address configuration, change to your zookeeper address.
- `simulator.log.path=/log/pradar/`Output address of log.

agent.properties, Configuration item meaning

```
agent.use.local -  Whether the agent uses local configuration, the agent supports remote pull and load, if this option is true, it will load locally by default, and no longer pull the agent package from remote.
agent.config.url - Configure the url of the current agent loading content configuration, such as the version number of the current agent loading, the list of modules that need to be loaded, this value is required when the agent.use.local configuration is false
agent.processlist.url - Upload the url of all scanned process list results. This is the list of all java processes that need to be known when the agent uses the independent process mode
agent.load.url - The download url of the probe package, it is required only when agent.use.local is false
simulator.config.url - The address to obtain the value of the probe configuration item, if all the probe configuration items are moved to the remote, the configuration url is pulled through this configuration item configuration
register.name - The name of the registration center, the default is zookeeper, currently only supports the zookeeper registration center, no configuration is needed for the time being
simulator.client.zk.path - The registration path of the zookeeper registration center, the default path is /config/log/pradar/client, no need to configure
simulator.zk.servers - Zookeeper registration center address, this value must be configured
simulator.zk.connection.timeout.ms - Zookeeper registration center connection timeout time, the default is 30000 ms, no need to configure
simulator.zk.session.timeout.ms - Zookeeper registration center session timeout time, the default is 60000 ms, no need to configure
simulator.log.path - all log paths, including probe log storage path and trace log path, this value must be configured
simulator.log.level - the level of the log, the default is info, you don’t need to configure it
```

#### simulator.properties

Modify `tro.web.url` only.

simulator.properties configuration meaning：

```
user_module  -  The load address of the user module, (no need to change)
provider  -  The loading path of the provider module, (no configuration required)
server.ip  -  Specifies the ip of the open management interface, corresponding to the command url address ip of the framework
server.port  -  Specifies the port of the open management interface, corresponding to the command url address port of the framework
server.charset  -  Specify the encoding method of the response returned by the open management interface
unsafe.enable  -  Whether to allow unsafe to be turned on, the result of the impact is whether to allow enhancement of jdk classes, which is enabled by default (do not modify)
simulator.dump.bytecode  -  Whether to enable dump bytecode, this parameter is only allowed to be used in debugging
simulator.dump.bytecode.path - the path to dump the bytecode, it only works when the parameter of simulator.dump.bytecode is true
Dump.verify - whether to enable bytecode verification during bytecode dump, if verification is enabled, the verification result will be printed in the log
simulator.dump.asm - whether to enable dump asm
simulator.dump.class.list - whitelist of dump classes, separated by commas, the default is empty to dump all enhanced classes
simulator.cost.dump - Whether to output the enhancement time of each enhancement interceptor, after enabling it, the time consumption of each enhancement will be output in the log
module.repository.mode - module repository mode, the default is local, this reserved configuration (no need to specify)
module.remote.repository.addr - remote extension warehouse address, this does not need to be configured, the attribute reservation configuration (does not need to be specified)
tro.web.url - The address of the tro console. This address is used to pull various configurations of the agent, such as data source configuration, whitelist, etc. (this must be configured)
pradar.charset - configure the encoding format of the specified output trace and other log files, the default is UTF - 8
pradar.data.pusher - log push type, the default is tcp, currently only this type is supported
pradar.server.zk.path - the zk registration path of the remote log service, the zk address is the address configured in agent.properties
pradar.data.pusher.timeout - the timeout period for log push, the default is 3000ms
pradar.push.serialize.protocol.code - the serialization method of the push log, the default 2 is java, and the options are 1: kryo 2: java
max.push.log.failure.sleep.interval - Maximum sleep duration when continuous log push fails, in milliseconds, default 10000
pradar.trace.log.version - trace log version number, each agent version will correspond to a fixed version number, do not change this
pradar.monitor.log.version - monitor log version number, each agent version will correspond to a fixed version number, do not change this
pradar.metrics.log.version - metrics log version number, each agent version will correspond to a fixed version number, don’t change this
pradar.switcher.trace - Whether to enable the trace switch, it is enabled by default
pradar.switcher.press - Whether to enable pressure measurement, it is enabled by default, if it is turned off, no pressure measurement flow will be accepted. This parameter does not need to be set, except in some scenarios where only logs are required
pradar.trace.max.file.size - Get the size of a single file of the trace log, the default is 200*1024*1024, which is 200M
pradar.monitor.max.file.size - Get the size of a single file of the monitor log, the default is 200*1024*1024, which is 200M
auto.create.queue.rabbitmq - whether rabbitmq enables automatic queue creation. If enabled, the corresponding shadow queue will be created when the business creates a queue
user.app.key - the appKey of the user currently used by the agent. This value is the user key of the stress test console user and is required
sf-kafka-plugin-param - The configuration in the sf - kafka plugin, you need to wait for Hengyu to complete the definition later
is.kafka.message.headers - whether to enable the writing of Kafka header information, because there may be clients that support the message header but the server does not support it. If an error is reported when writing the Kafka message header, you need to turn this value off at this time (False), the default is true
is.rabbitmq.routingkey - Whether to enable the routingkey mode of rabbitmq, when it is enabled, it will judge whether it is a pressure measurement flow according to the routingkey, and the routingkey will also be shadowed.
pradar.config.fetch.type - the way to get the agent configuration value, the default is http (no need to modify)
pradar.config.fetch.interval - the interval for fetching the agent configuration, the default is 60 (can be modified as needed)
pradar.config.fetch.unit - the interval time unit for agent configuration acquisition, the default SECONDS, the specific value can refer to the enumeration definition of TimeUnit
pradar.user.id - the user ID of the stress test console, this needs to be determined with the stress test console to determine what the current user ID is
pradar.biz.log.divider - true/false Whether to enable the normal flow of business logs and the log diversion of stress test traffic, if enabled, the business logs generated by stress test traffic will be output to different business log files, the default is false
pradar.biz.log.divider.path - The output directory of the business log generated by the pressure measurement traffic. If pradar.biz.log.divider is configured as true and this parameter is not configured, it will be output to the pradar log directory by default
pradar.trace.sampling.path - The path of the global sampling rate of pradar trace on zookeeper, the default is /config/log/trace/simpling
pradar.trace.app.sampling.path - the path of pradar trace application adoption rate on zookeeper, the default is /config/log/trace/%s/simpling, %s will be automatically replaced with the application name of the current application
pradar.perf.push.enabled - whether to enable performance analysis data push, not push by default
pradar.trace.queue.size - pradar trace log queue size, the default is 1024
pradar.monitor.queue.size - monitor log queue size, the default is 512
pradar.cluster.test.prefix - pradar pressure test prefix, the default is PT_
pradar.cluster.test.prefix.rod - the prefix in the pradar pressure test standard, the default is PT - 
pradar.cluster.test.suffix - pradar pressure test standard suffix, the default is _PT
pradar.cluster.test.suffix.rod - suffix in pradar pressure test standard, default is  - PT
plugin.request.size - the parameter length of the trace log record, the default is 5000
plugin.response.size - the response length of the trace log record, the default is 5000
plugin.request.on - Whether to enable parameter recording in the trace log, the default is true
plugin.response.on - Whether to enable response recording in the trace log, the default is true
plugin.exception.on - Whether to enable exception logging in the trace log, the default is true
mongodb322.enabled - Whether the mongdb 3.2.2 version plug - in is enabled, the default is false
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
These two parameters are only for debugging. It is not recommended to modify them in the real environment. Although the simulator.agentId is not specified, the system will generate a new agentId automatically, but in order to ensure uniqueness and readability, It is still recommended to explicitly specify an agentId (-Dsimulator.agentId)

> Note 
> 1. -Djdk.attach.allowAttachSelf=true Required items，and Its value must be true.
> 2. -Xbootclasspath/a:$JAVA_HOME/lib/tools.jar Required items, Prevent the situation that the environment variable CLASSPATH is modifed from causing exceptions.

### Verification

The application is access to the agent and the start is successful, the agent will output a log file in the log directory.

![log.png](./imgs/loglist.jpg)

`simulator.log` Will output detailed logs of the agent loading process.

![log.png](./imgs/img.png)

