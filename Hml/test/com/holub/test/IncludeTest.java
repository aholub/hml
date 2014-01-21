/**
 * 
 */
package com.holub.test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import com.holub.hml.Configuration;
import com.holub.hml.Include;
import com.holub.hml.Filter.BlockType;
import com.holub.text.ReportingStream;
import com.holub.text.Text;
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
public class IncludeTest
{
	private static	File outer;
	private static	File middle;
	private static	File inner;
	private static	File javaFile;
	private static  Include objectUnderTest;
	
	private static final ReportingStream error = new ReportingStream( new OutputStreamWriter(System.err) );
	private static final Configuration   config = new Configuration(error);
	
	@BeforeClass static public void setupSuite() throws IllegalStateException, IOException
	{
		objectUnderTest = new Include(config);
		
		outer 	  = Places.TMP.temporaryFile("IncludeTest-o-",".tmp");
		middle    = Places.TMP.temporaryFile("IncludeTest-m-",".tmp");
		inner 	  = Places.TMP.temporaryFile("IncludeTest-i-",".tmp");
		javaFile  = Places.TMP.temporaryFile("IncludeTest-j-",".tmp.java");
	}
	
	@After public void tearDown() throws IllegalStateException, IOException
	{
		outer.delete();			// harmless to delete a nonexistent file
		middle.delete();
		inner.delete();
		javaFile.delete();
	}
	
	@Test
	public void simpleImport() throws IOException
	{
		new Text("hello").export(middle);
		
		Text input    = new Text().appendln("<import src='"+ middle.getAbsolutePath() +"'>");
		
		objectUnderTest.filter(Text.EMPTY, input, Text.EMPTY, BlockType.TEXT );
		assertEquals( "hello", input.trim().toString() );	// junit requires the types to be equal
	}
	
	@Test
	public void simpleInclude() throws IOException
	{
		new Text("hello\n").export(middle);
		
		Text input  = new Text( "\n<include src='"+ middle.getAbsolutePath() +"'>\n" );
		
		Text expected = new Text("\n",
			"",
			"<listing file=\""+ middle.getAbsoluteFile() + "\">",
			"hello",
			"</listing>",
			""
		);
		
		objectUnderTest.filter(Text.EMPTY, input, Text.EMPTY, BlockType.TEXT );
		
		TestUtil.printDiff(expected, input);
		assertEquals(expected, input);
	}
	
	@Test
	public void multilineTag() throws IOException
	{
		new Text("hello\n").export(middle);
		
		Text input = new Text().appendln("<import\nsrc = '"+ middle.getAbsolutePath() +"'\n>");
		Text expected = new Text("hello\n");
		objectUnderTest.filter(Text.EMPTY, input, Text.EMPTY, BlockType.TEXT );
		
		assertEquals(expected, input);	// junit requires the types to be equal
	}
	
	@Test
	public void simpleIncludeFromURL() throws IOException
	{
		new Text("hello\n").export(middle);
		
		Text input    = new Text("\n",
							"",
							"<include href='file:" +middle.getAbsolutePath()+ "'>",
							"");
		
		Text expected = new Text("\n",
							"",
							"<listing file=\""+ "file:" + middle.getAbsolutePath() +"\">",
							"hello",
							"</listing>",
							""
							);
		
		objectUnderTest.filter(Text.EMPTY, input, Text.EMPTY, BlockType.TEXT );
		assertEquals( expected, input );
	}
	
	@Test public void extractOneMethodWithFromTo() throws IOException
	{
		new Text( "\n",
			"// stuff",
			"public void g()",
			"{	//...",
			"}",
			"public void f()",
			"{	//...",
			"}",
				"// more stuff",
			""
		).export(javaFile);
		
		Text input = new Text("",
			"<include src='",
			javaFile.getAbsolutePath(),
			"' " ,
			"from='public.*f()' ",
			"to='^\\s*}$' " ,
			">",
			"");
		
		Text expected = new Text( "\n",
			"<listing file=\"" + javaFile.getAbsolutePath() + "\">",
			"public void f()",
			"{	//...",
			"}",
			"</listing>",
			""
		).export(javaFile); 
		
		objectUnderTest.filter(Text.EMPTY, input, Text.EMPTY, BlockType.TEXT );
		assertEquals( expected, input );
	}
	
