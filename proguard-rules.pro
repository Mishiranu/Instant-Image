-dontoptimize
-dontobfuscate
-dontpreverify
-dontwarn scala.**
-dontwarn rx.**
-dontwarn com.squareup.picasso.**
-ignorewarnings
# temporary workaround; see Scala issue SI-5397
-keep class scala.collection.SeqLike {
    public protected *;
}
