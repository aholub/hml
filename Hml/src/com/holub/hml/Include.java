package com.holub.hml;

import com.holub.text.ReportingStream;
import com.holub.text.Text;
import com.holub.util.ExtendedLogger;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

// TODO: Rewrite this file to use Tags.processElement

/**	Processes:
 * <pre>
 * &lt;include src="file.name"&gt;
 * &lt;include href="file://c:/dir/file.name"&gt;
 * 
 * &lt;import src="file.name"&gt;
 * &lt;import href="file://c:/dir/file.name"&gt;
 * </pre>
 * See main documentation for more details.
 */

public class Include implements Filter
{
	private static ExtendedLogger log 		 = ExtendedLogger.getLogger(Include.class);
	private static final Pattern  tagPattern = Pattern.compile( "<\\s*(include|import)\\s+[^>]*>\\s*", Pattern.MULTILINE | Pattern.DOTALL );
	
	private ReportingStream error;
	
	public Include( Configuration config )
	{	this.error = config.error();
	}
	
	@Override public boolean isCodeBlockFilter(){ return false; }
	@Override public boolean isSnippetFilter() 	{ return false; }
	@Override public boolean isTextFilter() 	{ return true;  }
	
	@Override
	public void filter(Text prefix, Text body, Text suffix, final BlockType type)
	{
		assert type == BlockType.TEXT ;
		
		body.allowChanges(false);
		
		StringBuffer processedInput = new StringBuffer(); // Matcher can't use StringBuilder
		int			 start			= 0;
		Matcher		 currentTag		= tagPattern.matcher( body.toString() );
		
		try
		{
			while( currentTag.find() )									// for each <include...> tag in the file.
			{
				start = currentTag.start();
				
				@SuppressWarnings("unused")
				String currentTagString = currentTag.group(0);
				
				boolean isInclude = currentTag.group(1).equals("include");
					
				Text    includedFile = new Text("");
				String  fileName	 = "????";			// argument to src= or href= argument
				String  from	 	 = null;
				String  to	 		 = null;
				
				boolean 		    removeToFromIndicators	= false;
				String 			    argumentString			= currentTag.group(0);
				Map<String,String>  arguments				= new HashMap<String,String>();
				boolean				numbers					= true;
				
				Util.getArguments( argumentString, arguments, null );
				for( Entry<String,String> entry : arguments.entrySet() )
				{	
					String key   = entry.getKey();
					String value = entry.getValue();
					
					if( key.equals("href"))			{ fileName = expanded(value); includedFile = new Text( new URL(fileName));	}
					if( key.equals("src"))			{ fileName = expanded(value); includedFile = new Text( new File(fileName));	}
					if( key.equals("from"))			{ from = value;		}
					if( key.equals("to"))			{ to   = value;		}
					if( key.equals("line-numbers"))	{ numbers = Character.toLowerCase(value.charAt(0))=='t';		}
					if( key.equals("numbers"))		{ numbers = Character.toLowerCase(value.charAt(0))=='t';		}
					if( key.equals("remove-mark"))	{ removeToFromIndicators = isTrue(value); }
					
					if( fileName==null || fileName.length()==0 )
						error.report( start, body, "No filename specified in href or src.\n" );
				}
					
				// if there's a from= attribute, truncate everything up to (but not including)
				// the line that holds a match of the pattern specified in the attribute.
				//
				if( from != null )
				{	
					try
					{
						Matcher fromMatcher = Pattern.compile(from, Pattern.MULTILINE)
												  .matcher(includedFile.toString());
					
						if( !fromMatcher.find() )
						{	error.report( start, body, "Can't find match for from=\"" + from + "\"" );
						}
						else
						{	
							int position = fromMatcher.start();
							while( position >= 0 && includedFile.charAt(position) != '\n' )
								--position;
							includedFile = includedFile.substring( position+1 );
							if( removeToFromIndicators )
								includedFile.replaceFirst( "[ \\t]*" + from + "([ \\t]*\\n)?", "" );
						}
					}
    				catch( PatternSyntaxException e )
					{
    					error.report( start, body, "Malformed from='%s' argument: %s\n",
    						from, e.getMessage() );
					}
				}
	
				// if there's a to= attribute, truncate everything following
				// the first line that holds a match of the pattern specified in the attribute.
				//
				if( to != null )
				{	
					try
					{
						Matcher toMatcher = Pattern.compile(to, Pattern.MULTILINE)
													.matcher(includedFile.toString());
						if( !toMatcher.find() )
						{	error.report( start, body, "Can't find match for to=\"" + to + "\"" );
						}
						else
						{	int position = toMatcher.end();
							while( position < includedFile.length() && includedFile.charAt(position) != '\n')
								++position;
							includedFile = includedFile.substring( 0, position+1 );
							if( removeToFromIndicators )
							{
								includedFile.replaceFirst( "[ \\t]*" + to + "([ \\t]*\\n)?", "" );
							}
						}
					}
    				catch( PatternSyntaxException e )
					{
    					error.report( start, body, "Malformed to='%s' argument: %s\n",
    						to, e.getMessage() );
					}
				}
				
				// If it's an include, add a <listing> element around the inclusion, otherwise add a <pre>

				if( isInclude )	// don't add anything if it's an import
				{	
					arguments.put("file", fileName );
					
					// Unlike other elements, I do not add an implicit class= to argument to the
					// generated listing or pre element. The <listing> elements adds a class
					// based on the state of the line-numbers argument.
					
					Text passThroughArguments =
						Util.removeUnwantedArgumentsAndReturnTheRest( arguments,
								 "src", "href", "from", "to", "remove-mark", "numbers", "line-numbers" );
					
					String generatedElement = numbers ? "listing" : "pre" ;
					
					includedFile.prefix( "<"+ generatedElement + passThroughArguments +">\n");
					includedFile.append( "</" + generatedElement + ">\n");
				}

				// Process the included file recursively until
				// no more <include...> directives are found.
	
				filter( Text.EMPTY, includedFile, Text.EMPTY, BlockType.TEXT );
				
				// Can't use appendReplacement(), below, because the included file
				// might contain dollar signs, which are treated specially.
				// The following two lines are the equivalent of:
				// m.appendReplacement( processedInput, includedFile.toString() );
				// but treat the replacement string literally, and are more efficient
				// than calling Matcher.quoteReplacement() to escape suspect characters.
	
				currentTag.appendReplacement( processedInput, "" );
				processedInput.append( includedFile.toString() );
			}
		}
		catch( Exception e )
		{	
			String message = 
				error.report( start, body, "Error in %s:\n\t%s",
										currentTag.group(0), e.getMessage() );
			log.error(message,e);
		}
			
		currentTag.appendTail(processedInput);
		body.replace  (processedInput);
	}
	
	/** Replace a ~ in the asSpecified string with the path to your home
	 *  directory and return that string.
	 * @param asSpecified
	 */
	private String expanded( String asSpecified )
	{	return asSpecified.replaceAll("~", System.getProperty("user.home") );
	}
	
	private boolean isTrue( String value )
	{	return  Character.toLowerCase( value.charAt(0) ) == 't';
	}
}

