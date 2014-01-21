package com.holub.test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;

import org.junit.*;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.holub.hml.Hml;
import com.holub.text.Text;
import com.holub.util.IdGenerator;
import com.holub.util.IterableFile;
import com.holub.util.LocationsSupport;
import com.holub.util.Places;

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

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.apache.log4j.*", "javax.management.*", "javax.xml.parsers.*", "com.sun.org.apache.xerces.internal.jaxp.*", "ch.qos.logback.*", "org.slf4j.*"})
@PrepareForTest({Places.class, LocationsSupport.class})

public class IntegrationTests extends PowerMockito
{
	// This is the file processed by hmlDocs();
	private static final String TEST_INPUT = "/Users/allen/projects/src/Hml/doc/hmldoc.hml";
//	private static final String TEST_INPUT = "/Users/allen/projects/src/Hml/test-support/test.hml";
//	private static final String TEST_INPUT = "/Users/allen/text/exchange/onePager.html";
	
//	private static final ExtendedLogger log = ExtendedLogger.getLogger(IntegrationTests.class);
			
	private static File	javaFile;
	private static File	shortFile;
	
	@BeforeClass public static void setupSuite() throws IOException
	{
		javaFile = Places.TMP.temporaryFile("tmp-", ".java");
		new Text("\n",
			"/** My Class */",
			"class X",
			"{ ",
			"    public void top(){}",
			"}",
			""
		).export( javaFile );
		
		shortFile = Places.TMP.temporaryFile("tmp-", ".java");
		new Text("x\n").export( shortFile );
	}
	@AfterClass public static void teardownSuite()
	{
		javaFile.delete();			// harmless to delete a nonexistent file
		shortFile.delete();			// harmless to delete a nonexistent file
	}
	
	@Before public void setup() throws IllegalStateException, IOException
	{	IdGenerator.resetForTestingPurposes();
	}
	
	@After public void tearDown() throws IllegalStateException
	{
	}
	
	@Test public void nestedComments() throws IOException
	{
		Text input	  = new Text ( "<!= a =!>" );
		Text expected = new Text ( "" );
		runCoreExpansionsWithoutAddingHeadAndTail(input);
		TestUtil.assertEquals( expected, input );
		
		input		= new Text ( "x<!= a =!>y" );
		expected	= new Text ( "xy" );
		runCoreExpansionsWithoutAddingHeadAndTail(input);
		TestUtil.assertEquals( expected, input );
		
		input		= new Text ( "x<!= a <!= b =!> =!>y" );
		expected	= new Text ( "xy" );
		runCoreExpansionsWithoutAddingHeadAndTail(input);
		TestUtil.assertEquals( expected, input );
		
		input		= new Text ( "x<!= a <!= b <!= c <!= d =!> e =!> f =!> g =!>y" );
		expected	= new Text ( "xy" );
		runCoreExpansionsWithoutAddingHeadAndTail(input);
		TestUtil.assertEquals( expected, input );
		
		input		= new Text ( "x<!= a <!= b <!= c <!= d =!> e =!> f =!> g "
								+   " A <!= B <!= C <!= D =!> E =!> F =!> G "
								+ "=!>y" );
		expected	= new Text ( "xy" );
		runCoreExpansionsWithoutAddingHeadAndTail(input);
		TestUtil.assertEquals( expected, input );
		
		input		= new Text ( "x<!= a <!= b <!= c <!= d =!> e =!> f =!> g "
								+   " A <!= B <!= C <!= D =!> E =!> F =!> G "
								+ "=!>y" );
		
		expected	= new Text ( "xy" );
		runCoreExpansionsWithoutAddingHeadAndTail(input);
		TestUtil.assertEquals( expected, input );
	}
	
	@Test public void preInAPre() throws IOException
	{
		Text input = new Text( "<pre>.<pre>x</pre>.</pre>");
		Text expected = new Text("\n",
		"<div class=\"hmlPreGroup\">",
		"<div class=\"hmlCodeAnnotations\">",
		"<br>",
		"</div>",
		"<div class=\"hmlCode\">",
		"<pre class=\"hmlPre\">",
		".&lt;pre&gt;x&lt;/pre&gt;.",
		"</pre>",
		"</div>",
		"</div>"
		);
		
		runCoreExpansionsWithoutAddingHeadAndTail(input);
		TestUtil.assertEquals( expected, input );
	}
	
