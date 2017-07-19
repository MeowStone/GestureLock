# 仿制的手势解锁 #
原文：[Android 手势锁的实现](http://blog.csdn.net/lmj623565791/article/details/36236113)

一些修改：


- 回调区分开 错误 与 正确,并且增加不同时常的 延迟重置图案


- 重试次数消耗掉之后 手势面板不再处理 onTouch