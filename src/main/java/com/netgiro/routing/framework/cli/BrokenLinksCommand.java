/* -----------------------------------------------------------------------------
 * Created on 11 nov 2010 by kkarlberg
 * Copyright NetGiro Systems AB
 * Version: $Id: $
 * --------------------------------------------------------------------------- */
package com.netgiro.routing.framework.cli;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import com.netgiro.routing.framework.tansport.TimeoutLink;
import com.netgiro.utils.cli.command.Command;

public class BrokenLinksCommand implements Command {

    private final Map<String, List<TimeoutLink>> iRoutingTable;

    public BrokenLinksCommand(Map<String, List<TimeoutLink>> allLinks) {
        iRoutingTable = allLinks;
    }

    public void printDescription(PrintWriter aPw) {
        aPw.println("Run to list all broken links (and how long they have been broken, oldest first");
    }

    public void printUsage(PrintWriter aPw, String aCommandName) {
        aPw.println("Run to list all broken links (and how long they have been broken, oldest first");
    }

    public void execute(PrintWriter aPw, String aArguments) {
        //TODO Impl: MyLinksCommand.execute
        throw new UnsupportedOperationException("MyLinksCommand.execute");
    }

}
