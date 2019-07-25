package controllers;

import play.mvc.*;
import scala.util.parsing.json.JSONArray;
import play.data.Form;
import play.data.FormFactory;
import play.i18n.MessagesApi;
	
import play.api.libs.json.*;
import org.json.simple.JSONObject;
import com.google.gson.Gson;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;


import static play.libs.Scala.asScala;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.*;

import org.yaml.snakeyaml.Yaml;

import org.rosuda.REngine.*;
import org.rosuda.JRI.REXP;
import org.rosuda.JRI.Rengine;
import org.rosuda.JRI.RVector;
import org.rosuda.JRI.RList;


import com.typesafe.config.Config;
import play.mvc.Controller;
import javax.inject.Inject;

import models.*;

@Singleton
public class TotemBrowser extends Controller 
{

	private final Form<ExperimentForm> formSelExp;
	private MessagesApi messagesApi;	
	private final List<ExperimentData> listExperiments;
	private final String mapStrDesc;
	private final String mapStrImg;
	private REngine eng	= null;
	private final Config config;					// get configuration params from application.conf
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private List<String> niceColors;			// string of colors names from R
	private List<String> tissuesList;
	private List<String> filteredGeneList;
	private Path tempFolder;						// temporary folder to store images
	private ExperimentForm defaultFormValues; 		// default values for input form
		
	@Inject
	public TotemBrowser(FormFactory formFactory, MessagesApi messagesApi, Config config) 
	{
		this.formSelExp 		= formFactory.form(ExperimentForm.class);
		this.messagesApi 		= messagesApi;
		this.listExperiments	= com.google.common.collect.Lists.newArrayList();
		this.eng 				= null;
		this.config			 	= config;
		this.tissuesList 		= new ArrayList<String>();
    	this.filteredGeneList 	= new ArrayList<String>();
    	this.niceColors 		= new ArrayList<String>();
		
        // Foreach directory
        /// recover experiment data and create object with info
        
		Yaml yaml 		= new Yaml();
        File directory 	= new File("conf/experiments");
        File[] dirList 	= directory.listFiles();
        for (File file : dirList)
        {
        	String experiment_id = file.getName();
        	System.out.println("Found: " + file.getName() + " lets try.");
        	
        	if( experiment_id.equals(".DS_Store")) {
        		continue;
        	}
        	
        	InputStream inputStream = this.getClass()
        			.getClassLoader()
        			.getResourceAsStream("experiments/" + experiment_id.toString() + "/experiment_config.yml");
        	
        	Map<String, Map<String,String>> econfs = (Map<String, Map<String,String>>) yaml.load(inputStream);
        	
        	for(String key : econfs.keySet()) // it should iterate only once
        	{
        		System.out.println("key = " + key);
        		Map<String,String> econf = econfs.get(key);
        		
        		listExperiments.add( new ExperimentData( 
        				econf.get("experiment_id"),
        				econf.get("experiment_name"),
        				econf.get("experiment_description"),
        				econf.get("experiment_datafile"),
        				econf.get("experiment_svgfile"),
        				config.getString("svg2.color.final.default"),
        				config.getString("svg2.color.final.default")
        				));
        		
        	}
            System.out.println("ExpID: " + file.getName() + " retrieved.");
        }
                
        JSONObject mapRawDesc = new JSONObject();
        JSONObject mapRawImg = new JSONObject();
    	StringWriter outDesc = new StringWriter();
    	StringWriter outImg = new StringWriter();
    	
    	
    	for( ExperimentData exp : this.listExperiments) {
    		mapRawDesc.put( exp.experimentID, exp.experimentDesc );
    		mapRawImg.put(  exp.experimentID, exp.experimentSVGfile );
    	}
    
    	try {
    		  mapRawDesc.writeJSONString(outDesc);  
    		  mapRawImg.writeJSONString(outImg);  
    	}
    	catch( java.io.IOException e ) {
    		  System.out.println("Booooom: " + e.toString() );
    	}
    	mapStrDesc = outDesc.toString();
    	mapStrImg = outImg.toString();
    	
    	try {
	    	// create temp folder to store images
	    	tempFolder 		= Files.createTempDirectory("svg2tmp.");    	
		}
		catch( java.io.IOException e ) {
			System.out.println("SVG2Browser Booooom1: " + e.toString() );
		}
    	
    	// we load the first experiment, that should include the nice colors
    	this.startR();
    	this.loadRExperimentData( listExperiments.get(0).experimentID );    	
    	niceColors = Arrays.asList( this.evalR("nice_colors") ); 
    	//this.closeR();
      	
    	defaultFormValues = new ExperimentForm();
    	defaultFormValues.setQueryName("your query");
    	
        // https://www.playframework.com/documentation/2.7.x/api/scala/views/html/helper/index.html    	
	}
	

