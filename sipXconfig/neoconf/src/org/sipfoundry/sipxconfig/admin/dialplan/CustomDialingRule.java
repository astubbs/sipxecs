/*
 * 
 * 
 * Copyright (C) 2007 Pingtel Corp., certain elements licensed under a Contributor Agreement.  
 * Contributors retain copyright to elements licensed under a Contributor Agreement.
 * Licensed to the User under the LGPL license.
 * 
 * $
 */
package org.sipfoundry.sipxconfig.admin.dialplan;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.sipfoundry.sipxconfig.admin.dialplan.config.FullTransform;
import org.sipfoundry.sipxconfig.admin.dialplan.config.Transform;
import org.sipfoundry.sipxconfig.gateway.Gateway;
import org.sipfoundry.sipxconfig.permission.Permission;

/**
 * CustomDialingRule
 */
public class CustomDialingRule extends DialingRule {
    private static final String ROUTE_PATTERN = "route=%s";
    private static final String VALID_TIME_PARAM = "sipx-ValidTime=%s";

    private List<DialPattern> m_dialPatterns = new ArrayList<DialPattern>();
    private CallPattern m_callPattern = new CallPattern();
    private List<String> m_permissionNames = new ArrayList<String>();

    public CustomDialingRule() {
        m_dialPatterns.add(new DialPattern());
    }

    protected Object clone() throws CloneNotSupportedException {
        CustomDialingRule clone = (CustomDialingRule) super.clone();
        clone.m_permissionNames = new ArrayList(m_permissionNames);
        clone.m_dialPatterns = new ArrayList(m_dialPatterns);
        return clone;
    }

    public List<DialPattern> getDialPatterns() {
        return m_dialPatterns;
    }

    public void setDialPatterns(List<DialPattern> dialPaterns) {
        m_dialPatterns = dialPaterns;
    }

    public CallPattern getCallPattern() {
        return m_callPattern;
    }

    public void setCallPattern(CallPattern callPattern) {
        m_callPattern = callPattern;
    }

    @Override
    public String[] getPatterns() {
        String[] patterns = new String[m_dialPatterns.size()];
        for (int i = 0; i < patterns.length; i++) {
            DialPattern p = m_dialPatterns.get(i);
            patterns[i] = p.calculatePattern();
        }
        return patterns;
    }

    @Override
    public Transform[] getTransforms() {
        final String outPattern = getCallPattern().calculatePattern();
        List<Gateway> gateways = getGateways();
        Transform[] transforms;
        if (gateways.isEmpty()) {
            FullTransform transform = new FullTransform();
            transform.setUser(outPattern);
            if (getSchedule() != null) {
                String[] fieldParams = {
                    String.format(VALID_TIME_PARAM, getSchedule().calculateValidTime())
                };
                transform.setFieldParams(fieldParams);
            }
            transforms = new Transform[] {
                transform
            };
        } else {
            transforms = new Transform[gateways.size()];
            ForkQueueValue q = new ForkQueueValue(gateways.size());
            for (int i = 0; i < transforms.length; i++) {
                transforms[i] = getGatewayTransform(gateways.get(i), outPattern, q);
            }
        }
        return transforms;
    }

    private FullTransform getGatewayTransform(Gateway g, String pattern, ForkQueueValue q) {
        FullTransform transform = new FullTransform();
        transform.setHost(g.getGatewayAddress());
        transform.setUser(g.getCallPattern(pattern));
        String validTime = "";
        if (getSchedule() != null) {
            validTime = String.format(VALID_TIME_PARAM, getSchedule().calculateValidTime());
        }
        String[] fieldParams = {
            q.getSerial() + ";" + validTime
        };
        transform.setFieldParams(fieldParams);
        String route = g.getRoute();
        if (StringUtils.isNotBlank(route)) {
            String[] headerParams = {
                String.format(ROUTE_PATTERN, route)
            };
            transform.setHeaderParams(headerParams);
        }
        if (!g.getAddressTransport().equals(Gateway.AddressTransport.NONE)) {
            String transport = String.format("transport=%s", g.getAddressTransport().getName());
            transform.setUrlParams(transport);
        }
        return transform;
    }

    public DialingRuleType getType() {
        return DialingRuleType.CUSTOM;
    }

    public void setPermissions(List<Permission> permissions) {
        List<String> permissionNames = getPermissionNames();
        permissionNames.clear();
        for (Permission permission : permissions) {
            permissionNames.add(permission.getName());
        }
    }

    public void setPermissionNames(List<String> permissionNames) {
        m_permissionNames = permissionNames;
    }

    @Override
    public List<String> getPermissionNames() {
        return m_permissionNames;
    }

    /**
     * External rule if there are gateways. Internal if no gateways
     */
    public boolean isInternal() {
        return getGateways().isEmpty();
    }

    @Override
    public String[] getTransformedPatterns(Gateway gateway) {
        List<DialPattern> dialPatterns = getDialPatterns();
        Set<String> transformedPatterns = new LinkedHashSet<String>();
        for (DialPattern dp : dialPatterns) {
            DialPattern tdp = m_callPattern.transform(dp);
            String pattern = gateway.getCallPattern(tdp.calculatePattern());
            transformedPatterns.add(pattern);
        }
        return transformedPatterns.toArray(new String[transformedPatterns.size()]);
    }
}
