package com.holub.test;
import static org.junit.Assert.*;

import java.io.*;

import org.junit.Ignore;
import org.junit.Test;

import com.holub.hml.Configuration;
import com.holub.hml.Filter;
import com.holub.hml.Filter.BlockType;
import com.holub.hml.Macro;
import com.holub.hml.Pass;
import com.holub.text.ReportingStream;
import com.holub.text.Text;
import com.holub.util.ExtendedLogger;

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

public class MacroTest
{
	static ExtendedLogger log = ExtendedLogger.getLogger(MacroTest.class);
	
	private ReportingStream error = new ReportingStream( new OutputStreamWriter(System.err) );
	private Configuration   config = new Configuration(error);
	
	private Macro  macroManager = new Macro(config);
	private Filter textFilter   = macroManager.getTextFilter();
	
	/** The {@link Filter#filter(Text, BlockType)} method takes a Text argument,
	 * which it modifies (it returns void). That's convenient for real processing,
	 * but is inconvenient for testing. This method takes an input string, runs
	 * it through the macro filter, and returns the modified version. This way, you
	 * can do something like: {@code assertEquals("<em>i</em>",filter("{i x}"));}.
	 */
	private String filterText( String in )
	{
		Text t = new Text(in);
		textFilter.filter(Text.EMPTY, t, Text.EMPTY, BlockType.TEXT);
		return t.toString();
	}
	
	@Test public void checkLogLevels()
	{
		log.trace("TRACE is enabled");
		log.debug("DEBUG is enabled");
		log.info("INFO is enabled");
		log.warn("WARN is enabled");
		log.error("ERROR is enabled");
		log.fatal("FATAL is enabled");
	}
	