	@Test public void bigComment() throws IOException
	{
		Text input = new Text ("\n",
							"above above above above above above above above above above",
							"above above above above above above above above above above<!=",
							"gone gone gone gone gone gone gone gone gone gone gone gone",
							"gone gone gone gone gone gone gone gone gone gone gone gone=!>",
							"below below below below below below below below below below",
							"below below below below below below below below below below",
							""
						);
		Text expected = new Text ("\n",
							"above above above above above above above above above above",
							"above above above above above above above above above above",
							"below below below below below below below below below below",
							"below below below below below below below below below below",
							""
						);
		
		runCoreExpansionsWithoutAddingHeadAndTail(input);
		TestUtil.assertEquals( expected, input );
	}
	//----------------------------------------------------------------------
	@Test public void referenceInCodeSnippet() throws IOException
	{
		Text input		= new Text( "`{#Fred}`" );
		Text expected	= new Text( "<nobr><code>{#Fred}</code></nobr>" );
		
		runCoreExpansionsWithoutAddingHeadAndTail(input);
		TestUtil.assertEquals( expected, input );
	}
	//----------------------------------------------------------------------
	@Test public void twoIncludes() throws IOException
	{
		Text input = new Text("\n",
			"<include src='" +  shortFile.getAbsolutePath() + "' title='First Title'>",
			"<include src='" +  shortFile.getAbsolutePath() + "' title='Second Title'>",
			""
		);
		
		Text expected = new Text("\n",
			"<div class=\"hmlListingTitle\"><a name=\""+ shortFile.getName() +"\"><span class=\"hmlTitle\">Listing 1.</span> First Title</a></div>",
			"<div class=\"hmlListingGroup\">",
			"<div class=\"hmlCodeAnnotations\">",
			"1<br>",
			"</div>",
			"<div class=\"hmlCode\">",
			"<pre class=\"hmlPre\">",
			"x",
			"</pre>",
			"</div>",
			"</div>",
			"<div class=\"hmlListingTitle\"><a name=\""+ shortFile.getName() +"\"><span class=\"hmlTitle\">Listing 2.</span> Second Title</a></div>",
			"<div class=\"hmlListingGroup\">",
			"<div class=\"hmlCodeAnnotations\">",
			"2<br>",
			"</div>",
			"<div class=\"hmlCode\">",
			"<pre class=\"hmlPre\">",
			"x",
			"</pre>",
			"</div>",
			"</div>",
			""
		);		
		runCoreExpansionsWithoutAddingHeadAndTail(input);
		TestUtil.assertEquals( expected, input );
	}
	//----------------------------------------------------------------------
	@Test
	public void fullIncludePassesArgsThroughToPre() throws IOException
	{
		Text input = new Text("\n",
			"<include src='" +  javaFile.getAbsolutePath() + "' title='The Title' class=\"myClass\" style=\"padding-left:1em;\">",
			""
		);
	
		Text expected = new Text ("\n",
			"<div class=\"hmlListingTitle\"><a name=\"" + javaFile.getName() + "\"><span class=\"hmlTitle\">Listing 1.</span> The Title</a></div>",
			"<div class=\"hmlListingGroup\">",
			"<div class=\"hmlCodeAnnotations\">",
			"1<br>",
			"<a name=\"X\"></a>2<br>",
			"3<br>",
			"<a name=\"X.top\"></a>4<br>",
			"5<br>",
			"</div>",
			"<div class=\"hmlCode\">",
			"<pre style=\"padding-left:1em;\" class=\"myClass\">",
			"<span class=\"hmlComment\">/** My Class */</span>",
			"class X",
			"{ ",
			"    public void top(){}",
			"}",
			"</pre>",
			"</div>",
			"</div>",				
			""
			);
		
		runCoreExpansionsWithoutAddingHeadAndTail(input);
		TestUtil.assertEquals( expected, input );
	}
	//----------------------------------------------------------------------

