package ru.mipt.java2017.hw2;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Check whether number is prime.
 * @author Eugene Gostkin
 * @version 0.1
 */
public class Checker {
  private final ExecutorService executor;

  /**
   * Class constructor
   * @param threads - amount of threads
   */
  public Checker(int threads) {
    executor = Executors.newFixedThreadPool(threads);
  }

  /**
   * Main function to check if the number is prime.
   * @param number - the number
   * @return true if number is prime
   */
  public boolean isPrime(long number) {
    if (number <= 1)
      return false;

    for (int num = 2; num * num <= number; ++num)
      if (number % num == 0)
        return false;

    return true;
  }

  /**
   * Function for further calculation
   * @param number - a number to check
   * @return Future
   */
  public Future<Boolean> isPrimePromise(long number) {
    return executor.submit(() -> isPrime(number));
  }
}
