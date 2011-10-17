cmake_minimum_required (VERSION 2.8)
project (PhaseIn1)

include_directories(${SC_PATH}/include/plugin_interface)
include_directories(${SC_PATH}/include/common)
include_directories(${SC_PATH}/external_libraries/libsndfile/)

set(CMAKE_SHARED_MODULE_PREFIX "")
set(CMAKE_SHARED_MODULE_SUFFIX ".scx")

add_library(PhaseIn1 MODULE PhaseIn1.cpp)
add_library(PhaseInIrm MODULE PhaseInIrm.cpp)


#cmake -DSC_PATH=~/src/supercollider
#make
#mv PhaseIn1.scx PhaseIn1_sclib/