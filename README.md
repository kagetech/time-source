# NTP synchronized time source

An implementation of `java.time.InstantSource` independent of the local clock and synchronized with an NTP server.

## Getting started

**Maven configuration:**

```xml
<dependency>
    <groupId>tech.kage.time</groupId>
    <artifactId>tech.kage.time.ntp</artifactId>
    <version>1.0.0</version>
</dependency>
```

**module-info.java**

```java
module my.simple.mod {
    requires tech.kage.time.ntp;
}
```

**Initialize instant source**

```java
String ntpServer = "pool.ntp.org";

java.time.InstantSource instantSource = new NtpSynchronizedInstantSource(ntpServer);

((NtpSynchronizedInstantSource) instantSource).sync();
```

**Get the current instant**

```java
java.time.Instant now = instantSource.instant();
```

## License

This project is released under the [BSD 2-Clause License](LICENSE).
