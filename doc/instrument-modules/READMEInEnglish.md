# instrument-modules
`instrument-modules` Contains all user-defined modules, and all the middleware lists we support are placed in this project directory.

## Project Introduction
### biz-classloader-inject

在一些特殊场景下需要将插件中自定义的对象注入到业务中，但是由于类加载的问题，如果直接在插件中直接将自定义的对象直接注入到业务中，则会存在找不到类的问题，但是也不能直接将实现暴露到 bootstrap中因为会存在直接引用业务类的情况，所以此时就需要一种特殊的做法可以将某些类直接注入到业务类加载器中，这个模块就是为了解决这个场景而设计的。
需要注意的是此模块不能与 simulator 及扩展插件存在直接的交互，交互都必须要通过 bootstrap 中暴露的 API 来完成(不然会存在类加载不到的问题)，这个模块下的子模块在打包时都会打到biz-classloader-jars目录下，另外还需要对应的加载配置，需要在 biz-classloader-inject.properties 里面配置触发的类，classname=jar 包称,
当指定的 classname 触发加载时则将对应 jar 包里面的内容全部注入到对应的类加载器中

In some special scenarios, it need to inject the user-defined objects in the plug-in into the business. However, due to the problem of class loading, if the objects are directly injected into the business in the plug-in, there will cause problem that the class cannot be found. But it can not directly expose the implementation to bootstrap. Because there will be cases where business classes are directly referenced, a special method is needed to inject certain classes directly into the business class loader. This module is to solve this scene.
It should be noted that this module cannot directly interact with the simulator and extension plug-ins. The interaction must be done through the API exposed in bootstrap (otherwise it will cause the problem that the class cannot be loaded). The sub-modules under this module are packaged in biz-classloader-jars directory, and the corresponding loading configuration is also required. The triggered class needs to be configured in biz-classloader-inject.properties, classname=jar package name,When the specified classname triggers loading, all the contents of the corresponding jar package are injected into the class loader

### bootstrap-inject

This module defines APIs that all extension modules need to expose to bootstrap, which can be accessed by all class loaders injected into boostrap. It should be noted that the API exposed to bootstrap cannot have direct references to the simulator, extension modules, and business classes (because it cannot be directly accessed). Generally, extension modules need to expose some capabilities to the global module by defining corresponding hooks in the bootstrap module. , Implemented by the extension module and injected into it 

### user-modules
User extension module, which contains a list of all currently supported middleware 

## Plug-in Development Instruction

see [Plug-in Development](./PluginDevInEnglish.md)
