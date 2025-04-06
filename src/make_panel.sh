
#!/bin/bash


# stop on error
set -e

# Debugging:
# set -x


# needed packages:


# compile all binaries for all platforms
pwd
export MAKEOPTIONS=--silent
export MAKETARGETS="clean all"

# optimize all compiles, see makefiles
#export MAKE_CONFIGURATION=DEBUG
export MAKE_CONFIGURATION=RELEASE


(
# All classes and resources for all Java panels into one jar
# sudo apt-get install ant openjdk-7-jdk
cd 09_javapanelsim
ant -f build.xml compile jar
)



echo
echo "All OK!"
