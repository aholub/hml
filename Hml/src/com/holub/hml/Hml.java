package com.holub.hml;

import com.holub.text.ReportingStream;
import com.holub.text.Text;
import com.holub.util.ExtendedLogger;
import com.holub.util.Places;

import java.io.FileWriter;
import java.io.IOException;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Calendar;
import java.util.Date;

public class Hml
{	
	private static ExtendedLogger log = ExtendedLogger.getLogger(Hml.class);
	
	private static final String USAGE =
			new Text("\n",
					"Usage: java [-DCONFIG=/path/to/config/directory] com.holub.hml.Hml [-o outputFile] [--out outputFile] [files...]",
					"",
					"Process the listed files (or take input from standard input",
					"if no files are listed). Send processed input to standard output or to the file",
					"specified by most recent -o or --out.",
					"",
					"The -DCONFIG flag, if present, lets you specify a location for custom configuration files.",
					"",
					"This program (c)2013, Allen I Holub. Permission is granted to use this program",
					"for personal use only. There are no restrictions on distributing the output of this program."
				).toString();
	
	//----------------------------------------------------------------------
	/** Was invoked from the command line (via main()) */
	
	private final Configuration		config;
	private final ReportingStream	error;
	private final Printer 			outputPrinter;
	
	private final NoteSet   		endNotes;
	
	private	final Macro  			macroManager;
	private	final Filter    		include;
	private	final Tags      		tags;
	private final Filter			codeSnippets;
	private	final Listing   		listing;
	private final Filter			unmapEntities;
	private	final Titles    		title;
	
	//----------------------------------------------------------------------
	/** Create an Hml processor that writes to the indicated defaultOutput
	 *  writer and writes errors on the specified error writer.
	 * @param defaultOutput The initial output writer. This writer will
	 * 				can be replaced by calling {@link #openNewOutputStream(String)}.
	 * @param errors all error messages are sent here.
	 */
	public Hml( Writer defaultOutput, Writer errorWriter )
	{
		this.outputPrinter	= new Printer(defaultOutput);
		this.error			= new ReportingStream(errorWriter);
		
		this.config 		= new Configuration ( error	 );
		endNotes			= new NoteSet		( config );
		macroManager		= new Macro			( config );
		include				= new Include		( config );
		tags				= new Tags			( config, endNotes );
		codeSnippets		= new CodeSnippets	( config );
		listing				= new Listing		( config );
		unmapEntities		= new EntityUnmapper( config );
		title	  			= new Titles		( config );
	}
	
	//----------------------------------------------------------------------
	public static void main( String[] args ) throws Exception
	{	
		Calendar c = Calendar.getInstance();
		new Text("HML 2.01 (c) %s, Allen Holub. [compiled %s]\n", c.get(Calendar.YEAR), new Date().toString() ).write(System.err);
		new Text("Please download the most recent version of this program from handymarkup.org rather than redistributing it\n").write(System.err);
		
		StringWriter errors = new StringWriter();
		
		Hml processor = new Hml( new OutputStreamWriter(System.out), errors );
		
		int errorCount = doMain(processor, args);
		if( errorCount > 0 )
			System.err.println(errors.toString());
		
		new Text ("%s errors\n", errorCount ).write(System.err);
		System.exit( errorCount );
	}
	
	/** The integration tests call doMain() instead of main() in order to
	 *  avoid the System.exit() call in main(). System.exit(...) terminates
	 *  the Eclipse debugger abruptly. It also logs an error if any exceptions are
	 *  encountered.
	 *  @return the error count or -1 if an exception was caught, -2 if there's
	 *  		a missing output file name, -3 if an unknown command-line
	 *  		argument is encountered. Reports error messages for
	 *  		all of these conditions.
	 */
	
	public static int doMain( Hml processor, String[] args ) throws Exception
	{	
		try
		{
			String fileName = null;
			
			for( int i = 0; i < args.length; ++i )
			{	
				String argument = args[i].trim();
				
				if( argument.equals("-o") ||  argument.equals("--out") )
				{
					if( ++i >= args.length )
					{	
						processor.reportError( "Missing filename for -o or --out.\n%s", USAGE );
						return -2;
					}
					
					argument = args[i];
					if( fileName != null )						// Have processed some input,
						processor.closeCurrentOutputStream();	// so a close is required.
					processor.openNewOutputStream( argument );
				}
				else if( argument.startsWith("-") )
				{	
					processor.reportError("Unknown command-line argument: %s\n%s", argument, USAGE );
					return -3;
				}
				else
				{	
					fileName = argument;
					Text t = new Text(new FileReader(argument));
					processor.expandAndPrint( t );
				}
			}
			
			if( fileName == null ) // Then no input file was specified in the argument list.  Use standard input.
			{
				fileName = "standard input";
				Text t = new Text( System.in );
				processor.expandAndPrint(t);
			}
	
			processor.closeCurrentOutputStream();
			return processor.getErrorCount();
		}
		catch( Exception e )
		{
			log.error( "Uncaught exception in main", e );
			new Text( "%s\n", e.getMessage() ).write(System.err);
			return 1;
		}
	}
	