	@Test
	public void listingTitleGeneration() throws IOException
	{
		Text input = new Text( "\n",
				"<listing file=\"/Users/allen/projects/hml/Listings1.java\">",	 // no title
				"x</listing>",
				"<listing file=\"/Users/allen/projects/hml/Listings1.java\" title=\"Title 1\">",
				"x</listing>",
				"{listing Listings1.java}.",
				"<listing file=\"/Users/allen/projects/hml/Listings2.java\" title=\"Title 2\" label=\"MyLabel\">",
				"x</listing>",
				"{listing MyLabel}.",
				"<listing file=\"/Users/allen/projects/hml/Listings3.java\" title=\"\">",
				"x</listing>",
				""
				);
		Text expected = new Text( "\n",
			"<div class=\"hmlListingGroup\">",
				"<div class=\"hmlCodeAnnotations\">",
				"1<br>",
				"</div>",
				"<div class=\"hmlCode\">",
				"<pre class=\"hmlPre\">",
				"x",
				"</pre>",
				"</div>",
				"</div>",
				"<div class=\"hmlListingTitle\"><a name=\"Listings1.java\"><span class=\"hmlTitle\">Listing 1.</span> Title 1</a></div>",
				"<div class=\"hmlListingGroup\">",
				"<div class=\"hmlCodeAnnotations\">",
				"2<br>",
				"</div>",
				"<div class=\"hmlCode\">",
				"<pre class=\"hmlPre\">",
				"x",
				"</pre>",
				"</div>",
				"</div>",
				"<a href=\"#Listings1.java\">Listing 1</a>.",
				"<div class=\"hmlListingTitle\"><a name=\"MyLabel\"><span class=\"hmlTitle\">Listing 2.</span> Title 2</a></div>",
				"<div class=\"hmlListingGroup\">",
				"<div class=\"hmlCodeAnnotations\">",
				"1<br>",
				"</div>",
				"<div class=\"hmlCode\">",
				"<pre class=\"hmlPre\">",
				"x",
				"</pre>",
				"</div>",
				"</div>",
				"<a href=\"#MyLabel\">Listing 2</a>.",
				"<div class=\"hmlListingTitle\"><a name=\"Listings3.java\"><span class=\"hmlTitle\">Listing 3.</span> <em>Listings3.java</em></a></div>",
				"<div class=\"hmlListingGroup\">",
				"<div class=\"hmlCodeAnnotations\">",
				"1<br>",
				"</div>",
				"<div class=\"hmlCode\">",
				"<pre class=\"hmlPre\">",
				"x",
				"</pre>",
				"</div>",
				"</div>",				
				""				
				);
		
		runCoreExpansionsWithoutAddingHeadAndTail( input );
		TestUtil.assertEquals( expected, input );
	}
	
	@Test
	public void blockContainingCodeSnippets() throws IOException
	{
		Text input = new Text("\n",
				"<block>",
				"`x`",
				"</block>"
				);
		
		Text expected = new Text("\n",
				"<blockquote class=\"hmlBlock\">",
				"<nobr><code>x</code></nobr><br>",
				"</blockquote>"
				);
		
		runCoreExpansionsWithoutAddingHeadAndTail( input );
		TestUtil.assertEquals( expected, input );
	}
	
