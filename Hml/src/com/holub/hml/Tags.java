package com.holub.hml;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.holub.text.NumberFactory;
import com.holub.text.ReportingStream;
import com.holub.text.Text;

/**
 * @author Allen Holub
 * 
 * Handles nonstandard html-like tags other than &lt;listing&lt;, which is handled
 * in it's own filter (in Listing.java). In particular:
 * <p> 
 * &lt;block&gt;...&lt;/block&gt; is converted to &lt;blockquote&gt;, and a &lt;br&gt; is added
 * after each line of the block.
 * <p>
 * This class is a Singleton.
 * <p>
 * BUG: bad things happen if you try to nest any of the the elements process in this
 * file.
 *
 * <div style='font-size:8pt; margin-top:.25in;'>
 * &copy;2013 <!--copyright 2013--> Allen I Holub. All rights reserved.
 * This code is licensed under a variant on the BSD license. View
 * the complete text at <a href="http://holub.com/license.html">
 * http://www.holub.com/license.html</a>.
 * </div>
 */
public class Tags implements Filter
{
	private final Index			  index = new Index();
	private final Text 			  headAdditions	= new Text();
	private		  int			  noteNumber		= 0;
	private final NoteSet		  endNotes;
	private final ReportingStream error;
	
	public Tags( Configuration config, NoteSet endNotes )
	{	this.endNotes 	= endNotes;
		this.error		= config.error();
	}
	
	//----------------------------------------------------------------------
	@Override public boolean isCodeBlockFilter(){ return false; }
	@Override public boolean isSnippetFilter() 	{ return false; }
	@Override public boolean isTextFilter() 	{ return true;  }
	//----------------------------------------------------------------------
	
	/** Return a new text object that contains the original one, but with
	 *  the &lt;head&gt; element modified to reflect any &lt;head&gt; tags previously
	 *  found in the body of the document. main() calls (doEverything which calls) this method
	 *  to modify the real &lt;head&gt; element that's actually output. This method is not
	 *  used to process &lt;head&gt elements in the body of the document.
	 *  
	 * @param textContainingHeadElement The text containing the original head element. This
	 * 								object is not modified.
	 * @return a new Text object that contains the original text, but with an
	 * 			augmented &lt;head&gt; element.
	 */
	public Text appendAdditionsToHead( Text textContainingHeadElement )
	{
		return processElement( error, textContainingHeadElement, "head", null,
			new Handler() {
				public Text handle( String tag, Map<String,String> arguments, String body, String context, int start )
				{	
					Text argumentList = Util.removeUnwantedArgumentsAndReturnTheRest(arguments);
					return new Text( "<head" + argumentList + ">" + body + headAdditions + "</head>" );
				}
			}
		);
	}
	//----------------------------------------------------------------------
	/** Handles all extra elements not handled elsewhere: &lt;head&gt;, &lt;block&gt;, &lt;tabbed&gt;
	 */
	
