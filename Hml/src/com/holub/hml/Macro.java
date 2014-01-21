package com.holub.hml;

import com.holub.hml.Filter;
import com.holub.hml.Filter.BlockType;
import com.holub.text.ReportingStream;
import com.holub.text.Text;
import com.holub.util.ExtendedLogger;
import com.holub.util.Places;

import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.*;
import java.io.Reader;

import org.apache.log4j.Level;

/** 
 * The macro class manages the complete set of macros, and provide filters
 * for handling code, text, and reference, processing.
 * The current implementation uses brute-force
 * multiple passes through the input. (I'll fix this later if it's too slow.)
 * <p>
 * Simple macros (that can be defined using regular-expression replacement)
 * are all defined in the file $CONFIG/hml.macros.
 * Each line in the file holds three arguments, which are passed to
 * String:replaceAll as it's arguments. That is, the first argument becomes
 * replaceAll's first argument and the second argument becomes replaceAll's second
 * argument. The third argument must be one of the following flags, and it
 * will be mapped to the equivalently named constant in the Pattern class:
 * <pre>
 *		UNIX_LINES
 *		CASE_INSENSITIVE
 *		COMMENTS
 *		MULTILINE
 *		LITERAL
 *		DOTALL
 *		UNICODE_CASE
 *		CANON_EQ
 * </pre>
 * These flags can be ORed together (e.g. MULTILINE|DOTALL)
 * <p>
 * Lines that start with #,
 * blank lines, and the sequence ## and all text that follows it are ignored.
 * The first character on the line (which can't be a #) is used as the field separator, and
 * that characters cannot be escaped to render it meaningless.
 * Space and tab characters can be used as delimiters if you like, but lines
 * consisting only of whitespace are ignored.
 */

public class Macro
{
	private static ExtendedLogger log = ExtendedLogger.getLogger(Macro.class);
	
	private static final String[] month_names =
	{	"January", "February", "March", "April", "May", "June", "July",
		"August", "September", "October", "November", "December", };

	private static final String[] day_names =
	{	null, "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
	};
	
	private final ReportingStream	error;
	//----------------------------------------------------------------------
	
	public Macro( Configuration config, boolean loadDefaultDefinitionsFromFile )
	{
		this.error  = config.error();
		
		if( loadDefaultDefinitionsFromFile )
		{
			loadDefaultMacroDefinitionsFromFile("hml.macros");
			log.debug( "Loaded macros from config file:\n%s\n", macros.toString());
		}
	}
	
	public Macro( Configuration config )
	{	this(config, true);
	}
	
	@Override public String toString()
	{	return macros.toString();
	}
	
	private final DefinitionSet macros = new DefinitionSet();
	//----------------------------------------------------------------------
	
