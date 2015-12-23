package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.myrobotlab.service.ProgramAB.Response;

public class ProgramABTest {
	
	private ProgramAB testService;
	private String session = "testUser";
	private String botName = "lloyd";
	private String path = "test/ProgramAB";

	
	@Before
	public void setUp() throws Exception {
		// Load the service under test
		// a test robot
		testService = new ProgramAB("lloyd");
		// start the service.
		testService.startService();
		// load the bot brain for the chat with the user
		testService.startSession(path, session, botName);
		
		// TODO: clean out any aimlif or existing predicates for the bot that might
		// have been saved in a previous test run!

	}

	@Test
	public void testProgramAB() throws Exception {
		// a response
		Response resp = testService.getResponse(session, "UNIT TEST PATTERN");
		// System.out.println(resp.msg);
		assertEquals("Unit Test Pattern Passed", resp.msg);
	}
	
	@Test
	public void testOOBTags() throws Exception {
		Response resp = testService.getResponse(session, "OOB TEST");
		assertEquals("OOB Tag Test", resp.msg);		
		// Thread.sleep(1000);
		Assert.assertNotNull(Runtime.getService("python"));

	}
	
	@Test
	public void testSavePredicates() throws IOException {
		long uniqueVal = System.currentTimeMillis();
		String testValue = String.valueOf(uniqueVal);
		Response resp = testService.getResponse(session, "SET FOO " + testValue);
		assertEquals(testValue, resp.msg);		
		testService.savePredicates();
		testService.reloadSession(path, session, botName);
		resp = testService.getResponse(session, "GET FOO");
		assertEquals("FOO IS " + testValue, resp.msg);		
	}
	
	@Test
	public void testLearn() throws IOException {
		//Response resp1 = testService.getResponse(session, "SET FOO BAR");
		//System.out.println(resp1.msg);
		Response resp = testService.getResponse(session, "LEARN AAA IS BBB");
		System.out.println(resp.msg);
		resp = testService.getResponse(session, "WHAT IS AAA");
		assertEquals("BBB", resp.msg);		
	}
	
	@After
	public void tearDown() throws Exception {
		testService.stopService();
		testService.releaseService();
	} 
	
}
