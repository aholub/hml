package com.holub.hml;

import com.holub.text.Text;
import com.holub.util.IdGenerator;

class EndNote
{	
	private String	id = IdGenerator.getId("");
	private String	mark, content;
	
	public EndNote(String mark, String content)
	{	this.mark = mark;
		this.content = content;
	}
	
	@Override public boolean equals( Object o )
	{	return (o instanceof EndNote) && ((EndNote)o).mark == mark;
	}
	
	@Override public int hashCode()
	{	return id.hashCode();
	}
	
	/** Return a string containing the HTML for an end-note reference. This is a hot link
	 *  to the actual note.
	 * @param argumentList The argument list from the original tag. Contains appropriate class="hmlRef" already.
	 */
	public Text reference()
	{	return new Text( "<a name=\"hmlRef-%s\" id=\"hmlRef-%s\" href=\"#hmlNote%s\">%s</a>", id, id, id, mark );
	}
	
	/** Return a string containing the HTML for an end note. The end note has the same mark
	 *  as the reference, and that mark is a hotlink that jumps to the reference.
	 *  to the actual note.
	 */
	public String toString()
	{
		Text noteHtml = new Text();
		noteHtml.appendf("<div class=\"hmlNoteGroup\" id=\"hmlNote-%s\">", id  );
		noteHtml.append(   "<div class=\"hmlNoteRef\">" );
		noteHtml.appendf(      "<a name=\"hmlNote%s\" href=\"#hmlRef-%s\">%s</a>", id, id, mark );
		noteHtml.append(   "</div>" );
		noteHtml.appendf(  "<div class=\"hmlNoteBody\" id=\"hmlNoteBody-%s\">%s</div>", id, content );
		noteHtml.append( "</div>\n" );
		return noteHtml.toString();
	}

	public String mark()   { return mark;			}
	public String target() { return "hmlNote" + id; }
}