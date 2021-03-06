/*
 *
 *
 * Copyright (C) 2009 Pingtel Corp., certain elements licensed under a Contributor Agreement.
 * Contributors retain copyright to elements licensed under a Contributor Agreement.
 * Licensed to the User under the LGPL license.
 *
 *
 */
package org.sipfoundry.sipxconfig.phonebook;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import com.google.gdata.data.contacts.ContactEntry;
import com.google.gdata.data.contacts.Relation;
import com.google.gdata.data.extensions.Im;
import com.google.gdata.data.extensions.Name;
import com.google.gdata.data.extensions.Organization;
import com.google.gdata.data.extensions.PhoneNumber;
import com.google.gdata.data.extensions.StructuredPostalAddress;

import static org.apache.commons.beanutils.BeanUtils.getSimpleProperty;
import static org.apache.commons.beanutils.PropertyUtils.getProperty;
import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.apache.commons.lang.StringUtils.defaultString;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.isNotEmpty;
import static org.apache.commons.lang.StringUtils.split;

public class PhonebookGmailEntryHelper {
    private static final String WORK = "work";
    private static final String GIVEN_NAME = "givenName";
    private static final String FAMILY_NAME = "familyName";
    private static final String FULL_NAME = "fullName";
    private static final String ORG_TITLE = "orgTitle";
    private static final String ORG_NAME = "orgName";
    private static final String ORG_DEPARTMENT = "orgDepartment";
    private static final String MOBILE = "mobile";
    private static final String HOME = "home";
    private static final String FAX = "fax";
    private static final String CITY = "city";
    private static final String COUNTRY = "country";
    private static final String REGION = "region";
    private static final String STREET = "street";
    private static final String POSTCODE = "postcode";
    private static final String VALUE = "value";
    private static final String ASSISTANT = "assistant";

    private final ContactEntry m_contactEntry;
    private final PhonebookEntry m_phonebookEntry;

    PhonebookGmailEntryHelper(ContactEntry entry) {
        m_contactEntry = entry;
        m_phonebookEntry = createPhonebookEntry();
    }

    public PhonebookEntry getPhonebookEntry() {
        return m_phonebookEntry;
    }

    private PhonebookEntry createPhonebookEntry() {
        PhonebookEntry phonebookEntry = new PhonebookEntry();
        extractName(phonebookEntry);
        AddressBookEntry abe = new AddressBookEntry();
        extractIMs(abe);
        extractAddress(abe);
        extractPhones(phonebookEntry, abe);
        extractOrgs(abe);
        extractRelations(abe);
        phonebookEntry.setAddressBookEntry(abe);
        return phonebookEntry;
    }

    private void extractName(PhonebookEntry phonebookEntry) {
        Name name = m_contactEntry.getName();
        if (name == null) {
            return;
        }
        String givenName = getGDataValue(name, GIVEN_NAME);
        String familyName = getGDataValue(name, FAMILY_NAME);
        if (isNotEmpty(givenName) || isNotEmpty(familyName)) {
            phonebookEntry.setFirstName(givenName);
            phonebookEntry.setLastName(familyName);
        } else {
            String fullName = getGDataValue(name, FULL_NAME);
            String[] names = split(fullName, " ", 2);
            if (names.length > 0) {
                phonebookEntry.setFirstName(names[0]);
            }
            if (names.length > 1) {
                phonebookEntry.setLastName(names[1]);
            }
        }
    }

    private void extractIMs(AddressBookEntry abe) {
        List<Im> ims = m_contactEntry.getImAddresses();
        for (Im im : ims) {
            if (isEmpty(abe.getImId())) {
                abe.setImId(im.getAddress());
            } else {
                if (isEmpty(abe.getAlternateImId())) {
                    abe.setAlternateImId(im.getAddress());
                }
            }
        }
    }

    private void extractPhones(PhonebookEntry phonebookEntry, AddressBookEntry abe) {
        if (!m_contactEntry.hasPhoneNumbers()) {
            return;
        }
        for (PhoneNumber number : m_contactEntry.getPhoneNumbers()) {
            String rel = defaultString(number.getRel());
            String phoneNumber = number.getPhoneNumber();
            if (rel.endsWith(WORK)) {
                phonebookEntry.setNumber(phoneNumber);
            } else if (rel.endsWith(MOBILE)) {
                abe.setCellPhoneNumber(phoneNumber);
            } else if (rel.endsWith(HOME)) {
                abe.setHomePhoneNumber(phoneNumber);
            } else if (rel.endsWith(FAX)) {
                abe.setFaxNumber(phoneNumber);
            }
        }
    }

    private void extractAddress(AddressBookEntry abe) {
        if (!m_contactEntry.hasStructuredPostalAddresses()) {
            return;
        }
        for (StructuredPostalAddress address : m_contactEntry.getStructuredPostalAddresses()) {
            if (address == null) {
                continue;
            }
            Address phonebookAddress = new Address();
            phonebookAddress.setCity(getGDataValue(address, CITY));
            phonebookAddress.setCountry(getGDataValue(address, COUNTRY));
            phonebookAddress.setState(getGDataValue(address, REGION));
            phonebookAddress.setStreet(getGDataValue(address, STREET));
            phonebookAddress.setZip(getGDataValue(address, POSTCODE));

            if (address.getRel().contains(WORK)) {
                abe.setOfficeAddress(phonebookAddress);
            } else {
                abe.setHomeAddress(phonebookAddress);
            }
        }
    }

    private void extractOrgs(AddressBookEntry abe) {
        List<Organization> orgs = m_contactEntry.getOrganizations();
        if (orgs.isEmpty()) {
            return;
        }
        Organization org = orgs.get(0);
        if (org == null) {
            return;
        }
        abe.setJobTitle(getGDataValue(org, ORG_TITLE));
        abe.setCompanyName(getGDataValue(org, ORG_NAME));
        abe.setJobDept(getGDataValue(org, ORG_DEPARTMENT));
        if (org.getWhere() != null) {
            abe.setLocation(org.getWhere().getValueString());
        }
    }

    private void extractRelations(AddressBookEntry abe) {
        for (Relation relation : m_contactEntry.getRelations()) {
            if (relation.hasRel() && relation.getRel().toValue().contains(ASSISTANT)) {
                abe.setAssistantName(relation.getValue());
            }
        }
    }

    /**
     * GData has its own system of "extensible" beans. This is a brute force way of extracting
     * values from them.
     */
    private static String getGDataValue(Object bean, String name) {
        try {
            String value = null;
            Object beanProp = getProperty(bean, name);
            if (beanProp != null) {
                value = getSimpleProperty(beanProp, VALUE);
            }
            return defaultString(value);
        } catch (IllegalAccessException e) {
            return EMPTY;
        } catch (InvocationTargetException e) {
            return EMPTY;
        } catch (NoSuchMethodException e) {
            return EMPTY;
        }
    }
}
