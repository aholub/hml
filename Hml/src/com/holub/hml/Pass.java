package com.holub.hml;

import java.util.*;
import java.util.regex.*;

import com.holub.text.ReportingStream;
import com.holub.text.Text;
import com.holub.util.ExtendedLogger;

import static com.holub.hml.Filter.BlockType.*;	// CODE, TEXT, SNIPPET, REF

/** A processing pass makes one pass through the specified Text object. It splits up the
 *  file into two categories of data (text and code) and applies filters to that data
 *  as requested.
 * @author allen
 */

public class Pass
{
	private static final ExtendedLogger log = ExtendedLogger.getLogger(Pass.class);
	
	/** Current input source. Changed with every {@link #process(Text)} call
	 */
	private String input;
	
	private List<Filter> codeFilters 	= new ArrayList<Filter>();
	private List<Filter> textFilters 	= new ArrayList<Filter>();
	private List<Filter> snippetFilters = new ArrayList<Filter>();
	
	//----------------------------------------------------------------------
	private static final Pattern commaPrefixes = Pattern.compile("\\n\\s*,\\t?", Pattern.MULTILINE);
	private static final Text	 PRE_START_TEXT	= new Text("<pre>");
	private static final Text	 PRE_END_TEXT	= new Text("</pre>");
	//----------------------------------------------------------------------
	private TokenStream		tokenStream  = new TokenStream();
	
	Pattern p;
	private static enum Type
	{	
		PRE_SHORTHAND	(", ...",			"^([\\t ]*,.*\\n)+", Pattern.MULTILINE ),
		PRE_START		("<pre...>", 		"<pre(?=[\\s>])[^>]*?>"),
		PRE_END			("</pre>",			"</pre\\s*>"),
		LISTING_START	("<listing...>",	"<listing(?=[\\s>])[^>]*?>"),
		LISTING_END		("</listing>",		"</listing\\s*>"),
		COMMENT_START	("<!=",				"(?<!\\\\)<!="),
		COMMENT_END		("=!>)",			"(?<!\\\\)=!>"),
		SNIPPET			("`",				"((?<![`\\\\])`(?!`))|(^`(?!`))|((?<![`\\\\])`$)", Pattern.MULTILINE ),
		TEXT			("...text...",		"x" );	// regex is ignored for TEXT tokens
	
		private String  error;
		private Pattern recognizer;
		private Type( String error, String regex			){ this.error=error; recognizer = Pattern.compile(regex);		}
		private Type( String error, String regex, int flags ){ this.error=error; recognizer = Pattern.compile(regex,flags);	}
		
		/* Get a matcher for the current token type.
		 * @throws UnsupportedOperationException if you try to get a match for the TEXT type.
		 */
		public Matcher matcher(String input)
		{	
			if( this == TEXT )
				throw new UnsupportedOperationException("Can't get a matcher for a TEXT token type");
			return recognizer.matcher(input);
		}
		
		/** Returns a representation of the type suitable for display in an error message
		 *  Use {@link #name()} to get the type name.
		 */
		@Override public String toString()
		{	return error;
		}
	};
	
	//----------------------------------------------------------------------
	/** An input token used by the {@link Pass} class. Tokens do implement
	 *  Comparable and sort by the starting position of the token in the
	 *  input stream.
	 * 
	 * @author allen
	 */
	private class Token implements Comparable<Token>
	{
		final Type type;
		final int start;
		final int end;
		
		public Token( Type type, int start, int end )
		{	this.type	= type;
			this.start	= start;
			this.end	= end;
		}
		@Override public boolean equals(Object other)
		{	return(other instanceof Token)
					&& (((Token)other).type == type)
					&& (((Token)other).start == start)
					&& (((Token)other).end == end);
		}
		@Override public int hashCode()
		{	return start;
		}
		@Override public int compareTo(Token other)	// sort by start position.
		{	return this.start - other.start;
		}
		@Override public String toString()
		{	return String.format( "%s (%d-%d) [%s]", type.name(), start, end, input.substring(start,end) );
		}
		
		/** @return true if the receiving Token is of the indicated type
		 */
		public boolean match( Type type )
		{	return this.type == type;
		}
		
		/** Return the lexeme (the input string) associated with the
		 *  current token.
		 */
		public Text lexeme()
		{	return new Text( input.substring(start,end) );
		}
	}
	//----------------------------------------------------------------------
	class TokenStream
	{
		private Token			current		 = null;
		private Token			searchFrom	 = null;
		private TreeSet<Token>	tokens		 = new TreeSet<Token>();
		
