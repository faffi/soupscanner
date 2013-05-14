soupscanner
===========

Java class that automates the creation of SoapUI projects with security scans

	java -jar soupscanner.jar wsdls.txt
		[+] Usage: java -jar <jarfile> <wsdlFile> 
		[-] Incorrect argument count: 0
		[+] Creates multiple project for each WSDL with 5 operations in each project. Configurable via -DopCount=X
		[+] Add security scans by specifying -DsecurityScans=Type1,Type2,Typ3
		[+] All JVM flags (-Dfoo=bar) should be passed in before the -jar flag
		[+] Available Security Scans:
		[-] Args: []
		[+] 	Boundary Scan
		[+] 	Cross Site Scripting
		[+] 	Custom Script
		[+] 	Fuzzing Scan
		[+] 	Invalid Types
		[+] 	Malformed XML
		[+] 	Malicious Attachment
		[+] 	SQL Injection
		[+] 	XML Bomb
		[+] 	XPath Injection

