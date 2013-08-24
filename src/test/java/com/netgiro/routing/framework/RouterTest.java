package com.netgiro.routing.framework;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.netgiro.routing.framework.exception.SystemDownException;
import com.netgiro.routing.framework.tansport.DataPipe;
import com.netgiro.routing.framework.tansport.TimeoutLink;
import com.netgiro.routing.framework.transport.ControllablePipe;


public class RouterTest {

    FailoverGrid router;

    @After
    public void tearDown() throws Exception {
        if (router!= null ) {
            router.stopMonitor();
            Thread.sleep(1000);
            router=null;
        }
    }

    @Test
    public void testMonitorDetectsSwitch() throws Exception {
        BasicConfigurator.configure(); // lousy static... can only do once...
        Logger.getLogger("com.netgiro.routing").setLevel(Level.INFO);
        DataPipe pipe1 = new ControllablePipe(50,100,5,0, true,"pipe1",1); //using this will error after 4 txs
        DataPipe pipe2 = new ControllablePipe(50,100,0,5, true,"pipe2",2); //switching to this "afo" fail after 4 txs

        router = new FailoverGrid(true, 500000, 500, 5, 8);
        //will select last added as current...
        router.addLink(pipe1);
        router.addLink(pipe2);

        List<Object> responses = new ArrayList<Object>();
        Object resp = null;
        for (int i=0; i<11; i++) {
            try {
                String data = "msg_"+i;
                resp = router.getMyCurrentLink().makeTimeoutCall(data , 500);
                responses.add(resp);
                if ( i == 3 ) { //will fail on fifth
                    Assert.fail("this message "+data+" should have failed...");
                }
            } catch (SystemDownException e ) {
                Assert.assertEquals("msg_5", resp); //last ok msg
            } catch ( RuntimeException e ) {
                Assert.assertEquals("msg_2", resp); //last ok msg
            }
            Thread.sleep(500);
        }
        //pipe 1 fails at 5:th msg = msg lost 1 ping -> 3 "real" messages successfully sent
        //maybe switch cases ping -> 1 msg lost
        //pipe 2 therefore fails on 3:rd msg -> 2 msg successfully sent
        Assert.assertEquals(5, responses.size());
        Iterator<Object> iter = responses.iterator();
        for (int i=0; i<7; i++) {
            if ( i == 3 ) //msg lost
                continue;
            if ( i == 6)
                continue;
            Assert.assertEquals("msg_"+i, iter.next() );
        }
    }

    @Test
    public void testLooseOnlyOneTxPerSwitch() throws Exception {
        router = new FailoverGrid(true, 5000, 100, 3, 5);

        DataPipe pipe1 = new ControllablePipe(50,100,2,0, true,"pipe1",1); //using this will error after 1 txs
        DataPipe pipe2 = new ControllablePipe(50,100,3,0, true,"pipe2",1); //using this will error after 1 txs (one lost in ping)
        DataPipe pipe3 = new ControllablePipe(50,100,4,0, true,"pipe3",2); //using this will error after 1 txs (two lost in ping)

        //will select last added as current...
        router.addLink(pipe1);
        router.addLink(pipe2);
        router.addLink(pipe3);

        List<Object> responses = new ArrayList<Object>();
        Object resp = null;
        for (int i=0; i<6; i++) {
            try {
                String data = "msg_"+i;
                resp = router.getMyCurrentLink().makeTimeoutCall(data , 500);
                responses.add(resp);
            } catch ( RuntimeException e ) {
                //ignore
            }
            Thread.sleep(300);
        }
        //pipe 1+2+3 fails at 2:nd msg -> 3 ok msgs
        Assert.assertEquals(3, responses.size());
    }
    @Test
    public void testSwitchLocallyOnSlow() throws Exception {
        router = new FailoverGrid(true, 2000, 50, 2, 5);

        DataPipe pipe1 = new ControllablePipe(500,1000,0,0, true,"pipe1",1);
        DataPipe pipe2 = new ControllablePipe(1,2,0,0, true,"pipe2",1);
        DataPipe pipe3 = new ControllablePipe(1,2,0,0, true,"pipe3",2);

        //will select last added as current...
        router.addLink(pipe1);
        router.addLink(pipe2);
        router.addLink(pipe3);

        DataPipe first = router.getMyCurrentLink();

        //wait 3 sec for router to switch (slow after 2-3 sec)
        Thread.sleep(3000);

        DataPipe second = router.getMyCurrentLink();

        Assert.assertEquals("pipe1",first.getPipeInfo());
        Assert.assertEquals("pipe2",second.getPipeInfo());
    }

