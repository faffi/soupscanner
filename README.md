soupscanner
===========

Main goal is the automation of testing web service endpoints via the command line. The application consumes a list of web service endpoints (http://www.example.com/?wsdl) and creates SoapUI project files. In order to deal with memory issues, the amount of web service operations per test suite can be throttled. After creating the project files, the security scans can be executed with the securitytestrunner.(bat|sh) and looped through.

	[+] Usage: java -jar <jarfile> <wsdlFile> 
	[+] Creates multiple project for each WSDL with 5 operations in each project. Configurable via -DopCount=X
	[+] Add security scans by specifying -DsecurityScans=Type1,Type2,Typ3
	[+] All JVM flags (-Dfoo=bar) should be passed in before the -jar flag
	[+] Available Security Scans:
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

##Compile:
Add the SoapUI Pro jar to the build path along with the required dependencies for SoapUI Pro (jar files). The code uses soapui classes in order to create the project files, which means it requires all the dependencies which soapui needs to run.

##Tips:
Increase JVM memory -Xmx=4096M
