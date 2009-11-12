package org.sipfoundry.commons.freeswitch;

public class ConferenceMember {
    String m_memberId;
    String m_memberName;
    String m_memberNumber;
    boolean m_muted;

    public boolean isMuted() {
        return m_muted;
    }

    public String memberId() {
        return m_memberId;
    }

    public String memberName() {
        return m_memberName;
    } 

    public String memberNumber() {
        return m_memberNumber;
    }
      
}

