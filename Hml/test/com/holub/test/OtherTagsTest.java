package com.holub.test;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.holub.hml.Configuration;
import com.holub.hml.NoteSet;
import com.holub.hml.Tags;
import com.holub.hml.Filter.BlockType;
import com.holub.text.ReportingStream;
import com.holub.text.Text;
import com.holub.util.IdGenerator;

/**
 * @author Allen Holub
 * 
 *
 * <div style='font-size:8pt; margin-top:.25in;'>
 * &copy;2013 <!--copyright 2013--> Allen I Holub. All rights reserved.
 * This code is licensed under a variant on the BSD license. View
 * the complete text at <a href="http://holub.com/license.html">
 * http://www.holub.com/license.html</a>.
 * </div>
 */
public class OtherTagsTest
{
	private Tags tagObject;
	ReportingStream error  = new ReportingStream(null);
	Configuration   config = new Configuration(error);
	NoteSet endNotes 	   = new NoteSet(config);
	
	@Before
	public void setup()
	{
		tagObject = new Tags(config, endNotes);
		
		// Reset the ID generator to 0 so that the tests will create predictable IDs. If you
		// didn't do this, the IDs would carry over from one test to another, and moving
		// the test int the source code could cause the test to fail.
		IdGenerator.resetForTestingPurposes();
	}
	
	/** This test is just checking that we're accumulating the tags correctly and that
	 *  we can insert them into a string. It doesn't insert them into the actual
	 *  <head> element. Find a test for that in IntegrationTests.java.
	 */
	@Test 
	public void headTag()
	{
		Text headText1 = new Text("\n",
			"    <style>",
			"    .foo { color=\"red\"; }",
			"    </style>"
			);
		
		Text headText2 = new Text("\n",
			"    <title>My Title</title>"
			);
				
		Text input = new Text("\n",
			"<head>",
			headText1,
			"</head>",
			"between",
			"<head>",
			headText2,
			"</head>",
			""
			);
		
		Text expected = new Text("\n",
			"" ,
			"between",
			"",
			""
			);

		tagObject.filter(Text.EMPTY, input, Text.EMPTY, BlockType.TEXT);
		TestUtil.assertEquals("Head elements stripped correctly", expected, input);
		
		// The previous code processed the head elements and squirelled away the results.
		// The following code inserts those results into an existing (empty) head
		// element
		//
		Text existingHead = new Text("\n",
			"<head>",
			"</head>",
			""
			);
		
		expected = new Text("\n",
			"<head>",
			"",
			headText1,
			"",
			headText2,
			"</head>",
			""
			);
		
		Text newHead = tagObject.appendAdditionsToHead(existingHead);
		TestUtil.assertEquals("Head elements inserted correctly", expected, newHead );
	}

	@Test
	public void blockTag()
	{
		Text input = new Text("\n",
				"above",
				"<blockquote>",
				"line A",
				"</blockquote>",
				
				"<block class=\"fred\">",
				"line 1",
				"line 2",
				"</block>",
				"below",
				""
				);
		
		Text expected = new Text("\n",
				"above",
				"<blockquote>",
				"line A",
				"</blockquote>",
				
				"<blockquote class=\"fred\">",
				"line 1<br>",
				"line 2<br>",
				"</blockquote>",
				"below",
				""
				);
		
		tagObject.filter(Text.EMPTY, input, Text.EMPTY, BlockType.TEXT);
		TestUtil.assertEquals(expected,input);
	}

	@Test
	public void index()
	{
		Text input = new Text("\n",
				"x1<index-entry topic=\"X\"></index-entry>",
				"a1<index-entry topic=\"A, A\"></index-entry>",
				"x2<index-entry topic=\"X\">X2</index-entry>",
				"x3<index-entry topic=\"X\"></index-entry>",
				"<index>title</index>"
				);
		
		Text expected = new Text("\n",
				"x1<a name=\"hmlIndex-1-1\"></a>",
				"a1<a name=\"hmlIndex-2-1\"></a>",
				"x2<a name=\"hmlIndex-1-2\"></a>",
				"x3<a name=\"hmlIndex-1-3\"></a>",
				"<div class=\"hmlIndex\"\">",
				"title",
				"<div class=\"hmlTopics\">",
				"<div class=\"hmlTopicGroup\" id=\"2\">",
				"	<div class=\"hmlTopic\">A, A</div>",
				"	<div class=\"hmlTopicLocationGroup\">",
				"<a class=\"hmlTopicLocation\" href=\"#hmlIndex-2-1\">1</a>",
				"	</div>",
				"</div>",
				"<div class=\"hmlTopicGroup\" id=\"1\">",
				"	<div class=\"hmlTopic\">X</div>",
				"	<div class=\"hmlTopicLocationGroup\">",
				"<a class=\"hmlTopicLocation\" href=\"#hmlIndex-1-1\">1</a>,",
				"<a class=\"hmlTopicLocation\" href=\"#hmlIndex-1-2\">X2</a>,",
				"<a class=\"hmlTopicLocation\" href=\"#hmlIndex-1-3\">3</a>",
				"	</div>",
				"</div>",
				"</div></div>"
				);
		tagObject.filter(Text.EMPTY, input, Text.EMPTY, BlockType.TEXT);
		TestUtil.assertEquals(expected,input);
	}
	
