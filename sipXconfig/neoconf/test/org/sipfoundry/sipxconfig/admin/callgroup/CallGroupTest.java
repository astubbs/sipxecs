/*
 * 
 * 
 * Copyright (C) 2007 Pingtel Corp., certain elements licensed under a Contributor Agreement.  
 * Contributors retain copyright to elements licensed under a Contributor Agreement.
 * Licensed to the User under the LGPL license.
 * 
 * $
 */
package org.sipfoundry.sipxconfig.admin.callgroup;

import java.util.List;

import junit.framework.TestCase;

import org.sipfoundry.sipxconfig.admin.forwarding.AliasMapping;
import org.sipfoundry.sipxconfig.common.User;

public class CallGroupTest extends TestCase {

    public void testInsertRingUser() {
        User u = new User();
        u.setUserName("testUser");
        CallGroup group = new CallGroup();
        UserRing ring = group.insertRingForUser(u);
        assertSame(u, ring.getUser());
        List calls = group.getRings();
        assertEquals(1, calls.size());
        assertSame(ring, calls.get(0));
        assertTrue(ring.isFirst());
    }

    public void testGenerateAliases() {
        int numRings = 5;
        CallGroup group = createCallGroupWithUsers("401", "sales", numRings);

        List<AliasMapping> aliases = group.generateAliases("kuku");
        // disabled group should not generate aliases
        assertTrue(aliases.isEmpty());

        group.setEnabled(true);
        aliases = group.generateAliases("kuku");

        assertEquals(numRings + 1, aliases.size());
        for (int i = 0; i < numRings; i++) {
            AliasMapping am = aliases.get(i);
            assertEquals(am.getIdentity(), group.getName() + "@kuku");
            assertTrue(am.getContact().startsWith("<sip:testUser" + i + "@kuku"));
            // for all but last we need sipx-noroute=Voicemail in the sequence
            assertEquals(i < numRings - 1, 0 < am.getContact().indexOf("sipx-noroute=Voicemail"));
        }

        // the last alias is an extension => identity
        AliasMapping am = aliases.get(aliases.size() - 1);
        assertEquals(am.getIdentity(), group.getExtension() + "@kuku");
        assertTrue(am.getContact().startsWith(group.getName() + "@kuku"));
    }

    public void testGenerateAliasesWithFallbackDestinationAsSipUri() {
        int numRings = 5;
        CallGroup group = createCallGroupWithUsers("401", "sales", numRings);
        String fallbackDestination = "fallback@kuku.com";
        group.setFallbackDestination(fallbackDestination);
        group.setEnabled(true);

        // assumption: the aliases list is ordered from highest q value to lowest,
        // with last element being the identity alias.
        List<AliasMapping> aliases = group.generateAliases("kuku");

        assertEquals(numRings + 2, aliases.size());
        for (int i = 0; i < numRings; i++) {
            AliasMapping am = aliases.get(i);
            assertEquals(am.getIdentity(), group.getName() + "@kuku");
            assertTrue(am.getContact().startsWith("<sip:testUser" + i + "@kuku"));
            // all of the contacts should contain sipx-noroute=Voicemail
            // (this is different from case without default sip uri)
            assertTrue(am.getContact().contains("sipx-noroute=Voicemail"));
        }

        // the second to last alias (lowest q value) should be the default sip uri
        AliasMapping defaultSipUriMapping = aliases.get(aliases.size() - 2);
        assertEquals(group.getName() + "@kuku", defaultSipUriMapping.getIdentity());
        assertTrue(defaultSipUriMapping.getContact().contains("fallback@kuku.com"));

        // the last alias is an extension => identity
        AliasMapping am = aliases.get(aliases.size() - 1);
        assertEquals(am.getIdentity(), group.getExtension() + "@kuku");
        assertTrue(am.getContact().startsWith(group.getName() + "@kuku"));
    }
    
