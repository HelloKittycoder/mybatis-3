package org.apache.ibatis.cache;

import org.junit.jupiter.api.Test;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;

/**
 * Created by shucheng on 2019-4-10 上午 8:09
 * 学习Java中的四种引用，为了能看懂WeakCache
 * 参考链接：https://juejin.im/post/5a5129f5f265da3e317dfc08
 */
public class ReferenceTest {

    @Test
    public void testStrongReference() {
        StrongReferenceTest.printlnMemory("1.原可用内存和总内存");

        // 实例化10M数组并与strongReference建立强引用
        byte[] strongReference = new byte[10 * StrongReferenceTest.M];
        StrongReferenceTest.printlnMemory("2.实例化10M的数组，并建立强引用");
        System.out.println("strongReference:" + strongReference);

        System.gc();
        StrongReferenceTest.printlnMemory("3.GC后");
        System.out.println("strongReference:" + strongReference);

        // strongReference = null后，强引用断开了
        strongReference = null;
        StrongReferenceTest.printlnMemory("4.强引用断开后");
        System.out.println("strongReference:" + strongReference);

        System.gc();
        StrongReferenceTest.printlnMemory("5.GC后");
        System.out.println("strongReference:" + strongReference);
    }

    @Test
    public void testWeakReference() {
        WeakReferenceTest.printlnMemory("1.原可用内存和总内存");

        // 创建弱引用
        WeakReference<Object> weakReference = new WeakReference<Object>(new byte[10*WeakReferenceTest.M]);
        WeakReferenceTest.printlnMemory("2.实例化10M的数组，并建立弱引用");
        System.out.println("weakReference.get():" + weakReference.get());

        System.gc();
        WeakReferenceTest.printlnMemory("3.GC后");
        System.out.println("weakReference.get():" + weakReference.get());
    }

    @Test
    public void testSoftReference() {
        SoftReferenceTest.printlnMemory("1.原可用内存和总内存");

        // 建立软引用
        SoftReference<Object> softReference = new SoftReference<Object>(new byte[10*SoftReferenceTest.M]);
        WeakReferenceTest.printlnMemory("2.实例化10M的数组，并建立软引用");
        System.out.println("softReference.get():" + softReference.get());

        System.gc();
        WeakReferenceTest.printlnMemory("3.内存可用容量充足，GC后");
        System.out.println("softReference.get():" + softReference.get());

        // 建立软引用
        SoftReference<Object> softReference2 = new SoftReference<Object>(new byte[4*SoftReferenceTest.M]);
        WeakReferenceTest.printlnMemory("4.实例化4M的数组，并建立软引用");
        System.out.println("softReference.get():" + softReference.get());
        System.out.println("softReference2.get():" + softReference2.get());

    }

    @Test
    public void testPhantomReference() {
        PhantomReferenceTest.printlnMemory("1.原可用内存和总内存");
        byte[] object = new byte[10*PhantomReferenceTest.M];
        PhantomReferenceTest.printlnMemory("2.实例化10M的数组后");

        // 建立虚引用
        ReferenceQueue<Object> referenceQueue = new ReferenceQueue<Object>();
        PhantomReference<Object> phantomReference = new PhantomReference<Object>(object, referenceQueue);
        PhantomReferenceTest.printlnMemory("3.建立虚引用后");
        System.out.println("phantomReference.get():" + phantomReference.get());
        System.out.println("referenceQueue.poll():" + referenceQueue.poll());

        // 断开byte[10*PhantomReferenceTest.M]的强引用
        object = null;
        PhantomReferenceTest.printlnMemory("4.执行object=null;强引用断开后");

        System.gc();
        PhantomReferenceTest.printlnMemory("5.GC后");
        System.out.println("phantomReference:" + phantomReference);
        System.out.println("phantomReference.get():" + phantomReference.get());
        System.out.println("referenceQueue.poll():" + referenceQueue.poll());

        // 断开虚引用
        phantomReference = null;
        System.gc();
        PhantomReferenceTest.printlnMemory("6.断开虚引用后GC");
        System.out.println("phantomReference:" + phantomReference);
        System.out.println("referenceQueue.poll():" + referenceQueue.poll());
    }
}

// 强引用 StrongReference
class StrongReferenceTest {
    public static int M = 1024*1024;

    public static void printlnMemory(String tag) {
        Runtime runtime = Runtime.getRuntime();
        int M = StrongReferenceTest.M;
        System.out.println("\n" + tag + ":");
        System.out.println(runtime.freeMemory()/M + "M(free)/)"
            + runtime.totalMemory()/M + "M(total)");
    }
}

// 弱引用 WeakReference
class WeakReferenceTest {
    public static int M  = 1024*1024;

    public static void printlnMemory(String tag) {
        Runtime runtime = Runtime.getRuntime();
        int M = WeakReferenceTest.M;
        System.out.println("\n" + tag + ":");
        System.out.println(runtime.freeMemory()/M + "M(free)/"
            + runtime.totalMemory()/M + "M(total)");
    }
}

// 软引用 SoftReference
class SoftReferenceTest {
    public static int M  = 1024*1024;

    public static void printlnMemory(String tag) {
        Runtime runtime = Runtime.getRuntime();
        int M = SoftReferenceTest.M;
        System.out.println("\n" + tag + ":");
        System.out.println(runtime.freeMemory()/M + "M(free)/"
            + runtime.totalMemory()/M + "M(total)");
    }
}

// 虚引用 PhantomReference
class PhantomReferenceTest {
    public static int M  = 1024*1024;

    public static void printlnMemory(String tag) {
        Runtime runtime = Runtime.getRuntime();
        int M = PhantomReferenceTest.M;
        System.out.println("\n" + tag + ":");
        System.out.println(runtime.freeMemory()/M + "M(free)/"
            + runtime.totalMemory()/M + "M(total)");
    }
}