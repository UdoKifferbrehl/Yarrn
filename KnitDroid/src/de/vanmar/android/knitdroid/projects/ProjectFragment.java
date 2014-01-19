package de.vanmar.android.knitdroid.projects;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.widget.TextView;

import com.meetme.android.horizontallistview.HorizontalListView;
import com.octo.android.robospice.SpiceManager;
import com.octo.android.robospice.UncachedSpiceService;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;

import de.vanmar.android.knitdroid.KnitdroidPrefs_;
import de.vanmar.android.knitdroid.R;
import de.vanmar.android.knitdroid.ravelry.IRavelryActivity;
import de.vanmar.android.knitdroid.ravelry.RavelryResultListener;
import de.vanmar.android.knitdroid.ravelry.dts.Project;
import de.vanmar.android.knitdroid.ravelry.dts.ProjectResult;

@EFragment(R.layout.fragment_project_detail)
public class ProjectFragment extends Fragment {

    protected SpiceManager spiceManager = new SpiceManager(UncachedSpiceService.class);

    public interface ProjectFragmentListener extends IRavelryActivity {

        void takePhoto();

        void pickImage();
    }

    @ViewById(R.id.name)
    TextView name;

    @ViewById(R.id.pattern_name)
    TextView patternName;

    @ViewById(R.id.status)
    TextView status;

    @ViewById(R.id.gallery)
    HorizontalListView gallery;

    private ProjectFragmentListener listener;

    private PhotoAdapter adapter;

    @Pref
    KnitdroidPrefs_ prefs;

    @AfterViews
    public void afterViews() {
        adapter = new PhotoAdapter(getActivity());
        gallery.setAdapter(adapter);
    }

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);
        if (activity instanceof ProjectFragmentListener) {
            listener = (ProjectFragmentListener) activity;
        } else {
            throw new ClassCastException(activity.toString()
                    + " must implement ProjectFragmentListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    public void onProjectSelected(final int projectId) {
        if (projectId == 0) {
            clearProject();
        } else {
            spiceManager.execute(new GetProjectRequest(this.getActivity(), prefs, projectId), new ProjectsListener(listener));
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        spiceManager.start(this.getActivity());

    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStop() {
        spiceManager.shouldStop();
        super.onStop();
    }

    @UiThread
    protected void clearProject() {
        name.setText(null);
        patternName.setText(null);
        status.setText(null);

        adapter.clear();
    }

    @UiThread
    protected void displayProject(final ProjectResult projectResult) {
        Project project = projectResult.project;
        name.setText(project.name);
        patternName.setText(project.patternName);
        status.setText(project.status);
        adapter.clear();
        adapter.addAll(project.photos);
    }

    @Click(R.id.addPhoto)
    public void onAddPhotoClicked() {
        listener.pickImage();
    }

    @Click(R.id.takePhoto)
    public void onTakePhotoClicked() {
        listener.takePhoto();
    }

    class ProjectsListener extends RavelryResultListener<ProjectResult> {

        protected ProjectsListener(IRavelryActivity activity) {
            super(activity);
        }

        @Override
        public void onRequestSuccess(ProjectResult result) {
            displayProject(result);
        }
    }
}