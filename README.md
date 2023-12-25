<div align="center">
    <h1 > NewQStory </h1>

[![Telegram](https://img.shields.io/static/v1?label=Telegram&message=Channel&color=0088cc)](https://t.me/WhenFlowersAreInBloom)
</div>  

---

## Xposed QQ模块  

* **预估支持的QQ版本 8.9.68 +**  
    * 其他版本未经测试 您或许可以自己尝试 
    
* **推荐使用的框架**
    - 1.[LSPosed](https://github.com/LSPosed/LSPosed)
    - 2.[LSPatch](https://github.com/LSPosed/LSPatch) 本地模式(没root的可以用这个)
    - 3.[LSPatch](https://github.com/LSPosed/LSPatch) 内置模式(便携模式 或者这个)
    - 4.太极-无极
 ---
* **更新频率**

    -  随缘更新
    - [Releases](https://github.com/Suzhelan/NewQStory/releases) 大约在版本迭代5-10个内工作日内更新
    - [Telegram](https://t.me/WhenFlowersAreInBloom) 在构建测试结束后会更新

---  

~~由于开发人员的急缺暂时无法测试其他框架是否可用~~  
经过大多数用户的测试目前在所有的框架中已经完成了适配  
### 该插件仅供学习与练习  
请勿使用此插件用于违法 商业行为  
插件只是提高日常方便性的工具 请勿过度依赖该插件  
此项目会因为各种可抗不可抗因素随时停止维护  
如果发生了更糟糕的情况我们会及时 **删除代码库** 以及 **发行渠道的所有版本**  
在此之前你或许可以及时保存我们的工作成品
#### 如果你有能力欢迎加入到代码贡献开发中 
> **此项目的流行语言是Java  
> JDK版本为17.0.9 请不要使用低于此版本的JDK编译**

可能你因为个人原因无法参与代码开发 , 可以为此项目点一个 <kbd>:star:STAR</kbd>  
或是学习此项目~~糟糕~~的代码 这会为未来带来很大的帮助 学习路慢慢  


### 此项目可能出现以下优秀代码  
#### 三角形具有稳定性
```java
    for (int i = 0; i < viewGroup.getChildCount(); i++) {
        if (viewGroup.getChildAt(i) instanceof FrameLayout QRCodeLayout) {
            if (QRCodeLayout.getChildCount() == 1) {
                if (QRCodeLayout.getChildAt(0) instanceof FrameLayout frameLayout) {
                    if (frameLayout.getChildCount() == 2) {
                        if (frameLayout.getChildAt(0) instanceof ImageView) {
                            viewGroup.removeView(QRCodeLayout);
                            break;
                        }
                    }
                 }
             }
        }
    }
```

---

#### 灵活使用变量名
*以下代码出现在同一个项目中*
```kt
    val UserName = 'a'
    val username = 65
    val user_name = "a"
    val yonghu名 = 65;
    val userName = 'a'
    val yonghuName = 0x41
    val 用户ming = "a"
    val user名 = 'a'
```
灵活使用变量名风格  
 - 灵活使用各种风格 , 可以给后来者深刻印象  
 - 完成工作之余也锻炼了大脑 , 不容易得老年痴呆  
 - 灵活切换需要一定经验 , 需要多练习才能掌握

---

#### 简约代码
:x:这样写太长了
```java
int height = 1920;
```
:heavy_check_mark:这样简约清晰
```java
int h = 1920;
int h2 = h/2;
```
命名尽可能短
- 能打一个字母为什么要打一个单词  
- 不让外人一眼看懂代码 为公司省一年几百万的加密费用  
- 一个月后自己也看不懂 安全系数高

---

#### 全球化发展
:x:这样写限制太多 基础类型还不能进行址传递导致参数不会被更新
```java
public int add(int i){return i+1;}
```
:heavy_check_mark:正确的是尽可能用全局变量 适应当今全球化发展
```java
int a;
public void add(){a = a+1;}//不留多余的空格 省内存
```

---

#### 正确注释
:x:看似规范的中英文注释 写错了还可能会被大佬嘲笑
```java
    //开始初始化
    private void close()

    /**
     * Use local cache mode to find the string in which the method appears
     * <p>
     * {@link Builder#setCachePath(String)}
     *
     * @param str A string constant that appears inside a method
     */
    private ArrayList<Method> useLocalLookupMethodString(String str)

```
:heavy_check_mark:正确姿势 使用不知名语言搭配不知名语言来写注释 自己看不懂别人也看不懂 提高不可替代性
```java
    //आरंभ करप
    private void init()

    /**
     * ක්‍රමය දිස්වන තන්තුව සොයා ගැනීමට දේශීය හැඹිලි මාදිලිය භාවිතා කරන්න
     * <p>
     *
     * @param str একটি স্ট্রিং ধ্রুবক যা একটি পদ্ধতির ভিতরে প্রদর্শিত হয়
     */
    private ArrayList<Method> useLocalLookupMethodString(String str)

```
---


  
**终于到最后啦 助你的开发之旅和你的人生一样顺利**
