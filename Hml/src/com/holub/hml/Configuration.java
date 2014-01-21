package com.holub.hml;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.Properties;

import com.holub.hml.Tags.Handler;
import com.holub.text.ReportingStream;
import com.holub.text.Text;

/** The configuration class holds document-level configuration information,
 *  set with the <config> element, which it parses from the input.
 *  The contents of the element defines a set of key/value pairs
 *  that define various configuration values. (See {@link Properties#load(java.io.Reader)}
 *  for the syntax.)
 *  The Config object also holds a reference to the current error-reporting object.
 *  
 * @author allen
 */
public class Configuration implements Filter
{
	private final ReportingStream	error;
	private final Properties		values = new Properties();
	
	public Configuration( ReportingStream error )
	{	this.error = error;
	}
	
	public ReportingStream error()
	{	return error;
	}

	@Override
	public void filter(Text prefix, Text body, Text suffix, BlockType type)
	{
		Text replacement = Tags.processElement( error, body, true, "config", "",
				new Handler() {
					public Text handle( String tag, Map<String,String> arguments, String body, String context, int start  )
					{	
						try
						{	values.load( new StringReader(body) );
						}
						catch( IOException e )
						{
							error.report(start, context,
								"Malformed configuration. Must use key=value pairs, one per line." );
						}
						
						return Text.EMPTY;	// remove <configuration> element from the document
					}
				}
			);
		
		body.replace(replacement);
	}
	
	/**
	 * @param key	the configuration-property key
	 * @return		the associated value or null if the key doesn't exist
	 */
	public String value( String key )
	{	return values.getProperty(key);
	}
	
	/** Inserts the key/value pair into the configuration list only if
	 *  the key isn't already there. This way, a filter can insert a
	 *  default value before requesting the actual value.
	 *  The request returns the user-supplied value if there is one,
	 *  and the default value if there isn't.
	 *  
	 * @param key
	 * @param value
	 */
	public void supplyDefault( String key, String value )
	{
		if( !values.containsKey(key) )
			values.setProperty(key, value);
	}

	@Override public boolean isCodeBlockFilter(){ return false; } 
	@Override public boolean isSnippetFilter() 	{ return false; }
	@Override public boolean isTextFilter() 	{ return true;  }
}
