DESTDIR ?= $(HOME)/bin
default: all

all: 
		@echo Building ...
		sbt assembly
		mkdir bin -p
		cp target/scala-2.13/noergler*.jar bin/.
		cat ./contrib/exec_dummy bin/noergler*.jar > bin/noergler
		chmod +x bin/noergler
		
install:
		install -m 0755 -d $(DESTDIR)
		install -m 0755 bin/noergler $(DESTDIR)

clean:
		rm -rf bin/
		rm -rf target/
