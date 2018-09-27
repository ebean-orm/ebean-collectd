package io.ebean.metrics.collectd;


import io.ebean.Ebean;
import io.ebean.Transaction;
import org.junit.Test;

public class CollectdReporterTest {

//    @ClassRule
//    public static Receiver receiver = new Receiver(25826);

  private CollectdReporter reporter;

//    @Before
//    public void setUp() {
//        reporter = CollectdReporter.forRegistry(registry)
//                .withHostName("eddie")
//                .build(new Sender("localhost", 25826));
//    }

  @Test
  public void reportsByteGauges() throws Exception {

    reporter = CollectdReporter.forServer(Ebean.getDefaultServer())
      .withHost("foo4bar")
      .withCollectdHost("localhost")
      .withCollectdPort(25826)
      .withSecurityLevel(SecurityLevel.ENCRYPT)
      .withUsername("user0")
      .withPassword("foo")
      .build();

    for (int i = 0; i < 10; i++) {
      try (Transaction transaction = Ebean.beginTransaction()) {
        Ebean.createSqlQuery("select 'one' as one").setLabel("aob").findOne();
        transaction.commit();
      }

    }
    reporter.report(60);
  }


}