	/** Load macro definitions from the specified macro-definition file into the specified macroTable.
	 */
	private void loadDefaultMacroDefinitionsFromFile( final String macroDefFile )
	{
		try
		{
			Reader reader = Places.CONFIG.reader(macroDefFile);
			_loadMacroDefinitions( macros, new Text(reader) );
			
			log.debug("Loading macros from" + Places.CONFIG.url("hml.macros") );
			
		}
		catch( Macro.DefinitionException e )
		{
			error.report( "Line %d: %s",  e.lineNumber, e.getMessage() );
		}
		catch( Exception e )
		{	
			error.report( "Couldn't read macro-defintion file: %s", e.getMessage() );
		}
	}
	//----------------------------------------------------------------------
	/**
	 * Load the macros ({@see Macro.ExternalMacros}) from the definitions in
	 * the definitions String, one definition per line. Lines in the file starting
	 * with # are comment lines, and are ignored. The rules
	 * are the same as the ones used in the hml.macros file (see the main
	 * documentation).
	 * <p>
	 * A ## sequence, along with all preceding whitespace and characters that follow, is discarded
	 * If a line ends with a \, then the next line is concatenated with the current line. In this case,
	 * all whitespace surrounding the \ is discarded, as is all leading whitespace on the following line.
	 * Use \#\#  to put a literal # in a macro.
	 * <p>
	 * Comments and line continuation don't play well together.
	 * <p>
	 * <b>This method is public only for testing purposes.</b>
	 * Do not call this method directly (see {@link #loadDefaultMacroDefinitionsFromFile(String)}).
	 * 
	 * @param definitions macro definitions, one per line.
	 * @param macroTable (output) load this table with the macros we find.
	 * 			 Macros must be loaded (and processed) in definition order. so this is a list, not a generic collection.
	 * @throws DefinitionException if there's something wrong with a definition.
	 */
	public void _loadMacroDefinitions( DefinitionSet macroTable, Text definitions) throws Exception
	{
		definitions.mergeContinuationLines(Text.Continuation.MERGE);
		
		int lineNum = 0;
		for( String line : definitions )
		{
			++lineNum;
			try
			{	
				// remove comments and blank lines
				line = line.replaceAll("((^#)|(##)).*", "");
				line = line.replaceAll("\\\\#", "#");
				
				line = line.trim();
				if( line.length()==0 )
					continue;
				
				BlockType type = BlockType.TEXT;
				
				if(      line.startsWith("code:") ){ type = BlockType.CODE; line = line.substring("code:".length()).trim(); }
				else if( line.startsWith("text:") ){ type = BlockType.TEXT; line = line.substring("text:".length()).trim(); }
				else if( line.startsWith("ref:")  ){ type = BlockType.REF;  line = line.substring("ref:" .length()).trim(); }
				
				String first = Pattern.quote( line.substring(0,1) );
				
				String[] chunk = line.split(first);
				
				// chunk[0] should be empty, so the search string is in chunk[1] and the
				// replacement is in chunk[2]
				
				if( chunk.length == 4 )	 	 macroTable.addLast(type, new Definition(error, chunk[1],chunk[2],chunk[3]));
				else if( chunk.length == 3 ) macroTable.addLast(type, new Definition(error, chunk[1],chunk[2],null	));
				else if( chunk.length == 2 ) macroTable.addLast(type, new Definition(error, chunk[1],"",null	));
				else
					throw new DefinitionException( lineNum,
													"Macro def must have either two or three fields:\n\t"
													+ line );
			}
			catch( IndexOutOfBoundsException e )
			{	// Line is empty. Loop back up and do the next line.
			}
		}
	}
	//----------------------------------------------------------------------
	/** Loads the "user macros" that are defined in <macro> elements in blocks.
	 *  are loaded into the front of the macro table, so they are processed before any
	 *  build-in or predefined macros. All <macro> elements are effectively
	 *  coalesced, and the user macros are applied in the order they were declared.
	 *  
	 * @param input
	 * @return input with &lt;macro&gt; elements removed.
	 */
	public Text loadUserMacros( Text input )
	{
		final DefinitionSet userMacros = new DefinitionSet();
	
		input = Tags.processElement( error, input, "macro", null,
			new Tags.Handler() {
				@Override public Text handle(String tag, Map<String, String> arguments, String body, String context, int start )
				{
					try
					{
						_loadMacroDefinitions( userMacros, new Text(body) );
					}
					catch (DefinitionException e)
					{
						Text contents = new Text(body);
						
						Text detailedError = new Text();
						int i = 0;
						for( String line : contents )
							detailedError.appendf( "\t\t" + line + (++i == e.lineNumber ? " <<<<<<<<<<<<<<<<<<<<" : "") );
						
						error.report( start, context, "%s:\n\t<macro>\n%s\t</macro>", e.getMessage(), detailedError );
					}
					catch( Exception e )
					{
						error.report( start, context, "Error loading macro: %s", e.getMessage() );
					}
					
					return Text.EMPTY;
				}
			}
		);
		
		macros.transferToHeadOfList( userMacros );
		return input;
	}
	//======================================================================
	/** A single macro definition. Holds the compiled pattern and the replacement
	 *  text. Replaces all instance of itself in a text block. This method is
	 *  used internally by the {@link Macro} class. It's public only for
	 *  testing purposes. (Making it public is harmless because an external
	 *  class that creates a Definition can't do anything with it!)
	 */
	public static class Definition
	{
		private static final Pattern  replacementVariables	= Pattern.compile(
			 "(?<!\\\\)%\\(("
			+"timestamp"
			+"|[mM]onth"
			+"|[dD]ay"
			+"|year"
			+"|hr"
			+"|min"
			+"|sec"
			+")\\)"
			);
	
		public Pattern regex;
		public String  replaceWith;

		private final Calendar now = Calendar.getInstance();
	
		public Definition( ReportingStream error, String regex, String replaceWith, String flagsString )
		{	
			// The replaceWith string can have characters like \n in it. Since these
			// strings haven't been through the compiler, they won't be interpreted
			// correctly. Make a pass through the string expanding escape characters
			//
			// Four backslashes are required because of the double compilation.
			// The java compiler turns \\\\ into \\, and the regex compiler treats
			// the \\ as a single \ character, thereby recognizing a \ character in
			// the input.
			//
			replaceWith = replaceWith.replaceAll("\\\\t",  "\t" )
									 .replaceAll("\\\\b",  "\b" )
									 .replaceAll("\\\\n",  "\n" )
									 .replaceAll("\\\\r",  "\r" )
									 .replaceAll("\\\\f",  "\f" )
									 ;
			int flags = 0;
			if( flagsString != null )
			{
				String[] allModifiers = flagsString.trim().split("\\s*\\|\\s*");
				for( String modifier : allModifiers )
				{	
					Modifiers current = null ;
					try 							    { current = Modifiers.valueOf(modifier); }
					catch( IllegalArgumentException e ) { /*retain null value in current*/ }
					
					if( current != null )
						flags = current.updateModifier( flags );
					else
					{	error.report("Ignoring illegal modifier (%s) found in macro definition: /%s/%s/%s/",
													modifier, regex, replaceWith, flagsString);
					}
				}
			}
			this.regex		 = Pattern.compile(regex, flags);
			this.replaceWith = replaceWith;
		}
	
