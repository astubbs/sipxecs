/*
 *
 *
 * Copyright (C) 2007 Pingtel Corp., certain elements licensed under a Contributor Agreement.
 * Contributors retain copyright to elements licensed under a Contributor Agreement.
 * Licensed to the User under the LGPL license.
 *
 * $
 */
package org.sipfoundry.sipxconfig.phone.nt1535;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sipfoundry.sipxconfig.common.SipUri;
import org.sipfoundry.sipxconfig.common.User;
import org.sipfoundry.sipxconfig.device.Device;
import org.sipfoundry.sipxconfig.device.DeviceDefaults;
import org.sipfoundry.sipxconfig.device.Profile;
import org.sipfoundry.sipxconfig.device.ProfileContext;
import org.sipfoundry.sipxconfig.device.ProfileFilter;
import org.sipfoundry.sipxconfig.device.ProfileLocation;
import org.sipfoundry.sipxconfig.phone.Line;
import org.sipfoundry.sipxconfig.phone.LineInfo;
import org.sipfoundry.sipxconfig.phone.Phone;
import org.sipfoundry.sipxconfig.setting.SettingEntry;

/**
 * Nortel 1535 phone.
 */
public class NT1535Phone extends Phone {

    private static final Log LOG = LogFactory.getLog(NT1535Phone.class);

    private static final String SYSTEM_CONFIG_FILE = "sysconf_2890d_sip.cfg";

    private static final String VERSION_HW_VERSION = "VERSION/hw_version";

    private static final String VERSION_SW_VERSION = "VERSION/sw_version";

    public NT1535Phone() {
    }

    public String getSoftwareVersion() {
        return getSettingValue(VERSION_SW_VERSION);
    }

    public String getHardwareVersion() {
        return getSettingValue(VERSION_HW_VERSION);
    }

    public void setSoftwareVersion(String softwareVersion) {
        setSettingValue(VERSION_SW_VERSION, softwareVersion);
    }

    public void setHardwareVersion(String hardwareVersion) {
        setSettingValue(VERSION_HW_VERSION, hardwareVersion);
    }

    @Override
    public void initializeLine(Line line) {
        DeviceDefaults phoneDefaults = getPhoneContext().getPhoneDefaults();
        NT1535PhoneDefaults defaults = new NT1535PhoneDefaults(phoneDefaults, line);
        line.addDefaultBeanSettingHandler(defaults);
    }

    @Override
    public void initialize() {
        DeviceDefaults phoneDefaults = getPhoneContext().getPhoneDefaults();
        Line line = new Line();
        NT1535PhoneDefaults defaults = new NT1535PhoneDefaults(phoneDefaults, line);
        defaults = new NT1535PhoneDefaults(phoneDefaults, line);
        addDefaultBeanSettingHandler(defaults);
    }

    public String getDeviceFileName() {
        return getSerialNumber().toUpperCase() + ".cfg";
    }

    @Override
    protected LineInfo getLineInfo(Line line) {
        LineInfo lineInfo = NT1535PhoneDefaults.getLineInfo(line);
        return lineInfo;
    }

    @Override
    protected void setLineInfo(Line line, LineInfo lineInfo) {

        NT1535PhoneDefaults.setLineInfo(line, lineInfo);
    }

    @Override
    public void restart() {
        sendCheckSyncToFirstLine();
    }

    @Override
    public void generateFiles(ProfileLocation location) {

        String hwVersion = getSettingValue(VERSION_HW_VERSION);
        String swVersion = getSettingValue(VERSION_SW_VERSION);

        if (hwVersion != null && swVersion != null) {

            String filePath = hwVersion + "/" + swVersion + "S/";
            LOG.debug(" NT1535Phone::generateFiles filePath = " + filePath);

            DeviceConfigContext deviceConfig = new DeviceConfigContext(this);
            getProfileGenerator().generate(location, deviceConfig, null,
                    filePath + getDeviceFileName());
            SystemConfigContext sip = new SystemConfigContext(this);
            getProfileGenerator().generate(location, sip, null, filePath + SYSTEM_CONFIG_FILE);
        } else {
            LOG.error("HW or SW version for NT1535Phone is NOT configured!");
        }
    }

    @Override
    public void removeProfiles(ProfileLocation location) {
        super.removeProfiles(location);
        location.removeProfile(getDeviceFileName());
        location.removeProfile(SYSTEM_CONFIG_FILE);
    }

    static class DeviceConfigProfile extends Profile {
        public DeviceConfigProfile(String name) {
            super(name);
        }

        protected ProfileFilter createFilter(Device device) {
            return null;
        }

        protected ProfileContext createContext(Device device) {
            NT1535Phone phone = (NT1535Phone) device;
            return new DeviceConfigContext(phone);
        }
    }

    static class SystemConfigProfile extends Profile {
        public SystemConfigProfile(String name) {
            super(name);
        }

        protected ProfileFilter createFilter(Device device) {
            return null;
        }

