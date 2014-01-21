/**
 * 
 */
package com.holub.test;

import static org.junit.Assert.*;

import java.io.OutputStreamWriter;

import org.junit.*;

import com.holub.hml.Configuration;
import com.holub.hml.NoteSet;
import com.holub.hml.Pass;
import com.holub.hml.Titles;
import com.holub.text.ReportingStream;
import com.holub.text.Text;

/**
 * @author Allen Holub
 * 
 * <div style='font-size:8pt; margin-top:.25in;'>
 * &copy;2013 <!--copyright 2013--> Allen I Holub. All rights reserved.
 * This code is licensed under a variant on the BSD license. View
 * the complete text at <a href="http://holub.com/license.html">
 * http://www.holub.com/license.html</a>.
 * </div>
 */
public class NumbersTest
{
	private static final ReportingStream error  = new ReportingStream( new OutputStreamWriter(System.err) );
	private static final Configuration   config = new Configuration(error);
	private static Titles titleFilter;
	
	NoteSet endNotes = new NoteSet(null);
	
	private void runTitleAndReferencePass( Text input )
	{	new Pass(config, titleFilter 							  ).process(input);
		new Pass(config, titleFilter.getReferenceExpander(endNotes)).process(input);
	}
	
	private void runTitlePass(  Text input )
	{	new Pass(config, titleFilter ).process(input);
	}
	
	@Before public void setup()
	{	titleFilter = new Titles(config);
	}
	
	@Test public void OneLabelAndReference()
	{
		Text input	  = new Text("\n",
								"<h2 label=\"label-2.1\">H-2.1</h2>" ,
								"{section label-2.1}" );
		Text expected = new Text("\n",
								"<a name=\"label-2.1\"><h2>0.1. H-2.1</h2></a>",
								"<a href=\"#label-2.1\">Section 0.1</a>"
								);		
		runTitleAndReferencePass( input );
		TestUtil.assertEquals( expected, input );
	}
	
	@Test public void adjustedHeadings()
	{
		Text input = new Text("\n",
				"<h1>Level-1 Heading</h1>",
				"<h2>Level-2 Heading</h2>",
				"<h1>Level-1 Heading</h1>",
				"<h2>Level-2 Heading</h2>",
				"",
				"<h1 chapter=\"5\">Level-1 Heading</h1>",
				"<h2>Level-2 Heading</h2>",
				"<h1>Level-1 Heading</h1>",
				"<h2>Level-2 Heading</h2>",
				"",
				"<h1 chapter=\"A\">Level-1 Heading</h1>",
				"<h2>Level-2 Heading</h2>",
				"<h1>Level-1 Heading</h1>",
				"<h2>Level-2 Heading</h2>",
				"",
				"<h1 chapter=\"Fred\">Level-1 Heading</h1>",
				"<h2>Level-2 Heading</h2>",
				"<h1>Level-1 Heading</h1>",
				"<h2>Level-2 Heading</h2>",
				""
				);

		Text expected = new Text("\n",
			"<a name=\"hmlContents0\"><h1>1. Level-1 Heading</h1></a>",
			"<a name=\"hmlContents1\"><h2>1.1. Level-2 Heading</h2></a>",
			"<a name=\"hmlContents2\"><h1>2. Level-1 Heading</h1></a>",
			"<a name=\"hmlContents3\"><h2>2.1. Level-2 Heading</h2></a>",
			"",
			"<a name=\"hmlContents4\"><h1>5. Level-1 Heading</h1></a>",
			"<a name=\"hmlContents5\"><h2>5.1. Level-2 Heading</h2></a>",
			"<a name=\"hmlContents6\"><h1>6. Level-1 Heading</h1></a>",
			"<a name=\"hmlContents7\"><h2>6.1. Level-2 Heading</h2></a>",
			"",
			"<a name=\"hmlContents8\"><h1>A. Level-1 Heading</h1></a>",
			"<a name=\"hmlContents9\"><h2>A.1. Level-2 Heading</h2></a>",
			"<a name=\"hmlContents10\"><h1>B. Level-1 Heading</h1></a>",
			"<a name=\"hmlContents11\"><h2>B.1. Level-2 Heading</h2></a>",
			"",
			"<a name=\"hmlContents12\"><h1>Fred. Level-1 Heading</h1></a>",
			"<a name=\"hmlContents13\"><h2>Fred.1. Level-2 Heading</h2></a>",
			"<a name=\"hmlContents14\"><h1>D. Level-1 Heading</h1></a>",
			"<a name=\"hmlContents15\"><h2>D.1. Level-2 Heading</h2></a>",
			""
			);		
		runTitlePass( input );
		TestUtil.assertEquals( expected, input );
	}
	
