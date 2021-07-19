# jnr-winfsp

A Java binding for WinFsp using Java Native Runtime (JNR)

_This is a work in progress and is currently not working correctly (help is appreciated :)_

This is inspired by jnr-fuse and winfspy.

You will need to install WinFsp ( http://www.secfs.net/winfsp/rel/ ) in order to work with this library.

To perform a quick test, install Maven ( https://maven.apache.org/ ) and run the following commands at the top directory
of this repository:
```
mvn test-compile
mvn -D"exec.mainClass=com.github.jnrwinfspteam.jnrwinfsp.MainTest" -D"exec.classpathScope=test" exec:java
```
