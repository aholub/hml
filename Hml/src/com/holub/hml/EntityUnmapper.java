package com.holub.hml;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.holub.text.Text;

public class EntityUnmapper implements Filter
{
	private static final Pattern htmlEntity = Pattern.compile("&#(\\d+);");
	
	public EntityUnmapper( Configuration config )
	{	// For now, ignore the configuration object.
	}
	
	@Override public boolean isCodeBlockFilter(){ return false; }
	@Override public boolean isSnippetFilter() 	{ return false; }
	@Override public boolean isTextFilter() 	{ return true;	}

	/**
	 * Replace those HTML numeric (e.g. &#123;) entities that represent
	 * printable ASCII characters---in the range space (32) to tilde (126)---
	 * with the equivalent ASCII character. These are the characters that
	 * are inserted by both macro (and code-snippet) processing. You can't
	 * just leave them as entities because they might be used as element
	 * arguments, etc. (and they take up a lot of space in the output file).
	 * Note that non-numeric entities (e.g. &lt;) are not replaced.
	 */
	
	@Override
	public void filter(Text prefix, Text body, Text suffix, BlockType type)
	{
		assert type == BlockType.TEXT;
		
		body.replaceAll( htmlEntity, 
			new Text.Replacer()
			{	
				@Override public String replace(Matcher m)
				{
					// The $ and \ symbols are meaningful to the regular-expression-replacement
					// subsystem, so have to be escaped to remove their special meaning.
				
					int value = Integer.parseInt( m.group(1) );
					return	( value == '\\') 				 ? "\\\\" :
							( value == '$' ) 				 ? "\\$"  :
							( ' ' <= value && value <= '~' ) ? String.valueOf((char)value) : m.group(0);
				}
			}
		);
}
}