	/** Test all of the macros defined in hml.macros that aren't covered by other tests
	 *  
	 * @throws IOException
	 */
	@Test public void testAllTextBlockMacros() throws IOException
	{
		String filtered = filterText("x[!]y");
	    TestUtil.assertEquals( "xy", filtered);
	    
		filtered = filterText
				    ( "|a|b|c|\n"
					+ "|d|e| |\n"
					+ "|g| |i|\n\n"
			        );
		TestUtil.assertEquals
					( "<table class=\"hmlTable\">\n"
					+ "<tr><td valign=\"top\">a</td><td valign=\"top\">b</td><td valign=\"top\">c\n"
					+ "</td></tr>\n"
					+ "<tr><td valign=\"top\">d</td><td valign=\"top\">e</td><td valign=\"top\">&nbsp; \n"
					+ "</td></tr>\n"
					+ "<tr><td valign=\"top\">g</td><td valign=\"top\">&nbsp;</td><td valign=\"top\">i\n"
					+ "</td></tr>\n"
					+ "</table>\n\n"
					, filtered
					);
					
		filtered =	filterText
				    (
				       "|	A		| B			| C\n"
			        +  "|- 3\n"
			        +  "|	first	| 	   		| third\n"
			        +  "|	doo		| wha|\n"
			        +  "|	hickory\n"
			        +  "	| dickory\n"
			        +  "	| dock\n"
			        +  " ery|\n"
			        +  "\n\n"
			        +  "Follows\n"
			        );
		TestUtil.assertEquals
					(
					  "<table class=\"hmlTable\">\n"
					+ "<tr><td valign=\"top\">	A</td><td valign=\"top\"> B</td><td valign=\"top\"> C\n"
					+ "</td></tr>\n"
					+ "<tr><td class=\"hmlTableLine\" colspan=\"3\">&nbsp;</td></tr>\n"
					+ "<tr><td valign=\"top\">	first</td><td valign=\"top\">&nbsp;</td><td valign=\"top\"> third\n"
					+ "</td></tr>\n"
					+ "<tr><td valign=\"top\">	doo</td><td valign=\"top\"> wha\n"
					+ "</td></tr>\n"
					+ "<tr><td valign=\"top\">	hickory</td><td valign=\"top\"> dickory</td><td valign=\"top\"> dock\n"
					+ " ery\n"
					+ "</td></tr>\n"
					+ "</table>\n"
					+ "<p>\n"
					+ "Follows\n"
					, filtered
			        );
	      
		filtered = filterText
				(
					  "[(] A [|]  C [|] E [| style=\"background-color:yellow\"] G [)]\n"
					+ "[(] B [|]  D [|] F [|]  [)]\n"
					+ "[(]   [|]    [|]   [|]  [)]\n"
				);
        TestUtil.assertEquals( 
				"<tr><td valign=\"top\"> A </td><td valign=\"top\">  C </td><td valign=\"top\"> E </td><td valign=\"top\" style=\"background-color:yellow\"> G </td></tr>\n"
				+ "<tr><td valign=\"top\"> B </td><td valign=\"top\">  D </td><td valign=\"top\"> F </td><td valign=\"top\"> &nbsp; </td></tr>\n"
				+ "<tr><td valign=\"top\"> &nbsp; </td><td valign=\"top\"> &nbsp; </td><td valign=\"top\"> &nbsp; </td><td valign=\"top\"> &nbsp; </td></tr>\n" ,
				filtered
			);
	
	    filtered = filterText("\\// x");			TestUtil.assertEquals( "// x", filtered);
	    filtered = filterText(" // x \n");			TestUtil.assertEquals( "", filtered);
	    filtered = filterText("// comment");		TestUtil.assertEquals( "", filtered);
	    filtered = filterText("http://x");			TestUtil.assertEquals( "http://x", filtered);
	    
		filtered = filterText("[timestamp]");
		TestUtil.assertMatchesRegex( 
				"(Mon|Tue|Wed|Thu|Fri|Sat|Sun)"
				+ " (Jan|Feb|Mar|Apr|May|Jun|Jul|"
				+ "Aug|Sep|Oct|Nov|Dec) [0-3][0-9] "
				+ "[012][0-9]:[0-5][0-9]:[0-5][0-9] "
				+ "[A-Z][A-Z][A-Z] 20\\d\\d", filtered);

		filtered = filterText("[month]");			TestUtil.assertMatchesRegex( "1?\\d", 		filtered);
		filtered = filterText("[day]");				TestUtil.assertMatchesRegex( "[0-3]?\\d",	filtered);
		filtered = filterText("[year]");			TestUtil.assertMatchesRegex( "20\\d\\d",	filtered);
		filtered = filterText("[hr]");				TestUtil.assertMatchesRegex( "[0-1]?\\d",	filtered);
		filtered = filterText("[min]");				TestUtil.assertMatchesRegex( "[0-5]?\\d",	filtered);
		filtered = filterText("[sec]");				TestUtil.assertMatchesRegex( "[0-5]?\\d",	filtered);

		filtered = filterText("[Month]");
		TestUtil.assertMatchesRegex( "January|February|March|April|May|June|July|August|September|October|November|December", filtered);

		filtered = filterText("[Day]");
		TestUtil.assertMatchesRegex( "Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday", filtered);
	
		filtered = filterText("\\[");				TestUtil.assertEquals( "&#91;", filtered);
		filtered = filterText("\\]");				TestUtil.assertEquals( "&#93;", filtered);
		filtered = filterText("\\{");				TestUtil.assertEquals( "&#123;", filtered);
		filtered = filterText("\\}");				TestUtil.assertEquals( "&#125;", filtered);
		filtered = filterText("\\`");				TestUtil.assertEquals( "&#96;", filtered);
		filtered = filterText("\\\\");				TestUtil.assertEquals( "&#92;", filtered);
		filtered = filterText("\\*");				TestUtil.assertEquals( "&#42;", filtered);
		filtered = filterText("\\^");				TestUtil.assertEquals( "&#94;", filtered);
		filtered = filterText("\\_");				TestUtil.assertEquals( "&#95;", filtered);
	
		filtered = filterText("abc //x");			TestUtil.assertEquals( "abc", filtered);
		filtered = filterText("[* xxx *]");			TestUtil.assertEquals( "&hellip;", filtered);
	
		filtered = filterText("---");				TestUtil.assertEquals( "<hr class=\"hmlRule\">", filtered);
		filtered = filterText("&");					TestUtil.assertEquals( "&amp;", filtered);
		filtered = filterText("&amp;"); 			TestUtil.assertEquals( "&amp;", filtered);
		filtered = filterText("&#xxx;"); 			TestUtil.assertEquals( "&#xxx;", filtered);
		filtered = filterText("<<");				TestUtil.assertEquals( "&laquo;", filtered);
		filtered = filterText(">>");				TestUtil.assertEquals( "&raquo;", filtered);
		filtered = filterText("<");					TestUtil.assertEquals( "&lt;", filtered);
		filtered = filterText(">");					TestUtil.assertEquals( "&gt;", filtered);
		filtered = filterText("<hr style=\"123\">");TestUtil.assertEquals( "<hr style=\"123\">", filtered);
		filtered = filterText("x---y");				TestUtil.assertEquals( "x&mdash;y", filtered);
		filtered = filterText("0--1");				TestUtil.assertEquals( "0&ndash;1", filtered);
		filtered = filterText("x [---] y");			TestUtil.assertEquals( "x &mdash; y", filtered);
		filtered = filterText("x [--] y");			TestUtil.assertEquals( "x &ndash; y", filtered);
		filtered = filterText("!=");				TestUtil.assertEquals( "&ne;", filtered);
		filtered = filterText("[tm]");				TestUtil.assertEquals( "&trade;", filtered);
		filtered = filterText("(c)");				TestUtil.assertEquals( "&copy;", filtered);
		filtered = filterText("(C)");				TestUtil.assertEquals( "&copy;", filtered);
		filtered = filterText("(r)");				TestUtil.assertEquals( "&reg;", filtered);
		filtered = filterText("(R)");				TestUtil.assertEquals( "&reg;", filtered);
		filtered = filterText("+-");				TestUtil.assertEquals( "&plusmn;", filtered);
		filtered = filterText("-+");				TestUtil.assertEquals( "&plusmn;", filtered);
		filtered = filterText("+/-");				TestUtil.assertEquals( "&plusmn;", filtered);
		filtered = filterText("[*]");				TestUtil.assertEquals( "&times;", filtered);
		filtered = filterText("[/]");				TestUtil.assertEquals( "&divide;", filtered);
		filtered = filterText("[+]");				TestUtil.assertEquals( "&plus;", filtered);
		filtered = filterText("[-]");				TestUtil.assertEquals( "&minus;", filtered);
		filtered = filterText("[!=]");				TestUtil.assertEquals( "&ne;", filtered);
		filtered = filterText("!=");				TestUtil.assertEquals( "&ne;", filtered);
		filtered = filterText("<=");				TestUtil.assertEquals( "&le;", filtered);
		filtered = filterText(">=");				TestUtil.assertEquals( "&ge;", filtered);
		filtered = filterText("1/4");				TestUtil.assertEquals( "&frac14;", filtered);
		filtered = filterText("1/2");				TestUtil.assertEquals( "&frac12;", filtered);
		filtered = filterText("3/4");				TestUtil.assertEquals( "&frac34;", filtered);
		filtered = filterText("^1");				TestUtil.assertEquals( "&sup1;", filtered);
		filtered = filterText("^2");				TestUtil.assertEquals( "&sup2;", filtered);
		filtered = filterText("^3");				TestUtil.assertEquals( "&sup3;", filtered);
		filtered = filterText("^o");				TestUtil.assertEquals( "&deg;", filtered);
		filtered = filterText("X^super");			TestUtil.assertEquals( "X<sup style=\"hmlSuperscript\">super</sup>",filtered);
		filtered = filterText("X__sub");			TestUtil.assertEquals( "X<sub style=\"hmlSubscript\">sub</sub>", filtered);
		filtered = filterText("X{^ a b}");			TestUtil.assertEquals( "X<sup style=\"hmlSuperscript\">a b</sup>", filtered);
		filtered = filterText("X{_ a b}");			TestUtil.assertEquals( "X<sub style=\"hmlSubscript\">a b</sub>", filtered);
		filtered = filterText("<-");				TestUtil.assertEquals( "<strong>&larr;</strong>", filtered);
		filtered = filterText("->");				TestUtil.assertEquals( "<strong>&rarr;</strong>", filtered);
	
		filtered = filterText("[SS]");				TestUtil.assertEquals( "&sect;", filtered);
		filtered = filterText("[dagger]");			TestUtil.assertEquals( "&dagger;", filtered);
		filtered = filterText("[Dagger]");			TestUtil.assertEquals( "&Dagger;", filtered);
		filtered = filterText("[Delta]");			TestUtil.assertEquals( "&Delta;", filtered);
		filtered = filterText("[a``]");				TestUtil.assertEquals( "&agrave;", filtered);
		filtered = filterText("[a']");				TestUtil.assertEquals( "&aacute;", filtered);
		filtered = filterText("[a^]");				TestUtil.assertEquals( "&acirc;", filtered);
		filtered = filterText("[e``]");				TestUtil.assertEquals( "&egrave;", filtered);
		filtered = filterText("[e']");				TestUtil.assertEquals( "&eacute;", filtered);
		filtered = filterText("[e^]");				TestUtil.assertEquals( "&ecirc;", filtered);
		filtered = filterText("[i``]");				TestUtil.assertEquals( "&igrave;", filtered);
		filtered = filterText("[i']");				TestUtil.assertEquals( "&iacute;", filtered);
		filtered = filterText("[i^]");				TestUtil.assertEquals( "&icirc;", filtered);
		filtered = filterText("[o``]");				TestUtil.assertEquals( "&ograve;", filtered);
		filtered = filterText("[o']");				TestUtil.assertEquals( "&oacute;", filtered);
		filtered = filterText("[o^]");				TestUtil.assertEquals( "&ocirc;", filtered);
		filtered = filterText("[u``]");				TestUtil.assertEquals( "&ugrave;", filtered);
		filtered = filterText("[u']");				TestUtil.assertEquals( "&uacute;", filtered);
		filtered = filterText("[u^]");				TestUtil.assertEquals( "&ucirc;", filtered);
		filtered = filterText("[A``]");				TestUtil.assertEquals( "&Agrave;", filtered);
		filtered = filterText("[A']");				TestUtil.assertEquals( "&Aacute;", filtered);
		filtered = filterText("[A^]");				TestUtil.assertEquals( "&Acirc;", filtered);
		filtered = filterText("[E``]");				TestUtil.assertEquals( "&Egrave;", filtered);
		filtered = filterText("[E']");				TestUtil.assertEquals( "&Eacute;", filtered);
		filtered = filterText("[E^]");				TestUtil.assertEquals( "&Ecirc;", filtered);
		filtered = filterText("[I``]");				TestUtil.assertEquals( "&Igrave;", filtered);
		filtered = filterText("[I']");				TestUtil.assertEquals( "&Iacute;", filtered);
		filtered = filterText("[I^]");				TestUtil.assertEquals( "&Icirc;", filtered);
		filtered = filterText("[O``]");				TestUtil.assertEquals( "&Ograve;", filtered);
		filtered = filterText("[O']");				TestUtil.assertEquals( "&Oacute;", filtered);
		filtered = filterText("[O^]");				TestUtil.assertEquals( "&Ocirc;", filtered);
		filtered = filterText("[U``]");				TestUtil.assertEquals( "&Ugrave;", filtered);
		filtered = filterText("[U']");				TestUtil.assertEquals( "&Uacute;", filtered);
		filtered = filterText("[U^]");				TestUtil.assertEquals( "&Ucirc;", filtered);
		filtered = filterText("\\`");				TestUtil.assertEquals( "&#96;", filtered);
		filtered = filterText("``");				TestUtil.assertEquals( "&lsquo;", filtered);
		filtered = filterText("''");				TestUtil.assertEquals( "&rsquo;", filtered);
		filtered = filterText("```");				TestUtil.assertEquals( "&ldquo;", filtered);
		filtered = filterText("'''");				TestUtil.assertEquals( "&rdquo;", filtered);
		filtered = filterText("{cb x}");			TestUtil.assertEquals( "<code><strong>x</strong></code>", filtered);
		filtered = filterText("{ci x}");			TestUtil.assertEquals( "<code><em>x</em></code>", filtered);
		filtered = filterText("{b x y}");			TestUtil.assertEquals( "<strong>x y</strong>", filtered);
		filtered = filterText("*x y*");				TestUtil.assertEquals( "<strong>x y</strong>", filtered);
		filtered = filterText("{i x y}");			TestUtil.assertEquals( "<em>x y</em>", filtered);
		filtered = filterText("_x y_");				TestUtil.assertEquals( "<em>x y</em>", filtered);
		filtered = filterText("{bi bold italic}");	TestUtil.assertEquals( "<strong><em>bold italic</em></strong>", filtered);
		filtered = filterText("{u underline text}");TestUtil.assertEquals( "<u>underline text</u>", filtered);
		filtered = filterText("{s X}");				TestUtil.assertEquals( "<span style=\"text-decoration:line-through;\">X</span>", filtered);
		filtered = filterText("{x style text}");	TestUtil.assertEquals( "<span class=\"style\">text</span>", filtered);

		filtered = filterText("{c <b>code</b> &amp; text}"); TestUtil.assertEquals( "<code><b>code</b> &amp; text</code>", filtered);

		filtered = filterText("[]");				TestUtil.assertEquals( "&nbsp;&nbsp;", filtered);
		filtered = filterText("[#]");				TestUtil.assertEquals( "&nbsp;&nbsp;&nbsp;", filtered);
		filtered = filterText("[##]");				TestUtil.assertEquals( "&nbsp;&nbsp;&nbsp;&nbsp;", filtered);
		filtered = filterText("[###]");				TestUtil.assertEquals( "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;", filtered);
		filtered = filterText("[####]");			TestUtil.assertEquals( "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;", filtered);
		filtered = filterText("[#####]");			TestUtil.assertEquals( "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;", filtered);
		filtered = filterText("[######]");			TestUtil.assertEquals( "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;", filtered);
		filtered = filterText("[#######]");			TestUtil.assertEquals( "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;", filtered);
		filtered = filterText("[########]");		TestUtil.assertEquals( "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;", filtered);
	
		filtered = filterText("{link http://www.holub.com}");	TestUtil.assertEquals( "<a target=\"_blank\" href=\"http://www.holub.com\">http://www.holub.com</a>", filtered);
		filtered = filterText("{link www.holub.com}");			TestUtil.assertEquals( "<a target=\"_blank\" href=\"http://www.holub.com\">www.holub.com</a>", filtered);
		filtered = filterText("[www.holub.com]");				TestUtil.assertEquals( "<a target=\"_blank\" href=\"http://www.holub.com\">www.holub.com</a>", filtered);
		filtered = filterText("[http://www.holub.com]");		TestUtil.assertEquals( "<a target=\"_blank\" href=\"http://www.holub.com\">http://www.holub.com</a>", filtered);
		filtered = filterText("<http://www.holub.com>");		TestUtil.assertEquals( "<a target=\"_blank\" href=\"http://www.holub.com\">www.holub.com</a>", filtered);
		filtered = filterText("{link www.holub.com some text}");TestUtil.assertEquals( "<a target=\"_blank\" href=\"http://www.holub.com\">some text</a>", filtered);
		filtered = filterText("[www.holub.com other text]");	TestUtil.assertEquals( "<a target=\"_blank\" href=\"http://www.holub.com\">other text</a>", filtered);
	
		filtered = filterText("{anchor foo}");				TestUtil.assertEquals( "<a name=\"foo\"></a>", filtered);
		filtered = filterText("<box>X</box>");				TestUtil.assertEquals( "<div class=\"hmlBox\">X</div>", filtered);
		filtered = filterText("====== a");					TestUtil.assertEquals( "<h6> a</h6>", filtered);
		filtered = filterText("===== b");					TestUtil.assertEquals( "<h5> b</h5>", filtered);
		filtered = filterText("==== c");					TestUtil.assertEquals( "<h4> c</h4>", filtered);
		filtered = filterText("=== d");						TestUtil.assertEquals( "<h3> d</h3>", filtered);
		filtered = filterText("== e");						TestUtil.assertEquals( "<h2> e</h2>", filtered);
		filtered = filterText("= f");						TestUtil.assertEquals( "<h0> f</h0>", filtered);
		filtered = filterText(" ======a====== ");			TestUtil.assertEquals( "<h6>a</h6>", filtered);
		filtered = filterText("===== b =====");				TestUtil.assertEquals( "<h5> b </h5>", filtered);
		filtered = filterText("==== c ====");				TestUtil.assertEquals( "<h4> c </h4>", filtered);
		filtered = filterText("=== d ===");					TestUtil.assertEquals( "<h3> d </h3>", filtered);
		filtered = filterText("== e ==");					TestUtil.assertEquals( "<h2> e </h2>", filtered);
		filtered = filterText("= f =");						TestUtil.assertEquals( "<h0> f </h0>", filtered);
		filtered = filterText("====== a ====== A ");		TestUtil.assertEquals( "<h6 label=\"A\"> a </h6>", filtered);
		filtered = filterText("===== b ===== B discard");	TestUtil.assertEquals( "<h5 label=\"B\"> b </h5>", filtered);
		filtered = filterText("==== c ====C");				TestUtil.assertEquals( "<h4 label=\"C\"> c </h4>", filtered);
		filtered = filterText("=== d === D");				TestUtil.assertEquals( "<h3 label=\"D\"> d </h3>", filtered);
		filtered = filterText("== e == E");					TestUtil.assertEquals( "<h2 label=\"E\"> e </h2>", filtered);
		filtered = filterText("= f = F");					TestUtil.assertEquals( "<h0 label=\"F\"> f </h0>", filtered);
	
		filtered = filterText("=/ g = G");					TestUtil.assertEquals( "<h1 label=\"G\"> g </h1>", filtered);
		filtered = filterText("=/ h = H");					TestUtil.assertEquals( "<h1 label=\"H\"> h </h1>", filtered);
		filtered = filterText("=/ i");						TestUtil.assertEquals( "<h1> i</h1>", filtered);
		filtered = filterText("=/ j");						TestUtil.assertEquals( "<h1> j</h1>", filtered);
	
		filtered = filterText("= g \nfollow" );				TestUtil.assertEquals( "<h0> g</h0>\nfollow", filtered);
		filtered = filterText("= h =\nfollow" );			TestUtil.assertEquals( "<h0> h </h0>\nfollow", filtered);
		filtered = filterText("= i = I\nfollow" );			TestUtil.assertEquals( "<h0 label=\"I\"> i </h0>\nfollow", filtered);
	
		// The underscore (&#95;) will be restored by the EntityUnmapper filter
		filtered = filterText("{amazon 159059388X Holub on Patterns}");
		TestUtil.assertEquals( "<a href=\"http://www.amazon.com/exec/obidos/ASIN/159059388X/alleiholuasso\" target=\"&#95;blank\"><em>Holub on Patterns</em></a>", filtered);
	}
	