		/** Returns a string representing the current and all lookahead
		 *  tokens. Returns a different string after every {@link #advance()}.
		 */
		@Override public String toString()
		{	
			Text theTokens = new Text();
			theTokens.appendf( "==> %s\n", current );
			for( Token t : tokens )
				 theTokens.appendf( "    %s\n", t );
			return theTokens.toString();
		}
		
		/** Load the tokens.
		 *  @param input
		 */

		private void load( String input )
		{
			// Search for tokens. Note that sequence is corrected by dint of the fact that
			// we're loading into a tree and tokens sort by start-location order. Consider a
			// TEXT CODE_BEGIN TEXT CODE_END situation. We'll load the CODE_BEGIN token first.
			// Then we'll load the CODE_END token, which will be inserted into the tree in
			// proper order (following the CODE_BEGIN). The TEXT tokens comprise
			// everything that's not recognized as a non-TEXT token.
			// existing tokens.
			//
			for( Type tokenType: Type.values() )
			{
				if( tokenType == Type.TEXT )
					continue;
				
				Matcher m = tokenType.matcher(input);
				while( m.find() )
					tokens.add( new Token(tokenType, m.start(), m.end()) );
			}
			
			// The PRE_SHORTHAND token is odd in that it can contain other tokens
			// (for example, a code snippet). Make an initial pass and remove
			// all such contained tokens.
			boolean lastTokenWasPreShorthand = false;;
			int		lastTokenEnd = -1;
			for( Iterator<Token> i = tokens.iterator(); i.hasNext(); )
			{	
				Token current = i.next();
				if( lastTokenWasPreShorthand && current.start < lastTokenEnd )
				{
					i.remove();
					continue;
				}
				lastTokenWasPreShorthand = current.match( Type.PRE_SHORTHAND);
				lastTokenEnd = current.end;
			}
				
			Iterator<Token> i = tokens.iterator();
			if( !i.hasNext() )	// it's entirely text.
				tokens.add( new Token( Type.TEXT, 0, input.length() ));
			else
			{
				// Make a second pass and create TEXT tokens that represent
				// all text that falls between other tokens.
				//
				List<Token> textTokens = new LinkedList<Token>();
				Token current = i.next();
				
				if( current.start != 0 )
					textTokens.add( new Token(Type.TEXT, 0, current.start ) );
				
				while( i.hasNext() )
				{	Token next = i.next();
					if( current.end < next.start )	// if end==start, they're adjacent. > is an error.
						textTokens.add( new Token(Type.TEXT, current.end, next.start) );
					current = next;
				}
				
				if( current.end < input.length() )
					textTokens.add( new Token(Type.TEXT, current.end, input.length()) );
				
				tokens.addAll(textTokens);
			}
			
			// Do the first advance (can't call advance() because it throws an exception if
			// current is null;
			current = tokens.pollFirst();
		}
			
		/** Advance to the next token
		 * @return The Token that is current after the advance.
		 * @throws IllegalStateException if you try to read past end of input.
		 */
		private Token advance()
		{	
			if( current == null )
				throw new IllegalStateException("Tried to advance past end of input");
			
			current = tokens.pollFirst();
			return current;
		}
		
		private boolean match( Type t ) throws IllegalStateException
		{	if( current == null )
				throw new IllegalStateException("Tried to match " + t + " at end of input");
			return current.match(t);
		}
		
		private Token current()
		{	if( current == null  )
				throw new IllegalStateException("Attempt to access current token when positioned at end of input");
			return current;
		}

		public boolean atEndOfInput()
		{	return current == null;
		}
		
		/** Skip forward to a token of the specified type. Prints an error message.
		 *  @throws NoSuchElementException if it can't find a token of the specified type.
		 *  @return true if the token was found.
		 */
		public boolean skipTo( Type t ) throws NoSuchElementException
		{	searchFrom = current;
			try
			{
				while( !match(t) )
					advance();
			}
			catch( IllegalStateException e )	// hit end of file
			{
				error.report( searchFrom.start, input, "Could not find %s", t.toString() );
				return false;
			}
			return true;
		}
		
		/** Advance to the end of a nested block that can contain begin/end pairs.
		 *  Token stream must be positioned at the start delimiter, and on exit,
		 *  it will be positioned at the end delimiter.
		 * @param begin
		 * @param end
		 * @throws IllegalStateException if end of input is reached without finding the matching end delimiter.
		 * 					An error message is also printed in this situation.
		 */
		public void findMatchingEndDelim( Type end )
		{
			log.trace("Looking for %s to match %s at position %d", end, current, current.start );
			
			Token beginToken   = current;
			int  startPosition = current.start;
			advance();
			
			int	 nestingLevel = 0;
			while( nestingLevel > 0 || !match(end) )
			{	if( match(beginToken.type) )
					++nestingLevel;
				else if( match(end) )
					--nestingLevel;
				advance();
			}
					
			if( tokenStream.atEndOfInput() )
			{
				error.report( startPosition, input, "Couldn't find %s to match %s", end.toString(), beginToken.lexeme() );
				throw new IllegalStateException("Reached end of input while looking for" + end.toString() );
			}
		}
	}

