# jnr-winfsp

A Java binding for WinFsp using Java Native Runtime (JNR)

This is inspired by jnr-fuse and winfspy.

You will need to install WinFsp ( https://winfsp.dev/rel/ ) in order to work with this library.

You will need to install Maven ( https://maven.apache.org/ ) in order to build this library.

## Deploy and use locally using Maven

To deploy and use this library locally, run the following command at the top directory of this repository
(replace `<path-to-repo>` with a local path to your local Maven repository):
```
mvn clean deploy '-DaltDeploymentRepository=jnr-winfsp-repo::default::file:<path-to-repo>'
```

Then, to use it locally, assuming your project uses Maven, declare in your project's `pom.xml` file the following
(replace `path-to-repo` with a local path to your local Maven repository):
```
<repositories>
   <repository>
      <id>jnr-winfsp-repo</id>
      <url>file:path-to-repo</url>
   </repository>
</repositories>
```

## Build and test memfs

To build the jar for this library, run the following command at the top directory of this repository:
```
mvn clean package
```

The library jar file will be in `target/jnr-winfsp-<version>.jar`

A standalone executable jar file with the testing in-memory file system will be in
`target/jnr-winfsp-<version>-memfs.jar`, which can be run with:
```
java -jar target/jnr-winfsp-<version>-memfs.jar
```

## How to use

Take a look at how the testing in-memory file system (MemFS) is implemented to help you start using jnr-winfsp.
The MemFS code is [here](https://github.com/jnr-winfsp-team/jnr-winfsp/blob/main/src/main/java/com/github/jnrwinfspteam/jnrwinfsp/memfs/WinFspMemFS.java).