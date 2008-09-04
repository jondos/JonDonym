JARS=/opt/java/log4j/log4j.jar:/opt/java/BouncyCastle/BouncyCastleLightForJAP.jar:/opt/java/http/http.jar:/opt/java/jai/jai_core.jar:/opt/java/jaf/activation.jar:/opt/java/mail/mail.jar:/opt/java/Jama/Jama.jar

JAVAC=/opt/java/jdk1.6/bin/javac
JAVACOPTS=-classpath $(JARS):./src/ -O -target 1.6 -g:none
JAVACOPTS_DEBUG=-classpath $(JARS):./src/ -target 1.5 -g 
#JAVACOPTS=-classpath $(JARS) -target 1.4 -g
JAR=/opt/java/jdk1.6/bin/jar
JAROPTS=i
GCJ=gcj-3.4
GCJOPTS=--main=infoservice.InfoService -classpath ./src/:$(JARS):$(HOME)/jaxp.jar $(HOME)/jaxp.jar $(HOME)/crimson.jar /opt/java/Bzip2/ApacheBzip2.jar /opt/java/Jama/Jama.jar /opt/java/log4j/log4j.jar /opt/java/BouncyCastle/BouncyCastleLightForJAP.jar /opt/java/http/http.jar 
#and -l-org-xml-sax.

InfoService.jar: ./src/*.java ./src/*/*.java
	rm -f MixISTest.java
	rm -r -f ./src/test/
	$(JAVAC) $(JAVACOPTS) ./src/infoservice/*.java ./src/anon/infoservice/*.java
	$(JAR) -cf InfoService.jar -C src . certificates/*.cer certificates/*/*.cer
	$(JAR) -i InfoService.jar

clean:
	rm -f ./src/*.class
	rm -f ./src/*/*/*.class
	rm -f ./src/*/*/*/*.class
	rm -f ./src/*/*.class
	rm -f *.jar

debug: ./src/*.java ./src/*/*.java
	rm -f MixISTest.java
	rm -r -f ./src/test/
	$(JAVAC) $(JAVACOPTS_DEBUG) ./src/infoservice/*.java ./src/anon/infoservice/*.java
	$(JAR) -cf InfoService.jar -C src . certificates/*.cer
	$(JAR) -i InfoService.jar

#gcj: ./src/*/*.java
#	rm -f MixISTest.java
#	rm -r -f ./src/test/
#	$(GCJ) $(GCJOPTS) ./src/infoservice/*.java ./src/infoservice/tor/*.java ./src/anon/infoservice/*.java ./src/anon/tor/ordescription/*.java ./src/logging/*.java ./src/anon/crypto/*.java ./src/anon/util/*.java ./src/infoservice/japforwarding/*.java ./src/anon/AnonServerDescription.java ./src/anon/AnonServiceEventListener.java ./src/anon/*.java ./src/anon/AnonChannel.java ./src/anon/client/*/*.java ./src/anon/pay/*.java ./src/anon/pay/xml/*.java ./src/anon/client/*.java ./src/anon/util/captcha/*.java ./src/anon/crypto/tinytls/*.java ./src/anon/crypto/tinytls/*/*.java ./src/anon/shared/*.java ./src/captcha/*.java ./src/captcha/graphics/*.java

gcj: ./src/*/*.java
	rm -f MixISTest.java
	rm -r -f ./src/test/
	$(GCJ) $(GCJOPTS) ./src/infoservice/*.java ./src/infoservice/*/*.java ./src/anon/infoservice/*.java ./src/anon/tor/ordescription/*.java ./src/logging/*.java ./src/anon/crypto/*.java ./src/anon/util/*.java ./src/anon/AnonServerDescription.java ./src/anon/util/captcha/*.java ./src/captcha/*.java ./src/captcha/graphics/*.java
    