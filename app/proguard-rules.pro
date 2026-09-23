# sherpa-onnx uses JNI with reflection-based field access from native code --
# keep everything in its package so R8/ProGuard doesn't strip or rename fields
# the native library expects to find by name.
-keep class com.k2fsa.sherpa.onnx.** { *; }