	//----------------------------------------------------------------------
	private final ReportingStream error;
	
	public Pass( Configuration config, Filter... filters )
	{
		this.error = config.error();
		
		for( Filter f : filters )
		{
			assert f.isTextFilter() || f.isCodeBlockFilter() || f.isSnippetFilter(): "Filter can't do anything!" ;
			
			if( f.isCodeBlockFilter() )
				codeFilters.add(f);
			
			if( f.isSnippetFilter() )
				snippetFilters.add(f);
			
			if( f.isTextFilter() )
				textFilters.add(f);
		}
			
		// Arrange for segments for which there is no filter to pass through to the output untouched.
		//
		if( codeFilters.size() 	  == 0 ){ codeFilters	.add( new Filter.Default(CODE) 	 ); }
		if( textFilters.size()	  == 0 ){ textFilters	.add( new Filter.Default(TEXT) 	 ); }
		if( snippetFilters.size() == 0 ){ snippetFilters.add( new Filter.Default(SNIPPET)); }
	}
		
	/** Make a pass through the inputText, filtering each segment
	 *  as appropriate. 
	 * @param inputText Both the input and the output. Contents are
	 * 			modified by processing.
	 * @return
	 */
	public boolean process(Text inputText)
	{
		Text processedText = new Text();
		try
		{
			input = inputText.toString();
			
			for( tokenStream.load(input); !tokenStream.atEndOfInput(); tokenStream.advance() )
			{
				if( tokenStream.match(Type.TEXT) )
				{
					Text body = tokenStream.current().lexeme();
										
					for( Filter f : textFilters )
						f.filter(	Text.EMPTY, body, Text.EMPTY, Filter.BlockType.TEXT );
					
					processedText.append(body);
				}
				else if( tokenStream.match(Type.COMMENT_START) )
				{
					tokenStream.current();
					tokenStream.findMatchingEndDelim(Type.COMMENT_END);
				}
				else if( tokenStream.match(Type.SNIPPET) )
				{
					Text prefix = tokenStream.current().lexeme();
					int start 	= tokenStream.advance().start;
					
					if( !tokenStream.skipTo(Type.SNIPPET) ) // error printed in skipTo
						return false;
					
					Text body = inputText.substring(start, tokenStream.current().start);
					if( body.indexOf('\n') >= 0 )
					{	error.report( start, input, "Code snippets (`code`) must be on a single line. Missing or extra backquote? Aborting this pass.");
						return false;
					}
					
					for( Filter f : snippetFilters )
						f.filter( prefix, body, tokenStream.current.lexeme(), Filter.BlockType.SNIPPET );
					
					processedText.append(body);
				}
				else if( tokenStream.match( Type.PRE_SHORTHAND) )	// lines that start with a comma are treated as a pre block
				{
					Text body = tokenStream.current().lexeme();
					body.prefix("\n");
					body.replaceAll(commaPrefixes, "\n");				// get rid of the comma prefix.
					for( Filter f : codeFilters )
						f.filter( PRE_START_TEXT, body, PRE_END_TEXT, Filter.BlockType.CODE );
					processedText.append(body);
				}
				else if( tokenStream.match(Type.LISTING_START) || tokenStream.match(Type.PRE_START) )
				{
					Token beginToken = tokenStream.current();
					Text prefix	 	 = tokenStream.current().lexeme();
					Type endDelim 	 = tokenStream.match(Type.LISTING_START) ? Type.LISTING_END : Type.PRE_END ;
					
					tokenStream.findMatchingEndDelim( endDelim );
					
					Text body = inputText.substring( beginToken.end, tokenStream.current().start);
					
					for( Filter f : codeFilters )
						f.filter( prefix, body, tokenStream.current().lexeme(), Filter.BlockType.CODE );
		
					processedText.append(body);
				}
				else if( tokenStream.match(Type.LISTING_END) || tokenStream.match(Type.PRE_END) || tokenStream.match(Type.COMMENT_END) )
				{
					error.report( tokenStream.current().start, input, "Found %s without matching start element", tokenStream.current().lexeme() );
					tokenStream.advance();
				}
				else
				{
					log.error( "Unexpected tag in input: %s", tokenStream.current().toString() );
					tokenStream.advance();
				}
			}
			
			inputText.replace( processedText );
		}
		catch( Exception e )
		{	
			log.error("Internal error: Unexpected exception %s.\nInput is:\n%s", e, new Text(input).indent(4) );
			return false;
		}
		
		return true;
	}
}