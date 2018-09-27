package io.ebean.metrics.collectd;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

class Sender {

  private final String host;
  private final int port;

  private InetSocketAddress address;
  private DatagramChannel channel;

  Sender(String host, int port) {
    this.host = host;
    this.port = port;
  }

  void connect() throws IOException {
    if (isConnected()) {
      throw new IllegalStateException("Already connected");
    }
    if (host != null) {
      address = new InetSocketAddress(host, port);
    }
    channel = DatagramChannel.open();
  }

  boolean isConnected() {
    return channel != null && !channel.socket().isClosed();
  }

  void send(ByteBuffer buffer) throws IOException {
    channel.send(buffer, address);
  }

  void disconnect() throws IOException {
    if (channel == null) {
      return;
    }
    try {
      channel.close();
    } finally {
      channel = null;
    }
  }

}
