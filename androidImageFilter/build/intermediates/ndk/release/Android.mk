LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := AndroidImageFilter
LOCAL_LDLIBS := \
	-llog \

LOCAL_SRC_FILES := \
	D:\Work\ATS\ho\app\tastes\androidImageFilter\src\main\jni\Android.mk \
	D:\Work\ATS\ho\app\tastes\androidImageFilter\src\main\jni\AndroidImageFilter.cpp \
	D:\Work\ATS\ho\app\tastes\androidImageFilter\src\main\jni\Application.mk \
	D:\Work\ATS\ho\app\tastes\androidImageFilter\src\main\jni\AverageSmoothFilter.cpp \
	D:\Work\ATS\ho\app\tastes\androidImageFilter\src\main\jni\BlockFilter.cpp \
	D:\Work\ATS\ho\app\tastes\androidImageFilter\src\main\jni\BrightContrastFilter.cpp \
	D:\Work\ATS\ho\app\tastes\androidImageFilter\src\main\jni\cn_Ragnarok_NativeFilterFunc.cpp \
	D:\Work\ATS\ho\app\tastes\androidImageFilter\src\main\jni\ColorTranslator.cpp \
	D:\Work\ATS\ho\app\tastes\androidImageFilter\src\main\jni\GammaCorrectionFilter.cpp \
	D:\Work\ATS\ho\app\tastes\androidImageFilter\src\main\jni\GaussianBlurFilter.cpp \
	D:\Work\ATS\ho\app\tastes\androidImageFilter\src\main\jni\GothamFilter.cpp \
	D:\Work\ATS\ho\app\tastes\androidImageFilter\src\main\jni\HDRFilter.cpp \
	D:\Work\ATS\ho\app\tastes\androidImageFilter\src\main\jni\HueSaturationFilter.cpp \
	D:\Work\ATS\ho\app\tastes\androidImageFilter\src\main\jni\LightFilter.cpp \
	D:\Work\ATS\ho\app\tastes\androidImageFilter\src\main\jni\LomoAddBlackRound.cpp \
	D:\Work\ATS\ho\app\tastes\androidImageFilter\src\main\jni\MotionBlurFilter.cpp \
	D:\Work\ATS\ho\app\tastes\androidImageFilter\src\main\jni\NeonFilter.cpp \
	D:\Work\ATS\ho\app\tastes\androidImageFilter\src\main\jni\OilFilter.cpp \
	D:\Work\ATS\ho\app\tastes\androidImageFilter\src\main\jni\PixelateFilter.cpp \
	D:\Work\ATS\ho\app\tastes\androidImageFilter\src\main\jni\ReliefFilter.cpp \
	D:\Work\ATS\ho\app\tastes\androidImageFilter\src\main\jni\SharpenFilter.cpp \
	D:\Work\ATS\ho\app\tastes\androidImageFilter\src\main\jni\SketchFilter.cpp \
	D:\Work\ATS\ho\app\tastes\androidImageFilter\src\main\jni\SoftGlowFilter.cpp \
	D:\Work\ATS\ho\app\tastes\androidImageFilter\src\main\jni\TvFilter.cpp \

LOCAL_C_INCLUDES += D:\Work\ATS\ho\app\tastes\androidImageFilter\src\main\jni
LOCAL_C_INCLUDES += D:\Work\ATS\ho\app\tastes\androidImageFilter\src\release\jni

include $(BUILD_SHARED_LIBRARY)
