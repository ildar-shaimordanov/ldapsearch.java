---
title: LDAP Search Filter Cheatsheet
author: Jon LaBelle
date: January 4, 2021
source: https://jonlabelle.com/snippets/view/markdown/ldap-search-filter-cheatsheet
notoc: true
---

# LDAP Search Filter Cheatsheet

- [Filter operators](#filter-operators)
    - [Comparison operators](#comparison-operators)
    - [Combination operators](#combination-operators)
- [Filter basics](#filter-basics)
    - [To match a single attribute](#to-match-a-single-attribute)
    - [To match two attributes \(and\)](#to-match-two-attributes-and)
    - [To match two attributes \(or\)](#to-match-two-attributes-or)
    - [To match three attributes \(and\)](#to-match-three-attributes-and)
    - [To match three attributes \(or\)](#to-match-three-attributes-or)
    - [To perform a wildcard search](#to-perform-a-wildcard-search)
- [Sample filters](#sample-filters)
    - [Users in group](#users-in-group)
    - [Users in group \(include nested\)](#users-in-group-include-nested)
    - [Users in multiple groups](#users-in-multiple-groups)
    - [Users that must change their password at next logon](#users-that-must-change-their-password-at-next-logon)
    - [Users starting with a particular name](#users-starting-with-a-particular-name)
    - [Users by job title](#users-by-job-title)
- [Active Directory filters](#active-directory-filters)
    - [Domain and Enterprise Admins](#domain-and-enterprise-admins)
    - [All users except blocked](#all-users-except-blocked)
    - [Disabled user accounts](#disabled-user-accounts)
    - [Users with password never expires enabled](#users-with-password-never-expires-enabled)
    - [Users with empty email](#users-with-empty-email)
    - [Users in department](#users-in-department)
    - [Exclude disabled users](#exclude-disabled-users)
- [More Active Directory filters](#more-active-directory-filters)
- [References](#references)
- [Additional Resources](#additional-resources)

## Filter operators

### Comparison operators

The following comparison operators can be used in a filter:

| Operator |         Meaning          |
| -------- | ------------------------ |
| `=`      | Equality                 |
| `>=`     | Greater than or equal to |
| `<=`     | Less than or equal to    |
| `~=`     | Approximately equal to   |

For example, the following filter returns all objects with *cn* (common name) attribute value *Jon*:

    (cn=Jon)

### Combination operators

Filters can be combined using boolean operators when there are multiple search conditions

| Operator |               Description                   |
|----------|-------------------------------------------- |
| `&`      | AND --- all conditions must be met          |
| `\|`      | OR --- any number of conditions can be met |
| `!`      | NOT --- the condition must not be met       |

For example, to select objects with *cn* equal to *Jon* and *sn* (surname/last name) equal to *Brian*:

    (&(cn=Jon)(sn=Brian))

### Special Characters

The LDAP filter specification assigns special meaning to the following characters:

| Character | Hex Representation |
|-----------|--------------------|
| `*`       | `\2A`              |
| `(`       | `\28`              |
| `)`       | `\29`              |
| `\`       | `\5C`              |
| `Nul`     | `\00`              |

For example, to find all objects where the common name is `James Jim*) Smith`, the LDAP filter would be:

    (cn=James Jim\2A\29 Smith)

## objectCategory and objectClass

|    objectCategory    |             objectClass             |          Result          |
| -------------------- | ----------------------------------- | ------------------------ |
| person               | user                                | user objects             |
| person               | n/a                                 | user and contact objects |
| person               | contact                             | contact objects          |
| user                 | user and computer objects           | n/a                      |
| computer             | n/a                                 | computer objects         |
| user                 | n/a                                 | user and contact objects |
| contact              | contact objects                     | n/a                      |
| computer             | computer objects                    | n/a                      |
| person               | user, computer, and contact objects | n/a                      |
| contact              | n/a                                 | user and contact objects |
| group                | n/a                                 | group objects            |
| n/a                  | group                               | n/a                      |
| person               | organizationalPerson                | user and contact objects |
| organizationalPerson | user, computer, and contact objects | n/a                      |
| organizationalPerson | n/a                                 | user and contact objects |

> Use the filter that makes your intent most clear. Also, if you have a choice
> between using *objectCategory* and *objectClass*, it is recommended that you use
> *objectCategory*. That is because *objectCategory* is both single valued and
> indexed, while *objectClass* is multi-valued and not indexed (except on Windows
> Server 2008 and above). A query using a filter with *objectCategory* will be
> more efficient than a similar filter with *objectClass*. Windows Server 2008
> domain controllers (and above) have a special behavior that indexes the
> *objectClass* attribute. You can take advantage of this if all of your domain
> controllers are Windows Server 2008, or if you specify a Windows Server 2008
> domain controller in your query. --- [Source](https://social.technet.microsoft.com/wiki/contents/articles/5392.active-directory-ldap-syntax-filters.aspx?Sort=MostRecent)

## Filter basics

### To match a single attribute

    (sAMAccountName=<SomeAccountName>)

### To match two attributes (and)

    (&(objectClass=<person>)(objectClass=<user>))

### To match two attributes (or)

    (|(objectClass=<person>)(objectClass=<user>))

### To match three attributes (and)

    (&(objectClass=<user>)(objectClass=<top>)(objectClass=<person>))

### To match three attributes (or)

    (!(objectClass=<user>)(objectClass=<top>)(objectClass=<person>))

### To perform a wildcard search

    (&(objectClass=<user>)(cn=<*Marketing*>))

## Sample filters

### Users in group

To retrieve user account names (`sAMAccountName`) that are a member of a particular group (`SomeGroupName`):

    (&(objectCategory=Person)(sAMAccountName=*)(memberOf=cn=<SomeGroupName>,ou=<users>,dc=<company>,dc=<com>))

### Users in group (include nested)

To retrieve user account names (`sAMAccountName`), and nested user account names that are a member of a particular group (`SomeGroupName`):

    (&(objectCategory=Person)(sAMAccountName=*)(memberOf:1.2.840.113556.1.4.1941:=cn=<SomeGroupName>,ou=users,dc=company,dc=com))

### Users in multiple groups

To retrieve user account names (`sAMAccountName`) that are a member of any, or all the 4 groups (`fire`, `wind`, `water`, `heart`):

    (&(objectCategory=Person)(sAMAccountName=*)(|(memberOf=cn=<fire>,ou=<users>,dc=<company>,dc=<com>)(memberOf=cn=<wind>,ou=<users>,dc=<company>,dc=<com>)(memberOf=cn=<water>,ou=<users>,dc=<company>,dc=<com>)(memberOf=cn=<heart>,ou=<users>,dc=<company>,dc=<com>)))

### Users that must change their password at next logon

To search Active Directory for users that must change their password at next logon:

    (objectCategory=person)(objectClass=user)(pwdLastSet=0)(!userAccountControl:1.2.840.113556.1.4.803:=2)

### Users starting with a particular name

To search *user* objects that start with Common Name *Brian* (`cn=Brian*`):

    (&(objectClass=user)(cn=<Brian*>))

### Users by job title

To find all users with a job title starting with *Manager* (`Title=Manager*`):

    (&(objectCategory=person)(objectClass=user)(Title=<Manager*>))

## Active Directory filters

Search filters supported only by Microsoft Active Directory.

### Domain and Enterprise Admins

To search for administrators in groups Domain Admins, Enterprise Admins:

    (objectClass=user)(objectCategory=Person)(adminCount=1)

### All users except blocked

To search all users except for blocked ones:

    (objectCategory=person)(objectClass=user)(!userAccountControl:1.2.840.113556.1.4.803:=2)

### Disabled user accounts

To list only disabled user accounts:

    (objectCategory=person)(objectClass=user)(userAccountControl:1.2.840.113556.1.4.803:=16)

### Users with password never expires enabled

    (objectCategory=user)(userAccountControl:1.2.840.113556.1.4.803:=65536)

### Users with empty email

    (objectCategory=person)(!mail=*)

### Users in department

To search users in a particular department:

    (&(objectCategory=person)(objectClass=user)(department=<Sales>))

### Exclude disabled users

To find as user (`sAMAccountName=<username>`) that isn't disabled:

    (&(objectCategory=person)
    (objectClass=user)
    (sAMAccountType=805306368)
    (!(userAccountControl:1.2.840.113556.1.4.803:=2))
    (sAMAccountName=<username>))

- The filter `(sAMAccountType=805306368)` on user objects is more efficient, but is harder to remember. \([Source](https://social.technet.microsoft.com/wiki/contents/articles/5392.active-directory-ldap-syntax-filters.aspx?Sort=MostRecent)\)
- The filter `(!(UserAccountControl:1.2.840.113556.1.4.803:=2))` excludes disabled user objects. \([Source](https://community.atlassian.com/t5/Jira-questions/Ignoring-disabled-users-in-LDAP-Active-Directory/qaq-p/451709)\)

## More Active Directory filters

Kore Active Directory filter samples can be found [here](https://social.technet.microsoft.com/wiki/contents/articles/5392.active-directory-ldap-syntax-filters.aspx?Sort=MostRecent).

## References

- [Atlassian Support: How to write LDAP search filters](https://confluence.atlassian.com/kb/how-to-write-ldap-search-filters-792496933.html)
- [TheITBros.com: Active Directory LDAP Query Examples](https://theitbros.com/ldap-query-examples-active-directory/)
- [Active Directory: LDAP Syntax Filters](https://social.technet.microsoft.com/wiki/contents/articles/5392.active-directory-ldap-syntax-filters.aspx)

## Additional Resources

- [Active Directory Glossary](https://social.technet.microsoft.com/wiki/contents/articles/16757.active-directory-glossary.aspx) - This is a glossary of terms and acronyms used in Active Directory and related technologies.
- [Microsoft Docs: Active Directory Schema (AD Schema) Definitions](https://docs.microsoft.com/en-us/windows/win32/adschema/active-directory-schema) - Formal definitions of every attribute that can exist in an Active Directory object.

