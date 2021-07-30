# jnr-winfsp

A Java binding for WinFsp using Java Native Runtime (JNR)

This is inspired by jnr-fuse and winfspy.

You will need to install WinFsp ( http://www.secfs.net/winfsp/rel/ ) in order to work with this library.

To run the in-memory file system, install Maven ( https://maven.apache.org/ ) and run the following commands at the top directory
of this repository:
```
mvn compile
mvn -D"exec.mainClass=com.github.jnrwinfspteam.jnrwinfsp.memfs.WinFspMemFS" exec:java
```