	@Test public void lineContinuationInDefinitionFile() throws Exception
	{
		Macro.DefinitionSet macroTable = new Macro.DefinitionSet();
		new Macro(config)._loadMacroDefinitions(
				macroTable,
				new Text("\n",
						 "/x \\" ,
						 "  /y \\",
						 " z/\\",
						 "MULTILINE ## comment can go here!"
						)
		);
		
		System.err.println( macroTable.toString() );
		
		assertEquals( 1, macroTable.textMacros().size() );
		assertTrue( "Continuation failed.", macroTable.textMacros().contains(new Macro.Definition(error, "x", "yz", null)) );
	}
	
	@Test public void commentsInDefinitionFile() throws Exception
	{
		Macro.DefinitionSet macroTable = new Macro.DefinitionSet();
		new Macro(config)._loadMacroDefinitions(
				macroTable,
				new Text("\n",
						 "## abc", 
						 "#m1#x#", 
						 "/m2/y/ ## comment at end of line",
						 "/#/#/", 
						 "/\\#\\#/z/"
						)
				);
		
		System.err.println( macroTable.toString() );
		
		assertEquals( 3, macroTable.textMacros().size() );
		assertFalse( "m1 present and shouldn't be.", macroTable.textMacros().contains(new Macro.Definition(error, "m1", "x", null)) );
		assertTrue ( "#  missing", macroTable.textMacros().contains(new Macro.Definition(error, "#", "#",  null)) );
		assertTrue ( "## missing", macroTable.textMacros().contains(new Macro.Definition(error, "##", "z", null)) );
		assertTrue ( "m2 missing", macroTable.textMacros().contains(new Macro.Definition(error, "m2", "y", null)) );
	}
	