	@Override public void filter(Text prefix,Text inputText, Text suffix, BlockType type )
	{
		assert type == BlockType.TEXT;
		inputText.allowChanges(false);
		
		Text replacement = processElement( error, inputText, "head", null,
			new Handler() {
				public Text handle( String tag, Map<String,String> arguments, String body, String context, int start  )
				{
					headAdditions.append( body );
					return Text.EMPTY;
				}
			}
		);
		
		replacement = processElement( error, replacement, true, "note", "hmlNote",
			new Handler() {
				public Text handle( String tag, Map<String,String> arguments, String body, String context, int start  )
				{
					String mark    = arguments.get("mark");
					String suffix  = arguments.get("suffix");
					String prefix  = arguments.get("prefix");
					String label   = arguments.get("label");
					
					if( mark == null )
						mark = String.valueOf( ++noteNumber );
					else
					{
						try
						{	noteNumber = NumberFactory.parse(mark).intValue();
						}
						catch( NumberFormatException e ){ /*do nothing (i.e. don't reset the note number).*/ }
					}
						
					Text argumentList = Util.removeUnwantedArgumentsAndReturnTheRest(arguments, "mark", "label", "suffix", "prefix");
					
					EndNote newNote = endNotes.add(mark, body, label);
					if( newNote != null ) 
					{
						Text reference = new Text( "<span%s>%s%s%s</span>",
													argumentList.toString(),
													prefix==null ? "" : prefix,
													newNote.reference(),
													suffix==null ? "" : suffix  );
						return reference;
					}
					
					error.report( start, context, "Mark specified in <endNote mark='%s'> has already been used.", mark );
					return new Text("????");
				}
			}
		);
		
		// Must do this one *after* you do the notes
		// If there are no notes, this element (and all its contents) are simply
		// removed from the input. Otherwise, the contents are printed followed by
		// the list of notes. If clear= attribute is found, then clear notes
		// (and reset the note number) after evaluation. An error is reported if there
		// are no notes, on the assumption that you wouldn't have an <endNotes> element
		// unless you expected there to be notes.
		
		replacement = processElement( error, replacement, "end[Nn]otes", "hmlNotes",
			new Handler() {
				public Text handle( String tag, Map<String,String> arguments, String body, String context, int start  )
				{
					boolean clear = arguments.get("clear") != null;
					
					Text argumentList = Util.removeUnwantedArgumentsAndReturnTheRest(arguments, "clear");
					Text noteBlock = new Text();
					
					noteBlock.appendf("<div %s>\n", argumentList );
					noteBlock.append(body);
					if( !endNotes.appendNotesToBlock( noteBlock ) )
						error.report(start, context, "No notes to print!" );
					noteBlock.append("</div>");
					
					if( clear )
					{	endNotes.clear();
						noteNumber = 0;
					}
					return noteBlock;
				}
			}
		);
		
		replacement = processElement( error, replacement, true, "index-entry", "",
				new Handler() {
					public Text handle( String tag, Map<String,String> arguments, String body, String context, int start  )
					{	
						String topic = arguments.get("topic");
						if( topic == null || topic.trim().length() == 0 )
							error.report( start, context, "Missing topic name for index entry.", body );
						
						return index.getAnchorForTopic(topic, body);	// the body holds the subtopic
					}
				}
			);
		
		replacement = processElement( error, replacement, "index", "hmlIndex",
			new Handler() {
				public Text handle( String tag, Map<String,String> arguments, String body, String context, int start  )
				{
					Text argumentList = Util.removeUnwantedArgumentsAndReturnTheRest(arguments);
					Text indexBlock = index.getIndex(body, argumentList );
					return indexBlock;
				}
			}
		);
				
		replacement = processElement( error, replacement, "block(?!quote)", "hmlBlock",
			new Handler() {
				public Text handle( String tag, Map<String,String> arguments, String body, String context, int start  )
				{
					Text processedText = new Text();
					Text argumentList  = Util.removeUnwantedArgumentsAndReturnTheRest(arguments);
					
					// Process the body of the tag
					processedText.concat( "", "<blockquote", argumentList, ">\n" );
					body = body.trim();
					processedText.append( body.replaceAll( "\\n", "<br>\n") );
					processedText.append( "<br>\n</blockquote>" );
					return processedText;
				}
			}
		);
		
		inputText.replace( replacement );
	}
	
	// ======================================================================
	// Generic tag-handling method. Used here and in other files.
	// ======================================================================
	
	/** Used by {@ Tags#processTag(Text, String, String, String[], Handler)} to process the body of the element.
	 * 
	 * @author Allen Holub
	 * 
	 * <div style='font-size:8pt; margin-top:.25in;'>
	 * &copy;2013 <!--copyright 2013--> Allen I Holub. All rights reserved.
	 * This code is licensed under a variant on the BSD license. View
	 * the complete text at <a href="http://holub.com/license.html">
	 * http://www.holub.com/license.html</a>.
	 * </div>
	 */
	public interface Handler
	{
		/**
		 * @param tag The tag name (no angle brackets or arguments&mdash;just the name).
		 * @param arguments The key="value" arguments in the start tag. A class=
		 * 					member for the default class will be in this Map if
		 * 					a default class was passed to {@ Tags#processTag(Text, String, String, String[], Handler)}
		 * @param body the text between the start and end tag. The tag is not included in this text.
		 * @param surroundingContext the larger string from which this tag was extracted.
		 * @param the start index of the tag in the surrounding context.
		 * @return the text to output in place of the element.
		 */
		Text handle( String tag, Map<String,String> arguments, String body, String surroundingContext, int start );
	};
	