	@Test
	public void listingInCodeBlockEtc() throws IOException
	{
		Text input = new Text( "\n",
			"`<listing>A</listing><pre>B</pre>`",
			"<pre>`C`</pre>",
			"<pre><listing>D</listing></pre>",
			"<listing><pre>E</pre></listing>",
			""
			);

		Text expected = new Text( "\n",
			"<nobr><code>&lt;listing&gt;A&lt;/listing&gt;&lt;pre&gt;B&lt;/pre&gt;</code></nobr>",
			"<div class=\"hmlPreGroup\">",
			"<div class=\"hmlCodeAnnotations\">",
			"<br>",
			"</div>",
			"<div class=\"hmlCode\">",
			"<pre class=\"hmlPre\">",
			"`C`",
			"</pre>",
			"</div>",
			"</div>",
			"<div class=\"hmlPreGroup\">",
			"<div class=\"hmlCodeAnnotations\">",
			"<br>",
			"</div>",
			"<div class=\"hmlCode\">",
			"<pre class=\"hmlPre\">",
			"&lt;listing&gt;D&lt;/listing&gt;",
			"</pre>",
			"</div>",
			"</div>",
			"<div class=\"hmlListingGroup\">",
			"<div class=\"hmlCodeAnnotations\">",
			"1<br>",
			"</div>",
			"<div class=\"hmlCode\">",
			"<pre class=\"hmlPre\">",
			"&lt;pre&gt;E&lt;/pre&gt;",
			"</pre>",
			"</div>",
			"</div>",
			""
			);		
		runCoreExpansionsWithoutAddingHeadAndTail( input );
		TestUtil.assertEquals( expected, input );
	}
	//----------------------------------------------------------------------
	@Test
	public void internalMacrosInCode() throws IOException
	{
		Text input 	  = new Text(	"w `{maxRows}` x `{maxRow}` y `{?(}` z `{?|}`" );
		Text expected =	new Text(	"w <nobr><code>{maxRows}</code></nobr> " +
									"x <nobr><code>{maxRow}</code></nobr> " +
									"y <nobr><code>{?(}</code></nobr> " +
									"z <nobr><code>{?|}</code></nobr>"
								);
		
		runCoreExpansionsWithoutAddingHeadAndTail( input );
		TestUtil.assertEquals( expected, input );
	}
	//----------------------------------------------------------------------
	@Test
	public void noExpansionInCodeBlock() throws IOException
	{
		Text input = new Text(
			"```A'''.{i x}.<listing>{i x}</listing>.B.{i x}.<pre>{i x}</pre>.C.{i x}.D.`<{i x}>`.``E''\n");
		
		Text expected = new Text( "\n",
			"&ldquo;A&rdquo;.<em>x</em>.<div class=\"hmlListingGroup\">",
			"<div class=\"hmlCodeAnnotations\">",
			"1<br>",
			"</div>",
			"<div class=\"hmlCode\">",
			"<pre class=\"hmlPre\">",
			"{i x}",
			"</pre>",
			"</div>",
			"</div>.B.<em>x</em>.<div class=\"hmlPreGroup\">",
			"<div class=\"hmlCodeAnnotations\">",
			"<br>",
			"</div>",
			"<div class=\"hmlCode\">",
			"<pre class=\"hmlPre\">",
			"{i x}",
			"</pre>",
			"</div>",
			"</div>.C.<em>x</em>.D.<nobr><code>&lt;{i&nbsp;x}&gt;</code></nobr>.&lsquo;E&rsquo;",				
			""
			);
		
		runCoreExpansionsWithoutAddingHeadAndTail( input );
		TestUtil.assertEquals( expected, input );
	}
	//----------------------------------------------------------------------
	@Test
	public void endNotes() throws IOException
	{
		Text input = new Text("\n",
			"This is the first sentence.<note prefix=\"\\[\" suffix=\"\\]\">Note 1.</note>",
			"",
			"This is the second sentence.<note mark=\"*\">Note 2.</note>",
			"It's in the second paragraph.",
			"",
			"This is the third sentence.<note style=\"font-weight:bold;\">Note 3.</note>",
			"It's in the third paragraph.",
			"",
			"Note 10<note mark=\"10\" label=\"ten\">Note 10.</note>.",
			"Note 11\n<note>Note 11.</note>",
			"Note with link\n<note>[http://www.holub.com]</note>",
			"{note ten}",
			"{note-number ten}",
			"{note-number ten TEN}",
			"<endNotes>",
			"Notes:",
			"</endNotes>"
			);
		
		Text expected = new Text ("\n",
			"This is the first sentence.<span class=\"hmlNote\">[<a name=\"hmlRef-1\" id=\"hmlRef-1\" href=\"#hmlNote1\">1</a>]</span>",
			"<p>",
			"This is the second sentence.<span class=\"hmlNote\"><a name=\"hmlRef-2\" id=\"hmlRef-2\" href=\"#hmlNote2\">*</a></span>",
			"It's in the second paragraph.",
			"<p>",
			"This is the third sentence.<span style=\"font-weight:bold;\" class=\"hmlNote\"><a name=\"hmlRef-3\" id=\"hmlRef-3\" href=\"#hmlNote3\">2</a></span>",
			"It's in the third paragraph.",
			"<p>",
			"Note 10<span class=\"hmlNote\"><a name=\"hmlRef-4\" id=\"hmlRef-4\" href=\"#hmlNote4\">10</a></span>.",
			"Note 11<span class=\"hmlNote\"><a name=\"hmlRef-5\" id=\"hmlRef-5\" href=\"#hmlNote5\">11</a></span>",
			"Note with link<span class=\"hmlNote\"><a name=\"hmlRef-6\" id=\"hmlRef-6\" href=\"#hmlNote6\">12</a></span>",
			"<a href=\"#hmlNote4\">Note 10</a>",
			"<a href=\"#hmlNote4\">10</a>",
			"<a href=\"#hmlNote4\">TEN</a>",
			"<div  class=\"hmlNotes\">",
			"",
			"Notes:",
			"<div class=\"hmlNoteGroup\" id=\"hmlNote-1\"><div class=\"hmlNoteRef\"><a name=\"hmlNote1\" href=\"#hmlRef-1\">1</a></div><div class=\"hmlNoteBody\" id=\"hmlNoteBody-1\">Note 1.</div></div>",
			"<div class=\"hmlNoteGroup\" id=\"hmlNote-2\"><div class=\"hmlNoteRef\"><a name=\"hmlNote2\" href=\"#hmlRef-2\">*</a></div><div class=\"hmlNoteBody\" id=\"hmlNoteBody-2\">Note 2.</div></div>",
			"<div class=\"hmlNoteGroup\" id=\"hmlNote-3\"><div class=\"hmlNoteRef\"><a name=\"hmlNote3\" href=\"#hmlRef-3\">2</a></div><div class=\"hmlNoteBody\" id=\"hmlNoteBody-3\">Note 3.</div></div>",
			"<div class=\"hmlNoteGroup\" id=\"hmlNote-4\"><div class=\"hmlNoteRef\"><a name=\"hmlNote4\" href=\"#hmlRef-4\">10</a></div><div class=\"hmlNoteBody\" id=\"hmlNoteBody-4\">Note 10.</div></div>",
			"<div class=\"hmlNoteGroup\" id=\"hmlNote-5\"><div class=\"hmlNoteRef\"><a name=\"hmlNote5\" href=\"#hmlRef-5\">11</a></div><div class=\"hmlNoteBody\" id=\"hmlNoteBody-5\">Note 11.</div></div>",
			"<div class=\"hmlNoteGroup\" id=\"hmlNote-6\"><div class=\"hmlNoteRef\"><a name=\"hmlNote6\" href=\"#hmlRef-6\">12</a></div><div class=\"hmlNoteBody\" id=\"hmlNoteBody-6\"><a target=\"_blank\" href=\"http://www.holub.com\">http://www.holub.com</a></div></div>",
			"</div>"		
);		
		
		runCoreExpansionsWithoutAddingHeadAndTail( input );
		TestUtil.assertEquals( expected, input );
	}
	
