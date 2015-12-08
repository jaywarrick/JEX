package function.plugin.plugins.adhesion;

import rtools.ScriptRepository;

public class AdhesionUtility {
	
	public static void loadAdhesionScripts()
	{
		ScriptRepository.sourceGitHubFile("jaywarrick", "R-General", "master", ".Rprofile");
		ScriptRepository.sourceGitHubFile("jaywarrick", "R-Adhesion", "master", "CellTracking/R/PackageFunctions.R");
		ScriptRepository.sourceGitHubFile("jaywarrick", "R-Adhesion", "master", "CellTracking/R/LAPJV.R");
		ScriptRepository.sourceGitHubFile("jaywarrick", "R-General", "master", "CellTracking/R/Tracking.R");
		ScriptRepository.sourceGitHubFile("jaywarrick", "R-General", "master", "CellTracking/R/TrackList.R");
		ScriptRepository.sourceGitHubFile("jaywarrick", "R-General", "master", "CellTracking/R/Track.R");
		ScriptRepository.sourceGitHubFile("jaywarrick", "R-General", "master", "CellTracking/R/Maxima.R");
		ScriptRepository.sourceGitHubFile("jaywarrick", "R-General", "master", "CellTracking/R/MaximaList.R");
		ScriptRepository.sourceGitHubFile("jaywarrick", "R-General", "master", "CellTracking/R/TrackFilters.R");
		ScriptRepository.sourceGitHubFile("jaywarrick", "R-General", "master", "CellTracking/R/TrackFitting.R");
		ScriptRepository.sourceGitHubFile("jaywarrick", "R-General", "master", "CellTracking/R/PackageFunctions.R");
	}

}
