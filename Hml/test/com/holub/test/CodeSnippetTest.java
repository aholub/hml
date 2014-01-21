package com.holub.test;

import static org.junit.Assert.*;

import java.io.OutputStreamWriter;

import org.junit.Test;

import com.holub.hml.CodeSnippets;
import com.holub.hml.Configuration;
import com.holub.hml.Filter;
import com.holub.hml.Pass;
import com.holub.text.ReportingStream;
import com.holub.text.Text;

public class CodeSnippetTest
{
	private static final Filter codeSnippetFilter = new CodeSnippets( null );
	private static final ReportingStream error  = new ReportingStream( new OutputStreamWriter(System.err) );
	private static final Configuration   config = new Configuration(error);
	
	@Test
	public void simpleSnippet()
	{
		Text input    = new Text("`x`");
		Text expected = new Text("<nobr><code>x</code></nobr>");
		Pass processor = new Pass( config, codeSnippetFilter );
		assertTrue( processor.process( input ) );
		TestUtil.assertEquals(expected, input);
	}

	@Test
	public void mapping()
	{
		Text input = new Text("",
							"`",
							" !\"#$%&'()*+,-./",
							"0123456789",
							":;<=>?@",
							"ABCDEFGHIJKLMNOPQRSTUVWXYZ" ,
							"[\\]^_\\`",
							"abcdefghijklmnopqrstuvwxyz" ,
							"{|}~",
							"`"
							);
			
		Text expected = new Text(""
							, "<nobr><code>"
							, "&nbsp;&#33;&quot;&#35;&#36;&#37;&amp;&apos;&#40;&#41;&#42;&#43;&#44;&#45;&#46;&#47;"
							, "0123456789"
							, "&#58;&#59;&lt;&#61;&gt;&#63;&#64;"
							, "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
							, "&#91;&#92;&#93;&#94;&#95;&#96;"
							, "abcdefghijklmnopqrstuvwxyz"
							, "&#123;&#124;&#125;&#126;"
							, "</code></nobr>"
							);
		Pass processor = new Pass( config, codeSnippetFilter );
		assertTrue( processor.process( input ) );
		TestUtil.assertEquals(expected, input);
	}

	@Test
	public void backtickInCodeSnippet()
	{
		Text input    = new Text("`x(\"\\`\")`");
		Text expected = new Text(
				"<nobr><code>x&#40;&quot;&#96;&quot;&#41;</code></nobr>");		
		Pass processor = new Pass( config, codeSnippetFilter );
		assertTrue( processor.process( input ) );
		TestUtil.assertEquals(expected, input);
	}
	
	@Test
	public void tabExpansion()
	{
		Text input    = new Text("\n", "`   X   Y`",
									   "`\tX\tY`" );
		
		Text expected = new Text("\n", "<nobr><code>&nbsp;&nbsp;&nbsp;X&nbsp;&nbsp;&nbsp;Y</code></nobr>",
									   "<nobr><code>&nbsp;&nbsp;&nbsp;X&nbsp;&nbsp;&nbsp;Y</code></nobr>" );
				
		Pass processor = new Pass( config, codeSnippetFilter );
		assertTrue( processor.process( input ) );
		TestUtil.assertEquals(expected, input);
	}
}
