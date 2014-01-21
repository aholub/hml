package com.holub.hml;

import com.holub.text.ReportingStream;
import com.holub.text.Text;
import com.holub.util.ExtendedLogger;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handle figure, table, listing, and section numbering, both declarations 
 * (e.g. {@code <h}<em>N</em>{@code >}, {@code <listing-title>}, etc.)
 * and references (e.g. <code>{section X}</code>, <code>{listing X}</code>)
 * 
 * The declarations are handled directly by an object of the current class.
 * The references are handled by an instance of the inner {@link Titles.FigureTableListingSectionNoteReference} class
 * (see {@link #getReferenceExpander()}). Finally, table-of-contents expansion is handled by the
 * {@link	Titles.TocFilter} inner class (see {@link #getTocReplacementFilter()}).
 */

public class Titles implements Filter
{
	ExtendedLogger log = ExtendedLogger.getLogger(Titles.class);
	
	private int[] sectionNumbers 		= new int[]{ 0, 0, 0, 0, 0, 0, 0, 0, 0 };
	private int	  currentHeadingLevel	= 0;
	private int	  listingNumber			= 0;
	private int	  figureNumber			= 0;
	private int	  tableNumber			= 0;
	
	// Things that can have labels. Indexed by label, evaluates to the identifying text (e.g. (Listing 1.3").
	// The labeled things will all have name anchors around them.
	// Note that, if a table of contents is active, all <hN> elements will also have anchors around them,
	// but these have arbitrary name= attributes that are used only by the table-of-contents items.
	// (They take the form  name="section-1.2.")
	//
	private Map<String,String>	figures	 = new HashMap<String,String>();
	private Map<String,String>	tables	 = new HashMap<String,String>();
	private Map<String,String>  listings = new HashMap<String,String>();
	private Map<String,String>	sections = new HashMap<String,String>();	// sections associated with <hN label=xxxx>.
	
	public static enum TitleType
	{
		FIGURE, TABLE, LISTING, SECTION;

		/** Covert a string ("figure" "table" "listing" "label" to the appropriate
		 *  enum type. The string is not case sensitive.
		 **/
		public static TitleType getTypeForString(String s)
		{
			int key = Character.toLowerCase( s.charAt(0) );
			TitleType t =
					key=='l' ? LISTING :
		  			key=='f' ? FIGURE  :
		  			key=='t' ? TABLE   :
		  			key=='s' ? SECTION : null ;
			
			if( t == null )
				throw new IllegalArgumentException( s + " is not a legal label type. Must be listing, figure, table, or label ");
			
			return t;
		}
	};
	
	private boolean addSectionNumbers			= true;
	private String  chapterId					= null;	// If not null, use instead of the outermost chapter number.
	private String  label						= null;	// The "x" in label=x. null if no label= was found.
	private boolean useLettersForChapterNumbers = false;
	//----------------------------------------------------------------------
	
	private final ReportingStream error;
	
	public Titles( Configuration config )
	{	this.error = config.error();
	}
	
	//----------------------------------------------------------------------
	/** Return the visible text associated with a label of the given type or
	 *  null if there is no such label.
	 * @param label
	 * @param type
	 */
	public String getIdentifierForLabel(String label, String typeName )
	{
		TitleType type = TitleType.getTypeForString( typeName );
		switch( type )
		{
		case LISTING : return listings.get(label);
		case FIGURE  : return figures.get(label);
		case TABLE   : return tables.get(label);
		case SECTION : return sections.get(label);
		}
		throw new IllegalArgumentException( typeName + " not a legitimate label type" );
	}
	
	//----------------------------------------------------------------------
	@Override public boolean isSnippetFilter()	{ return false; }
	@Override public boolean isCodeBlockFilter(){ return false; }
	@Override public boolean isTextFilter()		{ return true;  }
	
	@Override
	public void filter(Text prefis, Text body, Text suffix, BlockType blockType)
	{
		assert blockType == BlockType.TEXT;
		
		Text output  = body;
		body.allowChanges(false);
		
		body = Tags.processElement( error, body, "h[0-9]", null /*no default class= */,
			new Tags.Handler() 
			{
				public Text handle( String tag, Map<String,String> arguments, String body, String context, int start  )
				{
					label = arguments.get("label");
					
					int	requestedHeadingLevel = Integer.parseInt( tag.substring(1) );
	
					if( requestedHeadingLevel == 0 || requestedHeadingLevel == 1 )	// <h0> or <h1>
					{	
						listingNumber = figureNumber = tableNumber = 0;
						addSectionNumbers = (requestedHeadingLevel != 0);
					
						chapterId = arguments.get("chapter");
						if( chapterId != null )
						{
							if( chapterId.matches("[0-9]+") )
							{
								useLettersForChapterNumbers = false;
								sectionNumbers[1] = Integer.parseInt(chapterId) -1;	// it will be incremented back to where it belongs shortly
								chapterId = null;
							}
							else if( chapterId.matches("[a-zA-Z]") )
							{
								useLettersForChapterNumbers = true;
								sectionNumbers[1] = chapterId.charAt(0) -1;	// it will be incremented back to where it belongs shortly
								chapterId = null;
							}
						}
					}
	
					while( currentHeadingLevel > requestedHeadingLevel )			// clear out lower levels
						sectionNumbers[currentHeadingLevel--] = 0;
	
					currentHeadingLevel = requestedHeadingLevel;
					++sectionNumbers[currentHeadingLevel];
	
					String tocArgument = arguments.get("toc");
					Text argumentList  = Util.removeUnwantedArgumentsAndReturnTheRest(arguments, "chapter","label","toc" );
					
					String tocTarget	 = (label != null) ? label : ("hmlContents" + contentsTarget++); 
					String sectionNumber = assembleSectionNumber(chapterId, useLettersForChapterNumbers);
					Text fullSectionHead = new Text("", (!addSectionNumbers ? "" : (sectionNumber + ". ")), body );
					
					if( label != null )
						sections.put( label, "Section " + sectionNumber );
					
					boolean thereIsAToc = ( tocArgument == null || (  (Character.toLowerCase(tocArgument.charAt(0)) != 'f')
															       && (Character.toLowerCase(tocArgument.charAt(0)) != 'n') ));
					if( thereIsAToc )
					{
						String tocText = fullSectionHead.trim().toString();
						
						tocText = String.format("<a href=\"#%s\">%s</a>" , tocTarget , tocText );
						tocText = String.format("<div class=\"hmlTocLev%d\">%s</div>", requestedHeadingLevel, tocText );
						tableOfContents.concat( "", tocText, "\n" );
					}
					
					int displayedHeadingLevel =	// (used in H element)
						requestedHeadingLevel == 0 ? 1 : requestedHeadingLevel;
					
					
					Text headingText = new Text("%s<h%d%s>%s</h%d>%s",
									(thereIsAToc ? ("<a name=\"" + tocTarget + "\">") : ""),
									displayedHeadingLevel,
									argumentList,
									fullSectionHead,
									displayedHeadingLevel,
									(thereIsAToc ? "</a>" : "") );
					return headingText;
				}
			}
		);
		
		body = Tags.processElement( error, body, "(?:listing|figure|table)-title", null /*no default class=*/,
			new Tags.Handler() 
			{	public Text handle( String tag, Map<String,String> arguments, String body, String context, int start  )
				{
					String	label	= arguments.get("label");
					int     number	= -1;
	
					String 	titleType		= null;	// String preceding number in generated title. e.g.: Listing 2.1
					String	defaultClass	= null;
					
					Map<String,String> 	symbols	=  null;	// the symbol table for the current tag type
					
					if( tag.equals("listing-title") )
					{
						number 			= ++listingNumber;
						titleType   	= "Listing ";
						symbols			= listings;
						defaultClass	= "hmlListingTitle";
					}
					else if( tag.equals("figure-title") )
					{
						number 			= ++figureNumber;
						titleType   	= "Figure ";
						symbols			= figures;
						defaultClass	= "hmlFigureTitle";
					}
					else if( tag.equals("table-title") )
					{
						number 			= ++tableNumber;
						titleType   	= "Table ";
						symbols			= tables;
						defaultClass	= "hmlTableTitle";
					}
					else
					{
						log.error("Internal error: Unknown title type recognized (%s)", tag );
						throw new IllegalArgumentException( "Unknown tag: " + tag );
					}
					
					Text identifyingText = new Text( titleType );
					
					// Add section numbers only if the feature has been turned on and
					// we've seen an <h1>.
					//
					if( addSectionNumbers && sectionNumbers[1] > 0 )
					{	if( chapterId != null )
							identifyingText.append( chapterId );
						else if( useLettersForChapterNumbers )
							identifyingText.append( (char)sectionNumbers[1] );
						else
							identifyingText.append( sectionNumbers[1] );
	
						identifyingText.append( "." );
					}
					
					identifyingText.append( number );
					symbols.put( label, identifyingText.toString() );
					
					Text replacementText = new Text("",
											"<div class=\"" + defaultClass + "\">" ,
											"<a name=\"" + label + "\">",
											"<span class=\"hmlTitle\">",
											identifyingText,
											".</span> ",
											body,
											"</a></div>"
										  );
					return replacementText;
				}
			}
		);
		output.replace(body);
	}

	/** Create a section number.
	 * 
	 * @param topLevelHeading if not null, use this for the chapter number
	 * @param useLetters if true, then assume that sectionNumbers[1] is the Ascii code for a letter and print a letter
	 * @return
	 */
	public String assembleSectionNumber(String topLevelHeading, boolean useLetters )
	{	
		StringBuffer b = new StringBuffer();

		if( topLevelHeading != null )
			b.append( topLevelHeading );
		else if( useLetters )
			b.append( (char)sectionNumbers[1] );
		else
			b.append( sectionNumbers[1] );

		if( currentHeadingLevel > 1 )
			b.append( '.' );

		for( int i=2; i <= currentHeadingLevel; ++i )
		{	b.append( sectionNumbers[i] );
			if( i != currentHeadingLevel )
				b.append( '.'  	 );
		}

		return b.toString();
	}
	
	//======================================================================
	// TOC
	//======================================================================
	
	private int	 contentsTarget		= 0;			// used for table-of-contents anchor targets.
	private Text tableOfContents	= new Text() ;	// If not empty, we're building a table of contents.
	
	public Filter getTocReplacementFilter()
	{
		return new TocFilter();
	}
	
	private class TocFilter implements Filter
	{
		@Override public boolean isCodeBlockFilter(){ return false; } 
		@Override public boolean isSnippetFilter()	{ return false; } 
		@Override public boolean isTextFilter()		{ return true;  }
		
		@Override public void filter(Text prefix, Text body, Text suffix, BlockType type)
		{
			Text result = Tags.processElement( error, body, "toc", "hmlToc",
					new Tags.Handler() {
						@Override public Text handle( String tag, Map<String,String> arguments, String body, String surroundingContext, int start )
						{	
							Text argumentList = Util.removeUnwantedArgumentsAndReturnTheRest(arguments);
							
							if( tableOfContents.length() == 0 )
							{
								error.report( start, surroundingContext, "Requested table of contents is empty!" );
								return Text.EMPTY;
							}
							
							return new Text("",
								"<div" + argumentList + ">\n<div class=\"hmlTocTitle\">" , body , "</div>\n",
								tableOfContents,
								"</div>"
								);
						}
					}
				);
			body.replace(result);
		}
	}
	
	//======================================================================
	// Reference expansion.
	//======================================================================
	
	public Filter getReferenceExpander( final NoteSet endNotes )
	{
		return new FigureTableListingSectionNoteReference( endNotes );
	}
	
	private class FigureTableListingSectionNoteReference implements Filter
	{
		@Override public boolean isCodeBlockFilter(){ return true; } 
		@Override public boolean isSnippetFilter()	{ return false; } 
		@Override public boolean isTextFilter()		{ return true;  }
		
		private final NoteSet endNotes;
		public FigureTableListingSectionNoteReference( NoteSet endNotes )
		{	this.endNotes = endNotes;
		}
		
		private Pattern references = Pattern.compile
				// groups:   1                             		2             3                4  {
						("\\{(listing|figure|table|section|note)(-number)?\\s+([^\\s]*?)(?:\\s+([^\\}]+?))?\\s*\\}");
	
		@Override
		public void filter(Text prefix, Text body, Text suffix, BlockType inputType)
		{
			body.prefix( prefix );
			body.append( suffix );
			
			if( inputType == BlockType.SNIPPET )	// don't touch code snippets.
				return;
			
			StringBuffer processedInput		= new StringBuffer();
			Matcher		 referencesMatcher	= references.matcher( body.toString() );
	
			while( referencesMatcher.find() )
			{	
				Text replacementText = new Text();
	
				String 	type  		= referencesMatcher.group(1);				// figure, table, listing, section, note
				boolean numberOnly	= referencesMatcher.group(2) != null;		// -number
				String	label 		= referencesMatcher.group(3);
				String	visibleText = referencesMatcher.group(4);
				
				String  target			= "????";
				String  identifyingText = "????";
				{
					if( Character.toLowerCase(type.charAt(0)) == 'n' )	// it's a note
					{
						try
						{
							identifyingText = visibleText != null ? visibleText : (numberOnly ? "" : "Note ") + endNotes.getNoteMark(label);
							target          = endNotes.getNoteTarget(label);
						}
						catch( Exception e )
						{
							error.report("Can't find `<note label=\"%s\"> for {note %s}", label, label );
						}
					}
					else
					{
						if( visibleText != null && visibleText.length() > 0 )
							identifyingText = visibleText;
						else
							identifyingText = Titles.this.getIdentifierForLabel( label, type ); 
						
						if( identifyingText == null )
						{
							String tag = referencesMatcher.group(1).startsWith("s")
									 ? "<hN" : ("<" + referencesMatcher.group(1) ) ;
								
							error.report( "No %s label=\"%s\"> to match %s (or {ref...}). Missing title= in <include>?\n", tag, label, referencesMatcher.group(0) );
						}
						else if( numberOnly )
						{
							// TODO: Don't put it in and then take it out. Don't need first word in table at all!
							identifyingText = identifyingText.replaceAll( "([Ff]igure|[Tt]able|[Ll]isting|[Ss]ection)\\s+", "" );
						}
						target = label;
					}
				}
	
				replacementText.append( "<a href=\"#" + target + "\">" );
				replacementText.append( identifyingText );
				replacementText.append( "</a>" );
	
				referencesMatcher.appendReplacement( processedInput, replacementText.toString() );
			}
			
			referencesMatcher.appendTail(processedInput);
			body.replace( processedInput );
		}
	}
}