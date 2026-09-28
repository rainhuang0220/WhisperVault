cmake_minimum_required(VERSION 3.22.1)
project(whispervault_jni)

# Phase 1: JNI skeleton. whisper.cpp sources are wired in the ASR phase.
add_library(whispervault SHARED whispervault_jni.cpp)

find_library(log-lib log)
target_link_libraries(whispervault ${log-lib})
