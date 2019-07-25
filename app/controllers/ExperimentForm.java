package controllers;

import play.mvc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import play.data.validation.Constraints;

import models.*;


public class ExperimentForm {

    @Constraints.Required
    private String experimentID;
    
    private String queryName;

    @Constraints.Required
    private String geneList;
    
    private String colorSVG;
    
    private String colorBarplot;
    
    private List<String> tissueList;
    
    private String selectedTissue;

    public ExperimentForm() 
    {
    	
    	queryName = "your query";
    }
    
    public void setExperimentID(String _experimentID) {
        this.experimentID = _experimentID;
    }
    
    public String getExperimentID() {
    	return this.experimentID;
    }
        
    public String getColorSVG() {
		return colorSVG;
	}

	public void setColorSVG(String colorSVG) {
		this.colorSVG = colorSVG;
	}

	public String getColorBarplot() {
		return colorBarplot;
	}

	public void setColorBarplot(String colorBarplot) {
		this.colorBarplot = colorBarplot;
	}

	public String getSelectedTissue() {
		return selectedTissue;
	}

	public void setSelectedTissue(String selectedTissue) {
		this.selectedTissue = selectedTissue;
	}

	public String getGeneList() {
    	return this.geneList;
    }
    
    public List<String> getGeneListOfStrings() 
    {
    	List<String> genes = new ArrayList<String>();
    	
    	String csvRegEx = "(?:,\"|^\")(\"\"|[\\w\\W]*?)(?=\",|\"$)|(?:,(?!\")|^(?!\"))([^,]*?)(?=$|,)|(\\r\\n|\\n)";
    	
        Pattern p = Pattern.compile( csvRegEx );
        Matcher m = p.matcher( this.geneList );

        while ( m.find() ) 
        {
        	if ( m.group(1) == null ) {
        		genes.add( m.group(2) );
        	}
        	else {
        		genes.add( m.group(1) );
        	}
        }
      	
    	return genes;
    }

	public List<String> getTissueList() {
		return tissueList;
	}

	public void setTissueList(List<String> tissueList) {
		this.tissueList = tissueList;
	}

	public void setGeneList(String geneList) {
		this.geneList = geneList;
	}
	
    public String getQueryName() {
		return queryName;
	}

	public void setQueryName(String queryName) {
		this.queryName = queryName;
	}
}