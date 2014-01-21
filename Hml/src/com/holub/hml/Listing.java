package com.holub.hml;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.*;

import com.holub.text.ReportingStream;
import com.holub.text.Text;
import com.holub.util.ExtendedLogger;
import com.holub.hml.Filter;

// TODO: Rewrite this file to use Tags.processElement

/** Handles &lt;listing&gt; and &lt;pre&gt; elements. See main documentation for details.
 *  The line numbers (and targets for links) are output in a separate div that's displayed
 *  to the left of the listing div. This cleans up the listings themselves quite a bit,
 *  and makes them more cut-and-paste-able.
 *  
 *  TODO: provide a link that goes to a completely unadorned listing so that it can be
 *  	  copied easily.
 */

public class Listing implements Filter
{
	private static final ExtendedLogger log = ExtendedLogger.getLogger(Listing.class);
	
	/** HTML Numeric entity for a < */ private static final String LT = "\\&#60;";
	/** HTML Numeric entity for a > */ private static final String GT = "\\&#62;";
	/** HTML Numeric entity for a & */ private static final String AMPERSAND = "\\&#38;";
	
	//----------------------------------------------------------------------
	@Override public boolean isCodeBlockFilter(){ return true;  }
	@Override public boolean isSnippetFilter()	{ return false; }
	@Override public boolean isTextFilter()		{ return false; }
	
	private final ReportingStream error;
	private final Pattern bangComment;
	
	public Listing( Configuration config )
	{	this.error = config.error();
	
	  	config.supplyDefault("bangComment", "(?://|(?<!&)#+)!" );
	  	bangComment = Pattern.compile( "(.*?)\\s*" + config.value("bangComment") + "\\s*(.*?)\\s*$" , Pattern.MULTILINE );
	}
	//----------------------------------------------------------------------
	/** The line number (and listing label) associated with things that can be referenced
	 * with {: ...}, etc.
	 */
	public static class Symbol
	{	
		public int 	  lineNumber;
		public String label;
		
		@Override
		public String toString()
		{	return	"{line:" + lineNumber + ", label:" + label + "}" ;
		}
	}
	
	private Map<String,Symbol> symbols = new TreeMap<String,Symbol>();	// symbols found in listings
		
	/** Add a new symbol and return the key used to access that symbol.
	 */
	public String addNewSymbol( int lineNumber, String prefix, String label, String name )
	{	
		Symbol sym = new Symbol();
		String key = (prefix != null && prefix.length() > 0) ? (prefix + "." + name) : name;
		
		sym.lineNumber = lineNumber;
		sym.label      = label;
		symbols.put(key, sym);
		
		return key;
	}
	
	/** Used by ListingReferences to get symbols
	 */
	public Symbol getSymbol(String id)
	{	return symbols.get(id);
	}

	// Information about the files that we've processed in the past. Used primarily
	// for line-number continuation if a file is discussed in pieces.
	//
	// The line number is indexed by the argument to the file= attribute of the
	// listing tag. It holds a fileInfo object that specifies the line
	// number of the last line of the previous listing with the same label
	// (or 0 if no lines have been printed yet).

	private class FileInfo
	{	public int 			lastLineProcessed = 0;
		public  ClassStack	stack = new ClassStack();
	}
	
	private FileInfo			 unnamedFile = new FileInfo();
	private FileInfo			 currentFile = unnamedFile;
	private Map<String,FileInfo> fileInfo	 = new HashMap<String,FileInfo>();
	
	// Ugly but simple. Shared state between various methods that process references and code.
	//
	private	int  braceNestingLevel = 0;
	
	// An entire <listing>...</listing> or <pre>...</pre> element. The groups are:
	// 1. the element name
	// 2. the argument list in the start-element tag
	// 3. the contents of the element.
	// 4. the end-element name (to make sure it matches the start element)
	//
	// Not that I'm using a reluctant qualifier in group 3 so that I don't
	// suck up everything between the first <pre> and the last </pre> (including
	// other <pre>...</pre> elements.
	
	private static final Pattern tag = Pattern.compile(
		"<\\s*" +
		"(listing|pre)\\s*" +	// Group 1: tag name
		"([^>]*)>", 			// Group 2: tag arguments
		Pattern.MULTILINE | Pattern.DOTALL );
		
	// Java stuff
	//
	private static final String javaId		= "([a-zA-Z_][a-zA-Z0-9_]*)";
	private	static final String access	  	= "(public|private|protected|/\\*\\s*package\\s*\\*/)";
	private static final String classifier	= "(class|interface|enum)";

	// The following two definitions are shared by the Include class.

