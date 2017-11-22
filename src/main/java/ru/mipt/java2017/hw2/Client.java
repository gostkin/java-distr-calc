package ru.mipt.java2017.hw2;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Check whether number is prime.
 * @author Eugene Gostkin
 * @version 0.1
 */
public class Client {
  private static final Logger logger = LoggerFactory.getLogger("Client");

  /**
   * Main function of the client
   * @param args - borders of segment and servers - host & port
   * @throws RuntimeException - thrown if sth is wrong
   */
  public static void main(String[] args) throws RuntimeException {
    if (args.length < 2 || args.length % 2 != 0) {
      logger.error("Invalid args, valid count is 2 + 2 * n: segment and n servers.");
      System.exit(1);
    }

    long leftBorder = Long.parseLong(args[0]);
    long rightBorder = Long.parseLong(args[1]);

    List<Connection> connections = new ArrayList<>((args.length - 2) / 2);
    for (int i = 2; i < args.length; i += 2)
      connections.add(new Connection(args[i], Integer.parseInt(args[i + 1])));

    try {
      long result = solve(leftBorder, rightBorder, connections);
      System.out.println(result);
    } catch (InterruptedException exception) {
      logger.error("Interrupted exception.");
    } finally {
      for (Connection connection : connections)
        if (connection != null)
          try {
            connection.shutdown();
          } catch (Exception exception) {
            logger.error("An exception occurred during shutdown - {}.", exception);
          }
    }
  }

  private static void prepare(long leftBorder, long rightBorder, List <Connection> connections,
      HashMap<SumRequest, ListenableFuture<SumResponse>> futures, HashMap<SumRequest, Integer> used) {

    long alive = connections.stream()
        .filter((Object object) -> (object != null))
        .count();

    if (alive == 0) {
      logger.error("No alive servers.");
      System.exit(1);
    }

    long temp = 0;
    long diff = rightBorder - leftBorder;

    for (int conn = 0; conn < connections.size(); ++conn) {
      if(connections.get(conn) == null)
        continue;

      SumRequest request = SumRequest.newBuilder()
          .setLeft(leftBorder + diff * temp / alive)
          .setRight(leftBorder + diff * (temp + 1) / alive)
          .build();

      used.put(request, conn);
      futures.put(request, connections.get(conn).getStub().getSum(request));
      ++temp;
    }
  }

  private static long solve(long leftBorder, long rightBorder, List<Connection> connections)
      throws InterruptedException {
    long result = 0;
    HashMap<SumRequest, ListenableFuture<SumResponse>> futures = new HashMap<>();
    HashMap<SumRequest, Integer> used = new HashMap<>();

    prepare(leftBorder, rightBorder, connections, futures, used);

    while (!futures.isEmpty()) {
      SumRequest request = futures.keySet().iterator().next();
      try {
        result += futures.get(request).get().getSum();
      } catch (ExecutionException exception) {
        logger.warn("Server error, ignoring {}", used.get(request));
        Connection connection = connections.get(used.get(request));
        if(connection != null) {
          connection.shutdown();
          connections.set(used.get(request), null);
        }

        prepare(request.getLeft(), request.getRight(), connections, futures, used);
      }
      finally {
        used.remove(request);
        futures.remove(request);
      }
    }

    return result;
  }
}