    public Result index() 
    {	    
    	return ok(views.html.index.render() );
    }

    /**
     * 
     * @param request
     * @return
     */
    public Result inputSelection( Http.Request request ) 
    {    	

        return ok(views.html.inputSelection.render( 
        		asScala(listExperiments), 
        		formSelExp.fill( defaultFormValues ), 
        		mapStrDesc, 
        		mapStrImg,
    			request, messagesApi.preferred(request) ));
    }

    /**
     * 
     * @param request
     * @return
     */
    public Result aboutTotemBrowser( Http.Request request ) {
        return ok(views.html.about.render());
    }
    
    
    /**
     * Given an experiment ID it returns a list of genes (strings) encoded as JSON
     * 
     * @param request
     * @return
     */
    //@ BodyParser.Of(Json.class)
    public Result getExampleGenes( String _expID ) 
    {
    	Gson gson = new Gson();
    	List<String> geneList = new ArrayList<String>();
    
    	
    	ExperimentData exp = this.getExperimentData( _expID );
    	if( exp != null ) 
		{	
    		this.loadRExperimentData( exp.experimentID );
	    	
	    	String s[] = this.evalR( "paste(example,collapse=\",\")" );
	    	for( String gene: s) {
	    		geneList.add( gene );
	    	}	    	
		}
    
    	return ok( gson.toJson(geneList) );
    }
    
    /**
     * 
     * @param _expID
     * @param _tissue
     * @return
     */
    public Result getGenesFromTissue( String _expID, String _tissue ) 
    {
    	Gson gson = new Gson();
    	List<String> geneList = new ArrayList<String>();
    
    	
    	ExperimentData exp = this.getExperimentData( _expID );
    	if( exp != null ) 
		{	
    		this.loadRExperimentData( exp.experimentID );

    		this.evalR( "mygenelist <- as.character( read.table(\"" + this.tempFolder.toString() + "/gene_list.txt\")[[1]] )" );

    		String s[] = this.evalR( "finder( mygenelist, \"" + _tissue + "\", " + _expID + ", niceprint=F)" );

	    	for( String gene: s) {
	    		geneList.add( gene );
	    	}			
		}
    	
    	if( geneList.size() == 0 ){
    		geneList.add("No genes detected");
    	}
    
    	return ok( gson.toJson(geneList) );
    }
    
    /**
     * 
     * @param _expID
     * @return
     */
    public Result getGenesFromNoTissue( String _expID ) 
    {
    	Gson gson = new Gson();
    	List<String> geneList = new ArrayList<String>();
    	
    	ExperimentData exp = this.getExperimentData( _expID );

    	if( exp != null ) 
		{	
    		this.loadRExperimentData( exp.experimentID );
	    	
    		this.evalR( "mygenelist <- as.character( read.table(\"" + this.tempFolder.toString() + "/gene_list.txt\")[[1]] )" );
    		
            String s[] = this.evalR( "notenriched( mygenelist )" );
	    	
            for( String gene: s) {
	    		geneList.add( gene );
	    	}			    	
		}
    	
    	if( geneList.size() == 0 ){
    		geneList.add("No genes detected");
    	}
    
    	return ok( gson.toJson(geneList) );
    }
    
    /**
     * 
     * @param _expID
     * @return
     */
    public Result getGenesNotFound( String _expID ) 
    {
    	Gson gson = new Gson();
    	List<String> geneList = new ArrayList<String>();
    	
    	ExperimentData exp = this.getExperimentData( _expID );

    	if( exp != null ) 
		{	
    		this.loadRExperimentData( exp.experimentID );
	    	
    		this.evalR( "mygenelist <- as.character( read.table(\"" + this.tempFolder.toString() + "/gene_list.txt\")[[1]] )" );
    		
            String s[] = this.evalR( "notfound( mygenelist )" );
	    	for( String gene: s) {
	    		geneList.add( gene );
	    	}
		}
    	
    	if( geneList.size() == 0 ){
    		geneList.add("No genes detected");
    	}
    
    	return ok( gson.toJson(geneList) );
    }
        
