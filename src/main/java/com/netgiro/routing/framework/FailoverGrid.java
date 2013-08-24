package com.netgiro.routing.framework;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import javax.xml.bind.JAXBContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netgiro.routing.framework.beans.Grid;
import com.netgiro.routing.framework.beans.GridConfig;
import com.netgiro.routing.framework.exception.SystemDownException;
import com.netgiro.routing.framework.tansport.DataPipe;
import com.netgiro.utils.application.AppInfo;

/**
 * The Router is simply a map of link groups one "service" several "service points"
 * The Router needs to keep track of how each link is doing. The links in turn needs to
 * keep track of how its doing. See doc in link classes for how this is done
 * 
 * Lots and lots of threads...
 *
 */
public class FailoverGrid {

    private static Logger cLogger = LoggerFactory.getLogger(FailoverGrid.class);

    final List<DataPipe> iLinks = new ArrayList<DataPipe>();

    DataPipe iCurrentLink;

    volatile Object mutex = "mutex";
    long iPingInterval = 5000; //TODO
    long iPingTimeout = 100; //TODO
    AppInfo iAppInfo;

    //<link cfg: url:order:ping_interval:ping timeout>
    private int iLocalSwitchAfterNbrTimeouts; //TODO
    private int iPanicSwitchAfterNbrTimeouts; //TODO


    private LinkMonitor iMonitor;

    private GridConfig iGridConfig;

    GridConfig theGrid;
    public FailoverGrid( File xmlCfgFile ) {
        theGrid = buildGrid(xmlCfgFile);
    }

    private static GridConfig buildGrid(File xmlCfgFile) {
        try {
            JAXBContext jaxbCtx = JAXBContext.newInstance(Grid.class);
            Grid g = (Grid) jaxbCtx.createUnmarshaller().unmarshal(xmlCfgFile);
            return new GridConfig(g);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
    //
    //    public FailoverGrid( Configuration aCfg ) {
    //        buildGridFromCfg(aCfg);
    //        startupCheck();
    //        startMonitor();
    //    }

    private static void startupCheck() {
        // TODO Auto-generated method stub
    }

    private void startMonitor() {
        if ( iMonitor == null ) {
            cLogger.info("starting monitor");
            iMonitor = new LinkMonitor();
            new Thread(iMonitor,"LinkMonitor").start();
        }
    }

    public Object sendData(Object data, SystemType type, long timeout) throws TimeoutException, SystemDownException {
        return null;
    }

    public DataPipe getMyCurrentLink() {
        return iCurrentLink;
    }

    //    private void buildGridFromCfg(Configuration cfg) {
    //        try {
    //            iGridConfig = new GridConfig(cfg);
    //        } catch ( Exception e ) {
    //            throw new NgConfigException("Unable to build router from cfg",e);
    //        }
    //    }

    public static void main(String[] args) {
        File f = new File(FailoverGrid.class.getResource("/DynamicRoutingTable.xml").getFile());
        FailoverGrid g = new FailoverGrid(f);
        System.err.println("hello");
    }

    /**
     * This guy pings all links to monitor their status. Each turn its selects a new link.
     * Don't worry about needless switching, current link is ALWAYS preferred.
     * 
     * Some special stuff: If the monitor's mutex is notified it assumes that some problem
     * Occurred for some link and directly tries to find a good link
     *
     */
    class LinkMonitor implements Runnable {

        boolean isRunning = true;

        public void run() {
            while ( isRunning ) {
                try {
                    for ( DataPipe link : iLinks ) {
                        cLogger.debug("pinging: "+link);
                        link.ping( iPingTimeout);
                    }
                    iCurrentLink = selectMyLink(iLinks, iCurrentLink);
                    try {
                        long startTime = System.currentTimeMillis();
                        synchronized (mutex) {
                            mutex.wait(iPingInterval);
                        }
                        if ( System.currentTimeMillis()-startTime < iPingInterval-10
                                && isRunning ) { // assume notified
                            cLogger.warn("suspecting link problems, checking liks");
                            iCurrentLink = selectMyLink(iLinks, iCurrentLink);
                        }
                    } catch (InterruptedException e) {
                        //no big deal
                    }
                } catch ( Exception e ) {
                    cLogger.error("problems pinging links!",e);
                }
            }
            cLogger.warn("monitor stopped!");
        }
    }


    /**
     * Only switch "site" if the link is VERY bad otherwise switch locally
     */
    DataPipe selectMyLink(List<DataPipe> links,
            DataPipe myCurrent ) {
        if ( !myCurrent.isPipeOk() ||
                myCurrent.getNbrFailedPing() >= iPanicSwitchAfterNbrTimeouts ) { // my link is bad or very slow -> switch to anywhere!
            cLogger.warn("current link is BAD! Try to find new good link");
            DataPipe maybeFound = findHighestPrioGoodLink(links, myCurrent);
            if ( maybeFound == null ) {
                cLogger.warn("NO other good links found, sticking with current :-(");
                return myCurrent;
            }
            cLogger.warn("FOUND GOOD link, swithing to: "+maybeFound);
            return maybeFound;
        } else if ( myCurrent.getNbrFailedPing() >= iLocalSwitchAfterNbrTimeouts ) { // my link is pretty slow -> local switch
            cLogger.warn("current link is SLOW! Try to find new good LOCAL link");
            return getLocalBetterLink(links, myCurrent );
        }
        return myCurrent; // ok link!
    }

    private DataPipe findHighestPrioGoodLink(List<DataPipe> links, DataPipe current ) {
        DataPipe best = null;
        cLogger.warn("trying to find new link. link is: "+current.getPipeInfo() );

        for (DataPipe link : links) {
            if ( !link.isPipeOk()
                    || link.getNbrFailedPing() >= iPanicSwitchAfterNbrTimeouts ) //bad or slow link, ignore!
                continue;
            if ( best == null )
                best = link; //first good link...
            if ( link.getPriority() < best.getPriority() ) {
                best = link; // found good link with higher prio == better
            }
        }
        return best;
    }

    /**
     * Supports local switch
     */
    private DataPipe getLocalBetterLink( List<DataPipe> links, DataPipe current ) {
        cLogger.warn("trying to find better local link. link is: "+current.getPipeInfo() );

        for (DataPipe link : links) {
            if ( link.isPipeOk() && //never select a bad link
                    link.getPriority() == current.getPriority() &&
                    link.getNbrFailedPing() < current.getNbrFailedPing() &&
                    link.getNbrFailedPing() < iLocalSwitchAfterNbrTimeouts )
                return link; // no need to select best, just better
        }
        cLogger.warn("Could not find any better link sticking with: "+current.getPipeInfo() );
        return current; //if we cant find a better stick with the same
    }

    /**
     * Sorry for the double constructor, needed for junit...
     */
    FailoverGrid( boolean doMonitor, long pingInterval, long pingTO,
            int localSwitchAfterNbrTO, int panicAfterNbrTO  ) { //junit
        iPingInterval = pingInterval;
        iPingTimeout = pingTO;
        iLocalSwitchAfterNbrTimeouts = localSwitchAfterNbrTO;
        iPanicSwitchAfterNbrTimeouts = panicAfterNbrTO;
        if ( doMonitor ) {
            startMonitor();
        }
    }

    void addLink(DataPipe l) {
        iLinks.add(l);
        iCurrentLink = l;
    }

    public void stopMonitor() {
        if ( iMonitor != null ) {
            cLogger.info("Stopping monitor...");
            iMonitor.isRunning = false;
            synchronized (mutex) {
                mutex.notify();
            }
        }
    }
}
