# ProGuard / R8 rules for Shayari Lok (Ultra-Lightweight Release Build)
-repackageclasses 'o'
-allowaccessmodification
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int d(...);
}
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

