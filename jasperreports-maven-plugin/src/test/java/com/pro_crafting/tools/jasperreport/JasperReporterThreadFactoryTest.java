package com.pro_crafting.tools.jasperreport;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

class JasperReporterThreadFactoryTest {

    @Test
    void testThreadNumbering() throws InterruptedException, ExecutionException {

        List<ThreadNameRetrieverTask> tasks = new ArrayList<>();
        tasks.add(new ThreadNameRetrieverTask());
        tasks.add(new ThreadNameRetrieverTask());
        tasks.add(new ThreadNameRetrieverTask());

        ExecutorService executorService = Executors.newFixedThreadPool(2, new JasperReporterThreadFactory());
        List<Future<String>> output = executorService.invokeAll(tasks);

        Assertions.assertTrue(output.get(0).get().startsWith(JasperReporterThreadFactory.THREAD_PREFIX));
        Assertions.assertTrue(output.get(1).get().startsWith(JasperReporterThreadFactory.THREAD_PREFIX));
        Assertions.assertTrue(output.get(2).get().startsWith(JasperReporterThreadFactory.THREAD_PREFIX));


        List<Integer> threadNumbers = new ArrayList<>();

        for (Future<String> future : output) {
            threadNumbers.add(Integer.parseInt(future.get().substring(JasperReporterThreadFactory.THREAD_PREFIX.length())));
        }

        Assertions.assertTrue(threadNumbers.get(0) < threadNumbers.get(1));
        Assertions.assertTrue(threadNumbers.get(2) <= threadNumbers.get(1));
    }

    private static class ThreadNameRetrieverTask implements Callable<String> {

        @Override
        public String call() throws Exception {
            Thread.sleep(500);
            return Thread.currentThread().getName();
        }
    }
}
