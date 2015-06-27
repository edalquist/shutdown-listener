Provides an easy API that opens up a TCP socket to listen for shutdown requests. Useful for long-running Java applications that need a way to have a shutdown requested so that the application can gracefully shut down.

Extensions to the base ShutdownHandler are included for Spring to make the graceful shutdown of a Spring managed application easier.

Latest Release: **1.0.0**
Available via Maven:
```
<dependency>
    <groupId>com.googlecode.shutdown-listener</groupId>
    <artifactId>shutdown-listener</artifactId>
    <version>1.0.0</version>
</dependency>
```