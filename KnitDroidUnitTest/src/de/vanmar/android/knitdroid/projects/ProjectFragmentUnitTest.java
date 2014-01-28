package de.vanmar.android.knitdroid.projects;

import android.widget.RatingBar;
import android.widget.Spinner;

import com.octo.android.robospice.SpiceManager;
import com.octo.android.robospice.request.SpiceRequest;
import com.octo.android.robospice.request.listener.RequestListener;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.util.ActivityController;

import java.util.Calendar;
import java.util.GregorianCalendar;

import de.vanmar.android.knitdroid.FragmentFactory;
import de.vanmar.android.knitdroid.MainActivity;
import de.vanmar.android.knitdroid.MainActivity_;
import de.vanmar.android.knitdroid.R;
import de.vanmar.android.knitdroid.components.ViewEditText;
import de.vanmar.android.knitdroid.ravelry.dts.Project;
import de.vanmar.android.knitdroid.ravelry.dts.ProjectResult;
import de.vanmar.android.knitdroid.util.TestUtil;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doAnswer;

@RunWith(RobolectricTestRunner.class)
public class ProjectFragmentUnitTest {

    public static final int PROJECT_ID = 10014463;
    public static final String USERNAME = "Jillda";

    @Mock
    private FragmentFactory fragmentFactory;
    @Mock
    private SpiceManager spiceManager;
    @Mock
    private ProjectsFragment projectsFragment;

    private ProjectFragment_ projectFragment;
    private SpiceRequest request;

    @Before
    public void prepare() {
        MockitoAnnotations.initMocks(this);
        TestUtil.mockBackgroundExecutor();

        projectFragment = new ProjectFragment_();
        projectFragment.spiceManager = this.spiceManager;
        ActivityController<MainActivity_> activityController = Robolectric.buildActivity(MainActivity_.class).create();
        MainActivity activity = activityController.get();
        activity.fragmentFactory = this.fragmentFactory;
        given(fragmentFactory.getProjectsFragment()).willReturn(projectsFragment);
        given(fragmentFactory.getProjectFragment()).willReturn(projectFragment);
        activityController.start().resume();

        activity.onProjectSelected(PROJECT_ID, USERNAME);
    }

    @Test
    public void shouldGetProjectDetails() throws Exception {
        // given
        mockSpiceCall(createProjectResult());

        // when
        projectFragment.onProjectSelected(PROJECT_ID, USERNAME);

        // then
        assertThat(projectFragment.name.getText().toString(), is("aqua diva"));
        assertThat(projectFragment.status.getText().toString(), is("Started"));
        assertThat(projectFragment.progressBar.getProgress(), is(5));
        assertThat((String) projectFragment.progressSpinner.getSelectedItem(), is("5%"));
        assertThat(projectFragment.started.getText().toString(), is("17.05.2013")); // testing with German Locale
        assertThat(projectFragment.completed.getText().toString(), is("Juni 2013"));
    }

    @Test
    public void shouldRefreshProjectDetails() throws Exception {
        // given
        assertThat(projectFragment.name.getText().toString(), is(""));
        mockSpiceCall(createProjectResult());

        // when
        projectFragment.menuRefresh();

        // then
        assertThat(projectFragment.name.getText().toString(), is("aqua diva"));
        assertThat(projectFragment.status.getText().toString(), is("Started"));
        assertThat(projectFragment.progressBar.getProgress(), is(5));
        assertThat((String) projectFragment.progressSpinner.getSelectedItem(), is("5%"));
        assertThat(projectFragment.started.getText().toString(), is("17.05.2013")); // testing with German Locale
        assertThat(projectFragment.completed.getText().toString(), is("Juni 2013"));
    }

    @Test
    public void shouldUpdateProgress() {
        // given
        Spinner progressSpinner = projectFragment.progressSpinner;
        assertThat((String) progressSpinner.getItemAtPosition(7), is("35%"));
        mockSpiceCall(createProjectResult());
        projectFragment.onProjectSelected(PROJECT_ID, USERNAME);

        // when
        progressSpinner.getOnItemSelectedListener().onItemSelected(progressSpinner, null, 7, 0);

        // then
        assertThat(request, is(UpdateProjectRequest.class));
        UpdateProjectRequest updateProjectRequest = (UpdateProjectRequest) request;
        assertThat(updateProjectRequest.getProjectId(), is(PROJECT_ID));
        assertThat(updateProjectRequest.getUpdateData().get("progress").getAsInt(), is(35));
    }

    @Test
    public void shouldUpdateNotes() {
        // given
        ViewEditText notes = projectFragment.notes;
        mockSpiceCall(createProjectResult());
        projectFragment.onProjectSelected(PROJECT_ID, USERNAME);
        assertThat(notes.getBodyText().toString(), is("Notizfeld"));
        assertFalse(notes.isEditMode());

        // when
        notes.findViewById(R.id.toggleButton).performClick();
        assertTrue(notes.isEditMode());
        notes.setBodyText("Neue Notiz");
        notes.findViewById(R.id.toggleButton).performClick();
        assertFalse(notes.isEditMode());

        // then
        assertThat(request, is(UpdateProjectRequest.class));
        UpdateProjectRequest updateProjectRequest = (UpdateProjectRequest) request;
        assertThat(updateProjectRequest.getProjectId(), is(PROJECT_ID));
        assertThat(updateProjectRequest.getUpdateData().get("notes").getAsString(), is("Neue Notiz"));
    }

    @Test
    public void shouldUpdateRating() {
        // given
        RatingBar rating = projectFragment.rating;
        mockSpiceCall(createProjectResult());
        projectFragment.onProjectSelected(PROJECT_ID, USERNAME);
        assertThat(rating.getRating(), is(3.0f));

        // when
        rating.getOnRatingBarChangeListener().onRatingChanged(rating, 5, true);

        // then
        assertThat(request, is(UpdateProjectRequest.class));
        UpdateProjectRequest updateProjectRequest = (UpdateProjectRequest) request;
        assertThat(updateProjectRequest.getProjectId(), is(PROJECT_ID));
        assertThat(updateProjectRequest.getUpdateData().get("rating").getAsInt(), is(4));
    }

    private void mockSpiceCall(final ProjectResult projectResult) {
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                request = (SpiceRequest) invocationOnMock.getArguments()[0];
                RequestListener<ProjectResult> listener = (RequestListener<ProjectResult>) invocationOnMock.getArguments()[3];
                listener.onRequestSuccess(projectResult);
                return null;
            }
        }).when(spiceManager).execute(any(SpiceRequest.class), any(), anyLong(), any(RequestListener.class));
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                request = (SpiceRequest) invocationOnMock.getArguments()[0];
                RequestListener<ProjectResult> listener = (RequestListener<ProjectResult>) invocationOnMock.getArguments()[1];
                listener.onRequestSuccess(projectResult);
                return null;
            }
        }).when(spiceManager).execute(any(SpiceRequest.class), any(RequestListener.class));
    }

    private ProjectResult createProjectResult() {
        ProjectResult projectResult = new ProjectResult();
        Project project = new Project();
        project.name = "aqua diva";
        project.progress = 5;
        project.rating = 2;
        project.status = "Started";
        project.notes = "Notizfeld";
        GregorianCalendar started = new GregorianCalendar();
        started.set(2013, Calendar.MAY, 17);
        project.started = started.getTime();
        project.startedDaySet = true;
        GregorianCalendar completed = new GregorianCalendar();
        completed.set(2013, Calendar.JUNE, 21);
        project.completed = completed.getTime();
        project.completedDaySet = false;
        projectResult.project = project;
        return projectResult;
    }
}