	@Test public void macroTypesRecognized() throws Exception
	{
		Macro.DefinitionSet macroTable = new Macro.DefinitionSet( );
		new Macro(config)._loadMacroDefinitions(
				macroTable,
				new Text("\n",
						 "code: /a/b",
						 " text: /c/d",
						 "ref:/e/f"
						)
		 );
		
		System.err.println( macroTable.toString() );
		
		assertEquals( 1, macroTable.textMacros().size() );
		assertEquals( 1, macroTable.codeMacros().size() );
		assertEquals( 1, macroTable.refMacros().size() );
		
		assertTrue( "code macro missing", macroTable.codeMacros().contains(new Macro.Definition(error, "a", "b", null)) );
		assertTrue( "text macro missing", macroTable.textMacros().contains(new Macro.Definition(error, "c", "d", null)) );
		assertTrue( "ref macro missing",  macroTable.refMacros() .contains(new Macro.Definition(error, "e", "f", null)) );
		
		Text input = new Text("\n",
				"<macro>",
				"code: /c/C",
				"text: /t/T",
				"ref:  /r/R",
				"</macro>",
				"ctr <pre>ctr</pre>"
				);
				
		Macro macroManager = new Macro( config, false );
		new Pass( config, macroManager.getTextFilter() ).process( input );
		TestUtil.assertEquals( "\ncTr <pre>ctr</pre>", input.toString() );
		
		new Pass( config, macroManager.getCodeFilter() ).process( input );
		TestUtil.assertEquals( "\ncTr <pre>Ctr</pre>", input.toString() );
		
		new Pass( config, macroManager.getRefFilter()  ).process( input );
		TestUtil.assertEquals( "\ncTR <pre>CtR</pre>", input.toString() );
		
		return;
	}
	
