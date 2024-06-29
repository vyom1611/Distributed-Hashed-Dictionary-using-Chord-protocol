# Makefile

JAVAC = javac
JAVA = java

.SUFFIXES: .java .class

.java.class:
	$(JAVAC) $<

CLASSES = NodeImpl.java

MAIN = Main

default: classes

classes: $(CLASSES:.java=.class)

run:
	$(JAVA) -cp . Main $(ARGS)

clean:
	rm -f *.class