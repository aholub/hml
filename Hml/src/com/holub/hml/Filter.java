package com.holub.hml;

import com.holub.text.Text;

public interface Filter
{
	enum BlockType
	{
		/** Processing a code block */ CODE, 
		/** Processing a text block */ TEXT,
		/** Processing a code snippet */ SNIPPET,
		/** Handling a reference pass through code or text */ REF
	};
	
	/** Replace the contents of the body parameter with the filtered version. The output is the contents of the body
	 *  parameter. That is, the body will hold the filtered input.
	 *  
	 * @param prefix empty with TEXT blocks. Holds the surrounding markup (e.g. the &lt;listing&gt; tag or backquote) for code.
	 * 			Note that the replacement text is specified on return by the {@code body} object. If you don't transfer
	 * 			the prefix or suffix to the body within your filter, they are effectively removed from the output.
	 * @param body (OUTPUT) the entire text in a TEXT block or the body of the code block in the case of code.
	 * 			On exit, the contents of this object will replace the prefix-body-suffix set in the input.
	 * @param suffix empty with TEXT blocks. Holds the surrounding markup (e.g. the &lt;/listing&gt; tag or backquote) for code.
	 * 			Note that the replacement text is specified on return by the {@code body} object. If you don't transfer
	 * 			the prefix or suffix to the body within your filter, they are effectively removed from the output.
	 */
	void filter( Text prefix, Text body, Text suffix, BlockType type );

	/** Return true if this filter should be applied
	 *  to a code block ({@code <listing>} or (@code <pre>}).
	 * @return
	 */
	boolean isCodeBlockFilter();
	
	/** Return true if this filter should be applied
	 *  to code snippets (`code`).
	 * @return
	 */
	boolean isSnippetFilter();
	
	/** Return true if this filter should be applied
	 *  to text.
	 * @return
	 */
	boolean isTextFilter();
	
	//----------------------------------------------------------------------
	/** A default {@link Filter} that you can extend to simplify your
	 *  implementation a bit. The default filter method outputs the concatenation
	 *  of the prefix, body, and suffix. You can use it two ways:
	 *  <pre>
	 *  new Filter.Default( Filter.BlockType.CODE );
	 *  </pre>
	 *  creates a default filter that's used for code blocks, as does:
	 *  <pre>
	 *  new Filter.Default()
	 *  {	//...
			@Override public boolean isCodeBlockFilter(){ return true; }
	 *  }
	 *  </pre>
	 *  as does:
	 *  <pre>
	 *  Filter MyFilter extends Filter.Default
	 *  {
	 *  	MyFilter(){ super(Filter.BlockType.CODE); }
	 *  }
	 *  </pre>
	 *  <p>
	 *  This filter also overrides toString to return the string "Filter.Default";
	 *  This behavior is handy when you're debugging because it makes more sense
	 *  than the output of the default toString(). You may want to override
	 *  toString() in any Filter.Default extension to print something meaningful.
	 *  
	 * @author allen
	 */
	public static class Default implements Filter
	{
		BlockType type;
		public Default( BlockType type	) { this.type = type; }
		public Default( 				) { this.type = null; }
		
		@Override public void filter(Text prefix, Text body, Text suffix, BlockType type)
		{	body.prefix(prefix);
			body.append(suffix);
		}
		@Override public boolean isCodeBlockFilter(){ return type == BlockType.CODE; }
		@Override public boolean isSnippetFilter()	{ return type == BlockType.SNIPPET; }
		@Override public boolean isTextFilter()		{ return type == BlockType.TEXT; }
		
		@Override public String toString(){ return "Filter.Default"; }
	}
}
