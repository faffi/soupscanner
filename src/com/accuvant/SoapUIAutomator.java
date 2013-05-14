package com.accuvant;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;

import com.eviware.soapui.DefaultSoapUICore;
import com.eviware.soapui.SoapUI;
import com.eviware.soapui.config.SecurityTestConfig;
import com.eviware.soapui.config.TestCaseConfig;
import com.eviware.soapui.config.TestStepConfig;
import com.eviware.soapui.impl.WsdlInterfaceFactory;
import com.eviware.soapui.impl.wsdl.WsdlInterface;
import com.eviware.soapui.impl.wsdl.WsdlOperation;
import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.impl.wsdl.WsdlRequest;
import com.eviware.soapui.impl.wsdl.WsdlTestSuite;
import com.eviware.soapui.impl.wsdl.testcase.WsdlTestCase;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlTestStep;
import com.eviware.soapui.impl.wsdl.teststeps.assertions.TestAssertionRegistry;
import com.eviware.soapui.impl.wsdl.teststeps.registry.WsdlTestRequestStepFactory;
import com.eviware.soapui.model.security.SecurityScan;
import com.eviware.soapui.model.testsuite.TestStep;
import com.eviware.soapui.security.SecurityTest;
import com.eviware.soapui.security.actions.wizard.SecurityParameterTableWizard.WizardParameter;
import com.eviware.soapui.security.registry.SecurityScanRegistry;
import com.eviware.soapui.security.scan.AbstractSecurityScan;
import com.eviware.soapui.security.scan.AbstractSecurityScanWithProperties;
import com.eviware.soapui.security.support.ParameterExtractor;
import com.eviware.soapui.support.SoapUIException;


public class SoapUIAutomator {

	private static List<String> wsdls = new ArrayList<String>();
	private static Map<String,Throwable> failed = new HashMap<String,Throwable>(); 
	private static int opCount = 5;
	private static List<String> securityScans = new ArrayList<String>();
	
	private static void buildMultiThrottleProject() throws XmlException, IOException, SoapUIException
	{
		WsdlProject project = new WsdlProject();
		WsdlTestSuite currentSuite = null;
		WsdlTestCase currentCase = null;
		String wsdlName;
		int opCount = 0;
		print(String.format("WSDL's Identified: %d", wsdls.size()));
		for (final String w : wsdls)
		{
			try
			{
				int i = 0;
				project = new WsdlProject();
				WsdlInterface wsdlInt = WsdlInterfaceFactory.importWsdl(project, w, true)[0];
				wsdlName = wsdlInt.getName();	//BlahBlahSoap
				currentSuite = project.addNewTestSuite("TestSuite " + wsdlName);
				currentCase = currentSuite.addNewTestCase("TestCase " + wsdlName);
				opCount = wsdlInt.getOperationCount();
				print(String.format("Operations Identified: %d", opCount));
				for (i = 0; i < opCount; i++)
				{
					final WsdlTestStep step = buildTestStep(wsdlInt.getOperationAt(i), currentCase);
					if (currentCase.getTestStepCount() == SoapUIAutomator.opCount || i == wsdlInt.getOperationCount() - 1)
					{
						final String[] hostURL = wsdlInt.getDefinition().split("/");
						if (hostURL.length > 2)
							project.setName(String.format("%s - %s [%d - %d]", wsdlName,hostURL[2], (i+1) - currentCase.getTestStepCount(), i + 1));
						else
							project.setName(String.format("%s - %d [%d - %d]", wsdlName,wsdlInt.hashCode(), (i+1) - currentCase.getTestStepCount(), i + 1));
						currentSuite.setName("TestSuite " + project.getName());
						currentCase.setName("TestCase " + project.getName());
						addSecurityScan(currentCase);
						project.saveAs(project.getName() + ".xml");
						project = new WsdlProject();
//						project.setName(String.format("%s [%d-%d]", wsdlName, opCount, opCount+i));
						wsdlInt = WsdlInterfaceFactory.importWsdl(project, w, true)[0];
						currentSuite = project.addNewTestSuite("TestSuite " + project.getName());
						currentCase = currentSuite.addNewTestCase("TestCase " + project.getName());
						//Save project, init new one
					}
				}
			}
			catch (Exception e)
			{
				System.err.println("Failed to load wsdl: " + w);
				failed.put(w, e);
			}
		}
		
	}
	private static WsdlTestStep buildTestStep(final WsdlOperation op, final WsdlTestCase tc)
	{
		final String opName = op.getName();	
		final WsdlRequest wr = op.getRequestAt(0);
		final TestStepConfig conf = WsdlTestRequestStepFactory.createConfig(wr, opName);
		final WsdlTestStep step = tc.addTestStep(conf);
		return step;
	}

	
	
//	private static void addSecurityScans(final List<WsdlTestStep> scans)
//	{
//		//This would hypothetically just call addSecurityScan for each wsdl test step, but the ID's get messed up for some reason.
//	}
	
