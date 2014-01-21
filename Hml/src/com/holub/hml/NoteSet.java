package com.holub.hml;

import java.util.*;

import com.holub.text.Text;

public class NoteSet
{
	private final List<EndNote>			endNotes 	 = new LinkedList<EndNote>();
	private final Map<String,EndNote>	notesByLabel = new HashMap<String,EndNote>();
	
	public NoteSet( Configuration config )
	{	// for now, just ignore the configuration
	}
	
	public EndNote add( String mark, String body, String label )
	{
		EndNote newNote = new EndNote(mark,body);
		if( endNotes.contains(newNote) )
			return null;

		endNotes.add( newNote );
		if( label != null && label.length() > 0 )
			notesByLabel.put(label, newNote);
		return newNote;
	}
	
	public int numberOfNotes()
	{	return endNotes.size();
	}
	
	/** 
	 * @param noteBlock
	 * @return false if there were no notes.
	 */
	public boolean appendNotesToBlock( Text noteBlock )
	{	
		if( endNotes.size() > 0 )
			for( EndNote note : endNotes )
				noteBlock.append(note);
		
		return endNotes.size() > 0;
	}
	
	public String getNoteTarget( String label )
	{	return notesByLabel.get(label).target();
	}
	
	public String getNoteMark( String label )
	{	return notesByLabel.get(label).mark();
	}
	
	public void clear()
	{	endNotes.clear();
	}
}
