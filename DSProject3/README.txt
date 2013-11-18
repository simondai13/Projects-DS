Our code has been precompiled to have .class files in /bin.

So, to run our example programs:
(from root directory)
this is for each potential node in the system
java -cp ./bin MainNode [Port number]
Then, for each node, ensure that the config file has the correct locations of each machine running MainNode
then, from any machine, run:
java -cp ./bin MainMaxValue
or
java -cp ./bin MainHistogram
to run our examples.

If you don't want to use our precompiled classes,
compile our code using javac (code is located in the /src directory),
then use java -cp [CLASSPATH] as above to run the programs.