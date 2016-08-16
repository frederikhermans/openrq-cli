all:
	mkdir -p bin
	javac -cp libs/openrq-3.3.2.jar -d bin/ src/se/uu/it/uno/openrqcli/Main.java
	mkdir -p tmp
	unzip -q libs/openrq-3.3.2.jar -d tmp
	jar cfe openrq-cli.jar se.uu.it.uno.openrqcli.Main -C bin/ . -C tmp/ .
	rm -rf tmp
	cat header-template openrq-cli.jar > openrq-cli
	chmod +x openrq-cli