        protected ProfileContext createContext(Device device) {
            NT1535Phone phone = (NT1535Phone) device;
            return new SystemConfigContext(phone);
        }
    }

    // This method will put the list of profiles to the download link

    @Override
    public Profile[] getProfileTypes() {
        Profile[] profileTypes;
        profileTypes = new Profile[] {
            new DeviceConfigProfile(getDeviceFileName()),
            new SystemConfigProfile(SYSTEM_CONFIG_FILE)
        };

        return profileTypes;
    }

    public static class NT1535PhoneDefaults {

        private static final String VOIP_SIP_SERVICE_DOMAIN = "VOIP/sip_service_domain";
        private static final String VOIP_OUTBOUND_PROXY_SERVER = "VOIP/outbound_proxy_server";
        private static final String VOIP_OUTBOUND_PROXY_PORT = "VOIP/outbound_proxy_port";
        private static final String VOIP_LINE1_PROXY_ADDRESS = "VOIP/line1_proxy_address";
        private static final String VOIP_LINE1_PROXY_PORT = "VOIP/line1_proxy_port";
        private static final String VOIP_LINE1_NAME = "VOIP/line1_name";
        private static final String VOIP_LINE1_AUTHNAME = "VOIP/line1_authname";
        private static final String VOIP_LINE1_DISPLAYNAME = "VOIP/line1_displayname";
        private static final String VOIP_LINE1_PASSWORD = "VOIP/line1_password";
        private static final String VOIP_MOH_URL = "VOIP/moh_url";
        private static final String LAN_TFTP_SERVER_ADDRESS = "LAN/tftp_server_address";
        private static final String NETTIME_SNTP_SERVER_ADDRESS = "NETTIME/sntp_server_address";

        private Line m_line;
        private DeviceDefaults m_defaults;

        NT1535PhoneDefaults(DeviceDefaults defaults, Line line) {
            m_line = line;
            m_defaults = defaults;
        }

        @SettingEntry(path = VOIP_SIP_SERVICE_DOMAIN)
        public String getSipDomain1() {
            return m_defaults.getDomainName();
        }

        @SettingEntry(paths = {
            VOIP_OUTBOUND_PROXY_SERVER, VOIP_LINE1_PROXY_ADDRESS, LAN_TFTP_SERVER_ADDRESS
        })
        public String getServerIp() {
            return m_defaults.getProxyServerAddr();
        }

        @SettingEntry(paths = {
            VOIP_OUTBOUND_PROXY_PORT, VOIP_LINE1_PROXY_PORT
        })
        public String getProxyPort() {
            return m_defaults.getProxyServerSipPort();
        }

        @SettingEntry(paths = {
            VOIP_LINE1_NAME, VOIP_LINE1_AUTHNAME, VOIP_LINE1_DISPLAYNAME
        })
        public String getUserName() {
            String userName = null;
            User user = m_line.getUser();
            if (user != null) {
                userName = user.getUserName();
            }
            return userName;
        }

        @SettingEntry(path = VOIP_LINE1_PASSWORD)
        public String getSipPassword() {
            String sipPassword = null;
            User user = m_line.getUser();
            if (user != null) {
                sipPassword = user.getSipPassword();
            }
            return sipPassword;
        }

        @SettingEntry(path = NETTIME_SNTP_SERVER_ADDRESS)
        public String getNtpServer() {
            return m_defaults.getNtpServer();
        }

        @SettingEntry(path = VOIP_MOH_URL)
        public String getMohUrl() {
            String mohUri = m_defaults.getSipxServer().getMusicOnHoldUri(
                    m_defaults.getDomainName());
            return SipUri.stripSipPrefix(mohUri);
        }

        public static LineInfo getLineInfo(Line line) {
            LineInfo lineInfo = new LineInfo();
            lineInfo.setUserId(line.getSettingValue(VOIP_LINE1_AUTHNAME));
            lineInfo.setDisplayName(line.getSettingValue(VOIP_LINE1_DISPLAYNAME));
            lineInfo.setRegistrationServer(line.getSettingValue(VOIP_OUTBOUND_PROXY_SERVER));
            lineInfo.setRegistrationServerPort(line.getSettingValue(VOIP_OUTBOUND_PROXY_PORT));
            return lineInfo;
        }

        public static void setLineInfo(Line line, LineInfo lineInfo) {
            line.setSettingValue(VOIP_LINE1_NAME, lineInfo.getUserId());
            line.setSettingValue(VOIP_LINE1_AUTHNAME, lineInfo.getUserId());
            line.setSettingValue(VOIP_LINE1_DISPLAYNAME, lineInfo.getUserId());
            line.setSettingValue(VOIP_OUTBOUND_PROXY_SERVER, lineInfo.getRegistrationServer());
            line.setSettingValue(VOIP_OUTBOUND_PROXY_PORT, lineInfo.getRegistrationServerPort());
        }
    }
}
