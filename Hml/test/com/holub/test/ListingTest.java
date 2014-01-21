package com.holub.test;

import static org.junit.Assert.*;

import java.io.OutputStreamWriter;

import org.junit.*;

import com.holub.hml.*;
import com.holub.text.ReportingStream;
import com.holub.text.Text;
import com.holub.util.ExtendedLogger;

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
public class ListingTest
{
	static ExtendedLogger log = ExtendedLogger.getLogger(ListingTest.class);
	private static final ReportingStream error  = new ReportingStream( new OutputStreamWriter(System.err) );
	private static final Configuration   config = new Configuration(error);
	
	Listing	 listingFilter;
	Filter	 referenceFilter;
	Macro	 macroManager;
	
	@Before public void setup()
	{
		macroManager	= new Macro( config );
		listingFilter	= new Listing( config );
		referenceFilter = listingFilter.getReferenceExpander();
	}
	
	/** Run the listing filter in a pass and assert that the
	 *  result is equal to expected.
	 * @param expected expected result or null if the method shouldn't check.
	 * @param input
	 */
	private void runListingFilter( Text expected, Text input  )
	{
		new Pass( config, listingFilter 				).process(input);
		new Pass( config, macroManager.getCodeFilter()	).process(input);
		if( expected != null )
			TestUtil.assertEquals(expected, input);
	}
	
	/** Run the listing filter in a pass and assert that the
	 *  result is equal to expected.
	 * @param expected expected result or null if the method shouldn't check.
	 * @param input
	 */
	private void runListingAndReferenceFilter( Text expected, Text input  )
	{
		new Pass( config, listingFilter   				).process(input);
		new Pass( config, macroManager.getCodeFilter()	).process(input);
		new Pass( config, referenceFilter 				).process(input);
		if( expected != null )
			TestUtil.assertEquals(expected, input);
	}
	
	@Test
	public void noElements()
	{
		log.debug("noElements");
		Text expected = new Text( "\n",
			"=[",
			"contents" ,
			"]=\n"
		);
		Text input = new Text( "\n",
			"=[",
			"contents" ,
			"]=\n"
		);
		
		runListingFilter(expected, input);
	}
	
	@Test public void simplePre()
	{
		log.debug("simplePre");

		Text input = new Text( "\n",
			"<pre>" ,
			"x" ,
			"</pre>"
			);
		Text expected = new Text( "\n",
			"<div class=\"hmlPreGroup\">",
			"<div class=\"hmlCodeAnnotations\">",
			"<br>",
			"</div>",
			"<div class=\"hmlCode\">",
			"<pre class=\"hmlPre\">",
			"x",
			"</pre>",
			"</div>",
			"</div>"
			);			
		runListingFilter(expected, input);
	}
	
	@Test public void simpleListing()
	{
		log.debug("simpleListing");
		
		Text input = new Text( "\n",
			"<listing>" ,
			"x" ,
			"</listing>"
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
			"</div>"
			);
		runListingFilter(expected, input);
	}
	
	@Test public void listingWithMarkAndDeclaration()
	{
		Text input = new Text("\n",
			"{# x}",
			"<listing>",
			"class c",
			"{",
			"	public void f()",
			"	{	// {=x}",
			"	}",
			"}",
			"</listing>",
			"{# x}",
			"{# c.f}",
			""
			);
		
		Text expected = new Text( "\n",
			"<a href=\"#x\">4</a>",
			"<div class=\"hmlListingGroup\">",
			"<div class=\"hmlCodeAnnotations\">",
			"<a name=\"c\"></a>1<br>",
			"2<br>",
			"<a name=\"c.f\"></a>3<br>",
			"<a name=\"x\"></a>4<br>",
			"5<br>",
			"6<br>",
			"</div>",
			"<div class=\"hmlCode\">",
			"<pre class=\"hmlPre\">",
			"class c",
			"{",
			"    public void f()",
			"    {",
			"    }",
			"}",
			"</pre>",
			"</div>",
			"</div>",
			"<a href=\"#x\">4</a>",
			"<a href=\"#c.f\">3</a>",
			""
		);		
		runListingAndReferenceFilter(expected, input);
	}
	
