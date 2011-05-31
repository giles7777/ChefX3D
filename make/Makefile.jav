#*********************************************************************
#
#  (C) 2001 - 2006 Web3D Consortium
#    http://www.web3d.org/
#
# Makefile rules and useful functions for wide use for Java specific tasks
#
# Author: Justin Couch
# Version: $Revision: 1.6 $
#
#*********************************************************************

#
# Directories for standard stuff
#
include $(PROJECT_ROOT)/make/Makefile.inc

JAVA_DEV_ROOT = $(JAVA_DIR)

ifdef APP_ROOT 
  CLASS_DIR     = classes
  JAVADOC_DIR   = $(DOCS_DIR)/javadoc
  LIB_DIR       = ../../lib
  JAR_DIR       = jars
  JAR_MAKE_DIR  = $(MAKE_DIR)/jar
  ifdef TEST_BUILD
    JAVA_SRC_DIR = test/java
  else
    JAVA_SRC_DIR = src/java
  endif
  DESTINATION   = classes
  JAR_TMP_DIR   = .jar_tmp
  MANIFEST_DIR  = $(MAKE_DIR)/manifest
  APP_JAR_DIR   = $(APP_ROOT)/lib
else
  CLASS_DIR     = classes
  JAVADOC_DIR   = $(DOCS_DIR)/javadoc
  LIB_DIR       = lib
  JAR_DIR	      = jars
  JAR_MAKE_DIR  = $(MAKE_DIR)/jar
  ifdef TEST_BUILD
    JAVA_SRC_DIR = test/java
  else
    JAVA_SRC_DIR = src/java
  endif
  DESTINATION   = classes
  JAR_TMP_DIR   = .jar_tmp
  MANIFEST_DIR  = $(MAKE_DIR)/manifest
  APP_JAR_DIR   = $(APP_ROOT)/lib
endif

#
# Built up tool information
#
ifdef JAVA_HOME
  JAVAC    = $(JAVA_HOME)/bin/javac
  JAR      = $(JAVA_HOME)/bin/jar
  JAVADOC  = $(JAVA_HOME)/bin/javadoc
else
  JAVAC    = javac
  JAR      = jar
  JAVADOC  = javadoc
endif

EMPTY      =
SPACE      = $(EMPTY) $(EMPTY)

OS_NAME=$(shell uname)
ifeq (, $(strip $(findstring CYGWIN, $(OS_NAME))))
  PATH_SEP:=":"
else
  IS_WIN32=t
  PATH_SEP:=";"
endif

ifdef JARS
  LOCAL_JARTMP  = $(patsubst %,$(JAR_DIR)/%,$(JARS))
  LOCAL_JARLIST = $(subst $(SPACE),$(PATH_SEP),$(LOCAL_JARTMP))
endif

ifdef JARS_3RDPARTY
  OTHER_JARTMP  = $(patsubst %,$(LIB_DIR)/%,$(JARS_3RDPARTY))
  OTHER_JARLIST = $(subst $(SPACE),$(PATH_SEP),$(OTHER_JARTMP))
endif

ifdef JARS_JAVADOC
  JAVADOC_JARTMP  = $(patsubst %,$(LIB_DIR)/%,$(JARS_JAVADOC))
  JAVADOC_JARLIST = $(subst $(SPACE),$(PATH_SEP),$(JAVADOC_JARTMP))
endif


# if we have an app root, also append to the JAR list a variant using the
# APP_ROOT as base
ifdef APP_ROOT 
  ifdef JARS_3RDPARTY
    APP_JARTMP  = $(patsubst %,$(APP_JAR_DIR)/%,$(JARS_3RDPARTY))
  endif
endif

CP = $(CLASS_DIR)

ifdef LOCAL_JARLIST
  CP :="$(CP)$(PATH_SEP)$(LOCAL_JARLIST)"
endif

ifdef OTHER_JARLIST
  ifdef CLASSPATH
    CP1:="$(CP)$(PATH_SEP)$(OTHER_JARLIST)"
  else
    CP1:= "$(OTHER_JARLIST)"
  endif
endif

ifdef CP1
  CLASSPATH:="$(CP1)"
else
  CLASSPATH:="$(CP)"
endif

JAVADOC_CLASSPATH=$(CLASS_DIR)$(PATH_SEP)$(JAVADOC_JARLIST)

ifdef APP_ROOT
  ifdef APP_JARLIST
    CLASSPATH:=$(CLASSPATH)$(PATH_SEP)$(APP_JARLIST)
  endif
  CLASSPATH:=$(CLASSPATH)$(PATH_SEP)$(PROJECT_ROOT)/classes
  JAVADOC_CLASSPATH:=$(JAVADOC_CLASSPATH)$(PATH_SEP)$(PROJECT_ROOT)/classes
endif


