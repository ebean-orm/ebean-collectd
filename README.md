# ebean-collectd
Collectd reporter for Ebean metrics

## How to setup

### Add dependency

Add the dependency for ebean-collectd:

```xml
<dependency>
    <groupId>io.ebean</groupId>
    <artifactId>ebean-collectd</artifactId>
    <version>1.2.2</version>
</dependency>
```


### Use CollectdReporter 

On starting/creating the EbeanServer instance use CollectdReporter to 
configure how to collect the metrics and where to send them to.

For example:

```java

CollectdReporter.forServer(server)
    .withHost(containerHost)
    .withCollectdHost(collectdHost)
    .withCollectdPort(25826)
    .withUsername(user)
    .withPassword(pass)
    .withSecurityLevel(SecurityLevel.ENCRYPT)
    .reportEvery(60);

```
