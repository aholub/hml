package com.holub.test;

import static org.junit.Assert.*;

import java.io.OutputStreamWriter;
import java.util.Deque;
import java.util.LinkedList;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.holub.hml.Configuration;
import com.holub.hml.Filter;
import com.holub.hml.Pass;
import com.holub.hml.Filter.BlockType;
import com.holub.text.ReportingStream;
import com.holub.text.Text;

public class PassTest
{
	private static final ReportingStream error  = new ReportingStream( new OutputStreamWriter(System.err) );
	private static final Configuration   config = new Configuration(error);

	@BeforeClass public static void setUpBeforeClass() throws Exception { }
	@AfterClass public static void tearDownAfterClass() throws Exception { } 
	@Before public void setUp() throws Exception { } 
	@After public void tearDown() throws Exception { }
	
	/** Doesn't modify the input at all, but asserts that the blocks
	 *  passed into consecutive {@link #addExpected(String, BlockType)}
	 *  calls are processed in the expected order as the expected
	 *  type.
	 *  
	 * @author allen
	 */
	class GenericFilter implements Filter
	{
		Deque<String>    expectedStrings = new LinkedList<String>();
		Deque<BlockType> expectedTypes   = new LinkedList<BlockType>();
		Text			 input			 = new Text();
		
		/** Call this method several times, one for each block (text or code)
		 *  to add to the input string.
		 * @param text
		 * @param type
		 */
		public void addExpected( String text, BlockType type )
		{
			input.append(text);
			expectedStrings.addLast( text );
			expectedTypes.addLast( type );
		}
		
		/** Return the input string build by concatenating
		 *  the addExpected arguments in sequence.
		 * @return
		 */
		public Text getInput(){ return input; }
		
		@Override public boolean isCodeBlockFilter(){ return true; }
		@Override public boolean isSnippetFilter()	{ return true; }
		@Override public boolean isTextFilter()		{ return true; }
		@Override public void filter( Text prefix, Text body, Text suffix, BlockType type)
		{	
			assertEquals( expectedStrings.removeFirst(), prefix.toString() + body.toString() + suffix.toString() );
			assertEquals( expectedTypes.removeFirst(),   type );
			body.prefix( prefix );
			body.append( suffix );
		}
	}
	
	@Test public void verifyPrefixAndSuffixIsolatedCorrectly()
	{
		testContextIsolation( new Text("`"), new Text("`") );
		testContextIsolation( new Text("<pre label=\"fred\">"), new Text("</pre>") );
		testContextIsolation( new Text("<listing file=\"c:/a/b.g\" label=\"fred\">"), new Text("</listing>") );
		testContextIsolation( Text.EMPTY, Text.EMPTY );
	}
		
	private void testContextIsolation( final Text inputPrefix, final Text inputSuffix )
	{
		final Text inputBody = new Text("\tf();");
		final Text success 	 = new Text("ok");
		Pass p = new Pass( config,
			new Filter()
			{
				@Override public boolean isCodeBlockFilter(){ return true; }
				@Override public boolean isSnippetFilter() 	{ return true; }
				@Override public boolean isTextFilter()		{ return true; }
				
				@Override public void filter(Text prefix, Text body, Text suffix, BlockType type)
				{
					if( prefix == Text.EMPTY )		assertEquals( BlockType.TEXT, type );
					else if( prefix.equals("`") )	assertEquals( BlockType.SNIPPET, type );
					else 							assertEquals( BlockType.CODE, type );
					assertEquals( inputPrefix,	prefix );
					assertEquals( inputBody, 	body );
					assertEquals( inputSuffix,	suffix );
					body.replace( success );
				}
			}
		);
		
		Text input  = new Text("", inputPrefix, inputBody, inputSuffix );
		p.process(input);
		assertEquals( success, input );
	}
	