    /**
     * Called by input form. It generates the results produced with R.
     * 
     * @param request
     * @return
     */
    public Result generateResults( Http.Request request ) 
    {    	
    	final Form<ExperimentForm> inExpDataForm = formSelExp.bindFromRequest(request);

        if( inExpDataForm.hasErrors() ) 
        {
            logger.error("errors = {}", inExpDataForm.errors());

            return badRequest(
            		views.html.inputSelection.render( 
            				asScala( listExperiments ), 
            				formSelExp.fill( defaultFormValues ), 
            				mapStrDesc, 
            				mapStrImg,
            				request, messagesApi.preferred(request) ));
        } 
        else 
        {
        	ExperimentForm expData 	= inExpDataForm.get();
        	List<String> geneList	= expData.getGeneListOfStrings();        		
        	
        	try {
	        	// write down to a file the list of genes provided by the user
	        	FileWriter fw = new FileWriter( tempFolder.toString() + "/gene_list.txt" );
	        	for( String gene: geneList ) {
	        		fw.write( gene + "\n");
	        	}   
	        	fw.close();
        	}
        	catch( java.io.IOException e ) {
    			System.out.println("generateResults Booooom1: " + e.toString() );
    		}
        	
        	
        	this.loadRExperimentData( expData.getExperimentID() );

        	// this is just to keep internal functions easy to maintain. Memory wasted is not a big deal here.
        	this.evalR( "experiment_id <- " + expData.getExperimentID() );
			
    		// R: list of tissues : names( calcEnrichment )
    		// <- String s[] = eng.parseAndEval("c('foo', NA, 'NA')").asStrings();	    
    		String tissuesArray[] = this.evalR( "names( " + expData.getExperimentID() +  " )");
        		
		    // R: list of genes, given a tissue: finder( geneList:charvec, tissue:char )		    	
		    // TODO: Check if genelist & selected Tissue are empty
		    //this.eng.assign( "mygenelist", geneList.toArray(new String[0]) );        		
        	this.evalR( "mygenelist <- names( " + expData.getExperimentID() +  " )");
        		
	    	String inputcmd = null;
	    	if( expData.getSelectedTissue() == null ) {
	    		inputcmd = "finder( mygenelist, NULL, " + expData.getExperimentID() + "  )";
	    	} else {
	    		inputcmd = "finder( mygenelist, \"" + expData.getSelectedTissue() + "\", " + expData.getExperimentID() + "  )";
	    	}

	    	String filteredGeneArray[] = this.evalR( inputcmd );
	    	//	this.eng.parseAndEval("finder( mygenelist, \"" + expData.getSelectedTissue() + "\", " + expData.getExperimentID() + "  )" ).asStrings();    			    		    	
	    	
	    	this.tissuesList 		= Arrays.asList( tissuesArray );
	    	this.filteredGeneList 	= Arrays.asList( filteredGeneArray );	    	
       	        	
        	expData.setColorSVG(		config.getString("svg2.color.final.default") );
        	expData.setColorBarplot( 	config.getString("svg2.color.final.default") );
        	
        	return ok( views.html.showResults.render( 
		        			expData.getExperimentID(),
		        			expData.getQueryName(),
		        			expData.getColorSVG(),
		        			expData.getColorBarplot(),
		        			inExpDataForm,
		        			asScala(this.niceColors),
		        			asScala(this.tissuesList),
		        			asScala(this.filteredGeneList),
		        			request, messagesApi.preferred(request) ));
		}
        
    }
    
  
	/**
	 * 
	 * @param _experimentID
	 * @param _newColor
	 * @return
	 */
    public Result getImageSVG( String _experimentID, String _newColor ) 
    {
    	// previously gene_list must be generated. You are expected to call this after generateResults from the web page
    	
    	// Update new color in experiment and modify file
    	   	
    	ExperimentData exp = this.getExperimentData(_experimentID);
		if( exp != null ) {
			exp.setExperimentColorSVG(_newColor);
		}
		
    	makeOutputSVG( _experimentID, _newColor, this.tempFolder.toString() ); 
	
		return ok( new File( this.tempFolder.toString() + "/SVG.png" ) );
	}
    
    /**
     * 
     * @param _newColor
     * @return
     */
    public Result getImageBarplot( String _experimentID, String _newColor ) 
    {
    	// previously gene_list must be generated. You are expected to call this after generateResults from the web page
    	
    	ExperimentData exp = this.getExperimentData(_experimentID);
		if( exp != null ) {
			exp.setExperimentColorBarplot(_newColor);
		}
    	
    	makeOutputBarplot( _experimentID, _newColor ); 
	
		return ok( new File( this.tempFolder.toString() + "/barplot.png" ) );
    }
    