	@Test public void htmlInComment()
	{
		Text input = new Text( "\n",
			"<pre>" ,
			"a < b > c & d // !<b>! bold !</b>!" ,
			"</pre>",
			""
		);
		
		Text expected = new Text( "\n",
			"<div class=\"hmlPreGroup\">",
			"<div class=\"hmlCodeAnnotations\">",
			"<br>",
			"</div>",
			"<div class=\"hmlCode\">",
			"<pre class=\"hmlPre\">",
			"a &lt; b &gt; c &amp; d <span class=\"hmlComment\">// <b> bold </b></span>",
			"</pre>",
			"</div>",
			"</div>",
			""
			);
			
		runListingFilter(expected, input);
	}
	
	@Test public void emptyPre()
	{
		System.err.print("THIS TEST OUTPUTS a pre-with-no-content ERROR MESSAGE.\n");
		
		Text input = new Text( "\n",
			"<pre>" ,
			"</pre>",
			"" 
			);
		
		Text expected = new Text("\n",
			"<div class=\"hmlPreGroup\">",
			"<div class=\"hmlCodeAnnotations\">",
			"<br>",
			"</div>",
			"<div class=\"hmlCode\">",
			"<pre class=\"hmlPre\">",
			"",
			"</pre>",
			"</div>",
			"</div>",
			""
			);
			
		runListingFilter(expected, input);
	}
	
	@Test public void singleLineCStyleComment()
	{
		Text input = new Text("\n",
			"<pre>",
			"   0 { CONST    2 0 { } { } /*  8476d28 */ }",
			"</pre>"
			);

		Text expected = new Text("\n",
			"<div class=\"hmlPreGroup\">",
			"<div class=\"hmlCodeAnnotations\">",
			"<br>",
			"</div>",
			"<div class=\"hmlCode\">",
			"<pre class=\"hmlPre\">",
			"   0 { CONST    2 0 { } { } <span class=\"hmlComment\">/*  8476d28 */</span> }",
			"</pre>",
			"</div>",
			"</div>"
		);
		runListingFilter(expected, input);
	}
	
	public void preWithArgumentsIncludingClass()
	{
		log.debug("preWithArgumentsIncludingClass");
		Text expected = new Text( "\n",
			"=[",
			"<div class=\"hmlListingGroup\">",
			"<pre class=\"myClass\" style=\"border-top:10; border-bottom:20\">" ,
			"contents" ,
			"</pre>",
			"</div>",
			"]=\n"
		);
		Text input = new Text( "\n",
			"=[",
			"<pre class=\"myClass\" style=\"border-top:10; border-bottom:20\">" ,
			"contents" ,
			"</pre>",
			"]=\n"
		);
		runListingFilter(expected, input);
	}
	
	@Test
	public void twoPres()
	{
		Text input = new Text("\n",
				"<pre>x</pre>",
				"<pre>y</pre>",
				""
				);

		Text expected = new Text("\n",
				"<div class=\"hmlPreGroup\">",
				"<div class=\"hmlCodeAnnotations\">",
				"<br>",
				"</div>",
				"<div class=\"hmlCode\">",
				"<pre class=\"hmlPre\">",
				"x",
				"</pre>",
				"</div>",
				"</div>",
				"<div class=\"hmlPreGroup\">",
				"<div class=\"hmlCodeAnnotations\">",
				"<br>",
				"</div>",
				"<div class=\"hmlCode\">",
				"<pre class=\"hmlPre\">",
				"y",
				"</pre>",
				"</div>",
				"</div>",
				""
				);
		runListingFilter(expected, input);
	}
	
	@Test
	public void listingWithArguments()
	{
		log.debug("listingWithArguments");
		Text expected = new Text("\n",
				"<div class=\"hmlListingGroup\">",
				"<div class=\"hmlCodeAnnotations\">",
				"1<br>",
				"</div>",
				"<div class=\"hmlCode\">",
				"<pre style=\"border-top:10; border-bottom:20\" class=\"myClass\">",
				"x",
				"</pre>",
				"</div>",
				"</div>",
				""
				);

	Text input = new Text( "\n",
			"<listing style=\"border-top:10; border-bottom:20\" class=\"myClass\">" ,
			"x" ,
			"</listing>",
			""
		);
		runListingFilter(expected, input);
	}
	
