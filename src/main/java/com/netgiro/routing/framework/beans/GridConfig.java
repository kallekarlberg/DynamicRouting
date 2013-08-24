package com.netgiro.routing.framework.beans;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.netgiro.routing.framework.SystemType;

public class GridConfig {

    private final Map<SystemType,List<Link>> gridConfig = new HashMap<SystemType, List<Link>>();

    public GridConfig(Grid theGrid) {
        for (System s : theGrid.getSystem()) {
            if ( s.getName().equalsIgnoreCase( SystemType.DDOnline.name() ))
                gridConfig.put( SystemType.DDOnline, s.getLink() );
            if ( s.getName().equalsIgnoreCase( SystemType.EFTOnline.name() ))
                gridConfig.put( SystemType.EFTOnline, s.getLink() );
            if ( s.getName().equalsIgnoreCase( SystemType.PTS.name() ))
                gridConfig.put( SystemType.PTS, s.getLink() );
            if ( s.getName().equalsIgnoreCase( SystemType.PayoutOnline.name() ))
                gridConfig.put( SystemType.PayoutOnline, s.getLink() );
        }
    }
}
