/**
 * 
 */
package com.holub.test;

import java.io.OutputStreamWriter;

import org.junit.*;

import com.holub.hml.Configuration;
import com.holub.hml.Titles;
import com.holub.hml.Filter.BlockType;
import com.holub.text.ReportingStream;
import com.holub.text.Text;

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
public class TocTest
{
	private Titles objectUnderTest;
	private static final ReportingStream error  = new ReportingStream( new OutputStreamWriter(System.err) );
	private static final Configuration   config = new Configuration(error);
	
	@Before
	public void setup()
	{
		objectUnderTest = new Titles( config );
	}
	
	@Test
	public void tableOfContentsWithStyle()
	{
		Text input = new Text("\n",
				"<toc class=\"myClass\" style=\"color:red;\">Title</toc>",
				"<h1>Level-1 Heading</h1>"
			);
		
		Text expected = new Text("\n",
				"<div style=\"color:red;\" class=\"myClass\">",
				"<div class=\"hmlTocTitle\">Title</div>",
				"<div class=\"hmlTocLev1\"><a href=\"#hmlContents0\">1. Level-1 Heading</a></div>",
				"</div>",
				"<a name=\"hmlContents0\"><h1>1. Level-1 Heading</h1></a>"
			);
		
		objectUnderTest.filter( Text.EMPTY, input, Text.EMPTY, BlockType.TEXT );
		objectUnderTest.getTocReplacementFilter().filter( Text.EMPTY, input, Text.EMPTY, BlockType.TEXT );
		
		TestUtil.assertEquals( expected, input);
	}
	
	@Test
	public void tableOfContents()
	{
		Text input = new Text("\n",
				"<toc> TOC </toc>",
				"<h0>H-0</h0>",
				"<h1>H-1</h1>",
				"<h2>H-1.1</h2>",
				"<h1 label=\"label-2\">H-2</h1>",
				"<h2 label=\"label-2.1\">H-2.1</h2>",
				"<h1 toc=\"no\">H-3</h1>",
				"<h2 toc=\"no\" label=\"label-3.1\">H-3.1</h2>"				
			);		
		
		Text expected = new Text("\n",
				"<div class=\"hmlToc\">",
				"<div class=\"hmlTocTitle\"> TOC </div>",
				"<div class=\"hmlTocLev0\"><a href=\"#hmlContents0\">H-0</a></div>",
				"<div class=\"hmlTocLev1\"><a href=\"#hmlContents1\">1. H-1</a></div>",
				"<div class=\"hmlTocLev2\"><a href=\"#hmlContents2\">1.1. H-1.1</a></div>",
				"<div class=\"hmlTocLev1\"><a href=\"#label-2\">2. H-2</a></div>",
				"<div class=\"hmlTocLev2\"><a href=\"#label-2.1\">2.1. H-2.1</a></div>",
				"</div>",
				"<a name=\"hmlContents0\"><h1>H-0</h1></a>",
				"<a name=\"hmlContents1\"><h1>1. H-1</h1></a>",
				"<a name=\"hmlContents2\"><h2>1.1. H-1.1</h2></a>",
				"<a name=\"label-2\"><h1>2. H-2</h1></a>",
				"<a name=\"label-2.1\"><h2>2.1. H-2.1</h2></a>",
				"<h1>3. H-3</h1>",
				"<h2>3.1. H-3.1</h2>"				
				);
	
		objectUnderTest.filter( Text.EMPTY, input, Text.EMPTY, BlockType.TEXT );
		objectUnderTest.getTocReplacementFilter().filter(Text.EMPTY, input, Text.EMPTY, BlockType.TEXT );
		
		TestUtil.assertEquals( expected, input );
	}
}
