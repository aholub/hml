/**
 * 
 */
package com.holub.hml;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.holub.text.Text;

/**
 * @author Allen Holub
 * 
 * A Utility that holds static methods used elsewhere.
 *
 * <div style='font-size:8pt; margin-top:.25in;'>
 * &copy;2013 <!--copyright 2013--> Allen I Holub. All rights reserved.
 * This code is licensed under a variant on the BSD license. View
 * the complete text at <a href="http://holub.com/license.html">
 * http://www.holub.com/license.html</a>.
 * </div>
 */
public class Util
{
	private static final Pattern argumentFinder = Pattern.compile( "([a-zA-Z-]+)\\s*=\\s*[\"']([^\"']*)[\"']" );
	
	/** Puts the arguments from the string element into a map. The general
	 * form is name="value", but you can use single instead of double quotes. (However,
	 * the parser doesn't distinguish between the two (name='xxx" and name="...' are both legal).
	 * The value can be empty (name=""). Argument names must be made up of upper or lower
	 * case letters or dashes.
	 * 
	 * @param argumentList	A string containing the argument list. Can be the entire tag.
	 * @param theMatches	put the found arguments into this Map
	 * @param defaultClass	if the found arguments don't have a class= argument, then add one
	 * 						with this value. Don't do anything with the class= if this argument
	 * 						is null or empty.
	 * @return theMatches	
	 */
	public static Map<String,String> getArguments( String argumentList, Map<String,String> theMatches, String defaultClass )
	{	
		Matcher arguments = argumentFinder.matcher( argumentList );
		while( arguments.find() )
		{
			String key   = arguments.group(1);
			String value = arguments.group(2);
			if( key == null )
				throw new IllegalArgumentException( "Missing key in argument list: " + argumentList );
				
			if( value == null )
				value = "";
			
			theMatches.put( key, value);
		}
		
		if( defaultClass != null && defaultClass.length()>0  && !theMatches.containsKey("class") )
			theMatches.put("class", defaultClass );
		
		return theMatches;
	}

	/** Remove all arguments that are in the removeThese list and return a string
	 *  that represents the remaining arguments in key="value" form. If the list
	 *  of argumentNames is empty or null, then return all the values in the map
	 *  as a string. If the map is empty (after removing known arguments), then
	 *  return an empty Text object. The returned list has been trim()ed.
	 *  @param parsedArguments A Map of the arguments passed in to the original tab.
	 *  @param removeThese A variable-length string of arguments to remove from the map.
	 *  
	 *  @return the list of arguments, assembled into a string. If there are no
	 *  		arguments, the string is empty, otherwise, it starts with a leading
	 *  		space followed by the arguments. The ordering of the arguments is
	 *  		undefined.
	 */ 
	public static Text removeUnwantedArgumentsAndReturnTheRest( Map<String,String> parsedArguments, String... removeThese )
	{
		if( removeThese != null && removeThese.length > 0 )
			for( String argument : removeThese )
				parsedArguments.remove( argument );
		
		Text newArgumentList = new Text();

		for( Map.Entry<String,String> entry : parsedArguments.entrySet() )
		{	
			String key   = entry.getKey();
			String value = entry.getValue();
			if( value == null)
				throw new IllegalArgumentException("Found null value: " + key + "=null" );
			
			newArgumentList.concat( "", " ", key, "=\"", value, "\"" );
		}
		
		return newArgumentList;
	}
}