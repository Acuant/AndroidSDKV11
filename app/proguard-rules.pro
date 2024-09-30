# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Both of the following -keep settings are needed for the echip library to work:
-keep class org.bouncycastle.jcajce.provider.** {
    <fields>;
    <methods>;
}
-keep class net.sf.scuba.** {
    <fields>;
    <methods>;
}

#sometimes r8 can strip these fields out resulting in null fields in release variants
#the following prevents this:
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}