	@Test
	public void twoLinksOnTheSameLine()
	{
		Text input = new Text("\n",
				"{link http://www.holub.com home}, {link http://linkedin.com/in/allenholub resume}",
				""  );
		Text expected = new Text("\n",
				  "<a target=\"_blank\" href=\"http://www.holub.com\">home</a>, "
				+ "<a target=\"_blank\" href=\"http://linkedin.com/in/allenholub\">resume</a>",
				"" );
		textFilter.filter(Text.EMPTY, input, Text.EMPTY, Filter.BlockType.TEXT );
		TestUtil.assertEquals(expected,input );
	}
	
	@Test
	public void userMacroTest()
	{
		Text input = new Text("\n",
				"<macro>",
				"/==>/<b>/",
				"'<=='</b>'",
				"</macro>",
				"<macro>",
				"|_([A-Za-z]+)_|{i $1}|",	// Verify that user macro can expand to built-in
				"</macro>",
				"==>bold<== _italic_",
				""
		);
		Text expected = new Text("\n",
				"",
				"",
				"<b>bold</b> <em>italic</em>",
				""
		);
		textFilter.filter(Text.EMPTY, input, Text.EMPTY, Filter.BlockType.TEXT);
		TestUtil.assertEquals(expected,input);
	}
	
	@Test
	public void multipleModifiers()
	{
		Text input = new Text("\n",
				"<macro>",
				"'^x.*x$'yes'DOTALL|MULTILINE",
				"</macro>",
				"x__",
				"--x",
				""
		);
		Text expected = new Text("\n",
				"",
				"yes",
				""
		);
		textFilter.filter(Text.EMPTY, input, Text.EMPTY, Filter.BlockType.TEXT);
		TestUtil.assertEquals(expected, input);
	}
	
