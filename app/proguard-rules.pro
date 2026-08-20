# ==============================================================================
# 余粮 (GrainLedger) ProGuard / R8 混淆与代码优化规则
# ==============================================================================

# ------------------------------------------------------------------------------
# 1. 基础配置与通用代码保留规则
# ------------------------------------------------------------------------------
# 优化等级与遍数配置
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose

# 保留源代码行号与文件名，便于线上崩溃分析与堆栈回溯
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,InnerClasses,EnclosingMethod

# 保留所有 Android 原生基础组件与清单注册类
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference

# 保留自定义 View 及其构造函数（XML/Compose 反射加载）
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# 保留枚举类的 values 和 valueOf 方法
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 保留 Parcelable / Serializable 相关序列化成员
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    !private <fields>;
    !private <methods>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ------------------------------------------------------------------------------
# 2. Kotlin 标准库与协程 (Coroutines & Flow) 保留规则
# ------------------------------------------------------------------------------
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keep class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keep class kotlinx.coroutines.CoroutineExceptionHandler { *; }
-keep class kotlinx.coroutines.android.AndroidExceptionPreHandler { *; }
-keep class kotlinx.coroutines.android.AndroidDispatcherFactory { *; }

# ------------------------------------------------------------------------------
# 3. Jetpack Compose 混淆规则
# ------------------------------------------------------------------------------
# 保留 Compose 编译器生成的元数据与可组合函数标记
-keepattributes *Annotation*
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
    @androidx.compose.runtime.ReadOnlyComposable *;
}

# ------------------------------------------------------------------------------
# 4. MIUIX UI 组件库保留规则 (top.yukonga.miuix)
# ------------------------------------------------------------------------------
-dontwarn top.yukonga.miuix.**
-keep class top.yukonga.miuix.** { *; }
-keepclassmembers class top.yukonga.miuix.** { *; }
-keep interface top.yukonga.miuix.** { *; }

# ------------------------------------------------------------------------------
# 5. 余粮业务数据模型与实体类保留规则 (Data Models & Updater)
# ------------------------------------------------------------------------------
# 保留数据模型类及其字段与 getter/setter（防止 JSON/数据库映射反射混淆丢失）
-keep class com.vincent.grainledger.data.model.** { *; }
-keepclassmembers class com.vincent.grainledger.data.model.** { *; }

-keep class com.vincent.grainledger.data.updater.** { *; }
-keepclassmembers class com.vincent.grainledger.data.updater.** { *; }

-keep class com.vincent.grainledger.data.excel.** { *; }
-keepclassmembers class com.vincent.grainledger.data.excel.** { *; }

# ------------------------------------------------------------------------------
# 6. BuildConfig 保留
# ------------------------------------------------------------------------------
-keep class com.vincent.grainledger.BuildConfig { *; }