    /**
     * 
     * @param request
     * @return
     */
    public Result updateOutputExperiment( Http.Request request ) 
    {
    	// previously gene_list must be generated. You are expected to call this after generateResults from the web page
    	final Form<ExperimentForm> boundForm = this.formSelExp.bindFromRequest(request);

        if (boundForm.hasErrors()) {
            logger.error("errors = {}", boundForm.errors());
            return badRequest(views.html.inputSelection.render( 
            		asScala(listExperiments), 
            		formSelExp.fill( defaultFormValues ), 
            		mapStrDesc, 
            		mapStrImg,
        			request, messagesApi.preferred(request) ));
        } 
        else {
        	// subir a poner los colores
        	ExperimentForm expData 	= boundForm.get();
        	ExperimentData exp = this.getExperimentData( expData.getExperimentID() );
        	
        	if( exp != null ) 
        	{    		
        		return ok(views.html.showResults.render( 
        			exp.getExperimentID(),
        			expData.getQueryName(),
        			exp.getExperimentColorSVG(),
        			exp.getExperimentColorBarplot(),
        			formSelExp,        			
        			asScala(this.niceColors),
        			asScala(this.tissuesList),
        			asScala(this.filteredGeneList),
        			request, messagesApi.preferred(request) ));
        	} 
        	else {
        		return badRequest(views.html.inputSelection.render( 
        				asScala(listExperiments), 
        				formSelExp.fill( defaultFormValues ), 
        				mapStrDesc, 
        				mapStrImg,
            			request, messagesApi.preferred(request) ));
        	}
        }
    }
    
    /**
     * 
     * @param _experimentID
     * @param _color
     * @param _tempFolder
     * @return
     */
    public Boolean makeOutputSVG( String _experimentID, String _color, String _tempFolder ) 
    {
		List<String> cmdArray = new ArrayList<String>();
		List<String> envArray = new ArrayList<String>();
		
		cmdArray.add( config.getString("javahome.path") + "/bin/java" );
		cmdArray.add( "-Djava.library.path=" + config.getString("java.library.JRI") );
		
		String fileSVG 		= "";
		String fileRData 	= "";
		
		ExperimentData exp = this.getExperimentData(_experimentID);
		if( exp != null ) {			
			fileSVG = exp.getExperimentSVGfile();
			fileRData = exp.getExperimentDatafile();
		}
		
		cmdArray.add( "-jar" );
		cmdArray.add( config.getString("svgmap-cli.jar.path") );
		cmdArray.add( "-S" );
		cmdArray.add( config.getString("experiments.path") + "/" + _experimentID + "/" + fileSVG );
		cmdArray.add( "-R" );
		cmdArray.add( config.getString("experiments.path") + "/" + _experimentID + "/" + fileRData );
		cmdArray.add( "-K" );
		cmdArray.add( _tempFolder + "/gene_list.txt" );
		cmdArray.add( "-D" );
		cmdArray.add( tempFolder + "/enrichment_output.tsv" );
		cmdArray.add( "-CF" );
		cmdArray.add( _color );
		cmdArray.add( "-C" );
		cmdArray.add( "3" );
		cmdArray.add( "-P" );
		cmdArray.add( "-O" );
		cmdArray.add( _tempFolder + "/SVG.png" );  
		
		envArray.add( "JAVA_HOME=" 	+ config.getString("javahome.path") );
		envArray.add( "R_HOME=" 	+ config.getString("R.home.path"));
		envArray.add( "PATH=" 		+ config.getString("R.home.path") + "/bin/:" + System.getenv("PATH") );
// REMOVE	
		for( String cmd: cmdArray ) {
			System.out.println("CMD: " + cmd );
		}
		
		for( String cmd: envArray ) {
			System.out.println("ENV: " + cmd );
		}
		
		
		try {
						
			//String [] comandos = (String[]) cmdArray.toArray();
			Process p = Runtime.getRuntime().exec( 
					(String []) cmdArray.toArray( new String[0]), 
					(String []) envArray.toArray( new String[0]) );			
			
			// any error message?
            StreamGobbler errorGobbler = new 
                StreamGobbler(p.getErrorStream(), "ERROR");            
            
            // any output?
            StreamGobbler outputGobbler = new 
                StreamGobbler(p.getInputStream(), "OUTPUT");
                
            // kick them off
            errorGobbler.start();
            outputGobbler.start();
                                    
            // any error???
            int exitVal = p.waitFor();
            System.out.println("ExitValue: " + exitVal);
			
		 	//p.waitFor();
		}
		catch( java.io.IOException e ) {
			System.out.println("makeOutputSVG Booooom1: " + e.toString() );
		}
		catch( java.lang.InterruptedException e ){
			System.out.println("makeOutputSVG Booooom2: " + e.toString() );
		}
		catch (Throwable t)
        {
          t.printStackTrace();
        }
		
    	return true;
    }
   
    
    /**
     * 
     * @param _barColor
     * @param _tempFolder
     * @return
     */
    public Boolean makeOutputBarplot( String _experimentID, String _barColor ) 
    {
    	// R:barplot
    	// Recover list of genes from input (mygenelist), and recalc drawing_vector(), to get data to feed to barplot
    	// "calcEnrichment <- drawing_vector( mygenelist );"; 
    	// mybarplot( myvec, "red", "barplot.png" )
    	// mybarplot( drawing_vector( mygenelist ), "red", "barplot.png" )
    	
    	this.loadRExperimentData( _experimentID );
    	 
    	//this.eng.assign( "mygenelist", _mygenelist.toArray(new String[0]) );    		
    	String outputFile = this.tempFolder.toString() + "/barplot.png" ; 
			   
    	// 		String rline =  "mygenelist <- as.character( read.table(\"" + tempFolder + "/gene_list.txt\")[[1]] )";
    	// 		System.out.println("rline : "+ rline );
        this.evalR( "mygenelist <- as.character( read.table(\"" + this.tempFolder.toString() + "/gene_list.txt\")[[1]] )" );
    	this.evalR( "mybarplot( enrichment( mygenelist ), \"" + _barColor + "\", \""+ outputFile +"\" )" );

    	return true;
    }
    
    
    public ExperimentData getExperimentData( String _experimentId ) 
    {
    	if( listExperiments != null ) {
	    	for( ExperimentData exp: this.listExperiments ) {
	    		if( exp.getExperimentID().equals(_experimentId)) {
	    			return exp;
	    		}
	    	}
    	}
    	
    	return null;
    }
    
    
    public Boolean loadRExperimentData( String _expID )
    {
    	if( this.eng == null ) { //someone forgot to close after finished
    		return Boolean.FALSE;	
    	}
    	
    	try 
    	{
    		ExperimentData exp = this.getExperimentData( _expID );
    		if( exp == null ) {
    			return Boolean.FALSE;
    		}    		
    		
    		System.out.println( "Cargando: " + 
    			this.evalR( "load(\"conf/experiments/" + _expID + "/" + exp.getExperimentDatafile() + "\")")  );
    	}		
    	catch( Exception e ) {
    		System.out.println("loadRExperimentData. Error trying to retrieve data from Experiment ID: "+ _expID + "\n Booooom: " + e.toString() );
    		return Boolean.FALSE;
    	}
    	
    	return Boolean.TRUE;
    }
    
    
    /**
     * 
     * @return
     */
    
