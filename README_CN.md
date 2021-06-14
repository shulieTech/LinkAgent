# AgentX

AgentX是一个基于Java的开源agent，旨在通过JVM字节码增强，实现对Java应用程序的数据收集和逻辑控制，而无需修改应用程序代码。AgentX会把数据和控制接口开放出来，提供给外部第三方程序使用。

已支持的项目列表:
- [ArchGuadian](https://github.com/shulieTech/ArchGuadian)

## 快速上手
- see [快速上手 中文版](https://github.com/shulieTech/AgentX/blob/main/doc/QuickStart.md)

## 使用指引

### simulator-agent
该模块主要负责与控制平台交互，负责agent的升级、加载、卸载等操作。agent通常不能直接升级，只能重新安装并重启。

- see [simulator-agent 中文版](https://github.com/shulieTech/AgentX/blob/main/doc/instrument-simulator/README.md)


### instrument-simulator
这个模块是agent的框架，负责程序的生命周期管理和提供一些内置的命令工具。
- see [instrument-simulator 中文版](https://github.com/shulieTech/AgentX/blob/main/doc/instrument-simulator/README.md)

### instrument-modules
该项目包含用户自定义的模块和已经支持的中间件，用户可以为不同的Java中间件实现新的支持模块。
- see [instrument-modules 中文版](https://github.com/shulieTech/AgentX/blob/main/doc/instrument-modules/README.md)

## 如何构建
- see [如何构建 中文版](https://github.com/shulieTech/AgentX/blob/main/doc/HowToBuild.md)


# 社区
邮件地址: Mail to shulie@shulie.io<br/>
微信群<br/>
<img src="https://raw.githubusercontent.com/shulieTech/Images/main/code1.png" width="30%" height="30%">
<br/>
QQ群: **118098566**<br/>
二维码：<br/>
<img src="https://raw.githubusercontent.com/shulieTech/Images/main/qq_group2.png" width="30%" height="30%">
<br/>
微信公众号：<br/>
<img src="https://raw.githubusercontent.com/shulieTech/Images/main/shulie.png" width="30%" height="30%">

## 在官方论坛提问
[Official Forum](https://news.shulie.io/?page_id=2477)

# 开原许可
DaYuX is under the Apache 2.0 license. See the [LICENSE](https://github.com/shulieTech/DaYuX/blob/main/LICENSE?_blank) file for details.