    public void testGenerateAliasesWithFallbackDestinationAsExtension() {
        int numRings = 5;
        CallGroup group = createCallGroupWithUsers("401", "sales", numRings);
        String fallbackDestination = "1234";
        group.setFallbackDestination(fallbackDestination);
        group.setEnabled(true);

        // assumption: the aliases list is ordered from highest q value to lowest,
        // with last element being the identity alias.
        List<AliasMapping> aliases = group.generateAliases("kuku");

        assertEquals(numRings + 2, aliases.size());
        for (int i = 0; i < numRings; i++) {
            AliasMapping am = aliases.get(i);
            assertEquals(am.getIdentity(), group.getName() + "@kuku");
            assertTrue(am.getContact().startsWith("<sip:testUser" + i + "@kuku"));
            // all of the contacts should contain sipx-noroute=Voicemail
            // (this is different from case without default sip uri)
            assertTrue(am.getContact().contains("sipx-noroute=Voicemail"));
        }

        // the second to last alias (lowest q value) should be the default sip uri
        AliasMapping defaultSipUriMapping = aliases.get(aliases.size() - 2);
        assertEquals(group.getName() + "@kuku", defaultSipUriMapping.getIdentity());
        assertTrue(defaultSipUriMapping.getContact().contains("1234@kuku"));

        // the last alias is an extension => identity
        AliasMapping am = aliases.get(aliases.size() - 1);
        assertEquals(am.getIdentity(), group.getExtension() + "@kuku");
        assertTrue(am.getContact().startsWith(group.getName() + "@kuku"));
    }

    private CallGroup createCallGroupWithUsers(String extension, String name, int numUsers) {
        CallGroup group = new CallGroup();
        group.setName("sales");
        group.setExtension("401");

        final int ringsLen = 5;
        for (int i = 0; i < ringsLen; i++) {
            User u = new User();
            u.setUserName("testUser" + i);
            group.insertRingForUser(u);
        }

        return group;
    }

    public void testGenerateAliasesNameAndExtSame() {
        CallGroup group = new CallGroup();
        group.setName("402");
        group.setExtension("402");
        group.setEnabled(true);
        List aliases = group.generateAliases("kiwi");
        assertEquals(0, aliases.size());
    }

    public void testGenerateAliasesLastParallel() {
        CallGroup group = new CallGroup();
        group.setName("sales");
        group.setExtension("401");

        final int ringsLen = 5;
        for (int i = 0; i < ringsLen; i++) {
            User u = new User();
            u.setUserName("testUser" + i);
            UserRing ring = group.insertRingForUser(u);
            if (i == ringsLen - 1) {
                ring.setType(AbstractRing.Type.IMMEDIATE);
            }
        }

        List aliases = group.generateAliases("kuku");
        // disabled group should not generate aliases
        assertTrue(aliases.isEmpty());

        group.setEnabled(true);
        aliases = group.generateAliases("kuku");

        assertEquals(ringsLen + 1, aliases.size());
        for (int i = 0; i < ringsLen; i++) {
            AliasMapping am = (AliasMapping) aliases.get(i);
            assertEquals(am.getIdentity(), group.getName() + "@kuku");
            assertTrue(am.getContact().startsWith("<sip:testUser" + i + "@kuku"));
            // for all but last we need sipx-noroute=Voicemail in the sequence
            assertTrue(0 < am.getContact().indexOf("sipx-noroute=Voicemail"));
        }

        // the last alias is an extension => identity
        AliasMapping am = (AliasMapping) aliases.get(aliases.size() - 1);
        assertEquals(am.getIdentity(), group.getExtension() + "@kuku");
        assertTrue(am.getContact().startsWith(group.getName() + "@kuku"));
    }

    public void testClone() {
        CallGroup group = new CallGroup();
        group.setName("sales");
        group.setExtension("401");

        final int ringsLen = 5;
        for (int i = 0; i < ringsLen; i++) {
            User u = new User();
            u.setUserName("testUser" + i);
            group.insertRingForUser(u);
        }
        assertEquals(ringsLen, group.getRings().size());

        CallGroup clonedGroup = (CallGroup) group.duplicate();
        assertEquals("sales", clonedGroup.getName());
        assertEquals("401", clonedGroup.getExtension());
        List clonedCalls = clonedGroup.getRings();
        assertEquals(ringsLen, clonedCalls.size());
        for (int i = 0; i < ringsLen; i++) {
            UserRing ring = (UserRing) clonedCalls.get(i);
            assertEquals("testUser" + i, ring.getUser().getUserName());
            assertSame(clonedGroup, ring.getCallGroup());
        }

    }
}