	@Test
	public void h0headings()
	{
		Text input = new Text("\n",
				"<h0 label='h1'>Level-1 Heading</h1>",
				"<h2 label='h2'>Level-2 Heading</h2>",
				""
				);
		Text expected = new Text("\n",
				"<a name=\"h1\"><h1>Level-1 Heading</h1></a>",
				"<a name=\"h2\"><h2>Level-2 Heading</h2></a>",
				""
				);
		
		runTitlePass(input);
		assertEquals( expected, input );
	}
	
	@Test
	public void headings()
	{
		Text input = new Text("\n",
			"<h1 label=\"h1\">Level-1 Heading</h1>",
			"<h2 label=\"h2\">Level-2 Heading</h2>",
			"{section h1} {section-number h1}",
			"{section h2} {section-number h2}"
			);		
				
		Text expected = new Text("\n",
			"<a name=\"h1\"><h1>1. Level-1 Heading</h1></a>",
			"<a name=\"h2\"><h2>1.1. Level-2 Heading</h2></a>",
			"<a href=\"#h1\">Section 1</a> <a href=\"#h1\">1</a>",
			"<a href=\"#h2\">Section 1.1</a> <a href=\"#h2\">1.1</a>"
			);

		runTitleAndReferencePass( input );
		TestUtil.assertEquals( expected, input );
	}
	
	// TODO: Whey is there a period in the generated title? Shouldn't be there!
	@Test
	public void listingTitles()
	{
		Text input = new Text("\n",
				"<listing-title label='l1'>X</listing-title>",
				"{listing l1} {listing-number l1}",
				""
				);
		Text expected = new Text("\n",
				"<div class=\"hmlListingTitle\"><a name=\"l1\"><span class=\"hmlTitle\">Listing 1.</span> X</a></div>",
				"<a href=\"#l1\">Listing 1</a> <a href=\"#l1\">1</a>",
				""				
		);
		runTitleAndReferencePass( input );
		TestUtil.assertEquals( expected, input );
	}
	
	@Test
	public void allTitles()
	{
		Text input = new Text("\n",
				"<listing-title label='l1'>Listing 1</listing-title>",
				"<listing-title label='l2'>Listing 2</listing-title>",
				"<table-title	label='t1'>Table 1</table-title>",
				"<figure-title	label='f1'>Figure 1</figure-title>",
				"",
				"{listing l1} {listing-number l1}",
				"{listing l2} {listing-number l2}",
				"{table t1}   {table-number t1}",
				"{figure f1}  {figure-number f1}",
				""
		);
		Text expected = new Text("\n",
				"<div class=\"hmlListingTitle\"><a name=\"l1\"><span class=\"hmlTitle\">Listing 1.</span> Listing 1</a></div>",
				"<div class=\"hmlListingTitle\"><a name=\"l2\"><span class=\"hmlTitle\">Listing 2.</span> Listing 2</a></div>",
				"<div class=\"hmlTableTitle\"><a name=\"t1\"><span class=\"hmlTitle\">Table 1.</span> Table 1</a></div>",
				"<div class=\"hmlFigureTitle\"><a name=\"f1\"><span class=\"hmlTitle\">Figure 1.</span> Figure 1</a></div>",
				"",
				"<a href=\"#l1\">Listing 1</a> <a href=\"#l1\">1</a>",
				"<a href=\"#l2\">Listing 2</a> <a href=\"#l2\">2</a>",
				"<a href=\"#t1\">Table 1</a>   <a href=\"#t1\">1</a>",
				"<a href=\"#f1\">Figure 1</a>  <a href=\"#f1\">1</a>",
				""				
		);
		runTitleAndReferencePass( input );
		TestUtil.assertEquals( expected, input );
	}

	@Test
	public void referencesWithVisibleText()
	{
		Text input = new Text("\n",
				"<listing-title label='l1'>Listing 1</listing-title>",
				"",
				"{listing l1 visible text}",
				""
		);
		Text expected = new Text("\n",
				"<div class=\"hmlListingTitle\"><a name=\"l1\"><span class=\"hmlTitle\">Listing 1.</span> Listing 1</a></div>",
				"",
				"<a href=\"#l1\">visible text</a>",
				""				
		);
		
		runTitleAndReferencePass( input );
		TestUtil.assertEquals( expected, input );
	}
}
