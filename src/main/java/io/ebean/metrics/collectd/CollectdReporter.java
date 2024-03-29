package io.ebean.metrics.collectd;

import io.ebean.Database;
import io.ebean.meta.BasicMetricVisitor;
import io.ebean.meta.MetaCountMetric;
import io.ebean.meta.MetaQueryMetric;
import io.ebean.meta.MetaTimedMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.time.Clock;
import java.util.concurrent.TimeUnit;

/**
 * A reporter which publishes the Ebean metrics to a Collectd server.
 *
 * <pre>@{code
 *
 *   CollectdReporter.forServer(Ebean.getDefaultServer())
 *       .withHost("hostContainerName")
 *       .withCollectdHost("localhost")
 *       .withCollectdPort(25826)
 *       .withSecurityLevel(SecurityLevel.ENCRYPT)
 *       .withUsername("user")
 *       .withPassword("secret")
 *       .reportEvery(60);
 *
 * }</pre>
 */
public class CollectdReporter {

  public static Builder forServer(Database database) {
    return new Builder(database);
  }

  public static class Builder {

    private final Database database;
    private String collectdHost;
    private int collectdPort = 25826;
    private String sourceHost;
    private SecurityLevel securityLevel = SecurityLevel.NONE;
    private String username = "";
    private String password = "";
    private Clock clock = Clock.systemDefaultZone();
    private String prefixQuery = "db.query.";

    private Builder(Database database) {
      this.database = database;
    }

    /**
     * Set the Collectd hostname to send the metrics to.
     */
    public Builder withCollectdHost(String host) {
      this.collectdHost = host;
      return this;
    }

    /**
     * Set the Collectd port to send the metrics to. Defaults to 25826.
     */
    public Builder withCollectdPort(int port) {
      this.collectdPort = port;
      return this;
    }

    /**
     * Set the host of the source metrics (the container host name).
     */
    public Builder withHost(String hostName) {
      this.sourceHost = hostName;
      return this;
    }

    /**
     * Set the clock to use - defaults to the system clock.
     */
    public Builder withClock(Clock clock) {
      this.clock = clock;
      return this;
    }

    /**
     * Set username for authentication to Collectd for Sign or Encrypt.
     */
    public Builder withUsername(String username) {
      this.username = username;
      return this;
    }

    /**
     * Set password for authentication to Collectd for Sign or Encrypt.
     */
    public Builder withPassword(String password) {
      this.password = password;
      return this;
    }

    /**
     * Set the security level for authentication to Collectd.
     */
    public Builder withSecurityLevel(SecurityLevel securityLevel) {
      this.securityLevel = securityLevel;
      return this;
    }

    /**
     * Set prefix to use in front of named queries.
     */
    public Builder withPrefixQuery(String prefixQuery) {
      this.prefixQuery = prefixQuery;
      return this;
    }

    /**
     * Specify how frequently to report in seconds.
     * <p>
     * This registers a task to run periodically in the background.
     * </p>
     */
    public void reportEvery(long periodSecs) {
      CollectdReporter collectdReporter = build();
      Runnable runnable = collectdReporter.reportRunnable(periodSecs);
      database.backgroundExecutor().scheduleWithFixedDelay(runnable, periodSecs, periodSecs, TimeUnit.SECONDS);
    }

    /**
     * Build and return a CollectdReporter.
     */
    public CollectdReporter build() {
      if (securityLevel != SecurityLevel.NONE) {
        if (username.isEmpty()) {
          throw new IllegalArgumentException("username is required for securityLevel: " + securityLevel);
        }
        if (password.isEmpty()) {
          throw new IllegalArgumentException("password is required for securityLevel: " + securityLevel);
        }
      }
      Sender sender = new Sender(collectdHost, collectdPort);
      return new CollectdReporter(database, sourceHost, sender, username, password, securityLevel, clock, prefixQuery);
    }
  }

  private static final Logger log = LoggerFactory.getLogger(CollectdReporter.class);
  private static final String FALLBACK_HOST_NAME = "localhost";

