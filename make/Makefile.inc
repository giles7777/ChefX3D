#*********************************************************************
#
#  (C) 2001-07 Web3d Consortium
#    http://www.web3d.org/
#
# Lowest level common makefile for both native and Java code
# 
# Author: Justin Couch
# Version: $Revision: 1.11 $
#
#*********************************************************************

ifndef APP_ROOT
  BUILD_ROOT_DIR=$(PROJECT_ROOT)
else
  BUILD_ROOT_DIR=$(APP_ROOT)
endif

SRC_DIR=src
DOCS_DIR=docs
MAKE_DIR=make
TEST_DIR=test/java

JAVA_DIR=$(SRC_DIR)/java
PARSER_DIR=$(SRC_DIR)/javacc
NATIVE_DIR=$(SRC_DIR)/native
INCLUDE_DIR=$(NATIVE_DIR)/include
IMAGES_DIR=${SRC_DIR}/images
CONFIG_DIR=${SRC_DIR}/config
XML_DIR=${SRC_DIR}/xml
MODELS_DIR=${SRC_DIR}/models
ECLIPSE_DIR=${SRC_DIR}/eclipse
SHADER_DIR=$(SRC_DIR)/glsl

DELETE    = rm -f
MAKEDIR   = mkdir -p
RMDIR     = rm -fr
PRINT     = @echo
COPY      = cp 
COPY_PATH = cp --parents
CHMOD     = chmod
CD        = cd

#JAVAC_FLAGS=-deprecation
#JAVAC_FLAGS=-g:none
#JAVADOC_FLAGS=

MAKEFLAGS=-s