	@Test public void indexSortedCorrectly()
	{
		Text input = new Text("\n",
			"<index-entry topic=\"B@7\"></index-entry>",
			"<index-entry topic=\"A@5\"></index-entry>",
			"<index-entry topic=\"b@6\"></index-entry>",
			"<index-entry topic=\"a@4\"></index-entry>",
			"<index-entry topic=\"~@3\"></index-entry>",
			"<index-entry topic=\"^@2\"></index-entry>",
			"<index-entry topic=\"!@1\"></index-entry>",
			"<index></index>"
		);
		
		tagObject.filter(Text.EMPTY, input, Text.EMPTY, BlockType.TEXT);
		
		assertTrue( "1>=2", input.indexOf("@1") < input.indexOf("@2") );
		assertTrue( "2>=3", input.indexOf("@2") < input.indexOf("@3") );
		assertTrue( "3>=4", input.indexOf("@3") < input.indexOf("@4") );
		assertTrue( "4>=5", input.indexOf("@4") < input.indexOf("@5") );
		assertTrue( "5>=6", input.indexOf("@5") < input.indexOf("@6") );
		assertTrue( "6>=7", input.indexOf("@6") < input.indexOf("@7") );
	}
	
	@Test public void pluralTopics()
	{
		Text input = new Text("\n",
			"<index-entry topic=\"xx\"></index-entry>",
			"<index-entry topic=\"xxs\"></index-entry>",
			"<index-entry topic=\"xxes\"></index-entry>",
			"<index></index>"
		);
		Text expected = new Text("\n",
				"<a name=\"hmlIndex-1-1\"></a><a name=\"hmlIndex-1-2\"></a><a name=\"hmlIndex-1-3\"></a>",
				"<div class=\"hmlIndex\"\">",
				"",
				"<div class=\"hmlTopics\">",
				"<div class=\"hmlTopicGroup\" id=\"1\">",
				"	<div class=\"hmlTopic\">xx</div>",
				"	<div class=\"hmlTopicLocationGroup\">",
				"<a class=\"hmlTopicLocation\" href=\"#hmlIndex-1-1\">1</a>,",
				"<a class=\"hmlTopicLocation\" href=\"#hmlIndex-1-2\">2</a>,",
				"<a class=\"hmlTopicLocation\" href=\"#hmlIndex-1-3\">3</a>",
				"	</div>",
				"</div>",
				"</div></div>"
		);

		tagObject.filter(Text.EMPTY, input, Text.EMPTY, BlockType.TEXT);
		TestUtil.assertEquals(expected,input);
	}
	
	@Test public void indexTopicWithFormatting()
	{
		Text input = new Text("\n",
				"<index-entry topic=\"comments, (<nobr><code>&lt;</code></nobr><nobr><code>&#33;&#61;&nbsp;&#46;&#46;&#46;&nbsp;&#61;&#33;</code></nobr><nobr><code>&gt;</code></nobr>)\">",
				"</index-entry>",
				"<index></index>"
				);
		
		Text expected = new Text("\n",
				"<a name=\"hmlIndex-1-1\"></a>",
				"<div class=\"hmlIndex\"\">",
				"",
				"<div class=\"hmlTopics\">",
				"<div class=\"hmlTopicGroup\" id=\"1\">",
				"	<div class=\"hmlTopic\">comments, (<nobr><code>&lt;</code></nobr><nobr><code>&#33;&#61;&nbsp;&#46;&#46;&#46;&nbsp;&#61;&#33;</code></nobr><nobr><code>&gt;</code></nobr>)</div>",
				"	<div class=\"hmlTopicLocationGroup\">",
				"<a class=\"hmlTopicLocation\" href=\"#hmlIndex-1-1\">1</a>",
				"	</div>",
				"</div>",
				"</div></div>"
				);
		
		tagObject.filter(Text.EMPTY, input, Text.EMPTY, BlockType.TEXT);
		TestUtil.assertEquals(expected,input);
	}
}