    @Test
    public void testSwitchSiteOnVerySlow() throws Exception {
        router = new FailoverGrid(true, 2000, 50, 2, 5);

        DataPipe pipe1 = new ControllablePipe(100,200,0,0, true,"pipe1",1);
        DataPipe pipe2 = new ControllablePipe(505,700,0,0, true,"pipe2",1);
        DataPipe pipe3 = new ControllablePipe(1,2,0,0, true,"pipe3",2);

        //will select last added as current...
        router.addLink(pipe1);
        router.addLink(pipe2);
        router.addLink(pipe3);

        DataPipe first = router.getMyCurrentLink();

        //wait 4x2+2 sec for router to switch (slow after 2-3 sec, very slow after 8-9 sec)
        Thread.sleep(9000);

        DataPipe second = router.getMyCurrentLink();

        Assert.assertEquals("pipe1",first.getPipeInfo());
        Assert.assertEquals("pipe3",second.getPipeInfo());
    }
    @Test
    public void testSelectBestLink() {
        router = new FailoverGrid(false, 10000, 500, 3, 5);
        List<TimeoutLink> links = new ArrayList<TimeoutLink>();
        TimeoutLink prio1 = new TimeoutLink(new ControllablePipe(1,1000,0,0,false,"pipe1") , "mutex", 1);
        TimeoutLink prio2 = new TimeoutLink(new ControllablePipe(1,1000,0,0,true,"pipe2") , "mutex", 2);
        TimeoutLink prio3 = new TimeoutLink(new ControllablePipe(1,1000,0,0,true,"pipe3") , "mutex", 3);
        TimeoutLink prio4 = new TimeoutLink(new ControllablePipe(1,1000,0,0,false,"pipe4"), "mutex", 4);

        links.add(prio1);
        links.add(prio2);
        links.add(prio3);
        links.add(prio4);

        Assert.assertEquals(prio2, router.selectMyLink(links, prio1));

        links.clear();
        prio1 = new TimeoutLink(new ControllablePipe(1,1000,0,0,true,"pipe1") , "mutex", 1);
        prio2 = new TimeoutLink(new ControllablePipe(1,1000,0,0,true,"pipe2") , "mutex", 2);

        links.add(prio1);
        links.add(prio2);
        Assert.assertEquals(prio2, router.selectMyLink(links, prio2));

        links.clear();
        prio1 = new TimeoutLink(new ControllablePipe(1,1000,0,0,false,"pipe1") , "mutex", 1);
        prio2 = new TimeoutLink(new ControllablePipe(1,1000,0,0,false,"pipe2") , "mutex", 2);
        prio3 = new TimeoutLink(new ControllablePipe(1,1000,0,0,true,"pipe3") , "mutex", 2);

        links.add(prio1);
        links.add(prio2);
        links.add(prio3);
        Assert.assertEquals(prio3, router.selectMyLink(links, prio1));
    }
    /**
     * 5000 tx in 15 sec -> 334txs/sec -> max service time -> 3 ms (ish)
     */
    @Test
    public void testMaxLoadSystem_HandleAtLeast5000Tx_In15Sec_LessThan10PercentTimeOut() throws Exception {
        router = new FailoverGrid(false, 100000, 500, 3, 5);
        DataPipe aPipe = new ControllablePipe(1,4,0,0,true,"pipe1"); //1-4 ms service time
        TimeoutLink link = new TimeoutLink(aPipe , "mutex",1);
        router.addLink(link);

        List<Hammer> tasks = new ArrayList<Hammer>();
        for (int i=0; i<50; i++) { //50 threads
            Hammer h = new Hammer(router,"thread"+i,4,10L);
            tasks.add(h);
            new Thread(h).start();
        }
        Thread.sleep(15000);
        int totalCount = 0, timeOutCount=0;
        for (Hammer hammer : tasks) {
            System.err.println("stopping hammer...");
            totalCount += hammer.stop();
            timeOutCount += hammer.toCnt;
        }
        //wait to stop...

        Thread.sleep(500);

        Assert.assertTrue("more than 5000 txs should have been processed, i managed: "+(totalCount),
                totalCount > 5000);
        Assert.assertTrue("less than 10% txs should have timed out i failed: "+(timeOutCount*1.0/(totalCount))*1000+"%",
                timeOutCount*10 < totalCount);
        System.err.println("i managed totally "+(totalCount)+" timed out "+timeOutCount);
    }

