package com.netgiro.routing.framework;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;

import com.netgiro.routing.framework.tansport.CardPesPipe;
import com.netgiro.routing.framework.tansport.DataPipe;
import com.netgiro.utils.exception.NgConfigException;

public class GridConfig {

    final List<DataPipe> iEndPoints;
    final long iPingInterval;
    final long iPingTimeOut;
    final long iReceiveTimeOut;
    final long iConnectTimeout;

    GridConfig( Configuration cfg ) {
        iPingInterval = Long.parseLong(safeGet(cfg, "PingInterval"));
        iPingTimeOut = Long.parseLong(safeGet(cfg, "PingTimeOut"));
        iReceiveTimeOut = Long.parseLong(safeGet(cfg, "ReceiveTimeOut"));
        iConnectTimeout = Long.parseLong(safeGet(cfg, "ConnectTimeOut"));
        iEndPoints = getOrderedList(cfg);
    }

    private List<DataPipe> getOrderedList(Configuration cfg) {
        String clazz = safeGet(cfg, "DataPipeClass");
        List<String> urls = cfg.getList("link.wsdl_url");
        List<String> prios = cfg.getList("link.prio");
        List<DataPipe> res = new ArrayList<DataPipe>();
        for (int i=0; i<urls.size(); i++) {
            try {
                URL u = new URL(urls.get(i));
                int prio = Integer.parseInt(prios.get(i));
                res.add( createPipe(u, clazz, iReceiveTimeOut, iConnectTimeout, prio) );
            } catch (MalformedURLException e) {
                throw new NgConfigException("Bad url! "+e);
            }
        }
        return res;
    }

    private DataPipe createPipe(URL u, String type, long receiveTimeOut,
            long connectTimeOut, int prio) {
        if ( "PTS".equalsIgnoreCase(type))
            return new CardPesPipe(u, connectTimeOut, receiveTimeOut, prio);
        throw new NgConfigException("Unknown link type! "+type);
    }

    private static String safeGet(Configuration cfg, String key) {
        String res = cfg.getString(key);
        if (StringUtils.isEmpty(res))
            throw new NgConfigException("No value for propery "+key);
        return res;
    }
}
