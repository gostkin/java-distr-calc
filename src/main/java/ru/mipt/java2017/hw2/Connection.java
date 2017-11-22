package ru.mipt.java2017.hw2;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.mipt.java2017.hw2.SumSolverGrpc.SumSolverFutureStub;

/**
 * Connection class for communication.
 * @author Eugene Gostkin
 * @version 0.1
 */
public class Connection {
  private static final Logger logger = LoggerFactory.getLogger("Client");

  private final ManagedChannel channel;
  private final SumSolverGrpc.SumSolverFutureStub stub;

  /**
   * Set up connection.
   * @param host - host of server
   * @param port - port of server
   */
  public Connection(String host, int port) {
    this(ManagedChannelBuilder.forAddress(host, port)
        .usePlaintext(true)
        .build()
    );
  }

  private Connection(ManagedChannel channel) {
    this.channel = channel;
    stub = SumSolverGrpc.newFutureStub(channel);
  }

  /**
   * Shutdown connection.
   * @throws InterruptedException
   */
  public void shutdown() throws InterruptedException {
    channel.shutdown().awaitTermination(4, TimeUnit.SECONDS);
  }

  /**
   * Get stub
   * @return stub
   */
  public SumSolverFutureStub getStub() {
    return stub;
  }
}
