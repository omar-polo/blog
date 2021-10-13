JAVAC =		javac

CLASSES =	java/com/omarpolo/gemini/Request.class

.PHONY: all clean

all: ${CLASSES}

clean:
	find java -type f -iname '*.class' -exec rm {} +

.SUFFIXES: .java .class
.java.class:
	${JAVAC} $<
