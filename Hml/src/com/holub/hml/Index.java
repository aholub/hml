package com.holub.hml;

import java.util.*;

import com.holub.text.Text;
import com.holub.util.IdGenerator;

public class Index
{
	TreeMap<Topic,Topic> topics = new TreeMap<Topic,Topic>();
	
	/** Create an index entry.
	 * 
	 * @param topicName
	 * @param linkText 	null for automatically generated (a number).
	 * @return
	 */
	public Text getAnchorForTopic( String topicName, String subtopic )
	{	
		Topic template	= new Topic( topicName );
		Topic topic 	= topics.get(template);
		if( topic == null )
		{
			topics.put( template, template );
			topic = template;
		}
		return topic.getNewTarget(subtopic);
	}
	
	public Text getIndex( String title, Text argumentList )
	{
		Text index = new Text();
		index.appendf("<div%s\">\n%s\n", argumentList, title.trim() );
		index.append ("<div class=\"hmlTopics\">\n");
		for( Topic t : topics.values() )
			index.appendln( t.getIndexEntry() );
		index.append("</div></div>");
		return index;
	}
	
	/** A topic. Topics are sorted by putting them in a tree, and the
	 *  {@link #compareTo(Topic)} implementation that makes that
	 *  possible does a non-case-sensitive sort, where all
	 *  non-alphabet topic names sort to the head of the list.
	 * @author allen
	 *
	 */
	private static class Topic implements Comparable<Topic>
	{
		private int	 		  locationTag = 0;
		private List<Text>	  locations = new ArrayList<Text>();
		private final String  topic;
		private final boolean isAlnum;	// first character of topic is alphanumeric (needed for sorting)
		private 	  String  compare;	// this string is used for comparison. lower case with plural suffix removed.
		private final String  topicId = IdGenerator.getId("");
		
		public Topic( String topic )
		{	this.topic	 = topic;
			this.isAlnum = Character.isLetterOrDigit( topic.charAt(0) );
			this.compare = topic.toLowerCase();
			
			if( topic.length() > 2 )
			{	if( compare.endsWith("es") )
					compare = compare.substring(0, compare.length() - 2);
				else if( compare.endsWith("s") )
					compare = compare.substring(0, compare.length() - 1);
			}
		}
		
		/** Get an anchor to use at a new location, and add that anchor to the list
		 *  of locations for the current topic. 
		 * @return A String holding the entire HTML for the (invisible) anchor.
		 */
		public Text getNewTarget( String subtopic )
		{
			++locationTag;
			
			String anchorId = String.format("hmlIndex-%s-%d", topicId, locationTag );
			Text anchor 	= new Text("<a name=\"%s\"></a>", anchorId );
			Text indexEntry = new Text("<a class=\"hmlTopicLocation\" href=\"#%s\">%s</a>", 
										anchorId, 
										(subtopic==null || subtopic.trim().length()==0)? String.valueOf(locationTag) : subtopic );
			locations.add(indexEntry);
			return anchor;
		}
		
		public Text getIndexEntry()
		{
			Text indexEntry = new Text(
				"<div class=\"hmlTopicGroup\" id=\"%s\">\n"+
				"	<div class=\"hmlTopic\">%s</div>\n"+
				"	<div class=\"hmlTopicLocationGroup\">\n", topicId, topic );
			indexEntry.concat(",\n" , locations ); 
			indexEntry.concat("\n",
				"",
				"	</div>",
				"</div>"
			);
			return indexEntry;
		}
		
		@Override public boolean equals(Object o)
		{	return (o instanceof Topic ) && ((Topic)o).compare.equals(compare);
		}
		
		@Override public int hashCode()
		{	return topic.hashCode();
		}
		
		@Override public int compareTo(Topic arg)
		{
			return
				(!isAlnum && arg.isAlnum ) ? -1 :
				( isAlnum && !arg.isAlnum) ?  1 : compare.compareTo( arg.compare );
		}
	}
}
