package com.pro_crafting.tools.jasperreport;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class JasperReporterThreadFactoryTest {

    @Test
    public void testThreadNumbering() throws InterruptedException {

        ExecutorService executorService = Executors.newFixedThreadPool(4, new JasperReporterThreadFactory());
        executorService.execute(new ThreadNameSuffixAsserterRunnable("jasper-compiler-1"));
        executorService.execute(new ThreadNameSuffixAsserterRunnable("jasper-compiler-2"));
        executorService.execute(new ThreadNameSuffixAsserterRunnable("jasper-compiler-3"));
        executorService.execute(new ThreadNameSuffixAsserterRunnable("jasper-compiler-4"));
        executorService.awaitTermination(2, TimeUnit.SECONDS);
    }

    private static class ThreadNameSuffixAsserterRunnable implements Runnable {
        private String expectedName;

        public ThreadNameSuffixAsserterRunnable(String expectedName) {
            this.expectedName = expectedName;
        }

        @Override
        public void run() {
            System.out.println(Thread.currentThread().getName());
            Assertions.assertEquals(expectedName, Thread.currentThread().getName());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
