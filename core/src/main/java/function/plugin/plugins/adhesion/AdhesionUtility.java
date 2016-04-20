package function.plugin.plugins.adhesion;

import rtools.ScriptRepository;

public class AdhesionUtility {
	
	public static void loadAdhesionScripts()
	{
		ScriptRepository.sourceGitHubFile("jaywarrick", "R-General", "master", ".Rprofile");
		ScriptRepository.sourceGitHubFile("jaywarrick", "R-Adhesion2", "master", "ParticleTrackingPackage/R/PackageFunctions.R");
		ScriptRepository.sourceGitHubFile("jaywarrick", "R-Adhesion2", "master", "ParticleTrackingPackage/R/LAPJV.R");
		ScriptRepository.sourceGitHubFile("jaywarrick", "R-Adhesion2", "master", "ParticleTrackingPackage/R/Tracking.R");
		ScriptRepository.sourceGitHubFile("jaywarrick", "R-Adhesion2", "master", "ParticleTrackingPackage/R/TrackList.R");
		ScriptRepository.sourceGitHubFile("jaywarrick", "R-Adhesion2", "master", "ParticleTrackingPackage/R/Track.R");
		ScriptRepository.sourceGitHubFile("jaywarrick", "R-Adhesion2", "master", "ParticleTrackingPackage/R/Maxima.R");
		ScriptRepository.sourceGitHubFile("jaywarrick", "R-Adhesion2", "master", "ParticleTrackingPackage/R/MaximaList.R");
		ScriptRepository.sourceGitHubFile("jaywarrick", "R-Adhesion2", "master", "ParticleTrackingPackage/R/TrackFilters.R");
		ScriptRepository.sourceGitHubFile("jaywarrick", "R-Adhesion2", "master", "ParticleTrackingPackage/R/TrackFitting.R");
		ScriptRepository.sourceGitHubFile("jaywarrick", "R-Adhesion2", "master", "ParticleTrackingPackage/R/PackageFunctions.R");
	}

}
