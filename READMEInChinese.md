# Agent

AgentX is a Java-based open-source agent designed to collect data and control Functions for Java applications through JVM bytecode, without modifying applications codes.

Supported Project List:
- [DaYuX](https://github.com/shulieTech/DaYuX)
- ... ...

## 快速上手

- see [快速上手 中文版](https://github.com/shulieTech/AgentX/blob/main/doc/QuickStart.md)

## 使用指引

### simulator-agent
This module are mainly responsible for interacting with the Control Platform, for agent upgrade, loading, unloading, module upgrade and other operations. It cannot be upgraded directly and can only be reinstalled.

- see [simulator-agent 中文版](https://github.com/shulieTech/AgentX/blob/main/doc/instrument-simulator/README.md)


### instrument-simulator
This module is agent（Probe）framework that is responsible for the life cycle management, and provides some built-in command tools. 
- see [instrument-simulator 中文版](https://github.com/shulieTech/AgentX/blob/main/doc/instrument-simulator/README.md)

### instrument-modules
User-defined modules and supported middleware modules are in this project directory. Users can develop new modules for various Java middlewares. 
- see [instrument-modules 中文版](https://github.com/shulieTech/AgentX/blob/main/doc/instrument-modules/README.md)

## How to Build
- see [如何构建 中文版](https://github.com/shulieTech/AgentX/blob/main/doc/HowToBuild.md)


# Community
Mailing List: Mail to shulie@shulie.io<br/>
Wechat group<br/>
<img src="https://raw.githubusercontent.com/shulieTech/Images/main/code1.png" width="30%" height="30%">
<br/>
QQ group: **118098566**<br/>
QR code：<br/>
<img src="https://raw.githubusercontent.com/shulieTech/Images/main/qq_group2.png" width="30%" height="30%">
<br/>
WeChat Official Account：<br/>
<img src="https://raw.githubusercontent.com/shulieTech/Images/main/shulie.png" width="30%" height="30%">

## Ask Questions in Official Forum
[Official Forum](https://news.shulie.io/?page_id=2477)

# License
DaYuX is under the Apache 2.0 license. See the [LICENSE](https://github.com/shulieTech/DaYuX/blob/main/LICENSE?_blank) file for details.