	/** Process an individual element in the input. Elements may not nest! There are several examples
	 *  of how to use this method in <em>Tags.java</em>.
	 * 
	 * @param input			The input that we're processing
	 * @param boolean		removeLeadingSpace if true, then any space to the left of the start-element tag is removed. Useful for
	 * 						tags like &lt;note&gt;, which have to be bang up against whatever they're annotating.
	 * @param elementName	A regular expression that identifies the tag name. May identify multiple
	 * 						tags (e.g. <code>"h[0-9]"</code> to recognize <code>&lt;h0</code>, <code>&lt;h1</code>, <code>&lt;h2</code>, etc.,
	 * 						or <code>"listing|pre"</code> to recognize both <code>&lt;listing&gt;</code> and <code>&lt;pre&gt;</code>).
	 * 						The expression <b>may not contain capturing groups</b>. Use <code>(?:</code><em>X</em><code>)</code>
	 * 						for grouping (e.g. <code>"(?:figure|table|listing)-title"</code>).
	 * @param defaultClass	The default class= argument for the output tag. Used only if the
	 * 						tag doesn't already have a class= argument. Use null if there is not default class
	 * 						(in which case a user-defined class will pass through, but no default will be added).
	 * @param handler		Do whatever needs doing to the body of the element.
	 * @return a new Text object that contains the processed text. The original input object is not modified.
	 */
	
	public static Text processElement( ReportingStream error, Text input, boolean removeLeadingSpace, String elementName, String defaultClass, Handler handler )
	{
		Pattern tag = Pattern.compile(
			  (removeLeadingSpace ? "\\s*" : "")
			+ "<\\s*(" + elementName + ")"							// Group 1: tag name
			+ "((?:\\s*[a-zA-Z_-]+\\s*=\\s*[\"'][^\"']*[\"'])*)"	// Group 2: all attributes
			+ "\\s*>"												//			end tag.
			+ "(.*?)"												// Group 3: body of element
			+ "<\\s*/("+ elementName +")\\s*>",						// Group 4: tag name in closing element
			Pattern.MULTILINE | Pattern.DOTALL );
		
		Text processedText = new Text();
		
		int end   = 0 ;	// index of the end of the most-recent search.
		int start = 0;	// index of the start of the most recent search
		
		Matcher element = tag.matcher(input);
		while( element.find() )
		{
			String  startTag  = ( element.group(1) );
			String	arguments = ( element.group(2) );
			String	body      = ( element.group(3) );
			String  endTag    = ( element.group(4) );
			
			if( !startTag.equals(endTag) )	 // for patterns that recognize multiple tags
				error.report("Mismatched start (<%s>) and end (</%s>) tag.\n", startTag, endTag );
					
			start = element.start();
			processedText.append( input.substring(end, start) ); // from end of previous match to start of current one
			end   = element.end();
			
			Map<String,String> parsedArguments = new HashMap<String,String>();
			Util.getArguments( arguments, parsedArguments, defaultClass );
			
			processedText.append( handler.handle(startTag, parsedArguments, body, input.toString(), element.start()) );
		}
		
		processedText.append( input.substring(end) ); // add everything following the last tag (if there is anything)
		return processedText;
	}
	
	/** Convenience method, calls {@link #processElement(Text, boolean, String, String, Handler)} with a {@code false}
	 *  removeLeadSpace argument.
	 * @param input
	 * @param tagRecognizer
	 * @param defaultClass
	 * @param handler
	 * @return
	 */
	public static Text processElement( ReportingStream error, Text input, String tagRecognizer, String defaultClass, Handler handler )
	{	return processElement( error, input, false, tagRecognizer, defaultClass, handler );
	}
}
