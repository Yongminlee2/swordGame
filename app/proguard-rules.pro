# kotlinx-serialization 은 리플렉션 대신 생성된 serializer 를 쓰지만,
# 그 serializer 를 찾는 경로가 축소기에 지워지면 세이브를 읽지 못한다.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class com.geomgang.core.** {
    *** Companion;
}
-keepclasseswithmembers class com.geomgang.core.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.geomgang.core.**$$serializer { *; }

# 도메인 enum 은 이름으로 직렬화된다. 이름이 바뀌면 옛 세이브를 못 읽는다.
-keepclassmembers enum com.geomgang.core.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
