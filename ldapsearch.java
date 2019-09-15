/*

MIT License

Copyright (c) 2017 Ildar Shaimordanov

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

*/

import java.util.Hashtable;

import java.util.List;
import java.util.ArrayList;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

public class ldapsearch {

	static final String LDAP_CTX_FACTORY = "com.sun.jndi.ldap.LdapCtxFactory";

	static final String DEFAULT_SCOPE_NAME = "sub";
	static final String DEFAULT_DEREF_NAME = "never";
	static final String DEFAULT_SEARCH_FILTER = "(objectClass=*)";
	static final String DEFAULT_ATTRIBUTE_FILTER = "*";

	static String ldapProto = "ldap";
	static String ldapHost = "localhost";
	static String ldapPort = "389";
	static String ldapUrl;

	static String ldapBase = "";

	static String searchScopeName = DEFAULT_SCOPE_NAME;
	static int searchScope = SearchControls.SUBTREE_SCOPE;

	static String searchDerefName = DEFAULT_DEREF_NAME;
	static boolean searchDeref = false;

	static int searchTimeLimit = 0;
	static long searchCountLimit = 0;

	static String bindDN = null;
	static String bindPasswd = null;

	static String searchFilter = null;
	static String[] attributeFilter = null;

	static Hashtable<String, Boolean> options = new Hashtable<String, Boolean>();

	public static void main(String[] args) throws Exception {
		if ( parseArguments(args) ) {
			search();
		}
	}

	public static void search() throws Exception {
		if ( options.containsKey("verbose") ) {
			verbose();
		}

		if ( options.containsKey("dry-run") ) {
			return;
		}

		Hashtable<String, String> env = new Hashtable<String, String>();

		env.put(Context.PROVIDER_URL, ldapUrl);
		env.put(Context.INITIAL_CONTEXT_FACTORY, LDAP_CTX_FACTORY);
		if ( bindDN != null ) {
			env.put(Context.SECURITY_PRINCIPAL, bindDN);
		}
		if ( bindPasswd != null ) {
			env.put(Context.SECURITY_CREDENTIALS, bindPasswd);
		}
		if ( options.containsKey("simple-auth") ) {
			env.put(Context.SECURITY_AUTHENTICATION, "simple");
		}

		SearchControls sc = new SearchControls(
			searchScope, 
			searchCountLimit, 
			searchTimeLimit, 
			attributeFilter, 
			false, // return the object bound to the name of the entry
			searchDeref);

		DirContext dctx = new InitialDirContext(env);
		NamingEnumeration results = dctx.search(ldapBase, searchFilter, sc);

		boolean optionNamesOnly = options.containsKey("names-only");

		while ( results.hasMore() ) {
			SearchResult sr = (SearchResult) results.next();

			Attributes attrs = sr.getAttributes();
			NamingEnumeration<String> names = attrs.getIDs();

			while ( names.hasMore() ) {
				String name = names.next();

				if ( optionNamesOnly ) {
					System.out.println(name);
					continue;
				}

				Attribute attr = attrs.get(name);
				NamingEnumeration values = attr.getAll();

				while ( values.hasMore() ) {
					Object value = values.next();
					System.out.println(name + ": " + 
						( value == null ? "" : value.toString() ));
				}
			}

			System.out.println();
		}

		dctx.close();
	}

	public static void verbose() {
		String runLog = joinArray("\n", new String[] {
			"# Connection : %s",
			"# Base DN    : %s",
			"# Scope      : %s",
			"# Filter     : %s",
			"# Attributes : [ %s ]",
		});

		System.out.println(String.format(runLog, 
			ldapUrl, 
			ldapBase, 
			searchScopeName, 
			searchFilter, 
			joinArray(" ", attributeFilter)));
	}

	public static boolean parseArgumentScope(String arg) {
		searchScopeName = arg;
	
		boolean result = true;
		if ( "base".equalsIgnoreCase(arg) ) {
			searchScope = SearchControls.OBJECT_SCOPE;
		} else if ( "one".equalsIgnoreCase(arg) ) {
			searchScope = SearchControls.ONELEVEL_SCOPE;
		} else if ( "sub".equalsIgnoreCase(arg) ) {
			searchScope = SearchControls.SUBTREE_SCOPE;
		} else {
			result = false;
		}
		return result;
	}
	public static boolean parseArgumentDeref(String arg) {
		searchDerefName = arg;
	
		boolean result = true;
		if ( "never".equalsIgnoreCase(arg) ) {
			searchDeref = false;
		} else if ( "always".equalsIgnoreCase(arg) ) {
			searchDeref = true;
		} else {
			result = false;
		}
		return result;
	}

