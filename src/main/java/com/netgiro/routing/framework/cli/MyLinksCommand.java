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

public class MyLinksCommand implements Command {

    private final Map<String, List<TimeoutLink>> iRoutingTable;

    public MyLinksCommand(Map<String, List<TimeoutLink>> allLinks) {
        iRoutingTable = allLinks;
    }

    public void printDescription(PrintWriter aPw) {
        //TODO Impl: MyLinksCommand.printDescription
        throw new UnsupportedOperationException(
        "MyLinksCommand.printDescription");
    }

    public void printUsage(PrintWriter aPw, String aCommandName) {
        //TODO Impl: MyLinksCommand.printUsage
        throw new UnsupportedOperationException("MyLinksCommand.printUsage");
    }

    public void execute(PrintWriter aPw, String aArguments) {
        //TODO Impl: MyLinksCommand.execute
        throw new UnsupportedOperationException("MyLinksCommand.execute");
    }

}
