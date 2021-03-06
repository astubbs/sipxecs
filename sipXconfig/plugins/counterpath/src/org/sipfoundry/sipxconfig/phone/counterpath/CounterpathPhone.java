/*
 *
 *
 * Copyright (C) 2007 Pingtel Corp., certain elements licensed under a Contributor Agreement.
 * Contributors retain copyright to elements licensed under a Contributor Agreement.
 * Licensed to the User under the LGPL license.
 *
 *
 */
package org.sipfoundry.sipxconfig.phone.counterpath;

import org.sipfoundry.sipxconfig.common.SipUri;
import org.sipfoundry.sipxconfig.common.User;
import org.sipfoundry.sipxconfig.device.DeviceDefaults;
import org.sipfoundry.sipxconfig.device.ProfileContext;
import org.sipfoundry.sipxconfig.im.ImAccount;
import org.sipfoundry.sipxconfig.phone.Line;
import org.sipfoundry.sipxconfig.phone.LineInfo;
import org.sipfoundry.sipxconfig.phone.Phone;
import org.sipfoundry.sipxconfig.setting.SettingEntry;
import org.sipfoundry.sipxconfig.speeddial.SpeedDial;

public class CounterpathPhone extends Phone {
    private static final String REG_USERNAME = "registration/username";
    private static final String REG_AUTH_USERNAME = "registration/authorization_username";
    private static final String REG_DISPLAY_NAME = "registration/display_name";
    private static final String REG_PASSWORD = "registration/password";
    private static final String REG_DOMAIN = "registration/domain";
    private static final String SUBSCRIPTION_AOR = "network/sip_signaling/proxies:proxy0:workgroup_subscription_aor";
    private static final String VOICEMAIL_URL = "voicemail/voicemail_url";

    public CounterpathPhone() {
    }

    @Override
    public String getProfileFilename() {
        return getSerialNumber() + ".ini";
    }

    @Override
    public void initialize() {
        addDefaultBeanSettingHandler(new CounterpathPhoneDefaults(this));

    }

    @Override
    protected ProfileContext createContext() {
        return new CounterpathProfileContext(this, getModel().getProfileTemplate());
    }

    @Override
    public void initializeLine(Line line) {
        line.addDefaultBeanSettingHandler(new CounterpathLineDefaults(line));
    }

    @Override
    protected void setLineInfo(Line line, LineInfo info) {
        line.setSettingValue(REG_USERNAME, info.getUserId());
        line.setSettingValue(REG_AUTH_USERNAME, info.getUserId());
        line.setSettingValue(REG_DISPLAY_NAME, info.getDisplayName());
        line.setSettingValue(REG_PASSWORD, info.getPassword());
        line.setSettingValue(REG_DOMAIN, info.getRegistrationServer());
    }

    @Override
    protected LineInfo getLineInfo(Line line) {
        LineInfo info = new LineInfo();
        info.setDisplayName(line.getSettingValue(REG_DISPLAY_NAME));
        info.setUserId(line.getSettingValue(REG_USERNAME));
        info.setPassword(line.getSettingValue(REG_PASSWORD));
        info.setRegistrationServer(line.getSettingValue(REG_DOMAIN));
        return info;
    }

    public class CounterpathPhoneDefaults {
        private final Phone m_phone;

        public CounterpathPhoneDefaults(Phone phone) {
            m_phone = phone;
        }

        @SettingEntry(path = SUBSCRIPTION_AOR)
        public String getWorkgroupSubscriptionAor() {
            SpeedDial speedDial = getPhoneContext().getSpeedDial(m_phone);
            if (speedDial == null) {
                return null;
            }
            String domain = getPhoneContext().getPhoneDefaults().getDomainName();
            return SipUri.format(speedDial.getResourceListId(true), domain, false);
        }
    }

    public static class CounterpathLineDefaults {
        private final Line m_line;
        private final User m_user;
        private final ImAccount m_imAccount;

        public CounterpathLineDefaults(Line line) {
            m_line = line;
            m_user = m_line.getUser();
            if (m_user != null) {
                m_imAccount = new ImAccount(m_user);
            } else {
                m_imAccount = null;
            }
        }

        @SettingEntry(path = REG_USERNAME)
        public String getUserName() {
            return m_line.getUserName();
        }

        @SettingEntry(path = REG_AUTH_USERNAME)
        public String getAuthenticationUserName() {
            return m_line.getAuthenticationUserName();
        }

        @SettingEntry(path = "xmpp-config/enabled")
        public boolean isEnabled() {
            if (m_imAccount == null) {
                return false;
            }
            return m_imAccount.isEnabled();
        }

        @SettingEntry(path = "xmpp-config/username")
        public String getImId() {
            if (m_imAccount == null) {
                return null;
            }
            return m_imAccount.getImId();
        }

        @SettingEntry(path = REG_DISPLAY_NAME)
        public String getDisplayName() {
            if (m_user == null) {
                return null;
            }
            return m_user.getDisplayName();
        }

        @SettingEntry(path = "xmpp-config/account_name")
        public String getImDisplayName() {
            if (m_imAccount == null) {
                return null;
            }
            return m_imAccount.getImDisplayName();
        }

        @SettingEntry(path = REG_PASSWORD)
        public String getPassword() {
            if (m_user == null) {
                return null;
            }
            return m_user.getSipPassword();
        }

        @SettingEntry(path = "xmpp-config/password")
        public String getImPassword() {
            if (m_imAccount == null) {
                return null;
            }
            return m_imAccount.getImPassword();
        }

        @SettingEntry(paths = { REG_DOMAIN, "xmpp-config/domain" })
        public String getDomain() {
            DeviceDefaults defaults = m_line.getPhoneContext().getPhoneDefaults();
            return defaults.getDomainName();
        }

        @SettingEntry(path = VOICEMAIL_URL)
        public String getVoicemailURL() {
            return m_line.getPhoneContext().getPhoneDefaults().getVoiceMail();
        }
    }
}