	@Test
	public void gracefullyHandleBogusModifier()
	{
		Text input = new Text("\n",
				"<macro>",
				"'^x.*x$'yes'DOTALL|MULTILINE|BOGUS",
				"</macro>",
				"x__",
				"--x",
				""
		);
		Text expected = new Text("\n",
				"",
				"yes",
				""
		);
		textFilter.filter(Text.EMPTY, input, Text.EMPTY, Filter.BlockType.TEXT );
		TestUtil.assertEquals(expected,input);
	}
	
	@Test
	public void codeMacroCreateExpandTest()
	{
		Text input = new Text("\n",
				"<macro>",
				"code: /xxx/yyy/",
				"</macro>",
				"<pre>",
				"xxx",
				"</pre>"
		);
		Text expected = new Text("\n",
				"",
				"<pre>",
				"yyy",
				"</pre>"
		);
		
		Macro macroManager = new Macro( config, false );
		
		new Pass( config, macroManager.getTextFilter() ).process(input);	// to process <macro> def
		new Pass( config, macroManager.getCodeFilter() ).process(input);	// to process the defined macros
		TestUtil.assertEquals(expected,input);
	}
	
	@Ignore
	@Test
	public void javaCodeTest()
	{
		Text input = new Text("\n",
				"<pre>",
				"int f(;",
				"</pre>"
		);
		Text expected = new Text("\n",
				"<pre>",
				"<span class=\"hmlKeyword\">int</span> f(;",
				"</pre>"
		);
		Pass p = new Pass( config, textFilter );
		p.process(input);
		
		TestUtil.assertEquals(expected, input);
	}
	