		private void replaceVariables(Text t)
		{
			t.replaceAll( replacementVariables, 
				new Text.Replacer()
				{
					@Override public String replace(Matcher m)
					{
						switch( m.group(1).charAt(0) )
						{
						case 't':	return now.getTime().toString();
						case 'm':	return String.valueOf( m.group(1).charAt(1) == 'o' ? (now.get(Calendar.MONTH) + 1) : now.get(Calendar.MINUTE));
						case 'd':	return String.valueOf( now.get(Calendar.DATE)	);
						case 'y':	return String.valueOf( now.get(Calendar.YEAR)	);
						case 'h':	return String.valueOf( now.get(Calendar.HOUR)	);
						case 's':	return String.valueOf( now.get(Calendar.SECOND) );
						case 'M': 	return month_names[ now.get(Calendar.MONTH) 		];
						case 'D':  	return day_names  [ now.get(Calendar.DAY_OF_WEEK) ];
						default:	throw new RuntimeException( "Unexpected macro: [" + m.group() + "]" );	// shouldn't happen
						}
					}
				}
			);
		}
			
		/** Replace all instances of the current macro with its replacement
		 *  text in the specified Text object. The Text object's contents are
		 *  replaced by the expanded text.
		 *  
		 * @param t
		 */
		public void replaceAll(Text t) throws RuntimeException
		{	
			try
			{
				String before = "";
				if( log.isEnabledFor(Level.DEBUG ))
					before = t.toString();
				
				Matcher m = regex.matcher(t.toString());

				// We could (perhaps should) do this in the constructor. However,
				// by doing it here, we keep open the possibility of having
				// replacement variables whose values change over time instead
				// of assuming that the variable won't change after construction time.
				//
				Text replacementText = new Text(replaceWith);
				replaceVariables( replacementText );
				
				t.replace( m.replaceAll(replacementText.toString()) );
				
				if( log.isEnabledFor(Level.DEBUG ) )
				{	if( !before.equals(t.toString() ))	// macro did something!
					{	log.debug( "MACRO applied: /%s/%s/", regex, replaceWith ); 
						log.trace( "IN:\n%s\n" +
								   "OUT:\n%s\n",
								   new Text(before).indent("    "), 
								   new Text(t     ).indent("    ") );
					}
				}
			}
			catch( RuntimeException e )
			{
				log.error("Macro-expansion failure: /%s/%s/", regex, replaceWith );
				throw e;
			}
		}
		
		@Override public String toString()
		{	return String.format("{%s->%s}", regex.toString(), replaceWith );
		}
		
		@Override public int hashCode()
		{	return regex.hashCode();
		}
		
		@Override public boolean equals( Object o )
		{
			if( !(o instanceof Definition) )
				return false;
			
			boolean q1 = (((Definition) o).regex).toString().equals( this.regex.toString() );
			boolean q2 = (((Definition) o).replaceWith).equals( this.replaceWith );
			return q1 && q2;
		}
	}
	//======================================================================
	/** Thrown by {@see #loadMacroDefinitions(Iterable)} if something's wrong
	 *  in the input.
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
	public static class DefinitionException extends Exception
	{	
		private static final long serialVersionUID = 1L;
		
		/** The line number on which the error occurred */
		public final int lineNumber;
	
