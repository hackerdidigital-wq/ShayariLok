# ProGuard / R8 rules for Shayari Lok (Ultra-Lightweight Release Build)
-repackageclasses 'o'
-allowaccessmodification
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int d(...);
}
-keepattributes *Annotation*,Signature,SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keep class com.example.model.** { *; }
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
-keep class com.google.firebase.auth.** { *; }
-keep class com.google.android.gms.auth.** { *; }
-keep class com.google.android.gms.ads.** { *; }
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
