/*
 * 
 * 
 * Copyright (C) 2007 Pingtel Corp., certain elements licensed under a Contributor Agreement.  
 * Contributors retain copyright to elements licensed under a Contributor Agreement.
 * Licensed to the User under the LGPL license.
 * 
 * $
 */
package org.sipfoundry.sipxconfig.admin.commserver;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import junit.framework.TestCase;

import org.sipfoundry.sipxconfig.admin.commserver.SipxProcessContext.Command;
import org.sipfoundry.sipxconfig.admin.commserver.SipxProcessModel.ProcessName;

public class SipxProcessContextImplTest extends TestCase {
    private SipxProcessContextImpl m_processContextImpl;

    private int m_numberOfCalls;
    private List m_urlStrings;
    private List m_methodNameStrings;
    private List m_paramVectors;

    private final static ServiceStatus[] SERVICESTATUS = new ServiceStatus[] {
        new ServiceStatus(new Process(ProcessName.REGISTRAR), ServiceStatus.Status.STARTING),
        new ServiceStatus(new Process(ProcessName.MEDIA_SERVER), ServiceStatus.Status.STARTED),
        new ServiceStatus(new Process(ProcessName.PRESENCE_SERVER), ServiceStatus.Status.STOPPED),
        new ServiceStatus(new Process(ProcessName.PROXY), ServiceStatus.Status.FAILED),
        new ServiceStatus(new Process(ProcessName.ACD_SERVER), ServiceStatus.Status.UNKNOWN)
    };

    protected void setUp() throws Exception {
        m_processContextImpl = new SipxProcessContextImpl() {

            protected InputStream getTopologyAsStream() {
                return SipxProcessContextImplTest.class.getResourceAsStream("topology.test.xml");
            }

            protected Object invokeXmlRpcRequest(Location location, String methodName, Vector params) {
                m_numberOfCalls++;
                m_urlStrings.add(location.getProcessMonitorUrl());
                m_methodNameStrings.add(methodName);
                m_paramVectors.add(params);

                if (Command.STOP.getName() == methodName || Command.START.getName() == methodName
                        || Command.RESTART.getName() == methodName) {
                    return new Boolean(true);
                } else if ("getStateAll" == methodName) {
                    Hashtable result = new Hashtable();
                    for (int x = 0; x < SERVICESTATUS.length; x++) {
                        result.put(SERVICESTATUS[x].getServiceName(), SERVICESTATUS[x].getStatus().getName());
                    }
                    return result;
                }

                throw new RuntimeException("Unrecognized methodName: '" + methodName + "'.");
            }
        };

        m_methodNameStrings = new ArrayList();
        m_urlStrings = new ArrayList();
        m_paramVectors = new ArrayList();

        m_processContextImpl.setProcessModel(new SimpleSipxProcessModel());
    }

    public void testGetStatus() {
        ServiceStatus[] resultServiceStatus = m_processContextImpl.getStatus(m_processContextImpl.getLocations()[0]);

        assertEquals(1, m_numberOfCalls);

        // Build the set of expected Process-ServiceStatus combinations. The order is not
        // important.
        Set<String> expectedCombinations = new HashSet<String>();
        for (int x = 0; x < SERVICESTATUS.length; x++) {
            expectedCombinations.add(SERVICESTATUS[x].getServiceName() + SERVICESTATUS[x].getStatus().getName());
        }

        // Compare the expected Process-ServiceStatus combinations to what actually occured.
        for (int x = 0; x < resultServiceStatus.length; x++) {
            String value = resultServiceStatus[x].getServiceName() + resultServiceStatus[x].getStatus().getName();
            assertTrue(expectedCombinations.remove(value));
        }
        assertEquals(0, expectedCombinations.size());
    }

    public void testManageService() {
        Process[] processes = {
            new Process(ProcessName.REGISTRAR)
        };
        Location[] locations = {
            m_processContextImpl.getLocations()[0]
        };
        Command command = Command.STOP;

        m_processContextImpl.manageService(locations[0], processes[0], command);

        checkManageServicesResults(processes, locations, command);
    }

    public void testManageServices() {
        Process[] processes = {
            new Process(ProcessName.MEDIA_SERVER), new Process(ProcessName.PRESENCE_SERVER)
        };
        Location[] locations = m_processContextImpl.getLocations();
        Command command = Command.STOP;

        m_processContextImpl.manageServices(Arrays.asList(processes), command);

        checkManageServicesResults(processes, locations, command);
    }

    public void testManageServicesRestart() {
        Process[] processes = {
            new Process(ProcessName.MEDIA_SERVER), new Process(ProcessName.PRESENCE_SERVER)
        };
        Location[] locations = m_processContextImpl.getLocations();
        Command command = Command.RESTART;

        m_processContextImpl.manageServices(Arrays.asList(processes), command);

        // changing restart: stop and start
        int calls = processes.length * locations.length;
        assertEquals(2 * calls, m_numberOfCalls);

        for (int l = 0; l < locations.length; l++) {
            for (int x = 0; x < processes.length; x++) {
                assertEquals(Command.STOP.getName(), m_methodNameStrings.get(x));
            }
            for (int x = processes.length; x < 2 * processes.length; x++) {
                assertEquals(Command.START.getName(), m_methodNameStrings.get(x));
            }
        }
    }

    public void testManageServicesRestartWithConfigServer() {
        Process[] processes = {
            new Process(ProcessName.MEDIA_SERVER), new Process(ProcessName.CONFIG_SERVER)
        };
        Location[] locations = m_processContextImpl.getLocations();
        Command command = Command.RESTART;

        m_processContextImpl.manageServices(Arrays.asList(processes), command);

        // changing restart: stop and start - but not for sipXconfig
        assertEquals(3 * locations.length, m_numberOfCalls);

        for (int l = 0; l < locations.length; l++) {
            assertEquals(Command.STOP.getName(), m_methodNameStrings.get(0));
            assertEquals(Command.START.getName(), m_methodNameStrings.get(1));
            assertEquals(Command.RESTART.getName(), m_methodNameStrings.get(2));
        }
    }

    public void testManageServicesLocation() {
        Process[] processes = {
            new Process(ProcessName.PROXY), new Process(ProcessName.ACD_SERVER)
        };
        Location[] locations = {
            m_processContextImpl.getLocations()[1]
        };
        Command command = Command.START;

        m_processContextImpl.manageServices(locations[0], Arrays.asList(processes), command);

        checkManageServicesResults(processes, locations, command);
    }

    private void checkManageServicesResults(final Process[] PROCESSES, final Location[] LOCATIONS, final Command COMMAND) {
        assertEquals(LOCATIONS.length * PROCESSES.length, m_numberOfCalls);

        for (int x = 0; x < m_numberOfCalls; x++) {
            assertEquals(COMMAND.getName(), m_methodNameStrings.get(x));
        }

        // Build the set of expected Process-Location combinations. The order is not important.
        Set<String> expectedCombinations = new HashSet<String>();
        for (int x = 0; x < PROCESSES.length; x++) {
            for (int y = 0; y < LOCATIONS.length; y++) {
                expectedCombinations.add(PROCESSES[x].getName() + LOCATIONS[y].getProcessMonitorUrl());
            }
        }

        // Compare the expected Process-Location combinations to what actually occured.
        for (int x = 0; x < m_numberOfCalls; x++) {
            String value = ((Vector<Object>) m_paramVectors.get(x)).elementAt(0).toString() + m_urlStrings.get(x);
            assertTrue(expectedCombinations.remove(value));
        }
        assertEquals(0, expectedCombinations.size());
    }
}