	/*package*/ static final String CLASS_DEFINITION_WITH_ACCESS_PRIV =
				access			// $1
			+	".*?"
			+	classifier		// $2
			+	"\\s+"
			+	javaId			// $3
			+  "(\\s*\\{)?" 	// $4 }
			;

	/*package*/ static final String CLASS_DEFINITION_WITHOUT_ACCESS_PRIV =
				"^(\\s*)" 		// $1 This is a capturing group solely to allow groups to be the same as CLASS_DEFINITION_WITH_ACCESS
			+	classifier		// $2
			+	"\\s+"
			+	javaId			// $3
			+  "(\\s*\\{)?" 	// $4	 }
			;

	/*package*/ static final String MEMBER_DEFINITION =
				access			// $1
			+	"(.*?\\s)"		// $2 Make it a capturing group so that javaid will be $3
			+	javaId			// $3
			+	"\\s*[\\(,;=]"
			;

	private static final Pattern classDefinitionWithAccessPriv  	= Pattern.compile( CLASS_DEFINITION_WITH_ACCESS_PRIV  );
	private static final Pattern classDefinitionWithoutAccessPriv	= Pattern.compile( CLASS_DEFINITION_WITHOUT_ACCESS_PRIV );
	private static final Pattern memberDefinition = Pattern.compile( MEMBER_DEFINITION );
	
	private static final Pattern mark = Pattern.compile
										(	"!?\\{=\\s*" 
										+	"([a-zA-Z0-9_\\.\\-/:]+)"
										+	"\\}!?"
										);
	
	//----------------------------------------------------------------------
	private static final Pattern threeStarJavadocComment = Pattern.compile( "/\\*\\*\\*.*?\\*/" );
	private static final Pattern threeSlashComment 		 = Pattern.compile( "///.*?\n" );
	
	/************************************************************************
	 *  Do listing-related processing, as described above. This method may be
	 *  called several times. The symbol table (which holds the identified
	 *  declarations and associated line numbers) is preserved from previous
	 *  calls, and will be used on subsequent calls.
	 *
	 *  @param input a Text object pre-loaded with the file to process. This
	 *  			 object will be modified by the <code>process()</code> method.
	 */

