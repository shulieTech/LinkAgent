# LinkAgent
[![LICENSE](https://img.shields.io/github/license/pingcap/tidb.svg)](https://github.com/pingcap/tidb/blob/master/LICENSE)
[![Language](https://img.shields.io/badge/Language-Java-blue.svg)](https://www.java.com/)
<br/>
LinkAgent is a Java-based open-source agent designed to collect data and control functions for Java applications through JVM bytecode, without modifying application codes.

Supported Project List:
- [Takin](https://github.com/shulieTech/Takin)

English / [中文](README_CN.md)

## QuickStart
- see [QuickStart English](doc/QuickStartInEnglish.md)

## Instruction

### simulator-agent
This module is mainly responsible for interacting with the Control Platform, for agent upgrade, loading, unloading, module upgrade and other operations. It cannot be upgraded directly and can only be reinstalled.

- see [simulator-agent](doc/simulator-agent/README.md)


### instrument-simulator
This module is an agent（Probe）framework that is responsible for the life cycle management, and provides some built-in command tools. 
- see [instrument-simulator](doc/instrument-simulator/READMEInEnglish.md)

### instrument-modules
User-defined modules and supported middleware modules are in this project directory. Users can develop new modules for various Java middlewares in this directory. 

- see [instrument-modules](doc/instrument-modules/READMEInEnglish.md)

## How to Build

- see [HowToBuild](doc/HowToBuildInEnglish.md)


# Community
Mailing List: Mail to shulie@shulie.io<br/>
Wechat group<br/>
<img src="https://raw.githubusercontent.com/shulieTech/Images/main/wx_4.png" width="30%" height="30%">
<br/>
QQ group: **118098566**<br/>
QR code：<br/>
<img src="https://raw.githubusercontent.com/shulieTech/Images/main/qq_group_2.jpg" width="30%" height="30%">
<br/>
Dingding group：<br/>
<img src="https://raw.githubusercontent.com/shulieTech/Images/main/dingding_group.jpg" width="30%" height="30%">
<br/>
WeChat Official Account：<br/>
<img src="https://raw.githubusercontent.com/shulieTech/Images/main/shulie.png" width="30%" height="30%">
<br/>

## Ask Questions in Official Forum
[Official Forum](https://news.shulie.io/?page_id=2477)

# License
This project is under the Apache 2.0 license. See the [LICENSE](LICENSE) file for details.
