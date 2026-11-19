# ProGuard / R8 rules for Shayari Lok
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int d(...);
}
-keepattributes *Annotation*,Signature,SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# App Models & Entrypoints
-keep class com.example.model.** { *; }
-keep class com.example.MainActivity { *; }
-keep class com.example.SettingsActivity { *; }
-keep class com.example.PrivacyPolicyActivity { *; }
-keep class com.example.NotificationsActivity { *; }

# Firebase Core, Auth & Firestore
-keep class com.google.firebase.FirebaseApp { *; }
-keep class com.google.firebase.provider.FirebaseInitProvider { *; }
-keep class * implements com.google.firebase.components.ComponentRegistrar { *; }
-keepnames class * implements com.google.firebase.components.ComponentRegistrar
-keep class com.google.firebase.auth.** { *; }
-keep class com.google.firebase.firestore.** { *; }

# Google Play Services, Credential Manager & AdMob
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
-keep class com.google.android.gms.auth.** { *; }
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.android.gms.common.** { *; }

# Kotlin Coroutines & ServiceLoader
-keepnames class kotlinx.coroutines.android.AndroidDispatcherFactory
-keepnames class kotlinx.coroutines.android.AndroidExceptionPreHandler
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