	@Override
	public void filter(Text prefix, Text body, Text suffix, BlockType type)
	{
		log.trace("Running Listing Filter");
		
		assert type == BlockType.CODE;
		
		// I'm not clearing out the symbol table between invocations.
		// This means that when you process multiple files with multiple process
		// calls, a symbol created in a given file will be available to
		// all subsequent files.
		
		// Eliminate blank lines at the head of the code block
		while( body.length() > 0  &&  body.charAt(0) == '\n' )
			body.subText(1);
		
		if( body.length() <= 0 )
			error.report("Found <pre> or <listing> with no contents!");
			
		// Comments can't be done as macros because the change the line-number count.
		// * Replace all javadoc comments that start with three stars ("/***") with "/**...*/"
		// * Remove all three-slash end-of-line comments.

		body.detab(4,' ', "!<", ">!");
		body.replaceAll( threeStarJavadocComment,	"/**...*/", Pattern.DOTALL );
		body.replaceAll( threeSlashComment, 		"", Pattern.DOTALL );
		
		Matcher element = tag.matcher(prefix);
		if( !element.find() )
			error.report("Internal Error. Expected <listing> or <pre>, found %s", prefix );
		String startName = element.group(1);
		String arguments = element.group(2);
		
		if( !suffix.matches(".*" + startName + ".*") )
			error.report("Warning: mismatched listing/pre elements:\n\t" + "%s...%s", prefix, suffix );
		
		boolean isListing = startName.charAt(0) == 'l'; 
		
		Map<String,String> parsedArguments = new HashMap<String,String>();
		Util.getArguments( arguments, parsedArguments, "hmlPre" );
		
		final String  firstLineNumberAttribute	= parsedArguments.get("first-line");
			  String  labelAttribute			= parsedArguments.get("label");
			  String  titleAttribute 	    	= parsedArguments.get("title");
		final String  fileAttribute 	  		= parsedArguments.get("file");
		final String  prefixAttribute			= parsedArguments.get("prefix");
		final Text	  passthroughArguments	= Util.removeUnwantedArgumentsAndReturnTheRest( parsedArguments, "file","label","prefix","title","first-line" );
		
		if( labelAttribute == null && fileAttribute != null )
			labelAttribute = new File(fileAttribute).getName();
		
		Text listingHeader = new Text();
		Text listingFooter = new Text();
		Text processedCode = new Text();
		Text annotations   = new Text();
		
		if( titleAttribute != null )	// generate a listing-title element
		{	
			if( titleAttribute.trim().length() == 0 )
			{
				if( fileAttribute != null )
					titleAttribute = "<em>" + labelAttribute + "</em>" ;		// file names in italics
				else
					error.report( "Need a file=\"...\" when title=\"\" (with an empty argument) is specified:\n\t"+ "%s...%s", prefix, suffix);
			}
				
			listingHeader.append("<listing-title" );
			if( labelAttribute != null )
				listingHeader.append( " label=\"" + labelAttribute  + "\"" );
			listingHeader.append(">" + titleAttribute + "</listing-title>\n" );
		}
		
		listingHeader.concat(null, "<div class=\"hml", (isListing ? "Listing" : "Pre"), "Group\">\n" );
		listingFooter.append("</div>");
		
		processedCode.append( "<pre" + passthroughArguments.toString() + ">\n" );
		
		// If there's no file=attribute, start numbering at 0; otherwise, use previous largest line + 1 for line number
		//
		if( fileAttribute == null )
		{
			currentFile = unnamedFile;
			currentFile.lastLineProcessed = 0;
		}
		else
		{
			currentFile = fileInfo.get( fileAttribute );
			if( currentFile == null )									// first time we've seen this file
				fileInfo.put( fileAttribute, currentFile = new FileInfo() );
		}
		
		if( firstLineNumberAttribute != null )
			currentFile.lastLineProcessed = Integer.parseInt(firstLineNumberAttribute) - 1;
		
		for( String inputLine : body )
		{
			Text line = new Text(inputLine);
			annotations.append( ifMarkPresentRemoveMarkAndReturnAnchor(line, currentFile.lastLineProcessed + 1, prefixAttribute, labelAttribute)	);
			
			if( doBangComments(line, prefixAttribute, labelAttribute, fileAttribute ) )
			{
				annotations.append( markDeclarationsAndAddThemToSymbolTable(line, currentFile.lastLineProcessed + 1, prefixAttribute, labelAttribute) );
				
				++currentFile.lastLineProcessed;
				
				if( isListing )
					annotations.appendf( "%d", currentFile.lastLineProcessed );
				annotations.append("<br>\n");
			}
			processedCode.append(line);		// line could be empty if doListingLine removed everything on it, but that's okay.
		}
		processedCode.append("</pre>\n");
		
		listingHeader.concat("", "<div class=\"hmlCodeAnnotations\">\n"	, annotations  , "</div>\n");
		listingHeader.concat("", "<div class=\"hmlCode\">\n"	 		, processedCode, "</div>\n" );
		
		listingHeader.append( listingFooter );
		body.replace( listingHeader );
		
		logDebuggingInfo();
	}
			
	//----------------------------------------------------------------------
	/** Handle marks, The mark itself is removed from the line, and if the mark
	 *  is in a comment that holds nothing but the mark, the comment is removed, too.
	 *  The resulting line could be blank, but the line itself is never removed.
	 *  <p>
	 *  Note that removing the mark can result in an empty comment. hml.macros
	 *  contains a couple macros that remove these.
	 *  
	 *  @return the HTML anchor for the current mark or an empty string if there was no mark.
	 */
	private Text ifMarkPresentRemoveMarkAndReturnAnchor( Text line, int lineNumber, String prefix, String label )
	{
		Text annotation = new Text();
		Matcher m = mark.matcher( line.toString() );
		if( m.find() )
		{	
			String key = addNewSymbol( lineNumber, prefix, label, m.group(1) );
			line.replace( m.replaceAll("") );		// get rid of the mark
			annotation.append("<a name=\""+ key +"\"></a>");
		}
		return annotation;
	}
	//----------------------------------------------------------------------
	
