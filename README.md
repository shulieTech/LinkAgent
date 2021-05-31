# Agent

AgentX is a Java-based open-source agent designed to collect data and control Functions for Java applications through JVM bytecode, without modifying applications codes.

It support projects:
[DaYuX](https://github.com/shulieTech/DaYuX)

## QuickStart

see [QuickStart](https://github.com/shulieTech/AgentX/blob/main/doc/QuickStart.md)

## 模块说明

### simulator-agent
这个项目的职责主要是负责与控制台进行交互，负责 agent 的升级、加载、卸载、模块升级等操作，需要注意的是这个模块不能直接升级，如果需要升级则需要重新安装。

see [simulator-agent](https://github.com/shulieTech/AgentX/blob/main/doc/instrument-simulator/README.md)

### instrument-simulator
探针框架，负责整个探针和模块的生命周期管理，并且提供内置的一些工具命令
see [instrument-simulator](https://github.com/shulieTech/AgentX/blob/main/doc/instrument-simulator/README.md)

### instrument-modules
所有用户的自定义模块。所有我们支持的中间件列表全都放在这个工程目录下。

see [instrument-modules](https://github.com/shulieTech/AgentX/blob/main/doc/instrument-modules/README.md)

## 如何构建

see [HowToBuild](https://github.com/shulieTech/AgentX/blob/main/doc/HowToBuild.md)