	//----------------------------------------------------------------------
	/** This method is for use by non command-line based systems (servlets?).
	 * @param input		The HML input
	 * @param output	the .html output is sent to this writer
	 * @param errors	error messages are sent to this writer.
	 * @return			the error count or -1 if an unexpected exception was caught.
	 */
	public static int processInWebContext( String input, Writer output, Writer errors )
	{
		try
		{
			Hml	 	processor	  = new Hml	( output, errors );
			Text 	content		  = new Text( input );
			int  	errorCount 	  = processor.expandAndPrint( content );
			
			processor.closeCurrentOutputStream();
			return errorCount;
		}
		catch( Exception e )
		{
			log.error( "Uncaught exception in main", e );
			new Text( "%s", e.getMessage() ).write(errors);
		}
		return -1;
	}
	
	//----------------------------------------------------------------------
	// Highest-level methods, called from main() and processInWebContext(...)
	//
	
	/** Close the current output stream, flushing all buffered output
	 *  as necessary. 
	 * @throws IOException
	 */
	private void closeCurrentOutputStream() throws IOException
	{	outputPrinter.close();
	}
	
	/** Open a new output stream. If any output has been written to the current
	 *  output stream, you should call {@link #closeCurrentOutputStream()} before
	 *  calling this method.
	 *  
	 * @param fileName
	 * @throws IOException
	 */
	private void openNewOutputStream( String fileName ) throws IOException
	{	outputPrinter.open(fileName) ;	
	}
	
	private void reportError( String format, Object... args )
	{	error.report( format, args );
	}
	
	private int getErrorCount()
	{	return error.getErrorCount();
	}
	
	/** Expand the specified text and print it to the current output
	 *  stream. The head and tail files are added as required.
	 * 
	 * @param t
	 * @return
	 * @throws IOException
	 */
	private int expandAndPrint( Text t ) throws IOException
	{
		int errorCount = expand(t);
		outputPrinter.print(t);
		return errorCount;
	}
	//----------------------------------------------------------------------
		
	/** This method runs all the HML tags, macros, etc. It does not
	 *  wrap head and tail files around the processed input, however.
	 *
	 *  @return the error count after processing.
	 */
	public int expand( Text input )
	{	
		// TODO. Change {section ...}, etc. to macros that use special replacement variables.
		// Need to process macros much later in the chain if we do that, but moving the macro processing
		// introduces a bunch of test errors that I don't have time to deal with.
		
		if( new Pass(config, include 							 ).process(input))
		if( new Pass(config, config 							 ).process(input))
		if( new Pass(config, codeSnippets						 ).process(input))
		if( new Pass(config, macroManager.getTextFilter()		 ).process(input))
		if( new Pass(config, tags								 ).process(input))
		if( new Pass(config, listing							 ).process(input))
		if( new Pass(config, macroManager.getCodeFilter()		 ).process(input))
		if( new Pass(config, title								 ).process(input))
		if( new Pass(config, macroManager.getRefFilter()		 ).process(input))
		if( new Pass(config, listing.getReferenceExpander()		 ).process(input))
		if( new Pass(config, title.getReferenceExpander(endNotes)).process(input))
		if( new Pass(config, title.getTocReplacementFilter()	 ).process(input))
		    new Pass(config, unmapEntities						 ).process(input);
		
		return error.getErrorCount();
	}
	
	//----------------------------------------------------------------------
	private class Printer
	{
		private Writer	output;
		
		public Printer( Writer output )
		{	this.output = output;
		}
			
		// The following two calls are mocked in IntegreationTests.java. If you change
		// them, change the test.
		//
		private Reader head = Places.CONFIG.reader("hml.head");
		private Reader tail = Places.CONFIG.reader("hml.tail");
		
		private Text   tailContents = null;
		private Text   headContents = null;
		private Text   contents		= new Text();
		
		/** Outputs the specified processed content to the current
		 *  output file.
		 *  The hml.head file is output before the content,
		 *  only if the head has not been output since the last
		 *  close() call.
		 *  
		 *  For this method to work properly, you can't
		 *  output anything until all of the hml tags have been
		 *  processed (because some of the tags effectively
		 *  modify hml.head).
		 *  
		 * @param content
		 * @throws IOException
		 */
		public void print( Text content ) throws IOException
		{
			if( output == null )
				throw new IOException("No output file currently active");
			contents.append(content);
		}
		
		/** Finishes up processing by flushing the buffer and outputting the tail file. You
		 *  should only call this method once, at the end of processing. Use {@link #open(String)} to
		 *  change output files.
		 */
		public void close() throws IOException
		{
			if( output == null )	// nothing to do
				return;
			
			if( head == null )
				error.report( "ERROR: Cannot locate hml.head" );
			else
			{
				if( headContents == null )
					headContents = new Text(head);
					
				Text augmentedHead = tags.appendAdditionsToHead( headContents );
				augmentedHead.write(output);
			}
			
			contents.write(output);
			contents.clear();
			
			if( tail == null )
				error.report( "ERROR: Cannot locate hml.tail." );
			else
			{
				if( tailContents == null )
					tailContents = new Text(tail);
					
				tailContents.write(output);
				output.flush();
			}

			output.close();
			output = null;
		}
		
		/** Close the current output and open a new one 
		 * @throws IOException
		 */
		public void open( String fileName ) throws IOException
		{	output = new FileWriter( fileName );
		}
	}
}