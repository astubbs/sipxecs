/*
 *
 *
 * Copyright (C) 2008 Pingtel Corp., certain elements licensed under a Contributor Agreement.
 * Contributors retain copyright to elements licensed under a Contributor Agreement.
 * Licensed to the User under the LGPL license.
 *
 *
 */
package org.sipfoundry.sipxconfig.admin.dialplan.sbc.bridge;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singleton;

import org.sipfoundry.sipxconfig.admin.LoggingManager;
import org.sipfoundry.sipxconfig.admin.commserver.Location;
import org.sipfoundry.sipxconfig.admin.commserver.LocationsManager;
import org.sipfoundry.sipxconfig.admin.commserver.ServiceStatus.Status;
import org.sipfoundry.sipxconfig.admin.commserver.SipxProcessContext;
import org.sipfoundry.sipxconfig.admin.commserver.SipxReplicationContext;
import org.sipfoundry.sipxconfig.admin.dialplan.sbc.SbcDevice;
import org.sipfoundry.sipxconfig.device.DeviceDefaults;
import org.sipfoundry.sipxconfig.device.ProfileContext;
import org.sipfoundry.sipxconfig.device.ProfileLocation;
import org.sipfoundry.sipxconfig.device.ReplicatedProfileLocation;
import org.sipfoundry.sipxconfig.gateway.GatewayContext;
import org.sipfoundry.sipxconfig.gateway.SipTrunk;
import org.sipfoundry.sipxconfig.nattraversal.NatLocation;
import org.sipfoundry.sipxconfig.service.LoggingEntity;
import org.sipfoundry.sipxconfig.service.SipxBridgeService;
import org.sipfoundry.sipxconfig.service.SipxService;
import org.sipfoundry.sipxconfig.service.SipxServiceManager;
import org.sipfoundry.sipxconfig.setting.Setting;
import org.sipfoundry.sipxconfig.setting.SettingEntry;
import org.springframework.beans.factory.annotation.Required;

public class BridgeSbc extends SbcDevice implements LoggingEntity {

    public static final String LOG_SETTING = "bridge-configuration/log-level";

    public static final String LOCATION_ID_SETTING = "bridge-configuration/location-id";

    private GatewayContext m_gatewayContext;

    private SipxProcessContext m_processContext;

    private SipxReplicationContext m_sipxReplicationContext;

    private SipxServiceManager m_sipxServiceManager;

    private LocationsManager m_locationsManager;

    private String m_profileName;

    private String m_profileDirectory;

    private LoggingManager m_loggingManager;

    private Location m_location;

    @Required
    public void setGatewayContext(GatewayContext gatewayContext) {
        m_gatewayContext = gatewayContext;
    }

    @Required
    public void setProcessContext(SipxProcessContext processContext) {
        m_processContext = processContext;
    }

    public void setSipxReplicationContext(SipxReplicationContext sipxReplicationContext) {
        m_sipxReplicationContext = sipxReplicationContext;
    }

    @Required
    public void setSipxServiceManager(SipxServiceManager sipxServiceManager) {
        m_sipxServiceManager = sipxServiceManager;
    }

    @Required
    public void setLocationsManager(LocationsManager locationsManager) {
        m_locationsManager = locationsManager;
    }

    @Required
    public void setProfileName(String profileName) {
        m_profileName = profileName;
    }

    @Required
    public void setProfileDirectory(String profileDirectory) {
        m_profileDirectory = profileDirectory;
    }

    public void setLocation(Location location) {
        m_location = location;
    }

    @Override
    protected Setting loadSettings() {
        return getModelFilesContext().loadModelFile("bridge-sbc.xml", "sipxbridge");
    }

    @Override
    protected ProfileContext createContext() {
        return new Context(this, "sipxbridge/bridge.xml.vm");
    }

    @Override
    public String getProfileFilename() {
        return "sipxbridge.xml";
    }

    @Override
    public void initialize() {
        addDefaultBeanSettingHandler(new Defaults(getDefaults(), this, getLocation()));
    }

    @Override
    public ProfileLocation getProfileLocation() {
        ReplicatedProfileLocation profileLocation = new ReplicatedProfileLocation();
        Location location = getLocation();
        profileLocation.setLocation(location);
        profileLocation.setName(m_profileName);
        profileLocation.setDirectory(m_profileDirectory);
        profileLocation.setReplicationContext(m_sipxReplicationContext);
        return profileLocation;
    }