  private final Database database;
  private final String hostName;
  private final Sender sender;
  private final PacketWriter writer;
  private final Clock clock;
  private final String prefixQuery;

  private CollectdReporter(Database database, String hostname, Sender sender, String username, String password,
                           SecurityLevel securityLevel, Clock clock, String prefixQuery) {
    this.database = database;
    this.clock = clock;
    this.sender = sender;
    this.prefixQuery = prefixQuery;
    this.hostName = (hostname != null) ? hostname : resolveHostName();
    this.writer = new PacketWriter(sender, username, password, securityLevel);
  }

  private String resolveHostName() {
    try {
      return InetAddress.getLocalHost().getHostName();
    } catch (Exception e) {
      log.error("Failed to lookup local host name: {}", e.getMessage(), e);
      return FALLBACK_HOST_NAME;
    }
  }

  /**
   * Return a runnable to perform periodic reporting of the metrics.
   */
  public Runnable reportRunnable(long reportFreqSecs) {
    return new ReportRunner(reportFreqSecs);
  }

  private class ReportRunner implements Runnable {

    final long period;

    public ReportRunner(long reportFreqSecs) {
      this.period = reportFreqSecs;
    }

    @Override
    public void run() {
      report(period);
    }
  }

  public void report(long period) {
    log.debug("reporting metrics ...");
    long epochSecs = clock.millis() / 1000;
    MetaData metaData = new MetaData(hostName, epochSecs, period);
    try {
      connect(sender);

      BasicMetricVisitor basic = database.metaInfo().visitBasic();
      for (MetaTimedMetric timedMetric : basic.timedMetrics()) {
        reportMetric(metaData, timedMetric);
      }
      for (MetaQueryMetric queryMetric : basic.queryMetrics()) {
        reportQueryMetric(metaData, queryMetric);
      }
      for (MetaCountMetric countMetric : basic.countMetrics()) {
        reportCountMetric(metaData, countMetric);
      }

    } catch (Exception e) {
      log.warn("Error trying to send metrics to Collectd", e);
    } finally {
      disconnect(sender);
    }
  }

  private void reportCountMetric(MetaData metaData, MetaCountMetric countMetric) {
    metaData.plugin(countMetric.name());
    write(metaData.typeInstance("count"), countMetric.count());
  }

  private void reportQueryMetric(MetaData metaData, MetaQueryMetric metric) {
    String name = metric.name();
    if (name == null) {
      if (log.isTraceEnabled()) {
        log.debug("skip metric on type:{} count:{}", metric.type(), metric.count());
      }
    } else {
      metaData.plugin(prefixQuery + metric.type().getSimpleName() + "." + name);
      write(metaData.typeInstance("count"), metric.count());
      write(metaData.typeInstance("max"), metric.max());
      write(metaData.typeInstance("mean"), metric.mean());
      write(metaData.typeInstance("total"), metric.total());
    }
  }

  private void reportMetric(MetaData metaData, MetaTimedMetric timedMetric) {
    metaData.plugin(timedMetric.name());
    write(metaData.typeInstance("count"), timedMetric.count());
    write(metaData.typeInstance("max"), timedMetric.max());
    write(metaData.typeInstance("mean"), timedMetric.mean());
    write(metaData.typeInstance("total"), timedMetric.total());
  }

  private void connect(Sender sender) throws IOException {
    if (!sender.isConnected()) {
      sender.connect();
    }
  }

  private void disconnect(Sender sender) {
    try {
      sender.disconnect();
    } catch (Exception e) {
      log.warn("Error disconnecting from Collectd", e);
    }
  }

  private void write(MetaData metaData, Number... values) {
    try {
      writer.write(metaData, values);
    } catch (RuntimeException e) {
      log.warn("Failed to process metric '" + metaData.getPlugin() + "': " + e.getMessage());
    } catch (IOException e) {
      log.error("Failed to send metric to collectd", e);
    }
  }

}
