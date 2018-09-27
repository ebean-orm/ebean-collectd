package io.ebean.metrics.collectd;


import io.ebean.Ebean;
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

    reporter = CollectdReporter.forRegistry(Ebean.getDefaultServer())
      .withHostName("foobar")
      .withDestHostName("localhost")
      .withDestPort(25826)
      .build();

    reporter.report(60);
  }


}


