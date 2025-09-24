package com.huankeshu.spandroidshopping;

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
    }


}
