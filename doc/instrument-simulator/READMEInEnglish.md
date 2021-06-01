# instrument-simulator

Instrument-simulator is the framework of the agent, responsible for the life cycle management, and provides some built-in tool commands.

## Project Introduction
- **instrument-simulator-agent** the entrance of the simulator framework startup, where the core class startup class is AgentLauncher
- **instrument-simulator-agent-module** The entry module (jdk9 and above) started by the simulator framework is supported
- **instrument-simulator-api** simulator api that all extension modules need to rely on, through this api to realize the enhancement of middleware
- **instrument-simulator-base-api** The base api that all the simulator's extension modules depend on is dependent on instrument-simulator-api
- **instrument-simulator-compatible** simulator adaptation module, used to solve the adaptation problem of jdk multi-version and jvm multi-version
- The core class of **instrument-simulator-core** simulator, which implements the core loading process of the framework and the management of extended modules
- **instrument-simulator-jdk9-module** module support for simulator jdk9 and above
- **instrument-simulator-management-provider** The default spi implementation inside the simulator, which can customize the logic when loading the module jar package
- **instrument-simulator-messager** all classes involved in the enhancement of the simulator in bytecode are defined in this module, and this module defines hooks for enhancing code execution and exception handling, etc.
- **instrument-simulator-messager-api** The api exposed by instrument-simulator-messager currently only exposes the definition of bootstrap resource loading
- **instrument-simulator-messager-jdk9** jdk9's implementation of boostrap resource loading
- **instrument-simulator-spi** the definition of spi for simulator
- **system-modules** The realization of the built-in tool module of the simulator realizes some of the functions of arthas and provides http access