    @Test
    public void testLoadSingleLinkRacingCondition() throws Exception {
        router = new FailoverGrid(false, 100000, 500, 3, 5);
        DataPipe aPipe = new ControllablePipe(5,100,0,0,true,"pipe1");
        TimeoutLink link = new TimeoutLink(aPipe , "mutex",1);
        router.addLink(link);

        //since we are running 3 thread with up to 200 ms serice time expect to wait a bit...
        Hammer task1 = new Hammer(router,"thread 1",200, 650);
        Hammer task2 = new Hammer(router,"thread 2",200, 650);
        Hammer task3 = new Hammer(router,"thread 3",200, 650);
        Thread t1 = new Thread(task1);
        Thread t2 = new Thread(task2);
        Thread t3 = new Thread(task3);
        t1.start();
        t2.start();
        t3.start();

        Thread.sleep(20000);
        task1.stop();
        task2.stop();
        task3.stop();
        System.err.println("thread 1 caused: "+task1.toCnt+" timeouts (and "+task1.goodCnt+" ok)");
        System.err.println("thread 2 caused: "+task2.toCnt+" timeouts (and "+task1.goodCnt+" ok)");
        System.err.println("thread 3 caused: "+task3.toCnt+" timeouts (and "+task1.goodCnt+" ok)");
    }

    class Hammer implements Runnable {

        Hammer(FailoverGrid r, String aName, int maxSleepTime, long timeout) {
            name = aName;
            iMaxSleep = maxSleepTime;
            iTimeout = timeout;
            iRouter = r;
        }

        FailoverGrid iRouter;
        long iTimeout;
        int iMaxSleep;
        String name;
        AtomicBoolean isRunning = new AtomicBoolean(true);
        AtomicInteger incr = new AtomicInteger(0);
        int toCnt = 0;
        int goodCnt = 0;

        public void run() {
            while (isRunning.get()) {
                String data = name+"_dataPay_"+incr.incrementAndGet();
                try {
                    Object resp = iRouter.getMyCurrentLink().makeTimeoutCall(data, iTimeout);
                    if ( !resp.equals(data) )
                        throw new RuntimeException("data back not equal. strange...");
                    goodCnt++;
                } catch (TimeoutException e) {
                    // System.err.println("timeout!");
                    toCnt++;
                } catch ( Exception e ) {
                    throw new RuntimeException(e);
                }
                try {
                    Random rand = new Random();
                    Thread.sleep(rand.nextInt(iMaxSleep));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            System.err.println("hammer: "+name+" stopping");
        }

        public int stop() {
            isRunning.set(false);
            return toCnt+goodCnt;
        }
    }
}