	private boolean doBangComments( Text line, String prefix, String label, String fileName )
	{
		String originalLine 	 = line.toString();
		boolean bangCommentFound = false;
		String suffix = null;
		
		// Handle //! by splitting the line into two chunks and processing only the
		// first one. Can't use split because we won't be able to distinguish
		// between "x//!" and "//!x" (in both cases, there's only one partition).
		
		Matcher m = bangComment.matcher(line.toString());
		if( m.find() )
		{
			bangCommentFound = true;
			String content	= m.group(1);
			suffix			= m.group(2);
			line.replace(content);
			
			if( suffix.length() > 0 )
			{
				// Check that the bang comment contained nothing but markup and print
				// an error message if if did.
				//
				if( suffix.toString().replaceAll("\\s*<[^>]*>\\s*", "").length() > 0 )
				{
					error.report(
						"%s: found non-HTML element to right of //!\n\t[%s]\n", 
										fileName != null ? fileName : "Standard input",
										originalLine );
				}
				
				suffix.replaceAll("&", AMPERSAND );
				suffix = suffix.replaceAll("<", LT );	// these are replaced by the entity-unmapper filter.
				suffix = suffix.replaceAll(">", GT );
			}
		}
		
		if( !bangCommentFound )	// no suffix (no //! on the line)
		{
			line.append('\n');
			return true;
		}
		
		// There is a suffix---add it to the output.
		// If the suffix is the only thing on the line, don't number the current line. 
		
		boolean somethingToLeftOfSuffixOtherThanWhitespace = line.toString().trim().length() > 0;	// don't time the line itself!
		
		line.append(suffix);
		
		if( somethingToLeftOfSuffixOtherThanWhitespace )
		{
			line.append('\n');
			return true;
		}
		
		// There was nothing on the line other than the bang comment, so the line takes up
		// no space in the output.
		line.trim();
		return false;
	}
	//----------------------------------------------------------------------
	/**
	 * Look for a declaration and add an associated symbol if one is found.
	 * @param line The input line (is not modified)
	 * @param lineNumber The input line number
	 * @param prefix The prefix associated with a generated anchor.
	 * @param label The label to associate with a generated anchor.
	 * @return an annotation for the current line. Could be empty, but is never null.
	 */

	private Text markDeclarationsAndAddThemToSymbolTable(Text line, final int lineNumber, String prefix, String label)
	{
		Text annotation = new Text();
		
		int index = -1;
		while( (index = line.indexOf('{', index+1)) != -1 )
			++braceNestingLevel;

		String name = null;
		
		// The symbol's name is always m.group(3)
		
		Matcher m  = memberDefinition.matcher(line.toString());
		if( m.find() )
		{	name = currentFile.stack.fullyQualify(m.group(3));
		}
		else 
		{	
			m = classDefinitionWithAccessPriv.matcher( line.toString() );
			boolean foundWith 	 =  m.find();
			boolean foundWithout = false;
			if( !foundWith )
			{	
				m = classDefinitionWithoutAccessPriv.matcher(line.toString());
				foundWithout =  m.find();
			}
			
			if( foundWith || foundWithout )
			{
				name = m.group(3) ;
	
				// m.group(4) will hold the open brace if it's on the same line
				// as the declaration. The braceNestingLevel will have been
				// incremented, if this is the case, but the "level" of the
				// declaration should be outside the brace, not inside, so subtract.
				currentFile.stack.push( name.toString(),
							(m.group(4)!=null) ? braceNestingLevel -1 : braceNestingLevel );
	
				// The name of the current class has already been pushed, so the fully
				// qualified name is just the prefix appended by fullyQualify(), thus
				// the empty-string argument.
				name = currentFile.stack.fullyQualify("");
			}
		}
	
		if( name != null )
		{	
			String key = addNewSymbol( lineNumber, prefix, label, name );
			
			log.debug("Adding inferred symbol: " + key );
			
			annotation.concat( null, "<a name=\""+ name +"\"></a>");
		}

		index = -1;
		while( (index = line.indexOf('}', index+1)) != -1 )
			currentFile.stack.popIfAtLevel( --braceNestingLevel );
		
		return annotation;
	}

	/** Print debugging information if a -DDEBUG was specified
	 *  on the JVM command line.
	 */
	public void logDebuggingInfo()
	{
		if( symbols.size() > 0 )
		{
			Text t = new Text();
			for( String key: symbols.keySet() )
			{
				Symbol sym = symbols.get(key);
				t.appendf( "\t%-30s: %s\n", key, sym.toString() );
			}
			log.trace( "Symbols:\n" + t.toString() );
		}
	}

	/**********************************************************************
	 * A stack of class names. Names are pushed (along with the associated
	 * brace-nesting level when they're encountered. Names are popped when
	 * the matching end of brace is found.
	 */
	private static class ClassStack
	{	private LinkedList<ClassDefinition>	classes = new LinkedList<ClassDefinition>();

		private static class ClassDefinition
		{	public int level;
			public String name;
			public ClassDefinition( String name, int level )
			{	this.name	= name;
				this.level	= level;
			}
		}

		private void push( String name, int level )
		{
			classes.addLast( new ClassDefinition(name.toString(), level) );
		}

		private void popIfAtLevel( int level )
		{	if( classes.size() > 0 )
				if( classes.getLast().level == level )
					classes.removeLast();
		}

