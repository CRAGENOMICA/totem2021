package models;

public class ExperimentData {
	
    public String experimentID;
    public String experimentName;
    public String experimentDesc;
    public String experimentDatafile;
    public String experimentSVGfile;
    public String experimentColorSVG;
    public String experimentColorBarplot;
    
    
    public ExperimentData(  String _expID, 
    						String _expName, 
    						String _expDesc, 
    						String _expDatafile, 
    						String _expSVGfile, 
    						String _colorSVG, 
    						String _colorBarplot ) 
    {
    	
        this.experimentID 			= _expID;
        this.experimentName 		= _expName;
        this.experimentDesc 		= _expDesc;
        this.experimentDatafile 	= _expDatafile;
        this.experimentSVGfile 		= _expSVGfile;
        this.experimentColorSVG		= _colorSVG;
        this.experimentColorBarplot = _colorBarplot;
    }
    
    
    //getter and setters
    public String getExperimentID() {
    	return this.experimentID;
    }
    
    public String getExperimentDesc() {
    	return this.experimentDesc;
    }


	public String getExperimentDatafile() {
		return experimentDatafile;
	}


	public void setExperimentDatafile(String experimentDatafile) {
		this.experimentDatafile = experimentDatafile;
	}


	public String getExperimentSVGfile() {
		return experimentSVGfile;
	}


	public void setExperimentSVGfile(String experimentSVGfile) {
		this.experimentSVGfile = experimentSVGfile;
	}


	public String getExperimentColorSVG() {
		return experimentColorSVG;
	}


	public void setExperimentColorSVG(String experimentColorSVG) {
		this.experimentColorSVG = experimentColorSVG;
	}


	public String getExperimentColorBarplot() {
		return experimentColorBarplot;
	}


	public void setExperimentColorBarplot(String experimentColorBarplot) {
		this.experimentColorBarplot = experimentColorBarplot;
	}
  
}