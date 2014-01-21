package com.holub.hml;

import java.util.regex.Pattern;

import com.holub.text.*;

/** Code snippets have to be handled in their own pass, or at least
 *  they have to be handled before macros are processed. The problem
 *  is that several macros recognize patterns that span multiple
 *  lines. If there's a code block in one of those lines, the
 *  text is effectively split into multiple chunks, and the
 *  macro will fail because the entire multi-line sequence
 *  won't be present in the input.
 *  
 *  The consequence of this processing order is that macros in
 *  code snippets wouldn't be handled properly. Consequently,
 *  every character in the code snippet is converted to an HTML
 *  entity to protect it from the macro processor. The macro
 *  processor could convert the harmless characters (everything except 
 *  	< ` {
 *  ) back to the single character if it wanted to, but it doesn't.
 *  
 * @author allen
 */

public class CodeSnippets implements Filter
{
	private static final Pattern escapedBackquote = Pattern.compile("\\\\`");
	@Override public boolean isSnippetFilter() 	 { return true;  }
	@Override public boolean isCodeBlockFilter() { return false; }
	@Override public boolean isTextFilter() 	 { return false; }
	
	public CodeSnippets( Configuration config )
	{	// for now, ignore the configuration.
	}

	@Override public void filter(Text prefix, Text body, Text suffix, BlockType type)
	{
		assert type==BlockType.SNIPPET;
		
		// Detabbing is slightly complicated by the fact that the first tab
		// is expanded to 3, not 4, spaces. This way the following two lines
		// will align properly:
		// 	`   X` <-- three spaces to the left of the X
		//  `   Y` <-- one tab to the left of the Y
		//
		// Solve the problem by adding an extra character to the left of the
		// line, detabbing, then removing the extra character.
		body.prefix(' ');
		body.detab();
		body.subText(1);
		
		body.replaceAll( escapedBackquote, "`" );
				
		Text asEntities = new Text("<nobr><code>");
		for( char c : body.characters() )
			asEntities.append( c == ' ' ? "&nbsp;" : Entity.toEntity(c, Entity.Type.PUNCTUATION ) );
		asEntities.append("</code></nobr>");
		
		body.replace(asEntities);
	}
}