		/** Return a name, fully qualified by prefixing all the containing-class
		 *  names.
		 */
		private String fullyQualify( String memberName )
		{	Text name = new Text();
		
			for( Iterator<ClassDefinition> i = classes.iterator(); i.hasNext() ;)	// have to use an iterator
			{	name.append( i.next().name );
				if( i.hasNext() )
					name.append('.');	// add a dot only if there's something that follows
			}
			
			// classes.size == 0 when we're doing a <pre> tag that
			// describes code that's not in a class definition.

			if( memberName.length() > 0 )
			{   if( classes.size() > 0 )
					name.append( "." );
				name.append( memberName );
			}
			return name.toString();
		}
	}
	
	//======================================================================
	// References
	//======================================================================

	public Filter getReferenceExpander()
	{
		return new InternalListingReferences();
	}
	
	/** Used by InternalListingReferences, but can't be declared static unless
	 *  InternalListingReferences is made static.
	 */
	private static final String	 refId = "([a-zA-Z0-9_\\.\\-/:]+)";	// Used in code and also in references (which follow same rules as code)
	private static final Pattern memberReference = Pattern.compile
								( //    1					         	 2        3
									"\\{((?:[#:]|line|ref|sref)\\s*)"  + refId	+ "\\s*(.*?)\\s*\\}"
								);
	
	private class InternalListingReferences implements Filter
	{
		@Override public boolean isCodeBlockFilter(){ return true;  }
		@Override public boolean isSnippetFilter()	{ return false; }
		@Override public boolean isTextFilter()		{ return true;  }
	
		@Override
		public void filter(Text prefix, Text body, Text suffix, BlockType type)
		{
			body.prefix(prefix);
			body.append(suffix);
			
			StringBuffer output = new StringBuffer();
			int			 start	= 0;
			
			Matcher m = memberReference.matcher( body.toString() );
			if( !m.find() )
				return;
			do
			{
				start = m.start();
				
				char	requestType	= m.group(1).charAt(0);
				String	identifier	= m.group(2);
				Symbol  sym 		= Listing.this.getSymbol(identifier);
	
				if( sym == null )
				{	
					error.report(
							start, body,
							"Couldn't find a {= %s} or class/field/method definition that matches %s.\n"
									+ "\tIf you've used <listing prefix=\"myPrefix\">, refrences will take the form myPrefix.%s.\n"
									+ "\tAlso, a missing close brace in the listing can cause problems.\n",
									identifier, m.group(), identifier );
				}
				else
				{	
					String visibleText = "????";
					
					switch( requestType )
					{
					case '#':
						visibleText = String.valueOf(sym.lineNumber); 
						break;
						
					case 'l':
						visibleText = "line " + String.valueOf(sym.lineNumber);
						break;
						
					case ':':
						visibleText = nameToRightOfRightmostDotInCodeFont(identifier, m.group(3));
						break;
						
					case 's': // {sref...}
						visibleText = nameToRightOfRightmostDotInCodeFont(identifier, m.group(3)) 
											+ " (line " + String.valueOf(sym.lineNumber) + ")";
						break;
						
					case 'r': // {ref...}
						String label = sym.label;
						if( label == null || label.length() == 0 )
						{
							label = "????";
							error.report( start, body,
									"When using {ref " + identifier + "}, the surrounding <listing> must" +
									" have a label= argument (and that label must be used by the <listing-title>)" );
						}	
						
						visibleText = nameToRightOfRightmostDotInCodeFont(identifier);
						visibleText += m.group(3);
						visibleText +=
								  " ({listing " + label  + "}, "
								+ "<a href=\"#" + identifier+ "\">line "
								+      String.valueOf(sym.lineNumber)
								+ "</a>"
								+ ")";
						break;
					}
					
					m.appendReplacement(output,
						Matcher.quoteReplacement(
							( requestType=='r' ) // {ref x} processing inserts the anchor, so don't do it again.
								? visibleText
								: "<a href=\"#" +identifier+ "\">" + visibleText + "</a>" ));
				}
			}
			while( m.find() );
			m.appendTail( output );
			body.replace( output );
		}
		
		private String nameToRightOfRightmostDotInCodeFont( String identifier )
		{
			return nameToRightOfRightmostDotInCodeFont( identifier, "" );
		}
		
		private String nameToRightOfRightmostDotInCodeFont( String identifier, String suffix )
		{
			String visibleText = identifier;
			int dotPosition = identifier.lastIndexOf('.');
			if( dotPosition != -1 )
				visibleText =  identifier.substring( dotPosition + 1 );
			if( suffix != null )
				visibleText += suffix;
			
			return "<code>" + visibleText + "</code>";
		}
	}
}