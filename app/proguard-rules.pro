# Release builds log nothing (catalog F025): R8 removes every android.util.Log call, so a log line
# added by mistake can never leak a key, a token or personal data from a release APK.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
    public static int println(...);
}

# The Cast SDK finds CastOptionsProvider by the class name in the manifest's meta-data, so R8 must keep that name.
-keep class com.issasoda.issaspeaker.speaker.CastOptionsProvider { <init>(); }