	//----------------------------------------------------------------------
	// This test just runs an external file (the main documentation by default)
	// through HML. It fails if there were any errors.
	//	
	@Test
	public void processExternalFileDiscardingOutput() throws IOException
	{
		File testFile = new File(TEST_INPUT);
		Text input    = new Text( testFile );
		
		Writer errors = new WriterSpy( new StringWriter() );
		
		Hml hml = new Hml( null, errors );	// expand() doesn't use output stream. Force an error by passing null.
		
		int errorCount = hml.expand( input );
		
		assertEquals( "Found " + errorCount + " errors in " + TEST_INPUT + ". ", 0,  errorCount );
	}
	
	/** This class exists only to make it easier to set a breakpoint when an
	 *  error is encountered. It's easier to just declare a class than to set up a mock.
	 *  
	 * @author allen
	 */
	private static class WriterSpy extends Writer
	{
		private void breakpoint()
		{
			return;		// set your breakpoint here.
		}
		
		private final Writer spyOn;
		public WriterSpy( Writer spyOn ){ this.spyOn = spyOn; }
		
		@Override public void close() throws IOException { breakpoint(); spyOn.close(); }
		@Override public void flush() throws IOException { breakpoint(); spyOn.flush(); }
		@Override public void write(char[] arg0, int arg1, int arg2) throws IOException
		{	breakpoint(); spyOn.write(arg0, arg1, arg2);
		}
	};
	
