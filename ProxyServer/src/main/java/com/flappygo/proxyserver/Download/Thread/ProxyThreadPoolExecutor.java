package com.flappygo.proxyserver.Download.Thread;


import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

/************************************************
 * Created by yang on 2015/12/9. 执行线程的线程池
 ************************************************/


public class ProxyThreadPoolExecutor extends ScheduledThreadPoolExecutor {

    //当前正在执行或者即将执行的线程
    private ConcurrentHashMap<String, Thread> lthreads;

    //线程池的监听
    private ProxyThreadPoolListener listener;

    public ProxyThreadPoolExecutor(int corePoolSize) {
        super(corePoolSize);
        init();
    }

    public ProxyThreadPoolExecutor(int corePoolSize, RejectedExecutionHandler handler) {
        super(corePoolSize, handler);
        init();
    }

    public ProxyThreadPoolExecutor(int corePoolSize, ThreadFactory threadFactory) {
        super(corePoolSize, threadFactory);
        init();
    }

    public ProxyThreadPoolExecutor(int corePoolSize, ThreadFactory threadFactory,
                                   RejectedExecutionHandler handler) {
        super(corePoolSize, threadFactory, handler);
        init();
    }

    //获取监听
    public ProxyThreadPoolListener getProxyThreadPoolListener() {
        return listener;
    }

    //设置监听
    public void setProxyThreadPoolListener(ProxyThreadPoolListener listener) {
        this.listener = listener;
    }

    //初始化线程map
    private void init() {
        lthreads = new ConcurrentHashMap<String, Thread>();
    }

    //执行某个线程
    public void execute(Thread thread) {

        if (thread instanceof Thread) {
            // 添加到保存的hashMap中
            addThreadToHashMap(thread);
            // 动态代理，当线程执行完成后进行移除操作
            ProxyThreadInterceptor transactionInterceptor = new ProxyThreadInterceptor();
            transactionInterceptor.setTarget(thread);
            transactionInterceptor
                    .setThreadListener(new ProxyThreadInterceptor.ThreadListener() {

                        @Override
                        public void death(Thread thread) {
                            removeThreadFromHashMap(thread);
                        }

                        @Override
                        public void began(Thread thread) {

                        }
                    });
            Class<?> classType = transactionInterceptor.getClass();
            Object userServiceProxy = Proxy.newProxyInstance(
                    classType.getClassLoader(), Thread.class.getInterfaces(),
                    transactionInterceptor);
            super.execute((Runnable) userServiceProxy);
        } else {
            super.execute(thread);
        }
    }

    //添加线程到HashMap中
    private void addThreadToHashMap(Thread thread) {
        // 加入到当前正在进行的线程中
        synchronized (lthreads) {
            lthreads.put(Long.toString(thread.getId()), thread);
        }
    }

    //从hashmap中移除线程
    private void removeThreadFromHashMap(Thread thread) {
        Thread t = thread;
        String key = Long.toString(t.getId());
        // 保证原子操作
        synchronized (lthreads) {
            if (lthreads.containsKey(key)) {
                this.lthreads.remove(key);
            }
        }
        //每个线程执行完毕都会调用
        if (listener != null) {
            listener.threadDone(lthreads.size(), key);
        }
        //线程已经执行完成了
        if (lthreads.size() == 0) {
            //监听
            if (listener != null) {
                listener.threadNomore();
            }
        }
    }

    @Override
    public boolean remove(Runnable task) {
        if (task instanceof Thread) {
            // 从HashMap中移除某个线程
            removeThreadFromHashMap((Thread) task);
        }
        return super.remove(task);
    }

    //获取当前所有的线程
    public List<Thread> getAllThread() {
        List<Thread> ret = new ArrayList<Thread>();
        // 加锁进行原子操作
        synchronized (lthreads) {
            Iterator<Entry<String, Thread>> iterator = lthreads.entrySet().iterator();
            while (iterator.hasNext()) {
                //下一个
                Entry<String, Thread> entry = iterator.next();
                //线程
                Thread thread = entry.getValue();
                //添加
                ret.add(thread);
            }
        }
        return ret;
    }

    public List<Runnable> shutdownNow() {
        synchronized (lthreads) {
            lthreads.clear();
        }
        return super.shutdownNow();
    }
}
