cmake_minimum_required(VERSION 3.10.2)

# 프로젝트 이름 설정
project("hangulinputprocessor")

# libhangul 소스 및 헤더 파일 경로 설정
set(LIBHANGUL_SRC_DIR "${CMAKE_SOURCE_DIR}/../../../../libhangul/hangul")
set(LIBHANGUL_INCLUDE_DIR "${CMAKE_SOURCE_DIR}/../../../../libhangul/hangul")

# libhangul 라이브러리 빌드
add_library(libhangul
    STATIC
    ${LIBHANGUL_SRC_DIR}/hangulctype.c
    ${LIBHANGUL_SRC_DIR}/hangulinputcontext.c
    ${LIBHANGUL_SRC_DIR}/hangulkeyboard.c
    ${LIBHANGUL_SRC_DIR}/hanja.c
)

# JNI와 libhangul을 사용하는 hangul_input_processor.cpp 파일 빌드
add_library(hangulinputprocessor
    SHARED
    hangul_input_processor.cpp
)

# Include directories 설정 (libhangul 헤더 파일 위치)
target_include_directories(hangulinputprocessor PRIVATE ${LIBHANGUL_INCLUDE_DIR})

# 안드로이드 NDK에 포함된 log 라이브러리 링크
find_library(log-lib log)

# libhangul과 Android의 log 라이브러리 링크 설정
target_link_libraries(
    hangulinputprocessor
    libhangul
    ${log-lib}
)

# libhangul에 대한 설정
target_include_directories(libhangul PRIVATE ${LIBHANGUL_INCLUDE_DIR})

# C++11 표준 설정 (JNI를 사용할 때 C++ 지원)
set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

