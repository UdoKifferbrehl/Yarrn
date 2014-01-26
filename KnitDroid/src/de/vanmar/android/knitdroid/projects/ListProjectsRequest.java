package de.vanmar.android.knitdroid.projects;

import android.app.Application;

import com.google.gson.Gson;

import org.scribe.model.OAuthRequest;
import org.scribe.model.Verb;

import de.vanmar.android.knitdroid.KnitdroidPrefs_;
import de.vanmar.android.knitdroid.R;
import de.vanmar.android.knitdroid.ravelry.AbstractRavelryGetRequest;
import de.vanmar.android.knitdroid.ravelry.dts.ProjectsResult;

public class ListProjectsRequest extends AbstractRavelryGetRequest<ProjectsResult> {

    public ListProjectsRequest(Application application, KnitdroidPrefs_ prefs) {
        super(ProjectsResult.class, application, prefs);
    }

    protected ProjectsResult parseResult(String responseBody) {
        return new Gson().fromJson(responseBody, ProjectsResult.class);
    }

    protected OAuthRequest getRequest() {
        return new OAuthRequest(Verb.GET, String.format(
                application.getString(R.string.ravelry_url) + "/projects/%s/list.json",
                prefs.username().get()));
    }
}