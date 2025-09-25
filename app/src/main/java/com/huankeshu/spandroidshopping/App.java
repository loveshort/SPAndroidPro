package com.huankeshu.spandroidshopping;

import android.app.Activity;
import android.app.Application;

//安卓的启动项
public class App extends Application  implements HasActivityInjector {

    @inject
    DispatchingAndroidInjector<Activity> injector;

    private AppComponent appComponent;

    private User user;


    @Override
    public void onCreate() {
        super.onCreate();

        appComponent = DaggerAppComponent.builder()
                .appModule(new AppModule(this))
                .httpModule(new HttpModule(Constants.BASE_URL))
                .build();
        appComponent.inject(this);

        //安装LeakCanary内存泄露检测工具
        LeakCanary.install(this);

        //初始化Bugly崩溃上播和热更新
        Bugly.init(this,Constants.BUGLY_APP_ID,BuildConfig.DEBUG);

        //根据构建类型配置不同的初始化
        if (BuildConfig.DEBUG) {
            //Debug模式下的配置
            ARouter.openLog(); //开启ARouter日志
            ARouter.openDebug(); //开启ARouter调试模式
            Timber.plant(new Timber.DebugTree()); //开启Timber日志

        }else {
            //Release模式下的配置
            ARouter.closeLog(); //关闭ARouter日志
            ARouter.closeDebug(); //关闭ARouter调试模式
            Timber.plant(new Timber.tree() {
                @Override
                protected void log(int priority, String tag, String message, Throwable t) {
                    //在Release模式下不输出日志
                }
            }); //关闭Timber日志
        }

        //初始化全局异常处理
        NeverCrash.init((t,e) -> {
           //在非Debug模式下，将异常上报到Bugly
           if (!BuildConfig.DEBUG) {
               Timber.w(e); //记录警告日志
               CrashReport.postCatchedException(e); //上报异常
           }
        });

        //从缓存中获取用户信息
        user = getCacheUser();
        //初始化ARouter路由框架
        ARouter.init(this);
    }

    /**
     * 在基础上下文附加时调用
     * 用于多dex支持和Tinker热修复初始化
     * @param base 基础上下文
     */

}
