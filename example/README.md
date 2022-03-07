example
=======
示例工程主要有3个模块和app壳工程：
 * [Account](module-account)：账户模块，负责管理用户信息，登录，退登等；
 * [Mall](module-mall)：商城模块，负责管理收货地址，展示商城界面等；
 * [Main](module-main)：账户模块，负责展示主界面，包含测试拦截器的界面；
 * [app](app)：app壳，负责调用`P2M.init()`初始化，负责启动闪屏页；

依赖关系图：
<br/><br/><img src="/assets/p2m_project_example_all.png" width="500"  alt="image"/><br/>

`Mall`使用`Account`，因此`Mall`依赖`Account`：
 * 对于`Mall`来说，`Account`是依赖项；
 * 对于`Account`来说，`Mall`是外部模块；

`Main`使用`Account`和`Mall`，因此`Main`依赖`Account`和`Mall`：
 * 对于`Main`来说，`Account`和`Mall`是依赖项；
 * 对于`Account`来说，`Main`是外部模块；
 * 对于`Mall`来说，`Main`是外部模块；

`app`使用`Account`和`Main`，因此`app`依赖`Account`和`Main`：
 * 对于`app`来说，`Account`和`Main`是依赖项；
 * 对于`Account`来说，`app`是外部模块；
 * 对于`Main`来说，`app`是外部模块；

对于`Account`来说，`Mall`、`Main`、`app`是外部模块。
