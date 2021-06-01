# instrument-modules
`instrument-modules` Contains all user-defined modules, and all the middleware lists we support are placed in this project directory.

## Project Introduction
### biz-classloader-inject

In some special scenarios, it need to inject the user-defined objects in the plug-in into the business. However, due to the problem of class loading, if the objects are directly injected into the business in the plug-in, there will cause problem that the class cannot be found. But it can not directly expose the implementation to bootstrap. Because there will be cases where business classes are directly referenced, a special method is needed to inject certain classes directly into the business class loader. This module is to solve this scene.
It should be noted that this module cannot directly interact with the simulator and extension plug-ins. The interaction must be done through the API exposed in bootstrap (otherwise it will cause the problem that the class cannot be loaded). The sub-modules under this module are packaged in biz-classloader-jars directory, and the corresponding loading configuration is also required. The triggered class needs to be configured in biz-classloader-inject.properties, classname=jar package name,When the specified classname triggers loading, all the contents of the corresponding jar package are injected into the class loader

### bootstrap-inject

This module defines APIs that all extension modules need to expose to bootstrap, which can be accessed by all class loaders injected into boostrap. It should be noted that the API exposed to bootstrap cannot have direct references to the simulator, extension modules, and business classes (because it cannot be directly accessed). Generally, extension modules need to expose some capabilities to the global module by defining corresponding hooks in the bootstrap module. , Implemented by the extension module and injected into it 

### user-modules
User extension module, which contains a list of all currently supported middleware 

## Plug-in Development Instruction

see [Plug-in Development](./PluginDevInEnglish.md)
