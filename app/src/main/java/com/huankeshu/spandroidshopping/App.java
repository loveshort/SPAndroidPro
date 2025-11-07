package com.huankeshu.spandroidshopping;

import android.app.Activity;
import android.app.Application;
import android.content.Context;

import com.squareup.leakcanary.core.BuildConfig;


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

        //根据构建类型配置不同的初始化
        if (BuildConfig.DEBUG) {
            //Debug模式下的配置
            ARouter.openLog(); //开启ARouter日志
            ARouter.openDebug(); //开启ARouter调试模式


        }else {
            //Release模式下的配置
            ARouter.closeLog(); //关闭ARouter日志
            ARouter.closeDebug(); //关闭ARouter调试模式




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
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        //安装Tinker热修复框架
        Beta.installTinker();
    }
}
