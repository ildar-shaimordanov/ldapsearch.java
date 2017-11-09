# ldapsearch.java

__One weird attempt to understand Java and LDAP__

This experimental script was created with purpose to better understand 
Java and LDAP. As result the simple tool was developed implementing 
limited functionality of the standard utility ldapsearch. 

There are some source codes inspiring to realize the tool:

* http://iubio.bio.indiana.edu/grid/directories/ldapsearch.java
* http://www.java2s.com/Code/Java/JNDI-LDAP/LDAPSearch.htm
* http://www.adamretter.org.uk/blog/entries/LDAPTest.java
* https://linux.die.net/man/1/ldapsearch
* https://github.com/perl-ldap/perl-ldap

... and innumerable answers on a lot of my questions found in Google, 
StackOverflow and online Java documentation. 

The tool doesn't use any external Java libraries. It supports some of 
frequently used options of the ldapsearch utility. All you need is to 
compile the source once and run with options specific to your environment:

```
javac ldapsearch.java
java ldapseach [option] filter [attributes]
```