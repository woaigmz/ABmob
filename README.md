### 阿凡达：一个解决跨进程的事件订阅发布问题：
[项目地址Avatar](https://github.com/woaigmz/Avatar)
1：跨进程通信 aidl+service
2：发布的内容和订阅者的信息进程共享
![binder的应用](https://upload-images.jianshu.io/upload_images/8886407-86e1e2227da7a1cc.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)
跨进程的通信可用采用binder机制，这里用 aild.stub 的 binder 代理对象
订阅发布可用采用CS架构，将订阅信息和发布内容同步到一个Service，保证进程间数据一致
选择service+adil的原因：
①service 四大组件之一，由AMS管理，提供binder接口，可用传入 Messager.Binder/Aidl.Stub 等 IBinder的实现，来实现跨进程通信
②启动service：采用bind 启动,保证了客户端bindService(intent,binder,connection)后，Service的onBind()方法通过AMS代理将binder传递到AMS，ServiceDispatcher.ConnectionInfo,将binder的connect/disconnect的回调(ServiceConnection)返回给客户端
③每个进程有一份Avatar，每次register/unregister/post 都会通过bindService，向服务器Service发送订阅者和订阅内容，在Service进行同步和转发
![Avatar](https://upload-images.jianshu.io/upload_images/8886407-abae3870d15724e8.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

### 使用：
**Step 1.** Add the JitPack repository to your build file
Add it in your root build.gradle at the end of repositories:
```
	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```
**Step 2.** Add the dependency
```
	dependencies {
	        implementation 'com.github.woaigmz:Avatar:0.0.4'
	}

```
**Step 3.** 每个进程初始化 context 上下文，通过上下文来启动service
```
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        //初始化多进程库
        Avatar.init(this);
        if(!ProcessUtil.isMainProcess){
            return;
        }
        //初始化主进程库
    }

     @Override
        public void onTerminate() {
            super.onTerminate();
            Avatar.recycleSource();
        }

}
```
**Step 4.** 事件发布 home进程：HomeActivity
和EventBus对比，EventBus是不能跨进程的
```
public class HomeActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        findViewById(R.id.tv_close_main_page).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        EventBus.getDefault().post(new MessageEvent("改变背景颜色"));
        Avatar.get().post(BusConstants.CHANGE_TEXT, "我是：MainActivity");
        Avatar.get().post( BusConstants.CHANGE_COLOR, "#FF0000");
        finish();
    }
}
```
**Step 5.** 主进程：MainActivity 订阅事件
```
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv = findViewById(R.id.tv_name);
        Avatar.get().register(this);
        EventBus.getDefault().register(this);
        tv.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, HomeActivity.class)));

    }

    @Subscribe(thread = ThreadMode.MAIN, tag = BusConstants.CHANGE_TEXT)
    public void changeText(String s) {
        Log.e(TAG, s);
        tv.setText(s);
    }

    @Subscribe(thread = ThreadMode.MAIN, tag = BusConstants.CHANGE_COLOR)
    public void changeColor(String s) {
        Log.e(TAG, s);
        tv.setTextColor(Color.parseColor(s));
    }

    @org.greenrobot.eventbus.Subscribe(threadMode = org.greenrobot.eventbus.ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {
        Log.e("EventBus", Thread.currentThread().getName() + "- - -" + event.text);
        tv.setBackgroundColor(Color.YELLOW);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        Avatar.get().unregister(this);
    }
}
```