    public List<SipTrunk> getMySipTrunks() {
        List<SipTrunk> trunks = new ArrayList<SipTrunk>();
        for (SipTrunk t : m_gatewayContext.getGatewayByType(SipTrunk.class)) {
            if (equals(t.getSbcDevice()) && t.isEnabled()) {
                trunks.add(t);
            }
        }
        return trunks;
    }

    public Location getLocation() {
        if (m_location != null) {
            return m_location;
        }
        Integer id = (Integer) getSettings().getSetting(LOCATION_ID_SETTING).getTypedValue();
        if (id == null) {
            return m_locationsManager.getLocationByAddress(getAddress());
        }
        return m_locationsManager.getLocation(id);
    }

    public void updateBridgeLocationId() {
        Location location = m_locationsManager.getLocationByAddress(getAddress());
        setSettingTypedValue(LOCATION_ID_SETTING, location.getId());
    }

    public static class Context extends ProfileContext<BridgeSbc> {
        public Context(BridgeSbc device, String profileTemplate) {
            super(device, profileTemplate);
        }

        @Override
        public Map<String, Object> getContext() {
            Map<String, Object> context = super.getContext();
            BridgeSbc device = getDevice();
            context.put("trunks", device.getMySipTrunks());
            return context;
        }
    }

    public static class Defaults {
        private final DeviceDefaults m_defaults;
        private final SbcDevice m_device;
        private final Location m_location;
        private final NatLocation m_natLocation;

        Defaults(DeviceDefaults defaults, SbcDevice device, Location location) {
            m_defaults = defaults;
            m_device = device;
            m_location = location;
            m_natLocation = location.getNat();
        }

        @SettingEntry(paths = { "bridge-configuration/local-address", "bridge-configuration/external-address" })
        public String getExternalAddress() {
            return m_location.getAddress();
        }

        @SettingEntry(path = "bridge-configuration/global-address")
        public String getGlobalAddress() {
            return m_natLocation.getPublicAddress();
        }

        @SettingEntry(path = "bridge-configuration/local-port")
        public int getLocalPort() {
            return m_device.getPort();
        }

        @SettingEntry(path = "bridge-configuration/sipx-proxy-domain")
        public String getDomainName() {
            return m_defaults.getDomainName();
        }

        @SettingEntry(path = "bridge-configuration/log-directory")
        public String getLogDirectory() {
            return m_defaults.getLogDirectory() + "/";
        }

        @SettingEntry(path = "bridge-configuration/stun-server-address")
        public String getStunServerAddress() {
            return m_natLocation.getStunAddress();
        }

        @SettingEntry(path = "bridge-configuration/sipx-supervisor-host")
        public String getSipxSupervisorHost() {
            return m_location.getFqdn();
        }

        @SettingEntry(path = "bridge-configuration/sipx-supervisor-xml-rpc-port")
        public int getSipxSupervisorXmlRpcPort() {
            return Location.PROCESS_MONITOR_PORT;
        }
    }

    @Override
    public void restart() {
        SipxService sipxBridgeService = m_sipxServiceManager.getServiceByBeanId(SipxBridgeService.BEAN_ID);
        m_processContext.markServicesForRestart(getLocation(), singleton(sipxBridgeService));
    }

    public void setLoggingManager(LoggingManager loggingManager) {
        m_loggingManager = loggingManager;
    }

    public void setLogLevel(String logLevel) {
        if (logLevel != null) {
            String newLogLevel = logLevel;
            if (logLevel.equals("CRIT") || logLevel.equals("ALERT") || logLevel.equals("EMERG")) {
                newLogLevel = "ERR";
            }
            if (!newLogLevel.equals(getSettingValue(LOG_SETTING))) {
                setSettingValue(LOG_SETTING, newLogLevel);
                m_loggingManager.getEntitiesToProcess().add(this);
            }
        }
    }

    public String getLogLevel() {
        return getSettingValue(LOG_SETTING);
    }

    public String getLabelKey() {
        return "label.sipxBridgeService";
    }

    public String getBridgeSbcXmlRpcUrl() {
        return "https://" + getLocation().getFqdn() + ":" + getSettingValue("bridge-configuration/xml-rpc-port");
    }

    public boolean isBridgeSbcRunning() {
        Status bridgeStatus = m_processContext.getStatus(getLocation(), m_sipxServiceManager
                .getServiceByBeanId(SipxBridgeService.BEAN_ID));
        return (bridgeStatus == Status.Running);
    }
}