	@Test
	public void test()
	{
		Text input = new Text("",
			"t0",
			"<pre>c0</pre>",
			"t1",
			"<listing>c1</listing>",
			"t2 `c2` t3"
			);
		
		Text originalInput = new Text(input);	// make a copy.
		
		Filter codeBlockFilter = new Filter.Default()
		{
			private int call = 0;
			@Override public boolean isCodeBlockFilter(){ return true; }
			
			@Override public void filter(Text prefix, Text body, Text suffix, BlockType type)
			{	
				switch( ++call )
				{
				case 1: 
						assertEquals( "<pre>",	prefix.toString() );
						assertEquals( "c0", 	body.toString() );
						assertEquals( "</pre>", suffix.toString() );
						assertEquals( type, BlockType.CODE );
						break;
				case 2:
						assertEquals( "<listing>",	prefix.toString() );
						assertEquals( "c1", 		body.toString() );
						assertEquals( "</listing>", suffix.toString() );
						assertEquals( type, BlockType.CODE );
						break;
						
				default:fail("Shouldn't be called 3 times");
						break;
				}
				body.prefix(prefix);
				body.append(suffix);
			}
			@Override public String toString(){ return "codeBlockFilter"; }
		};
		
		Filter snippetFilter = new Filter.Default()
		{
			private int call = 0;
			@Override public boolean isSnippetFilter()	{ return true; }
			
			@Override public void filter(Text prefix, Text body, Text suffix, BlockType type)
			{	
				switch( ++call )
				{
				case 1:	assertEquals( "`", prefix.toString() );
						assertEquals( "c2", body.toString() );
						assertEquals( "`", suffix.toString() );
						assertEquals( type, BlockType.SNIPPET );
						break;
						
				default:fail("Shouldn't be called 2 times");
						break;
				}
				body.prefix(prefix);
				body.append(suffix);
			}
			@Override public String toString(){ return "snippetFilter"; }
		};
		
		Filter textBlockFilter = new Filter.Default()
		{
			private int call = 0;
			@Override public boolean isTextFilter()		{ return true;	}
			
			@Override public void filter(Text prefix, Text body, Text suffix, BlockType type)
			{	
				assertEquals( BlockType.TEXT, type );
				assertEquals( Text.EMPTY, prefix );
				assertEquals( Text.EMPTY, suffix );
				
				String s = body.toString();
				switch( ++call )
				{
				case 1: assertEquals( "t0", s ); break;
				case 2: assertEquals( "t1", s ); break;
				case 3: assertEquals( "t2 ", s ); break;
				case 4: assertEquals( " t3", s ); break;
				}
			}
			
			@Override public String toString(){ return "textBlockFilter"; }
		};
		
		Pass p = new Pass( config, snippetFilter, codeBlockFilter, textBlockFilter );
		p.process(input);
		assertEquals( originalInput, input );	// make sure input wasn't modified.
	}
	
	private Text runPassUsingGenericFilter( GenericFilter f )
	{
		Pass p = new Pass(config, f);
		Text input = f.getInput();
		p.process( input );
		return input;
	}
	
	@Test
	public void codeOnly()
	{
		GenericFilter f = new GenericFilter();
		f.addExpected( "<pre>x</pre>", BlockType.CODE );
		
		Text expected = new Text( f.getInput() );
		assertEquals( expected, runPassUsingGenericFilter(f) );
	}
	
	@Test
	public void inlineCodeOnly()
	{
		GenericFilter f = new GenericFilter();
		f.addExpected( "`x`", BlockType.SNIPPET );
		
		Text expected = new Text( f.getInput() );
		assertEquals( expected, runPassUsingGenericFilter(f) );
	}
	
	@Test
	public void headTailAndOneSnippet()
	{
		GenericFilter f = new GenericFilter();
		f.addExpected( ".",   BlockType.TEXT );
		f.addExpected( "`x`", BlockType.SNIPPET );
		f.addExpected( ".",   BlockType.TEXT );
		
		Text expected = new Text( f.getInput() );
		assertEquals( expected, runPassUsingGenericFilter(f) );
	}
	
