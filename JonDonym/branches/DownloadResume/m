#!/bin/sh
#This script is used for development under MacOS X
#
#####################################################
#
japjarspath="/Users/feder/Federrath/Java/libraries/japjars";
libs="${japjarspath}/http.jar:${japjarspath}/xml.jar:${japjarspath}/xml-1.1.jar:${japjarspath}/kasperftp.jar:.";
#
#####################################################
action=$1;
nocommand="0";
if [ -z "${action}" ]; then
 nocommand="1"
fi
while [ 1 ]
do
if [ "${nocommand}" = "1" ]; then
 echo "Usage: m [ make | clean | start | lean ]"
 echo " (m)ake  ... make all class files"
 echo " (c)lean ... remove all class files"
 echo " (s)tart ... start JAP"
 echo " (l)ean  ... start JAPLean"
 echo " (t)est  ... test something"
 read  action
fi
case "${action}" in
 "m")  action="make";;
 "c")  action="clean";;
 "s")  action="start";;
 "l")  action="lean";;
 "t")  action="test";;
esac
if [ "${action}" = "make" ]; then
# javac -classpath ${libs} JAPDebug.java
 javac -classpath ${libs} anon/*.java
 javac -classpath ${libs} rijndael/*.java
 javac -classpath ${libs} rmi/*.java
 javac -classpath ${libs} *.java
fi
if [ "${action}" = "clean" ]; then
 rm -r *.class
 rm -r anon/*.class
 rm -r rijndael/*.class
 rm -r rmi/*.class
fi
if [ "${action}" = "start" ]; then
 java -classpath ${libs} JAP &
fi
if [ "${action}" = "lean" ]; then
 java -classpath ${libs} JAPLean 4001 mix.inf.tu-dresden.de 6544
fi
if [ "${nocommand}" = "0" ]; then
 break;
fi
if [ "${action}" = "test" ]; then
 java -classpath ${libs} JAPInfoService
fi
done




