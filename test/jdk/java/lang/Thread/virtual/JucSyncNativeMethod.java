/*
 * Copyright (c) 2025, Alibaba Group Holding Limited. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/**
 * @test
 * @summary Test juc lock used by virtual threads with native frame context
 * @library /test/lib
 * @run main/othervm/native -Xint JucSyncNativeMethod
 * @run main/othervm/native -Xcomp -XX:CompileOnly=JucSyncNativeMethod::runFromNative JucSyncNativeMethod
 */

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.atomic.AtomicInteger;

import jdk.test.lib.Asserts;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class JucSyncNativeMethod {
    static final Lock lock = new ReentrantLock();
    static final int VTHREAD_COUNT = Runtime.getRuntime().availableProcessors() - 1; // 1 is used for Main
    static AtomicInteger counter = new AtomicInteger(0);

    private static native void runFromNative(Runnable runnable);

    private static void runUpcall(Runnable runnable) {
        lock.lock();
        runnable.run();
        lock.unlock();
    }

    public static void main(String[] args) throws Exception {
        System.loadLibrary("JucSyncNativeMethod");
        Thread t = Thread.ofVirtual().name("Main").start(()->{
            Thread[] vthreads = new Thread[VTHREAD_COUNT];
            try {
                startVThreads(vthreads);
                // Wait for all vthreads to finish
                for (int i = 0; i < VTHREAD_COUNT; i++) {
                    vthreads[i].join();
                }
            } catch (Exception e) {
                // TODO: handle exception
            }
            Asserts.assertTrue(counter.get() == VTHREAD_COUNT, counter.get() + "!=" + VTHREAD_COUNT);
        });
        t.join();
    }

    private static void startVThreads(Thread[] vthreads) throws Exception {
        lock.lock();
        for (int i = 0; i < VTHREAD_COUNT; i++) {
            var started = new CountDownLatch(1);
            var vthread = Thread.ofVirtual().name("vthread-"+(i+1)).start(() -> {
                started.countDown();
                runFromNative(() -> LockSupport.parkNanos(1));
                counter.getAndIncrement();
            });
            // wait for thread to start
            started.await();
            await(vthread, Thread.State.WAITING);
            vthreads[i] = vthread;
        }
        runFromNative(() -> LockSupport.parkNanos(1));
        lock.unlock();
    }

    private static void await(Thread thread, Thread.State expectedState) {
        Thread.State state = thread.getState();
        while (state != expectedState) {
            Asserts.assertTrue(state != Thread.State.TERMINATED, "Thread has terminated");
            Thread.yield();
            state = thread.getState();
        }
    }
}