	//----------------------------------------------------------------------
	// Runs main as if it had been invoked from the command line. No mocking is
	// done, so this is a close to a full execution from the command line as
	// one can get. Output is put in $TMP/testOut<someNumber>.html . Any
	// Any existing files named $TMP/testOut*.html are deleted before
	// the test runs, so there should be only one testOut*.html in the
	// TMP directory after the test executes.

	@Test public void testMainWithExternalFile() throws Exception
	{
		// Delete any previous versions
		File[] previousOutput = Places.TMP.directory().listFiles(
				new FilenameFilter()
				{	@Override public boolean accept(File dir, String name)
					{	return name.matches("testOut.*\\.html");
					}
				}
			);
		for( File existing : previousOutput )
			existing.delete();
		
		File dest = Places.TMP.temporaryFile("testOut",  ".html");
		Hml processor = new Hml( new OutputStreamWriter(System.out),
								 new OutputStreamWriter(System.err) );
		
		Hml.doMain( processor, new String[]{ "--out", dest.getAbsolutePath(), TEST_INPUT } );
		return;
	}
	
	@Test public void testMainWithTwoOutputFiles() throws Exception
	{
		File inFile1  = IterableFile.createTempFile("in1.", ".tmp");
		File inFile2  = IterableFile.createTempFile("in2.", ".tmp");
		File outFile1 = IterableFile.createTempFile("out1.", ".tmp");
		File outFile2 = IterableFile.createTempFile("out2.", ".tmp");
		
		String file1contents = "1\n";
		String file2contents = "2\n";
		
		new Text(file1contents).export(inFile1);
		new Text(file2contents).export(inFile2);
		
		Hml processor = new Hml( new OutputStreamWriter(System.out),
								 new OutputStreamWriter(System.err) );
		Hml.doMain( processor,
			new String[]{
				"--out",
				outFile1.getAbsolutePath(),
				inFile1.getAbsolutePath(),
				"--out",
				outFile2.getAbsolutePath(),
				inFile2.getAbsolutePath() 	});
		
		Text head = new Text( Places.CONFIG.file("hml.head") );
		Text tail = new Text( Places.CONFIG.file("hml.tail") );
		
		Text expected1 = new Text( null, head, file1contents, tail );
		Text expected2 = new Text( null, head, file2contents, tail );
		
		Text actual1 = new Text(outFile1);
		Text actual2 = new Text(outFile2);
		
		assertEquals( expected1, actual1 );
		assertEquals( expected2, actual2 );
		
		inFile1.delete();
		inFile2.delete();
		outFile1.delete();
		outFile2.delete();
	}
	//----------------------------------------------------------------------
	@Test public void testThatMainPrintsErrors() throws Exception
	{
		StringWriter errors = new StringWriter();
		
		Hml processor = new Hml( new OutputStreamWriter(System.out), errors );
								 
		@SuppressWarnings("unused")
		Text actual = testMain( processor, new Text("{section x}\n") );
		
		String errorText = errors.toString();
		assertEquals(
			"ERROR: No <hN label=\"x\"> to match {section x} (or {ref...}). Missing title= in <include>?\n",
			errorText );
	}
	
	@Test public void testMainWithSimpleContentAndStandardHeaders() throws Exception
	{
		Hml processor = new Hml( new OutputStreamWriter(System.out),
								 new OutputStreamWriter(System.err) );
		
		Text actual = testMain( processor, new Text("{i content}\n") );
	 
		Text expected = new Text( Places.CONFIG.file("hml.head"));
		expected.append( "<em>content</em>\n" );
		expected.append( Places.CONFIG.file("hml.tail") );
		
		TestUtil.assertEquals(expected,actual);
	}
	