	@Test
	public void nestedImports() throws IOException
	{
		new Text("hello").export(middle);
		
		Text outerText  = new Text().appendln("<import src='"+ middle.getAbsolutePath() +"'>");
						  new Text().appendln("<import src='"+ inner .getAbsolutePath() +"'>").export(middle);
						  new Text("hello").export( inner );
		
		objectUnderTest.filter(Text.EMPTY, outerText, Text.EMPTY, BlockType.TEXT );
		assertEquals( "hello", outerText.trim().toString() );	// junit requires the types to be equal
	}
	
	@SuppressWarnings("unused")
	@Test
	public void fromToWithoutMarks() throws IOException
	{
		Text inputText    = new Text().appendln("<import src='"+ middle.getAbsolutePath() +"' from='--\\(' to='\\)--' remove-mark='true'>");
		Text middleText   =	new Text().appendln("--(\n world\n)--").export(middle);
		
		objectUnderTest.filter( Text.EMPTY, inputText, Text.EMPTY, BlockType.TEXT);
		assertEquals( "world", inputText.trim().toString() );	// junit requires the types to be equal
	}
	
	@SuppressWarnings("unused")
	@Test
	public void fromToWithMarks() throws IOException
	{
		Text inputText    = new Text().appendln("<include src='"+ middle.getAbsolutePath() +"' from='--\\(' to='\\)--'>");
		Text middleText   =	new Text().appendln("ignored \n--(\n world\n)--\n ignored").export(middle);
		Text expected     = new Text("\n",
									"<listing file=\""+ middle.getAbsolutePath() + "\">",
									"--(",
									" world",
									")--",
									"</listing>",
									""
									);
		
		objectUnderTest.filter(Text.EMPTY, inputText, Text.EMPTY, BlockType.TEXT);
		assertEquals( expected, inputText );
	}
	
	@Test
	public void javaFileImport() throws IOException
	{
		new Text("class X{}\n").export(javaFile);
		
		Text input = new Text().appendln("<import src='"+ javaFile.getAbsolutePath() +"'>");
		objectUnderTest.filter(Text.EMPTY, input, Text.EMPTY, BlockType.TEXT );
		assertEquals( "class X{}", input.trim().toString() );	// junit requires the types to be equal
	}
	
	@Test
	public void javaFileInclude() throws IOException
	{
		new Text("class X{}\n").export(javaFile);
		
		Text input    = new Text().appendln("<include src='"+ javaFile.getAbsolutePath() +"'>");
		objectUnderTest.filter(Text.EMPTY, input, Text.EMPTY, BlockType.TEXT );
		assertEquals(	"<listing file=\"" + javaFile.getAbsoluteFile() + "\">\nclass X{}\n</listing>",
						input.trim().toString()
					);
	}
	
	@Test
	public void javaFileIncludeNoLineNumbers() throws IOException
	{
		new Text("class X{}\n").export(javaFile);
		
		Text input  = new Text("\n",
						"",
						"<include src='"+ javaFile.getAbsolutePath() +"' line-numbers='false'>\n"
		);
		Text expected = new Text("\n",
						"",
						"<pre file=\""+ javaFile.getAbsolutePath() +"\">",
						"class X{}",
						"</pre>",
						""
		);
		objectUnderTest.filter(Text.EMPTY, input, Text.EMPTY, BlockType.TEXT );
		assertEquals( expected, input );
	}
	
	@Test
	public void javaFileIncludeNoLineNumbers2() throws IOException
	{
		new Text("class X{}\n").export(javaFile);
		
		Text input  = new Text().appendln("<include src='"+ javaFile.getAbsolutePath() +"' numbers='false'>");
		Text expected = new Text("\n",
						"<pre file=\""+ javaFile.getAbsolutePath() +"\">",
						"class X{}",
						"</pre>",
						""
		);
		objectUnderTest.filter(Text.EMPTY, input, Text.EMPTY, BlockType.TEXT );
		assertEquals( expected, input );
	}
}
