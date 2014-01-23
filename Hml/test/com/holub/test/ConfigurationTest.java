package com.holub.test;

import static org.junit.Assert.*;

import java.io.StringWriter;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.holub.hml.Configuration;
import com.holub.hml.Filter;
import com.holub.text.ReportingStream;
import com.holub.text.Text;

public class ConfigurationTest
{

	@BeforeClass public static void setUpBeforeClass() throws Exception { }
	@AfterClass public static void tearDownAfterClass() throws Exception { }
	@After public void tearDown() throws Exception { }
	
	StringWriter 	errorOutput;
	ReportingStream error;
	Configuration	oat;
	
	@Before public void setUp() throws Exception
	{	
		errorOutput = new StringWriter();
		error 		= new ReportingStream( errorOutput );
		oat 		= new Configuration(error);
	}

	@Test public void testError()
	{
		oat.error().report("hello");
		assertTrue( errorOutput.toString().contains("hello") );
	}
	
	@Test public void testNewProperty()
	{
		Text input = new Text("\n",
			"<HMLconfig>",
			"key=value",
			"</HMLconfig>"
			);
		
		oat.filter(Text.EMPTY, input, Text.EMPTY, Filter.BlockType.TEXT);
		assertEquals("value", oat.value("key") );
	}
	
	@Test public void testDefaultOverride()
	{
		Text input = new Text("\n",
			"<HMLconfig>",
			"key=newValue",
			"</HMLconfig>"
			);
		
		oat.supplyDefault("key", "defaultValue");
		oat.filter(Text.EMPTY, input, Text.EMPTY, Filter.BlockType.TEXT);
		assertEquals("newValue", oat.value("key") );
	}
	
	@Test public void testDefault()
	{
		Text input = new Text("\n",
			"<HMLconfig>",
			"unusedKey=newValue",
			"</HMLconfig>"
			);
		
		oat.supplyDefault("key", "defaultValue");
		oat.filter(Text.EMPTY, input, Text.EMPTY, Filter.BlockType.TEXT);
		assertEquals("defaultValue", oat.value("key") );
	}
}
