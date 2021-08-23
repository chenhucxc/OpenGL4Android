# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.
set(CMAKE_BUILD_TYPE Release)

# 引入头文件
include_directories(${CMAKE_SOURCE_DIR})
include_directories(${CMAKE_SOURCE_DIR}/include)
include_directories(
        egl
        glm
        inc
        inc/assimp
        inc/freetype_2_9_1
        looper
        model
        render
        sample
        util
)

# 编译文件路径
file(GLOB_RECURSE src_files
        ${CMAKE_SOURCE_DIR}/render/*.cpp
        ${CMAKE_SOURCE_DIR}/util/*.cpp
        ${CMAKE_SOURCE_DIR}/sample/*.cpp
        ${CMAKE_SOURCE_DIR}/egl/*.cpp
        ${CMAKE_SOURCE_DIR}/looper/*.cpp
        ${CMAKE_SOURCE_DIR}/*.cpp
        )

add_library( # Sets the name of the library.
        native_render

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        ${src_files})

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

# 配置 jniLibs 路径方式一
set(jniLibs_DIR "${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}")
#set(lib_DIR ${CMAKE_SOURCE_DIR}/../jniLibs)
MESSAGE(STATUS "jniLibs = ${jniLibs_DIR}")

# opencv for so
add_library(opencv SHARED IMPORTED)
set_target_properties(opencv PROPERTIES IMPORTED_LOCATION ${jniLibs_DIR}/libopencv_java4.so)

add_library(assimp SHARED IMPORTED )
set_target_properties(assimp PROPERTIES IMPORTED_LOCATION ${jniLibs_DIR}/libassimp.so)

add_library(freetype STATIC IMPORTED )
set_target_properties(freetype PROPERTIES IMPORTED_LOCATION ${jniLibs_DIR}/libfreetype.a)

#SET(CMAKE_CXX_FLAGS  "${CMAKE_CXX_FLAGS} -fno-rtti -fno-exceptions")
set(third-party-libs
        freetype
        assimp
        opencv)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
        native_render

        # Links the target library to the log library
        # included in the NDK.
        ${log-lib}
        #jnigraphics jni中使用AndroidBitmap
        -ljnigraphics
        # opencv
        ${third-party-libs}
        EGL
        GLESv3
        android)