	@Test
	public void bulletAndNumberlists()
	{
		Text input = new Text("\n",
				"",
				"* First",
				"  item.",
				"* Second item.",
				"* Third",
				"  item",
				"  is three lines long",
				"",
				"1. one",
				"2. two",
				"	three",
				"3. four",
				"",
				"# first num",
				"# second",
				"   num",
				"# third num",
				"",
				"after",
				""
			);

			Text expected = new Text("\n",
				"",	
				"<ul>",
				"<li> First",
				"  item.",
				"</li>",
				"<li> Second item.",
				"</li>",
				"<li> Third",
				"  item",
				"  is three lines long",
				"</li>",
				"</ul>",
				"<ol>",
				"<li> one",
				"</li>",
				"<li> two",
				"	three",
				"</li>",
				"<li> four",
				"</li>",
				"</ol>",
				"<ol>",
				"<li> first num",
				"</li>",
				"<li> second",
				"   num",
				"</li>",
				"<li> third num",
				"</li>",
				"</ol>",
				"after",
				""
				);		
			
		textFilter.filter(Text.EMPTY, input, Text.EMPTY, Filter.BlockType.TEXT );
		TestUtil.assertEquals( expected, input );
	}
	@Test public void dictionaryList()
	{
		Text input = new Text("\n",
				"; term 1",
				": def",
				"  of term 1.",
				"; term 2",
				": def term 2.",
				": another def term 2.",
				"",
				""
		);
		
		Text expected = new Text("\n",
				"<dl>",
				"<dt> term 1",
				"</dt>",
				"<dd> def",
				"  of term 1.",
				"</dd>",
				"<dt> term 2",
				"</dt>",
				"<dd> def term 2.",
				"</dd>",
				"<dd> another def term 2.",
				"</dd>",
				"</dl>",
				""
		);
		
		textFilter.filter(Text.EMPTY, input, Text.EMPTY, Filter.BlockType.TEXT );
		TestUtil.assertEquals( expected, input );
	}
}