	@Test
	public void textOnly()
	{
		GenericFilter f = new GenericFilter();
		f.addExpected( "x", BlockType.TEXT );

		Text expected = new Text( f.getInput() );
		assertEquals( expected, runPassUsingGenericFilter(f) );
	}
	
	@Test
	public void listingInCodeBlockEtc()
	{
		GenericFilter f = new GenericFilter();
		f.addExpected( ".", BlockType.TEXT );
		f.addExpected( "`<listing>x()</listing>`", BlockType.SNIPPET );
		f.addExpected( ".", BlockType.TEXT );
		f.addExpected( "<pre>`x()`</pre>", BlockType.CODE );
		f.addExpected( ".", BlockType.TEXT );
		f.addExpected( "<pre><listing>x()</listing></pre>", BlockType.CODE );
		f.addExpected( ".", BlockType.TEXT );
		f.addExpected( "<listing><pre>x()</pre></listing>", BlockType.CODE );
		f.addExpected( ".\\'text\\'", BlockType.TEXT );
		f.addExpected( "`x\\`x`", BlockType.SNIPPET );

		Text expected = new Text( f.getInput() );
		assertEquals( expected, runPassUsingGenericFilter(f) );
	}
	
	@Test
	public void preInAPre()
	{
		GenericFilter f = new GenericFilter();
		f.addExpected( "<pre>.<pre>x</pre>.</pre>", BlockType.CODE );
		
		Text expected = new Text( f.getInput() );
		assertEquals( expected, runPassUsingGenericFilter(f) );
	}
	
	Filter codeBlockPassThroughFilter = new Filter()
	{
		@Override public boolean isCodeBlockFilter(){ return true; }
		@Override public boolean isSnippetFilter() 	{ return false; }
		@Override public boolean isTextFilter() 	{ return false; }
		
		@Override public void filter(Text prefix, Text body, Text suffix, BlockType type)
		{	body.replace( new Text("", prefix, body, suffix) );
		}
	};
	
	@Test public void implicitPreBlock() throws Exception
	{
		Pass p = new Pass( config, codeBlockPassThroughFilter );
		
		Text t;
		p.process( t = new Text(",code\n") );	 				TestUtil.assertEquals("<pre>\ncode\n</pre>", 			t.toString());
		p.process( t = new Text(",code\n,CODE\n") );			TestUtil.assertEquals("<pre>\ncode\nCODE\n</pre>",		t.toString());
		p.process( t = new Text(",\tcode\n") );					TestUtil.assertEquals("<pre>\ncode\n</pre>", 			t.toString());
		p.process( t = new Text(",\tcode\n,\tCODE\n") );		TestUtil.assertEquals("<pre>\ncode\nCODE\n</pre>",		t.toString());
		p.process( t = new Text(",\t\tcode\n") );				TestUtil.assertEquals("<pre>\n\tcode\n</pre>", 			t.toString());
		p.process( t = new Text(",\t\tcode\n,\t\tCODE\n") );	TestUtil.assertEquals("<pre>\n\tcode\n\tCODE\n</pre>",	t.toString());
	}

	@Test public void snippetInImplicitPreBlock() throws Exception
	{
		Pass p = new Pass( config, codeBlockPassThroughFilter );
		Text t;
		p.process( t = new Text(",\t{b `code`}\nx\n") );
		TestUtil.assertEquals("<pre>\n{b `code`}\n</pre>x\n", t.toString());
	}

	@Test public void multilineIndentedPreBlock() throws Exception
	{
		Pass p = new Pass( config, codeBlockPassThroughFilter );
		Text t;
		p.process( t = new Text("\t,\t0\n\t,\t\t1\n\t,\t2\n") );
		TestUtil.assertEquals(
				"<pre>\n" +
				"0\n" +
				"\t1\n" +
				"2\n" +
				"</pre>",
				 t.toString());
	}
}