		private DefinitionException( int lineNumber, String message )
		{	super(message);
			this.lineNumber = lineNumber;
		}
	}
	//======================================================================
	private enum Modifiers
	{
		UNIX_LINES		(Pattern. UNIX_LINES),
		CASE_INSENSITIVE(Pattern. CASE_INSENSITIVE),
		COMMENTS		(Pattern. COMMENTS),
		MULTILINE		(Pattern. MULTILINE),
		LITERAL			(Pattern. LITERAL),
		DOTALL			(Pattern. DOTALL),
		UNICODE_CASE	(Pattern. UNICODE_CASE),
		CANON_EQ		(Pattern. CANON_EQ);
		
		private int actualValue;
		
		private Modifiers( int actualValue )
		{	this.actualValue = actualValue;
		}
		
		private int updateModifier( int currentValue )
		{	return currentValue | actualValue;
		}
	};
	
	//======================================================================
	/** The set of macro definitions. It's public to make testing easier.
	 * @author allen
	 */
	public static class DefinitionSet implements Iterable<Definition>
	{
		/** This implementation keeps three different lists, but it might not. (The individual
		 *  macros could be tagged with the block type, for example.)
		 */
		public LinkedList<Definition> textMacros = new LinkedList<Definition>();
		public LinkedList<Definition> codeMacros = new LinkedList<Definition>();
		public LinkedList<Definition> refMacros  = new LinkedList<Definition>();
		
		/** The next iterator will return an iterator across this set of macros */
		public LinkedList<Definition> iterateAcross  = textMacros;
		
		public void addLast(BlockType type, Definition definition)
		{	
			switch( type )
			{
			case TEXT:	textMacros.addLast( definition);	break;
			case CODE:	codeMacros.addLast( definition);	break;
			case REF:	refMacros. addLast( definition);	break;
			
			case SNIPPET: /*ignore*/ break;
			}
		}
		
		/** Move (not copy) all the macros from source to the front of the current definitions list. The ordering
		 *  in the original list is preserved.
		 * @param source
		 */
		public void transferToHeadOfList(DefinitionSet source)
		{
			for( int i = source.textMacros.size(); --i >= 0; ) textMacros.addFirst( source.textMacros.removeLast() );
			for( int i = source.codeMacros.size(); --i >= 0; ) codeMacros.addFirst( source.codeMacros.removeLast() );
			for( int i = source.refMacros.size();  --i >= 0; ) refMacros. addFirst( source.refMacros. removeLast() );
		}
		
		/** The next iterator returns only macros that are valid on this type of input.
		 */
		public void iterateAcross( BlockType type )
		{	assert type != BlockType.SNIPPET;
		
			switch( type )
			{
			case TEXT:			iterateAcross = textMacros;	break;
			case REF:			iterateAcross = refMacros;	break;
			
			case SNIPPET:		log.error("SNIPPET not valid in iterateAcross."); // fall through to code.
			case CODE: 			iterateAcross = codeMacros;
								break;
			}
		}
		
		@Override public Iterator<Definition> iterator(){ return iterateAcross.iterator(); }
		
		public List<Definition> textMacros() { return Collections.unmodifiableList( textMacros ); }
		public List<Definition> codeMacros() { return Collections.unmodifiableList( codeMacros ); }
		public List<Definition> refMacros()  { return Collections.unmodifiableList( refMacros ); }
		
		@Override public String toString()
		{	return String.format( "Text: %s\nCode: %s\nRef:  %s\n",
							textMacros.toString(), codeMacros.toString(), refMacros.toString() );
		}
	}
	//======================================================================
	public Filter getCodeFilter() {	return new CodeFilter(); }
	
	private class CodeFilter implements Filter
	{
		@Override public boolean isCodeBlockFilter(){ return true;	}
		@Override public boolean isTextFilter()		{ return false;	}
		@Override public boolean isSnippetFilter()	{ return false; }
	
		@Override public void filter( Text prefix, Text body, Text suffix, Filter.BlockType type )
		{
			assert type == BlockType.CODE;
			
			log.trace("Running CodeFilter");
				
			macros.iterateAcross( BlockType.CODE );
			for( Definition macro : macros )
				macro.replaceAll(body);
			
			body.prefix( prefix );
			body.append( suffix );
		}
	}
	//======================================================================
	public Filter getRefFilter() {	return new RefFilter(); }
	
	private class RefFilter implements Filter
	{
		@Override public boolean isCodeBlockFilter(){ return true;	}
		@Override public boolean isTextFilter()		{ return true;	}
		@Override public boolean isSnippetFilter()	{ return false; }
	
		@Override public void filter( Text prefix, Text body, Text suffix, Filter.BlockType type )
		{
			assert type==BlockType.CODE || type==BlockType.TEXT ;
			
			log.trace("Running RefFilter");
			
			macros.iterateAcross( BlockType.REF );
			for( Definition macro : macros )
				macro.replaceAll(body);
			
			body.prefix( prefix );
			body.append( suffix );
		}
	}
	//======================================================================
	public Filter getTextFilter() {	return new TextFilter(); }
	
	private class TextFilter implements Filter
	{
		@Override public boolean isCodeBlockFilter(){ return false;	}
		@Override public boolean isTextFilter()		{ return true;	}
		@Override public boolean isSnippetFilter()	{ return false; }
	
		@Override public void filter( Text prefix, Text body, Text suffix, Filter.BlockType type )
		{
			assert type == BlockType.TEXT;
			
			log.trace("Running TextFilter");
			
			body.replace( loadUserMacros(body) );
			
			macros.iterateAcross( BlockType.TEXT );
			for( Definition macro : macros )
				macro.replaceAll(body);
			
			body.prefix( prefix );
			body.append( suffix );
			
		}
	}
}