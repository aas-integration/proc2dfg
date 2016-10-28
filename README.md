First implementation of DFG construction. Creates one dfg per method and inlines the DFGs of all procedures that are in the same class.

Use -j to specify the class dir, jar, or apk to be analyzed. 
Use -o to specify the folder where the dot files should be saved
Use -pdf if the pdfs for the dot files should be generated as well.

run with 
	
	./gradlew jar
	cd build/libs/
	java -jar prog2dfg.jar -j ../classes/main -o foobar -pdf

