/*
 * 
 * 
 * Copyright (C) 2007 Pingtel Corp., certain elements licensed under a Contributor Agreement.  
 * Contributors retain copyright to elements licensed under a Contributor Agreement.
 * Licensed to the User under the LGPL license.
 * 
 * $
 */
package org.sipfoundry.sipxconfig.domain;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.sipfoundry.sipxconfig.admin.commserver.SipxReplicationContext;
import org.sipfoundry.sipxconfig.admin.commserver.SipxServer;
import org.sipfoundry.sipxconfig.admin.dialplan.DialingRule;
import org.sipfoundry.sipxconfig.admin.localization.Localization;
import org.sipfoundry.sipxconfig.common.SipxHibernateDaoSupport;
import org.springframework.dao.support.DataAccessUtils;

public class DomainManagerImpl extends SipxHibernateDaoSupport<Domain> implements DomainManager {

    // do not reference m_server, see note in spring file
    private SipxServer m_server;
    private DomainConfiguration m_domainConfiguration;
    private SipxReplicationContext m_replicationContext;
    private String m_authorizationRealm;
    private String m_initialDomain;
    private String m_initialAlias;

    /**
     * Implemented by Spring AOP
     */
    public SipxServer getServer() {
        return m_server;
    }

    /**
     * For testing only.
     */
    void setServer(SipxServer server) {
        m_server = server;
    }

    public DomainConfiguration getDomainConfiguration() {
        return m_domainConfiguration;
    }

    public void setDomainConfiguration(DomainConfiguration domainConfiguration) {
        m_domainConfiguration = domainConfiguration;
    }

    public void setReplicationContext(SipxReplicationContext context) {
        m_replicationContext = context;
    }

    /**
     * @return non-null unless test environment
     */
    public Domain getDomain() {
        Domain domain = getExistingDomain();
        if (domain == null) {
            throw new DomainNotInitializedException();
        }

        return domain;
    }

    public void saveDomain(Domain domain) {
        if (domain.isNew()) {
            Domain existing = getExistingDomain();
            if (existing != null) {
                getHibernateTemplate().delete(getDomain());
            }
        }
        getHibernateTemplate().saveOrUpdate(domain);

        updateServer(domain);

        replicateDomainConfig();
    }

    /**
     * Sets newly updated Domain parameters on the SIPX server. This is probably not the best
     * idea. Ideally sipx server should get those parameters from domain manager whenever it needs
     * that. Sadly SipxServer is responsible for generating some configuration files that still
     * needs those info and we need to kick it to make sure everything gets updated.
     * 
     * @param domain domain that have been changed
     */
    private void updateServer(Domain domain) {
        SipxServer sipxServer = getServer();
        sipxServer.setDomainName(domain.getName());
        sipxServer.setRegistrarDomainAliases(domain.getAliases());
        sipxServer.applySettings();
    }

    public void replicateDomainConfig() {
        m_domainConfiguration.generate(getExistingDomain(), m_authorizationRealm,
                getExistingLocalization().getLanguage());
        m_replicationContext.replicate(m_domainConfiguration);
    }

    protected Domain getExistingDomain() {
        Collection<Domain> domains = getHibernateTemplate().findByNamedQuery("domain");
        return (Domain) DataAccessUtils.singleResult(domains);
    }

    private Localization getExistingLocalization() {
        List l = getHibernateTemplate().loadAll(Localization.class);
        return (Localization) DataAccessUtils.singleResult(l);
    }

    public List<DialingRule> getDialingRules() {
        List<DialingRule> rules;
        Domain d = getDomain();
        if (d.hasAliases()) {
            DialingRule domainRule = new DomainDialingRule(getDomain());
            rules = Collections.<DialingRule> singletonList(domainRule);
        } else {
            rules = Collections.<DialingRule> emptyList();
        }
        return rules;
    }

    /**
     * Initialize the firs and single domain supported by sipX at the moment. If domain does not
     * exist a new domain will be created and initialized with a 'm_initialDomain' name. If domain
     * does exist we make sure it has secret initialed - new secret is created if the secret is
     * empty.
     * 
     * It is save to call this function many times. Only the first call should result in actually
     * saving and replicating the domain
     */
    public void initialize() {
        Domain domain = getExistingDomain();
        if (domain == null) {
            domain = new Domain();
            domain.setName(m_initialDomain);
            if (StringUtils.isNotBlank(m_initialAlias)) {
                domain.addAlias(m_initialAlias);
            }
        }
        if (domain.initSecret()) {
            saveDomain(domain);
        }
    }

    public DomainConfiguration createDomainConfiguration() {
        return m_domainConfiguration;
    }

    public String getAuthorizationRealm() {
        return m_authorizationRealm;
    }

    public void setAuthorizationRealm(String authorizationRealm) {
        m_authorizationRealm = authorizationRealm;
    }

    public void setInitialDomain(String initialDomain) {
        m_initialDomain = initialDomain;
    }

    public void setInitialAlias(String initialAlias) {
        m_initialAlias = initialAlias;
    }
}