	public static boolean parseArguments(String[] args) throws Exception {
		int i = 0, n = args.length;

		if ( n == 0 ) {
			printUsage();
			return false;
		}

		List<String> attributeFilterList = new ArrayList<String>();

		while ( i < n ) {
			switch(args[i]) {
			case "-h":
				i++;
				ldapHost = args[i];
				break;
			case "-p":
				i++;
				ldapPort = args[i];
				break;
			case "-b":
				i++;
				ldapBase = args[i];
				break;
			case "-s":
				i++;
				if ( ! parseArgumentScope(args[i]) ) {
					throw new Exception("Unknown scope: " + searchScopeName);
				}
				break;
			case "-D":
				i++;
				bindDN = args[i];
				break;
			case "-w":
				i++;
				bindPasswd = args[i];
				break;
			case "-a":
				i++;
				if ( ! parseArgumentDeref(args[i]) ) {
					throw new Exception("Unknown deref: " + searchDerefName);
				}
				break;
			case "-l":
				i++;
				searchTimeLimit = 1000 * Integer.parseInt(args[i]);
				break;
			case "-z":
				i++;
				searchCountLimit = Long.parseLong(args[i]);
				break;
			case "-A":
				options.put("names-only", true);
				break;
			case "-n":
				options.put("dry-run", true);
				options.put("verbose", true);
				break;
			case "-v":
				options.put("verbose", true);
				break;
			case "-V":
				printVersion();
				return false;
			case "-x":
				options.put("simple-auth", true);
				break;
			default:
				if ( args[i].startsWith("-") ) {
					throw new Exception("Unrecognized option: " + args[i]);
				}

				if ( searchFilter == null ) {
					searchFilter = args[i];
				} else {
					attributeFilterList.add(args[i]);
				}
				break;
			}
			i++;
		}

		ldapUrl = String.format("%s://%s:%s", 
			ldapProto, ldapHost, ldapPort);

		if ( searchFilter == null ) {
			searchFilter = DEFAULT_SEARCH_FILTER;
		}

		if ( attributeFilterList.isEmpty() ) {
			attributeFilterList.add(DEFAULT_ATTRIBUTE_FILTER);
		}

		attributeFilter = new String[ attributeFilterList.size() ];
		attributeFilterList.toArray(attributeFilter);

		return true;
	}

	public static void printUsage() {
		System.out.println(usage);
	}

	public static void printVersion() {
		System.out.println(version);
	}

	// Since Java 8 String.join() is available
	public static String joinArray(String delimiter, String[] elements) {
		if ( elements == null || elements.length == 0 ) {
			return "";
		}
		int i = 0, n = elements.length;
		String result = elements[0];
		for (i = 1; i < n; i++) {
			result += delimiter + elements[i];
		}
		return result;
	}

	public static String version = "ldapsearch: java-based ldapsearch"
		+ "\nhttps://github.com/ildar-shaimordanov/ldapsearch.java"
		+ "\n";

	public static String usage = "Usage:"
		+ "\n    ldapsearch [options] filter [attributes...]"
		+ "\n"
		+ "\nwhere:"
		+ "\n    filter      LDAP search filter; default is " + DEFAULT_SEARCH_FILTER
		+ "\n    attributes  whitespace-separated list of attributes to retrieve;"
		+ "\n                no attributes means all attributes; default is " + DEFAULT_ATTRIBUTE_FILTER
		+ "\n"
		+ "\nSearch options:"
		+ "\n    -a deref    one of never, always; default is " + DEFAULT_DEREF_NAME
		+ "\n    -A          retrieve attribute names only (no values)"
		+ "\n    -b basedn   base dn for search"
		+ "\n    -l time     time limit (in seconds) for search"
//		+ "\n    -L          print entries in LDIF format"
		+ "\n    -s scope    one of base, one, or sub (search scope); default is " + DEFAULT_SCOPE_NAME
		+ "\n    -z size     size limit (in entries) for search"
		+ "\n"
		+ "\nCommon options:"
//		+ "\n    -d level    set LDAP debugging level to 'level'"
		+ "\n    -D binddn   bind dn"
		+ "\n    -h host     ldap server; default is localhost"
		+ "\n    -n          show what would be done but don't actually search"
		+ "\n    -p port     port on ldap server; default is 389"
		+ "\n    -v          run in verbose mode (diagnostics to standard output)"
		+ "\n    -V          print version info"
		+ "\n    -w passwd   bind passwd (for simple authentication)"
		+ "\n    -x          Simple authentication"
		+ "\n";

}
