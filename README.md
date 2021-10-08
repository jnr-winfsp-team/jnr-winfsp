# jnr-winfsp

A Java binding for WinFsp using Java Native Runtime (JNR)

This is inspired by jnr-fuse and winfspy.

You will need to install WinFsp ( http://www.secfs.net/winfsp/rel/ ) in order to work with this library.

## Build

To build this library, install Maven ( https://maven.apache.org/ ) and run the following command at the top
directory of this repository:
```
mvn clean package
```

The library jar file will be in `target/jnr-winfsp-<version>.jar`

A standalone executable jar file with the testing in-memory file system will be in
`target/jnr-winfsp-<version>-memfs.jar`, which can be run with:
```
java -jar target/jnr-winfsp-<version>-memfs.jar
```