# has the user defined an external classpath to use here? If so, append
# it to the ordinary classpath.
ifdef PROJECT_CLASSPATH
  CLASSPATH := $(CLASSPATH)$(PATH_SEP)"$(PROJECT_CLASSPATH)"
  JAVADOC_CLASSPATH := $(JAVADOC_CLASSPATH)$(PATH_SEP)"$(PROJECT_CLASSPATH)"
  
  ifndef IS_WIN32
    CLASSPATH := $(subst ",,$(CLASSPATH))
    JAVADOC_CLASSPATH := $(subst ",,$(JAVADOC_CLASSPATH))
  endif
endif

#
# Build rules.
#
PACKAGE_LOC     = $(subst .,/,$(PACKAGE))
PACKAGE_DIR     = $(DESTINATION)/$(PACKAGE_LOC)
JAVA_FILES      = $(filter  %.java,$(SOURCE))
NONJAVA_FILES   = $(patsubst %.java,,$(SOURCE))
CLASS_FILES     = $(JAVA_FILES:%.java=$(PACKAGE_DIR)/%.class)
OTHER_FILES     = $(EXTRA:%=$(PACKAGE_DIR)/%)

JAR_CLASS_FILES = $(patsubst %, %/*.*, $(JAR_CONTENT))

#JAR_EXTRA_FILES = $(EXTRA_FILES:%=$(JAVA_SRC_DIR)/%)

JAR_CONTENT_CMD = -C $(JAR_TMP_DIR) . $(patsubst %, -C $(JAVA_SRC_DIR) %, $(EXTRA_FILES))

# Make a list of all packages involved
ifdef PACKAGE
  PACKAGE_LIST  = $(subst .,/,$(PACKAGE))
else
  PACKAGE_LIST  = $(subst .,/,$(BUILD_ORDER))
endif

PLIST_CLEAN     = $(patsubst %,$(JAVA_SRC_DIR)/%/.clean,$(PACKAGE_LIST))
PLIST_BUILD     = $(patsubst %,$(JAVA_SRC_DIR)/%/.build,$(PACKAGE_LIST))

#
# Option listing for the various commands
#
JAVAC_OPTIONS = -d $(DESTINATION) -classpath $(CLASSPATH) $(JAVAC_FLAGS)
JAVAH_OPTIONS = -d $(INCLUDE_DIR) -classpath $(CLASSPATH)

ifdef MANIFEST
  JAR_OPTIONS = -cmf
  JAR_MANIFEST = $(MANIFEST_DIR)/$(MANIFEST)
else
  JAR_OPTIONS = -cf
endif

JAVADOC_OPTIONS  = \
     -d $(JAVADOC_DIR) \
     -sourcepath $(JAVA_SRC_DIR) \
     -classpath $(JAVADOC_CLASSPATH) \
     -author \
     -use \
     -version \
     -quiet \
     -windowtitle $(WINDOWTITLE) \
     -doctitle $(DOCTITLE) \
     -header $(HEADER) \
     -bottom $(BOTTOM) \
	 $(LINK_FILES)
	 
ifdef OVERVIEW
  JAVADOC_OPTIONS += -overview $(JAVA_SRC_DIR)/$(OVERVIEW)
endif

ifdef JAVADOC_FLAGS
  JAVADOC_OPTIONS += $(JAVADOC_FLAGS)
endif

#
# General build rules
#

# Rule 0. Applied when make is called without targets.
all: $(DESTINATION) $(CLASS_FILES) $(OTHER_FILES)

# Rule 1. If the destination dir is missing then create it
$(DESTINATION) :
	$(PRINT) Creating $(DESTINATION)
	@ $(MAKEDIR) $(DESTINATION)

# Rule 2. Build JNI .h files. Invokes rule 6.
jni : $(JNI_CLASS_FILES) $(JNI_HEADERS)

# Rule 3. Change ".build" tag to "Makefile", thus call the package makefile
# which in turn recalls this makefile with target all (rule 0).
%.build :
	$(PRINT) Building directory $(subst .build,' ',$@)
	@ $(MAKE) -k -f $(subst .build,Makefile,$@) all

# Rule 4. Call rule 3 for every package
buildall : $(PLIST_BUILD)
	$(PRINT) Done build.

#
# Specific dependency build rules
#

# Rule 5. Building a .class file from a .java file
$(PACKAGE_DIR)/%.class : $(JAVA_SRC_DIR)/$(PACKAGE_LOC)/%.java
	$(PRINT) Compiling $*.java
	if [ -n "$(IGNORE_CYCLES)" ] ; then \
	  $(JAVAC) $(JAVAC_OPTIONS) -sourcepath $(JAVA_SRC_DIR) $< ; \
	else  \
	  $(JAVAC) $(JAVAC_OPTIONS) -sourcepath $(PACKAGE_LOC) $< ; \
	fi

# Rule 6. Building a .class file from a .java file. Invokes rule 5.
%.class : $(JAVA_SRC_DIR)/$(PACKAGE_LOC)/%.java
	@ $(MAKE) -k $(PACKAGE_DIR)/$@

# Rule 9. Default behaviour within a package: Simply copy the object from src
# to classes. Note that the location of this rule is important. It must be after
# the package specifics.
$(PACKAGE_DIR)/% : $(JAVA_DIR)/$(PACKAGE_LOC)/%
	$(MAKEDIR)  $(PACKAGE_DIR)
	$(PRINT) Copying $*
	$(COPY) $< $@
	$(CHMOD) u+rw $<

#
# Cleanups
#

# Rule 10. Remove all produced files (except javadoc)
cleanall :
	$(DELETE) $(PACKAGE_DIR)/*.class $(OTHER_FILES) $(JNI_HEADERS)


# Rule 11. Change ".clean" tag to "Makefile", thus call the package makefile
# which in turn recalls this makefile with target cleanall (rule 10).
%.clean :
	$(MAKE) -k -f $(subst .clean,Makefile,$@) cleanall


# Rule 12: Call rule 11 for every package directory
clean : $(PLIST_CLEAN)
	$(PRINT) Done clean.

#
# JAR file related stuff
#

# Rule 13. Build a jar file. $* strips the last phony .JAR extension.
# Copy all the required directories to a temp dir and then build the 
# JAR from that. The -C option on the jar command recurses all the
# directories, which we don't want because we want to control the 
# packaging structure. 
%.JAR :
	@ $(MAKEDIR) $(JAR_DIR) $(JAR_TMP_DIR)
	$(PRINT) Deleting the old JAR file
	@ $(DELETE) $(JAR_DIR)/$*
	$(PRINT) Building the new JAR file $*
	@ $(RMDIR) $(JAR_TMP_DIR)/*
	if [ -n "$(JAR_CLASS_FILES)" ] ; then \
	  for X in $(JAR_CONTENT) ; do \
	    $(MAKEDIR) $(JAR_TMP_DIR)/"$$X" ; \
	    $(COPY) $(CLASS_DIR)/"$$X"/*.* $(JAR_TMP_DIR)/"$$X" ; \
	  done ; \
	fi
	if [ -x $(ECLIPSE_DIR)/plugins/$(subst _$(JAR_VERSION).jar,$(EMPTY),$*) ] ; then \
	  $(COPY) -r $(ECLIPSE_DIR)/plugins/$(subst _$(JAR_VERSION).jar,$(EMPTY),$*)/* $(JAR_TMP_DIR); \
	fi
	if [ -n "$(INCLUDE_JARS)" ] ; then \
	  for X in $(INCLUDE_JARS) ; do \
	    $(COPY) $(JAR_DIR)/"$$X" $(JAR_TMP_DIR) ; \
	  done ; \
	fi
	if [ -n "$(INCLUDE_LIBS)" ] ; then \
	  for X in $(INCLUDE_LIBS) ; do \
	    $(COPY) $(LIB_DIR)/"$$X" $(JAR_TMP_DIR) ; \
	  done ; \
	fi
	$(JAR) $(JAR_OPTIONS) $(JAR_MANIFEST) $(JAR_DIR)/$* $(JAR_CONTENT_CMD)

# Rule 14. Create given jar file by invoking its Makefile which triggers
# rule 13
%.jar :
	$(PRINT) Building JAR file $@
	@ $(MAKE) -k -f $(patsubst %,$(JAR_MAKE_DIR)/Makefile.$*,$@) $(patsubst %,$*_$(JAR_VERSION).jar,$@).JAR
	$(PRINT) Cleaning up
	@ $(RMDIR) $(JAR_TMP_DIR)


# Rule 15. Create all jar files by invoking rule 14
jar : $(JARS)
	$(PRINT) Done jars.


# Rule 16. Build javadoc for all listed packages
javadoc :
	@ $(MAKEDIR) $(JAVADOC_DIR)
	$(PRINT) Cleaning out old docs
	@ $(RMDIR) $(JAVADOC_DIR)/*
	@ $(PRINT) $(JAVADOC_PACKAGES) > packages.tmp
	$(PRINT) Starting Javadoc process
	@ $(JAVADOC) $(JAVADOC_OPTIONS) @packages.tmp
	@ $(DELETE) packages.tmp
	$(PRINT) Done JavaDoc.

# Rule 17. A combination of steps used for automatic building
complete : clean buildall jar javadoc

# Rule 18. Install the JAR files after we have created them
install: $(JAR_INSTALL_DIR)
	$(PRINT) Copying JAR files to $(JAR_INSTALL_DIR)
	$(COPY) $(JAR_DIR)/* $(JAR_INSTALL_DIR)

# Rule 19. Copy the properties files to the classes directory
properties: $(OTHER_FILES)