	@Test
	public void allListingArgsAndTitleGeneration()
	{
		log.debug("allListingArgs");
		Text input = new Text( "\n",
			"<listing label=\"theLabel\" title='theTitle' file='theFile' >" ,
			"contents" ,
			"</listing>",
			""
		);
		
		Text expected = new Text( "\n",
			"<listing-title label=\"theLabel\">theTitle</listing-title>",
			"<div class=\"hmlListingGroup\">",
			"<div class=\"hmlCodeAnnotations\">",
			"1<br>",
			"</div>",
			"<div class=\"hmlCode\">",
			"<pre class=\"hmlPre\">",
			"contents",
			"</pre>",
			"</div>",
			"</div>",
			""
			);
		
		runListingFilter(expected, input);
	}
	
	@Test
	public void defaultLabel()
	{
		Text input = new Text("\n",
				"<listing title=\"theTitle\" file=\"/dir/theFile\">x</listing>",
				""
				);

		Text expected = new Text("\n",
				"<listing-title label=\"theFile\">theTitle</listing-title>",
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
		
		runListingAndReferenceFilter(expected, input);
	}
	
	@Test 
	public void escapesWithinPre()
	{
		Text input = new Text("\n",
			"<pre>",
			"<include src=\"/Path/to/nonexistant/file.java\">",
			"&<>",
			"</pre>",
			""
			);

		Text expected = new Text("\n",
			"<div class=\"hmlPreGroup\">",
			"<div class=\"hmlCodeAnnotations\">",
			"<br>",
			"<br>",
			"</div>",
			"<div class=\"hmlCode\">",
			"<pre class=\"hmlPre\">",
			"&lt;include src=\"/Path/to/nonexistant/file.java\"&gt;",
			"&amp;&lt;&gt;",
			"</pre>",
			"</div>",
			"</div>",
			""
			);
		
		runListingFilter(expected, input);
	}
	
	@Test public void hashComment()
	{
		Text input = new Text("\n",
				"<pre>",
				"a # b",
				"# c",
				"d # ",
				" # ",
				"e",
				"</pre>"
				);
		Text expected = new Text("\n",
				"<div class=\"hmlPreGroup\">",
				"<div class=\"hmlCodeAnnotations\">",
				"<br>",
				"<br>",
				"<br>",
				"<br>",
				"<br>",
				"</div>",
				"<div class=\"hmlCode\">",
				"<pre class=\"hmlPre\">",
				"a <span class=\"hmlComment\"># b</span>",
				"<span class=\"hmlComment\"># c</span>",
				"d",
				" <span class=\"hmlComment\"># </span>",
				"e",
				"</pre>",
				"</div>",
				"</div>"
				);
		
		runListingFilter(expected, input);
		return;
	}
		
	@Test public void allMarkReferences()
	{
		Text input = new Text("\n",
				"<listing label=\"X\" title=\"T\">",
				"x // !{=mark}! Non-bang is tested elsewhere",
				"</listing>",
				"{: mark}",
				"{: mark(int,int)}",
				"{: mark (int, int) }",
				"{# mark}",
				"{line mark}",
				"{ref mark}",
				"{sref mark}",
				""
				);
		Text expected = new Text("\n",
				"<listing-title label=\"X\">T</listing-title>",
				"<div class=\"hmlListingGroup\">",
				"<div class=\"hmlCodeAnnotations\">",
				"<a name=\"mark\"></a>1<br>",
				"</div>",
				"<div class=\"hmlCode\">",
				"<pre class=\"hmlPre\">",
				"x <span class=\"hmlComment\">//  Non-bang is tested elsewhere</span>",
				"</pre>",
				"</div>",
				"</div>",
				"<a href=\"#mark\"><code>mark</code></a>",
				"<a href=\"#mark\"><code>mark(int,int)</code></a>",
				"<a href=\"#mark\"><code>mark(int, int)</code></a>",
				"<a href=\"#mark\">1</a>",
				"<a href=\"#mark\">line 1</a>",
				"<code>mark</code> ({listing X}, <a href=\"#mark\">line 1</a>)",
				"<a href=\"#mark\"><code>mark</code> (line 1)</a>",
				""
				);
		runListingAndReferenceFilter(expected, input);
	}
	
	@Test
	public void bangEscapesInComments()
	{
		Text input = new Text("\n",
			"<pre>",
			"// !<a>!",			// should yield		<span...>// <a>
			"//! <c>",			// should yield		<c>
			"e //! <f>",		// should yield 	e <f> {g}
			"//!",				// should disappear entirely */
			"//! ",				// should disappear entirely */
			" //!",				// should disappear entirely */
			"</pre>"
			);
		
		Text expected = new Text("\n",
			"<div class=\"hmlPreGroup\">",
			"<div class=\"hmlCodeAnnotations\">",
			"<br>",
			"<br>",
			"</div>",
			"<div class=\"hmlCode\">",
			"<pre class=\"hmlPre\">",
			"<span class=\"hmlComment\">// <a></span>",
			"<c>e<f>",
			"</pre>",
			"</div>",
			"</div>"
			);
		runListingFilter(expected, input);
	}
	
	@Test public void hashMarkBangEscapesInComments()
	{
		Text input = new Text("\n",
			"<pre>",
			"# !<a>!",			// should yield		<span...>// <a>
			"#! <c>",			// should yield		<c>
			"e #! <f>",		// should yield 	e <f> {g}
			"#!",				// should disappear entirely */
			"#! ",				// should disappear entirely */
			" #!",				// should disappear entirely */
			"</pre>"
			);
		
		Text expected = new Text("\n",
			"<div class=\"hmlPreGroup\">",
			"<div class=\"hmlCodeAnnotations\">",
			"<br>",
			"<br>",
			"</div>",
			"<div class=\"hmlCode\">",
			"<pre class=\"hmlPre\">",
			"<span class=\"hmlComment\"># <a></span>",
			"<c>e<f>",
			"</pre>",
			"</div>",
			"</div>"
			);
		runListingFilter(expected, input);
	}
	
	@Test public void linesContainingNothButBangCommentsDontMessUpLineNumbering()
	{
		Text input = new Text("\n",
			"<listing>",
			"plain //! <i>",
			"italic",
			"//! </i><b>",
			"bold",
			"//! </b>",
			"</listing>"
			);	
		
		Text expected = new Text("\n",
			"<div class=\"hmlListingGroup\">",
			"<div class=\"hmlCodeAnnotations\">",
			"1<br>",
			"2<br>",
			"3<br>",
			"</div>",
			"<div class=\"hmlCode\">",
			"<pre class=\"hmlPre\">",
			"plain<i>",
			"italic",
			"</i><b>bold",
			"</b></pre>",
			"</div>",
			"</div>"		
			);
		
		runListingFilter(expected, input);
	}
	
	@Test
	public void markedLinesWithPrefix()
	{
		log.debug("markedLinesWithPrefix");
		
		Text input = new Text( "\n",
			"<listing label=\"XXX\" prefix=\"ABC\">" ,
			" f(); // {=mark}" ,
			"</listing>",
			"{: ABC.mark}",
			"{# ABC.mark}",
			"{line ABC.mark}",
			"{sref ABC.mark}",
			"{ref ABC.mark}",
			""
		);
		
		Text expected = new Text( "\n",
			"<div class=\"hmlListingGroup\">",
			"<div class=\"hmlCodeAnnotations\">",
			"<a name=\"ABC.mark\"></a>1<br>",
			"</div>",
			"<div class=\"hmlCode\">",
			"<pre class=\"hmlPre\">",
			" f();",
			"</pre>",
			"</div>",
			"</div>",
			"<a href=\"#ABC.mark\"><code>mark</code></a>",
			"<a href=\"#ABC.mark\">1</a>",
			"<a href=\"#ABC.mark\">line 1</a>",
			"<a href=\"#ABC.mark\"><code>mark</code> (line 1)</a>",
			"<code>mark</code> ({listing XXX}, <a href=\"#ABC.mark\">line 1</a>)",
			""
		);
		
		runListingAndReferenceFilter(expected, input);
	}
	
	@Test
	public void refFailsWithoutLabel()
	{
		System.err.print("TEST CREATES A ...must have a label=... ERROR!\n");
		// TODO: mock Util.eprintf() and Util.error() to make sure error message gets printed.
		
		Text input = new Text( "\n",
			"<listing>" ,
			" f(); // {=mark}" ,
			"</listing>",
			"{ref mark}",
			""
			);

		Text expected = new Text("\n",
			"<div class=\"hmlListingGroup\">",
			"<div class=\"hmlCodeAnnotations\">",
			"<a name=\"mark\"></a>1<br>",
			"</div>",
			"<div class=\"hmlCode\">",
			"<pre class=\"hmlPre\">",
			" f();",
			"</pre>",
			"</div>",
			"</div>",
			"<code>mark</code> ({listing ????}, <a href=\"#mark\">line 1</a>)",
			""
			);
			
		runListingAndReferenceFilter(expected, input);
	}
	
	// Can be fixed by adding a file= attribute
	// to second listing!
	@Test public void twoListings()
	{
		Text input = new Text("\n",
				"<listing>",
				"class ClassName",
				"{",
				"}",
				"</listing>",
//				"<listing file=\"foo.java\" label=\"exampleListing\" title=\"Example Listing\">",
				"<listing label=\"exampleListing\" title=\"Example Listing\">",
				"public class Outer",
				"{",
				"}",
				"</listing>",
				"{: Outer}"
				);		
		Text expected = new Text("\n",
				"<div class=\"hmlListingGroup\">",
				"<div class=\"hmlCodeAnnotations\">",
				"<a name=\"ClassName\"></a>1<br>",
				"2<br>",
				"3<br>",
				"</div>",
				"<div class=\"hmlCode\">",
				"<pre class=\"hmlPre\">",
				"class ClassName",
				"{",
				"}",
				"</pre>",
				"</div>",
				"</div>",
				"<listing-title label=\"exampleListing\">Example Listing</listing-title>",
				"<div class=\"hmlListingGroup\">",
				"<div class=\"hmlCodeAnnotations\">",
				"<a name=\"Outer\"></a>1<br>",
				"2<br>",
				"3<br>",
				"</div>",
				"<div class=\"hmlCode\">",
				"<pre class=\"hmlPre\">",
				"public class Outer",
				"{",
				"}",
				"</pre>",
				"</div>",
				"</div>",
				"<a href=\"#Outer\"><code>Outer</code></a>"
				);		
		runListingAndReferenceFilter(null, input);
		
		int errorCount = error.getErrorCount();
		assertEquals( 0, errorCount );
		assertEquals( expected, input );
		
		return;
	}
	
	@Test public void oneImplicitMark()
	{
		Text input = new Text("\n",
				"<listing>",
				"public class C",
				"{",
				"  public void f() { }",
				"}",
				"</listing>",
				"{: C} {: C.f}"
				);		
		
		Text expected = new Text("\n",
				"<div class=\"hmlListingGroup\">",
				"<div class=\"hmlCodeAnnotations\">",
				"<a name=\"C\"></a>1<br>",
				"2<br>",
				"<a name=\"C.f\"></a>3<br>",
				"4<br>",
				"</div>",
				"<div class=\"hmlCode\">",
				"<pre class=\"hmlPre\">",
				"public class C",
				"{",
				"  public void f() { }",
				"}",
				"</pre>",
				"</div>",
				"</div>",
				"<a href=\"#C\"><code>C</code></a> <a href=\"#C.f\"><code>f</code></a>"
				);
		
		runListingAndReferenceFilter(expected, input);
		return;
	}
	
	@Test public void implicitMarks()
	{
		Text input = new Text("\n",
				"<listing>",
				"class class0 {",
				"}",
				"/***********",
				" * Should be elided.",
				" */",
				"/// should be gone",
				"",
				"public class Class1",
				"{	private int a = 0;  // a comment",
				"	private int b = 0;  /* another comment */",
				"	public boolean f(int[] x) {",
				"	}",
				"	/* package */ void g(){}",
				"	public class Inner {",
				"	    private int a;",
				"		public int f(){ return a; }",
				"		public class InnerInner {",
				"			public void f(){}",
				"		}",
				"	}",
				"	public class Inner2{",
				"		private int b;",
				"		public int f() {",
				"			return b;",
				"		}",
				"	}",
				"	public int inOuter() { if(true){return 1;}else{return 0;} }",
				"}",
				"interface Class3 { }",
				"class class_4",
				"{",
				"}",
				"</listing>",
				"{: Class1}<br>",
				"{: Class1.a}<br>",
				"{: Class1.b}<br>",
				"{: Class1.f}<br>",
				"{: Class1.g}<br>",
				"{: Class1.Inner}<br>",
				"{: Class1.Inner.a}<br>",
				"{: Class1.Inner.f}<br>",
				"{: Class1.Inner.InnerInner}<br>",
				"{: Class1.Inner.InnerInner.f}<br>",
				"{: Class1.Inner2.b}<br>",
				"{: Class1.Inner2.f}<br>",
				"{: Class1.inOuter}<br>",
				"{: class0}<br>",
				"{: Class3}<br>",
				"{: class_4}<br>",
				""
				);		
		
		Text expected = new Text("\n",
				"<div class=\"hmlListingGroup\">",
				"<div class=\"hmlCodeAnnotations\">",
				"<a name=\"class0\"></a>1<br>",
				"2<br>",
				"3<br>",
				"4<br>",
				"<a name=\"Class1\"></a>5<br>",
				"<a name=\"Class1.a\"></a>6<br>",
				"<a name=\"Class1.b\"></a>7<br>",
				"<a name=\"Class1.f\"></a>8<br>",
				"9<br>",
				"<a name=\"Class1.g\"></a>10<br>",
				"<a name=\"Class1.Inner\"></a>11<br>",
				"<a name=\"Class1.Inner.a\"></a>12<br>",
				"<a name=\"Class1.Inner.f\"></a>13<br>",
				"<a name=\"Class1.Inner.InnerInner\"></a>14<br>",
				"<a name=\"Class1.Inner.InnerInner.f\"></a>15<br>",
				"16<br>",
				"17<br>",
				"<a name=\"Class1.Inner2\"></a>18<br>",
				"<a name=\"Class1.Inner2.b\"></a>19<br>",
				"<a name=\"Class1.Inner2.f\"></a>20<br>",
				"21<br>",
				"22<br>",
				"23<br>",
				"<a name=\"Class1.inOuter\"></a>24<br>",
				"25<br>",
				"<a name=\"Class3\"></a>26<br>",
				"<a name=\"class_4\"></a>27<br>",
				"28<br>",
				"29<br>",
				"</div>",
				"<div class=\"hmlCode\">",
				"<pre class=\"hmlPre\">",
				"class class0 {",
				"}",
				"<span class=\"hmlComment\">/**...*/</span>",
				"",
				"public class Class1",
				"{   private int a = 0;  <span class=\"hmlComment\">// a comment</span>",
				"    private int b = 0;  <span class=\"hmlComment\">/* another comment */</span>",
				"    public boolean f(int[] x) {",
				"    }",
				"    <span class=\"hmlComment\">/* package */</span> void g(){}",
				"    public class Inner {",
				"        private int a;",
				"        public int f(){ return a; }",
				"        public class InnerInner {",
				"            public void f(){}",
				"        }",
				"    }",
				"    public class Inner2{",
				"        private int b;",
				"        public int f() {",
				"            return b;",
				"        }",
				"    }",
				"    public int inOuter() { if(true){return 1;}else{return 0;} }",
				"}",
				"interface Class3 { }",
				"class class_4",
				"{",
				"}",
				"</pre>",
				"</div>",
				"</div>",
				"<a href=\"#Class1\"><code>Class1</code></a><br>",
				"<a href=\"#Class1.a\"><code>a</code></a><br>",
				"<a href=\"#Class1.b\"><code>b</code></a><br>",
				"<a href=\"#Class1.f\"><code>f</code></a><br>",
				"<a href=\"#Class1.g\"><code>g</code></a><br>",
				"<a href=\"#Class1.Inner\"><code>Inner</code></a><br>",
				"<a href=\"#Class1.Inner.a\"><code>a</code></a><br>",
				"<a href=\"#Class1.Inner.f\"><code>f</code></a><br>",
				"<a href=\"#Class1.Inner.InnerInner\"><code>InnerInner</code></a><br>",
				"<a href=\"#Class1.Inner.InnerInner.f\"><code>f</code></a><br>",
				"<a href=\"#Class1.Inner2.b\"><code>b</code></a><br>",
				"<a href=\"#Class1.Inner2.f\"><code>f</code></a><br>",
				"<a href=\"#Class1.inOuter\"><code>inOuter</code></a><br>",
				"<a href=\"#class0\"><code>class0</code></a><br>",
				"<a href=\"#Class3\"><code>Class3</code></a><br>",
				"<a href=\"#class_4\"><code>class_4</code></a><br>",
				""
				);		
		runListingAndReferenceFilter(expected, input);
		return;
	}
}
