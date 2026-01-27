# log管理

### TAG

每个类应该有一个独立的tag，例如：

````
public class OmniAccessService extends AccessibilityService {

    private static final String TAG = "OmniAccessService";
    ...
}
````

TAG的值应该严格等于类名


### Log层级管理

**在写log时使用OmniLog，不要使用android.utils.log**

OmniLog分为四个层级

分别为

OmniLog.v(TAG, "message")

OmniLog.d(TAG, "message")

OmniLog.i(TAG, "message")

OmniLog.w(TAG, "message")

OmniLog.e(TAG, "message")



##### OmniLog.v (verbose级别)

打印繁琐信息

放在每次调用函数的开头,监控函数的调用，如：

```
public static boolean performLongClick(AccessibilityService service, AccessibilityNodeInfo node, boolean gesture) {
        OmniLog.v(TAG, "Omni perform long click!");
}
```



##### OmniLog.d (debug 级别)

描述调试信息，记录一些关键的调试信息

记录事件结果信息，如：

```
OmniLog.d(TAG, "Omni screen shot Success!");  // 截屏成功
OmniLog.d(TAG, "Omni action finished!");      // 动作执行成功
```



##### OmniLog.i (info 级别)

描述一般信息

记录用户的行为，重要的数据，如：

```
OmniLog.i(TAG, "Omni received operate " + Operate); // 服务器的指令信息
OmniLog.i(TAG, "Omni agent receive prompt:" + prompt); // 服务器接收到的用户prompt
```



##### OmniLog.w (waring 级别)

描述警告信息

记录不会引发程序崩溃的异常

```
if(window.getTitle() != null) {
     return root;
} else {
     OmniLog.w(TAG, "Omni find unfinished window info!");
}
```



##### OmniLog.e (error 级别)

描述错误信息

多为程序报错，会引发程序崩溃的值

```
try {
	...
} catch (Exception e) {
	OmniLog.e(TAG, "Omni error: " + e "(QnQ)");
}

if (node == null) {
	OmniLog.e(TAG, "Omni perform empty node!");
	return;
}
```

##### OmniLog.wtf (assert 级别)

描述异常信息

描述不应该发生的会引起程序严重崩溃的错误

```
try {
	...
} catch (Exception e) {
	OmniLog.wtf(TAG, "Omni error: " + e "(QnQ)");
}

if (node == null) {
	OmniLog.wtf(TAG, "Omni perform empty node!");
	return;
}
```