	public Text testMain( Hml processor, Text content ) throws Exception
	{
		Places.TMP.prepareForWrite();
		File source = Places.TMP.temporaryFile("mainTestIn",  ".hml");
		File dest   = Places.TMP.temporaryFile("mainTestOut", ".html");
		
		source.deleteOnExit();
		dest.deleteOnExit();
		
		// Very simple, one-line input file for testing.
		content.write( source ); 
		
		Hml.doMain( processor, new String[]{ "-o", dest.getAbsolutePath(), source.getAbsolutePath() } );
		
		return new Text(dest);
	}
	
	@Test public void webProcessing( ) throws Exception
	{
		String		 input  = "content";
		StringWriter output = new StringWriter();
		StringWriter errors = new StringWriter();
		
		int errorCount = Hml.processInWebContext( input, output, errors );
	 
		Text expected = new Text( Places.CONFIG.file("hml.head"));
		expected.append( input );
		expected.append( Places.CONFIG.file("hml.tail") );
		
		assertEquals( "unexpected errors", 0, errorCount );
		assertEquals( "unexpected errors", 0, errors.toString().length() );
		TestUtil.assertEquals( expected.toString(), output.toString() );
	}
	
	@Test public void webProcessingWithErrors( ) throws Exception
	{
		String		 input  = "`content";
		StringWriter output = new StringWriter();
		StringWriter errors = new StringWriter();
		
		int errorCount = Hml.processInWebContext( input, output, errors );
	 
		errors.close();
		Text expected = new Text( Places.CONFIG.file("hml.head"));
		expected.append( input );
		expected.append( Places.CONFIG.file("hml.tail") );
		
		assertEquals	( "unexpected errors", 1, errorCount );
		assertNotEquals	( "unexpected errors", 0, errors.toString().length() );
		TestUtil.assertEquals( expected.toString(), output.toString() );
	}
	//----------------------------------------------------------------------
	@Test public void pullConfigFilesFromInternalClassPath() throws Exception
	{
		// Force the LOCATIONS enum to go to fall back to the
		// class path by eliminating other alternatives:
		spy( System.class );
		when(System.getProperty("CONFIG")).thenReturn(null);
		when(System.getenv     ("CONFIG")).thenReturn(null);
		
		testMainWithSimpleContentAndStandardHeaders();
	}
	//----------------------------------------------------------------------
	@Test 
	public void addToHead() throws Exception
	{
		File source = Places.TMP.temporaryFile("mainTest", "tmp");
		File dest   = Places.TMP.temporaryFile("mainTest", "out");
		
		source.deleteOnExit();
		dest.deleteOnExit();
		
		String titleElement = "<title>My Title</title>";
		
		new Text("\n",
				"<head>",
				titleElement,
				"</head>",
				"content",
				""
		).write( source );
		
		Text expected = new Text( Places.CONFIG.file("hml.head"));
		expected.replaceAll("</head>", "\n" + titleElement + "\n</head>" );
		expected.appendln("\ncontent");
		expected.append( Places.CONFIG.file("hml.tail") );
		
		Hml processor = new Hml( new OutputStreamWriter(System.out),
								 new OutputStreamWriter(System.err) );
		
		Hml.doMain( processor, new String[]{ "--out", dest.getAbsolutePath(), source.getAbsolutePath() } );
		Text actual = new Text( dest );
		
		TestUtil.printDiff(expected, actual);
		assertEquals(expected,actual);
	}
	//======================================================================
	private int runCoreExpansionsWithoutAddingHeadAndTail( Text input ) throws IOException
	{
		Writer errors = new StringWriter();
		
		Hml hml = new Hml( null, errors );	// expand() doesn't use output stream. Force an error by passing null.
		
		int errorCount = hml.expand( input );
		if( errorCount > 0 )
			System.out.println( errors.toString() );
		
		return errorCount;
	}
}
