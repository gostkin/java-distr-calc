package ru.mipt.java2017.hw2;

import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class for server side of the application.
 * @author Eugene Gostkin
 * @version 0.1
 */
public class Server {
  private final static Logger logger = LoggerFactory.getLogger("Server");

  private final int port, threads;
  private final io.grpc.Server server;

  private Server(int threads, int port) {
    this(ServerBuilder.forPort(port), threads, port);
  }

  private Server(ServerBuilder<?> builder, int threads, int port) {
    this.threads = threads;
    this.port = port;

    server = builder.addService(new SumSolver(threads)).build();
  }

  private void start() throws IOException {
    logger.info("Starting server on {}", port);
    server.start();
    logger.info("Server started on {}", port);

    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        System.err.println("*** shutting down gRPC server since JVM is shutting down");
        Server.this.stop();
        System.err.println("*** server shut down");
      }
    });
  }

  private void stop() {
    if (server != null)
      server.shutdown();
  }

  private void blockUntilShutdown() throws InterruptedException {
    if (server != null)
      server.awaitTermination();
  }

  /**
   * Class for request handling.
   */
  private static class SumSolver extends SumSolverGrpc.SumSolverImplBase {
    private final Checker checker;

    /**
     * Constructor of request handler.
     * @param threads - amount of cores
     */
    private SumSolver(int threads) {
      checker = new Checker(threads);
    }

    /**
     * Calculates the result on segment denoted by request. Uses trivial checker for each number
     * from the segment
     * @param request - request
     * @param observer - receives notifications
     */
    @Override
    public void getSum(SumRequest request, StreamObserver<SumResponse> observer) {
      logger.info("Got request to calculate on sefment {}, {}.", request.getLeft(), request.getRight());

      Queue<Future<Boolean>> promises = new LinkedList<>();

      for (long number = request.getLeft(); number <= request.getRight(); ++number)
        promises.add(checker.isPrimePromise(number));

      long ret = 0, number = request.getLeft();

      while (!promises.isEmpty()) {
        Future<Boolean> promise = promises.remove();
        try {
          if (promise.get())
            ret += number;
        } catch (Exception exception) {
          logger.error(exception.getMessage());
          observer.onError(exception);
        }

        ++number;
      }

      observer.onNext(SumResponse.newBuilder().setSum(ret).build());
      observer.onCompleted();
    }
  }

  /**
   * Basic function to start the server
   * @param args - amount of cores and port to listen.
   * @throws Exception - throws if something goes wrong.
   */
  public static void main(String[] args) throws Exception {
    if (args.length != 2) {
      logger.error("Invalid arguments, valid count is 2: amount of cores and port.");
      System.exit(1);
    }

    int threads = Integer.parseInt(args[0]);
    int port = Integer.parseInt(args[1]);

    Server server = new Server(threads, port);
    server.start();
    server.blockUntilShutdown();
  }
}
