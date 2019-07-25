package models;

import java.util.HashMap;

public class ExperimentConfig {
	
    public String experiment_id;
    public String experiment_name;
    public String experiment_description;
    public String experiment_datafile;
    public String experiment_svgfile;

    public ExperimentConfig(String _id, String _name, String _description, String _datafile, String _svgfile )
    {
        this.experiment_id = _id;
        this.experiment_name = _name;
        this.experiment_description = _description;
        this.experiment_datafile = _datafile;
        this.experiment_svgfile = _svgfile;
    }

}
