package org.sipfoundry.openfire.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

public class XmppUserAccount  {

    private static final Logger logger = Logger.getLogger(XmppUserAccount.class);

    private String userName;
    
    private String sipUserName;
    
    private String displayName;
    
    private String password;
    
    private String onThePhoneMessage;
    
    private boolean bAdvertiseOnCallStatus;

    private boolean bShowOnCallDetails;
 
    private Map<String, XmppTransportRegistration> transportAccounts = 
    	new HashMap<String, XmppTransportRegistration>();

    public XmppUserAccount () {
    }
    public String getEmail() {
        return null;
    }
    
    public String getDisplayName() {
        return this.displayName;
    }
    
    public String getPassword() {
        return this.password;
    }


    /**
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }
    /**
     * @param userName the userName to set
     */
    public void setUserName(String userName) {
        this.userName = userName.toLowerCase();  // Necessary because openfire stores usernames in lowercase.  
                                                 // We force username to lowercase to ensure that compares
                                                 // between this username and ones taken from the openfire DB
                                                 // work as expected.
    }
    /**
     * @return the userName
     */
    public String getUserName() {
        return userName;
    }
    /**
     * @param sipUserName the sipUserName to set
     */
    public void setSipUserName(String sipUserName) {
        this.sipUserName = sipUserName;
    }
    /**
     * @return the sipUserName
     */
    public String getSipUserName() {
        return sipUserName;
    }
    /**
     * @param onThePhoneMessage the onThePhoneMessage to set
     */
    public void setOnThePhoneMessage(String onThePhoneMessage) {
        this.onThePhoneMessage = onThePhoneMessage;
    }
    /**
     * @return the onThePhoneMessage
     */
    public String getOnThePhoneMessage() {
        return onThePhoneMessage;
    }
    /**
     * @param displayName the displayName to set
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setAdvertiseOnCallPreference(String flag){
        this.bAdvertiseOnCallStatus = Boolean.parseBoolean(flag);
    }        
    
    public boolean getAdvertiseOnCallPreference(){
        return this.bAdvertiseOnCallStatus;
    }

    public void setShowOnCallDetailsPreference(String flag){
        this.bShowOnCallDetails = Boolean.parseBoolean(flag);
    }

    public boolean getShowOnCallDetailsPreference(){
        return this.bShowOnCallDetails;
    }

    public void addTransportRegistration(XmppTransportRegistration transportRegistration) throws Exception {

        transportRegistration.setUser(this.userName);
        this.transportAccounts.put(transportRegistration.getTransportType(), transportRegistration);
        
        logger.debug("addTransportRegistration: " + transportRegistration.toString());
    }

    public Map<String, XmppTransportRegistration> getTransportRegistrations() {
        return this.transportAccounts;
    }
}
