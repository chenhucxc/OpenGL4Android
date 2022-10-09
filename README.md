# Android 平台使用 OpenGL ES 绘制图像，实现全景图片和视屏
主要分为两个 Module 来处理，分别是在Android上层实现和在Jni层实现
在opengl的语法方面，在Android studio上可以安装一个glsl的插件，方便阅读和编辑glsl的语法。

## 在 Android 应用层使用 OpenGL ES
代码借鉴于最后部分的链接，java实现的地方，改用了kotlin来处理。这一部分主要是做一个全景播放器

## 在 Native 层使用 OpenGL ES
在jni层使用OpenGL实现各种效果,代码来源于[Android OpenGLES 3.0 开发系统性学习教程](https://github.com/githubhaohao/NDK_OpenGLES_3_0)

## 最后
这些资料基本上都来源于网上，主要借鉴了两个OpenGL的学习专栏，代码也是进行了引用，为了加深理解，应用层这一块又自己重写了一遍，最后做一个整理
- [OpenGL ES 3.0 应用层开发学习](https://blog.csdn.net/gongxiaoou/article/details/89199632)
- [OpenGL ES 3.0 Jni层开发学习](https://github.com/githubhaohao/NDK_OpenGLES_3_0)
- [OpenGL ES 3.0 制作一个简单的VR播放器](https://www.jianshu.com/p/084921eacf35)

上面的链接内容，对一个入门OpenGL的开发者来说非常友好，值得多看几遍，理清楚gl的使用流程。
