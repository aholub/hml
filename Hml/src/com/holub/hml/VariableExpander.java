package com.holub.hml;

/** Not used, but will be in future expansions.
 * 
 * @author allen
 */

public interface VariableExpander
{
	/** 
	 * Expand a macro replacement-text variable. The time-related variables
	 * are expanded in the Macro filter. There are two sorts of variables. The first
	 * set gets information about figures, tables, listings, and notes. The name is the
	 * label associated with the original element. If you apply these to marked lines
	 * within listing, you'll get information about the enclosing listing. The "scope"
	 * is one of: figure, table, listing, note
	 * 
	 * %(href:scope:name)		href for a <a...> element that will get us to the original location
	 * %(title:scope:name)		Title text
	 * %(id:scope:name)			Figure, table, listing, or note number (or note symbol).
	 * 
	 * These macros apply only to marks set within listings. The scope must be "code"
	 * 
	 * %(line:code:name)		The line number within the listing
	 * %(line-href:code:name)	The link to the line within the listing.
 	 * %(mark=value)			Associates a line number and listing
	 * 							information with the specified mark, and remembers it internally
	 * 							for subsequent use by %(href, etc). Does not generate a code anchor,
	 * 							but you can (in fact, must) do that in the macro expansion. For example,
	 * 							if you execute %(mark=fred) in a macro expansion, you must also create
	 * 							an anchor of the form <a name="fred"> ... </a>  surrounding the
	 * 							input that you want to mark. This macro evaluates recogizes a simple
	 * 							function definition in C/C++/Java, marks the line and surrounds the line with
	 * 							an anchor.
	 * 
	 * code: /(^\s*(int.*([\w_]*\s*\().*$)/<a name="%(mark=$2)">$1</a>/MULTILINE
	 * 
	 * @return the replacementText with any possible substitutions made
	 */
	
	boolean expandVariable( String replacementText );
}