	private static void addSecurityScan(final WsdlTestCase testCase)
	{
		final SecurityTestConfig securityTestConfig = ((TestCaseConfig) testCase.getConfig()).addNewSecurityTest();
		final SecurityTest secTest = new SecurityTest(testCase, (SecurityTestConfig) securityTestConfig);
		secTest.setName("SecurityTest");
		secTest.setDescription("\\x24\\x24\\x24\\x24");
		securityTestConfig.setFailOnError(false);
		print("Adding security scans: " + securityScans);
		for (final TestStep wts : testCase.getTestStepList())
		{
			print("Adding test step: " + wts.getName());
			for (final String secScan : securityScans)
			{
				print("Adding security scan: " + secScan);
				final SecurityScan ss = secTest.addNewSecurityScan(wts, secScan);
				final List<WizardParameter> parms = ParameterExtractor.extract(wts);
				for (final WizardParameter wp : parms)
				{
					if (ss instanceof AbstractSecurityScanWithProperties)
						((AbstractSecurityScanWithProperties)ss).getParameterHolder().addParameter(wp.getLabel(), wp.getName(), wp.getXPath(), true);
					else
						print("Ignoring adding properties to scan: " + ss.getName() + " as it does not support parameter based injection attacks");
				}
				ss.setRunOnlyOnce(true);
				ss.addWsdlAssertion(TestAssertionRegistry.getInstance().getAssertionTypeForName("Sensitive Information Exposure"));
			}

		}
		
	}
	
	public static void main(String[] args) 
	{
		Logger.getRootLogger().setLevel(Level.OFF);

		if (args.length != 2)
		{
			printe("Incorrect argument count: " + args.length);
			printe("Args: " + Arrays.asList(args));
			usage();
		}
		
		final String fileName = args[0];

		final File f = new File(fileName);
		if (!f.exists())
		{
			printe("File : " + fileName + " doesn't exist.");
			return;
		}
		BufferedReader reader;
		try
		{
			reader = new BufferedReader(new FileReader(f));
			String s;
			while ((s = reader.readLine()) != null)
			{
				print(String.format("Adding WSDL: [%s]", s.trim()));
				wsdls.add(s.trim());
			}
			reader.close();

		}
		catch (FileNotFoundException e)
		{
			printe("File not found: " + fileName);
			e.printStackTrace();
		}
		catch (IOException e)
		{
			printe("Great job, you broke it.");
			e.printStackTrace();
		}

		setOpCount();
		setSecurityScans();
		run();
		System.exit(0);
	}
	private static void run()
	{
		try
		{
			print("Building projects...");
			buildMultiThrottleProject();
		}
		catch (Exception e)
		{
			printe(e.getMessage());
		}
		
		if (failed.size() > 0 )
		{
			printe("Failed to load the following wsdls:");
			for (final Entry<String,Throwable> item : failed.entrySet())
			{
				printe("\t" + item.getKey());
				item.getValue().printStackTrace();
			}
		}
	}
	private static void setSecurityScans()
	{
		final String scans = System.getProperty("securityScans");
		if (null == scans)
		{
			printe("Security scan values cannot be null, please try again.");
			System.exit(0);
		}

		final String scanArray[] = scans.split(",");
		if (scanArray.length < 1)
		{
			printe("Try again with a security scan name declared");
			System.exit(0);
		}
		for (String requestedScan : scanArray)
		{
			requestedScan = requestedScan.trim();
			if (null == SecurityScanRegistry.getInstance().getFactoryByName(requestedScan))
			{
				printe(String.format("%s is not a valid scan, valid scans are %s", requestedScan, Arrays.asList(SecurityScanRegistry.getInstance().getAvailableSecurityScansNames())));
			}
			else
			{
				securityScans.add(requestedScan);
			}
		}
	}
	private static void setOpCount()
	{
		final String opCountString = System.getProperty("opCount");
		if (null == opCountString)
			return;
		try
		{
			opCount = Integer.parseInt(opCountString);
			if (opCount < 1)
			{
				printe("Try again with something > 0");
				System.exit(0);
			}
		}
		catch (Exception e)
		{
			printe("Try again with an integer.");
			System.exit(0);
		}	
	}
	
	private static void usage()
	{
		print("Usage: java -jar <jarfile> <wsdlFile>");
		print("Creates multiple project for each WSDL with 5 operations in each project. Configurable via -DopCount=X");
		print("Add security scans by specifying -DsecurityScans=Type1,Type2,Typ3");
		print("All JVM flags (-Dfoo=bar) should be passed in before the -jar flag");
		printAvailableSecurityScans();
		System.exit(0);
	}
	private static void printAvailableSecurityScans()
	{
		print("Available Security Scans:");
		for (final String secScan : SecurityScanRegistry.getInstance().getAvailableSecurityScansNames())
		{
			print("\t" + secScan);
		}
	}
	private static void print(final String s)
	{
		System.out.println(String.format("[+] %s", s));
	}
	private static void printe(final String s)
	{
		System.err.println(String.format("[-] %s", s));
	}
}