    public Boolean startR( ) 
    {
    	if( this.eng != null ) { //someone forgot to close after finished
    		this.closeR();    		
    	}
    	
    	// Start R engine
    	try {
    		// https://github.com/s-u/REngine/blob/master/JRI/test/RTest.java
    		this.eng = REngine.engineForClass("org.rosuda.REngine.JRI.JRIEngine", 
    					new String[] { "--vanilla", "--no-save" }, 
    					new REngineStdOutput(), 
    					false);
    		
    	}		
    	catch( Exception e ) {
    		System.out.println("startR. Error trying to start R: " + e.toString() );
    		return Boolean.FALSE;
    	}
    	
    	return Boolean.TRUE;
    }
    
    /**
     * 
     * @param _evalstring
     * @return
     */
    public String[] evalR( String _evalstring ) 
    {
    	if( this.eng == null ) {
    		// TODO: Generate a nice error message
    		return null;
    	}
    	
    	try {    		
    		return( this.eng.parseAndEval( _evalstring ).asStrings() );    	        	
    	}		
    	catch( Exception e ) {
    		System.out.println( "evalR "+
    							" When evaluating: " + _evalstring + "\n" +
    							" Failure: " + e.toString() );
    	}
    	return null;
    }
    
    /**
     * 
     * @return
     */
    public Boolean closeR() 
    {
    	if( this.eng != null ) 
    	{
    		this.eng.close();    	
    		this.eng = null;
    	}
    	
    	return Boolean.TRUE;
    }
}

class StreamGobbler extends Thread
{
    InputStream is;
    String type;
    
    StreamGobbler(InputStream is, String type)
    {
        this.is = is;
        this.type = type;
    }
    
    public void run()
    {
        try
        {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line=null;
            while ( (line = br.readLine()) != null)
                System.out.println(type + ">" + line);    
            } catch (IOException ioe)
              {
                ioe.printStackTrace();  
              }
    }
}
