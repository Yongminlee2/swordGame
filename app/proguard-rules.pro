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

# 광고 SDK 가 WorkManager 를 끌고 오고, WorkManager 는 Room 으로 만든
# WorkDatabase_Impl 을 **이름으로 찾아 기본 생성자로** 만든다. 축소기가 그
# 생성자를 지우면 앱이 켜지자마자 "Failed to create an instance of
# androidx.work.impl.WorkDatabase" 로 죽는다. 디버그 빌드는 축소하지 않아 멀쩡하고
# 릴리스에서만 터지므로, 릴리스를 실기기에서 켜 보기 전에는 드러나지 않는다.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
