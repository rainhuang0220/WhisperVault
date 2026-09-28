cmake_minimum_required(VERSION 3.22.1)
project(whispervault_jni)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(WHISPER_LIB_DIR ${CMAKE_SOURCE_DIR}/../../../../third_party/whisper.cpp)

set(
    SOURCE_FILES
    ${CMAKE_SOURCE_DIR}/whispervault_jni.cpp
    ${WHISPER_LIB_DIR}/src/whisper.cpp
    ${WHISPER_LIB_DIR}/ggml/src/ggml.c
    ${WHISPER_LIB_DIR}/ggml/src/ggml-alloc.c
    ${WHISPER_LIB_DIR}/ggml/src/ggml-backend.cpp
    ${WHISPER_LIB_DIR}/ggml/src/ggml-backend-reg.cpp
    ${WHISPER_LIB_DIR}/ggml/src/ggml-quants.c
    ${WHISPER_LIB_DIR}/ggml/src/ggml-threading.cpp
    ${WHISPER_LIB_DIR}/ggml/src/gguf.cpp
    ${WHISPER_LIB_DIR}/ggml/src/ggml-cpu/ggml-cpu.c
    ${WHISPER_LIB_DIR}/ggml/src/ggml-cpu/ggml-cpu.cpp
    ${WHISPER_LIB_DIR}/ggml/src/ggml-cpu/ggml-cpu-aarch64.cpp
    ${WHISPER_LIB_DIR}/ggml/src/ggml-cpu/ggml-cpu-hbm.cpp
    ${WHISPER_LIB_DIR}/ggml/src/ggml-cpu/ggml-cpu-quants.c
    ${WHISPER_LIB_DIR}/ggml/src/ggml-cpu/ggml-cpu-traits.cpp
    ${WHISPER_LIB_DIR}/ggml/src/ggml-cpu/unary-ops.cpp
    ${WHISPER_LIB_DIR}/ggml/src/ggml-cpu/binary-ops.cpp
    ${WHISPER_LIB_DIR}/ggml/src/ggml-cpu/llamafile/sgemm.cpp
)

add_library(whispervault SHARED ${SOURCE_FILES})

target_compile_definitions(whispervault PUBLIC GGML_USE_CPU)
target_compile_options(whispervault PRIVATE -O3 -fvisibility=hidden -ffunction-sections -fdata-sections)
target_link_options(whispervault PRIVATE -Wl,--gc-sections)

target_include_directories(
    whispervault PRIVATE
    ${WHISPER_LIB_DIR}
    ${WHISPER_LIB_DIR}/src
    ${WHISPER_LIB_DIR}/include
    ${WHISPER_LIB_DIR}/ggml/include
    ${WHISPER_LIB_DIR}/ggml/src
    ${WHISPER_LIB_DIR}/ggml/src/ggml-cpu
)

find_library(log-lib log)
find_library(android-lib android)
target_link_libraries(whispervault ${log-lib} ${android-lib})
