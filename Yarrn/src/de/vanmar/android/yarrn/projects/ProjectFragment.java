package de.vanmar.android.yarrn.projects;

import android.app.Activity;
import android.net.Uri;
import android.text.Editable;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.androidquery.util.AQUtility;
import com.google.gson.JsonObject;
import com.meetme.android.horizontallistview.HorizontalListView;
import com.octo.android.robospice.SpiceManager;
import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.RequestListener;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.FragmentArg;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.vanmar.android.yarrn.R;
import de.vanmar.android.yarrn.YarrnPrefs_;
import de.vanmar.android.yarrn.YarrnSpiceService;
import de.vanmar.android.yarrn.components.ImageDialog;
import de.vanmar.android.yarrn.components.SimpleImageArrayAdapter;
import de.vanmar.android.yarrn.components.ViewEditText;
import de.vanmar.android.yarrn.ravelry.IRavelryActivity;
import de.vanmar.android.yarrn.ravelry.RavelryResultListener;
import de.vanmar.android.yarrn.ravelry.dts.Photo;
import de.vanmar.android.yarrn.ravelry.dts.PhotoResult;
import de.vanmar.android.yarrn.ravelry.dts.Project;
import de.vanmar.android.yarrn.ravelry.dts.ProjectResult;
import de.vanmar.android.yarrn.requests.AbstractRavelryGetRequest;
import de.vanmar.android.yarrn.requests.GetProjectRequest;
import de.vanmar.android.yarrn.requests.ReorderProjectPhotosRequest;
import de.vanmar.android.yarrn.requests.UpdateProjectRequest;

@EFragment(R.layout.fragment_project_detail)
@OptionsMenu(R.menu.project_fragment_menu)
public class ProjectFragment extends SherlockFragment {

    public static final String ARG_PROJECT_ID = "projectId";
    public static final String ARG_USERNAME = "username";
    protected SpiceManager spiceManager;
    private AdapterView.OnItemSelectedListener progressListener;
    private ViewEditText.OnSaveListener notesListener;
    private AdapterView.OnItemSelectedListener ratingListener;
    private View.OnClickListener progressBarListener;
    private List<Photo> photos;
    private List<Photo> photosOriginal;

    public interface ProjectFragmentListener extends IRavelryActivity {

        void takePhoto();

        void pickImage();

        void onPatternSelected(int patternId);
    }

    @ViewById(R.id.name)
    TextView name;

    @ViewById(R.id.pattern_name)
    TextView patternName;

    @ViewById(R.id.status)
    TextView status;

    @ViewById(R.id.gallery_edit_done)
    ImageButton galleryEditDone;

    @ViewById(R.id.gallery_edit_cancel)
    ImageButton galleryEditCancel;

    @ViewById(R.id.gallery)
    HorizontalListView gallery;

    @ViewById(R.id.progressBar)
    ProgressBar progressBar;

    @ViewById(R.id.progressSpinner)
    Spinner progressSpinner;

    @ViewById(R.id.started)
    public TextView started;

    @ViewById(R.id.completed)
    public TextView completed;

    @ViewById(R.id.notes)
    public ViewEditText notes;

    @ViewById(R.id.rating)
    public Spinner rating;

    @FragmentArg(ARG_PROJECT_ID)
    int projectId;

    @FragmentArg(ARG_USERNAME)
    String username;

    private ProjectFragmentListener listener;

    private PhotoAdapter adapter;

    private boolean isEditable = false;

    @Pref
    YarrnPrefs_ prefs;

    @AfterViews
    public void afterViews() {
        if (spiceManager == null) {
            spiceManager = new SpiceManager(YarrnSpiceService.class);
        }
        notesListener = new ViewEditText.OnSaveListener() {
            @Override
            public void onSave(ViewEditText view, Editable text) {
                JsonObject updateData = new JsonObject();
                updateData.addProperty("notes", text.toString());
                executeUpdate(updateData);
            }
        };

        setupRatingSpinner();

        progressBarListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressSpinner.performClick();
            }
        };

        adapter = new PhotoAdapter(getActivity());
        gallery.setAdapter(adapter);
        gallery.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                new ImageDialog(getActivity(), adapter.getItem(position).mediumUrl).show();
            }
        });
        gallery.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                setGalleryEditable(true);
                return true;
            }
        });
        adapter.setPhotoAdapterListener(new PhotoAdapter.PhotoAdapterListener() {
            private void movePhotoFromTo(int fromPosition, int toPosition) {
                Photo photo = photos.remove(fromPosition);
                photos.add(toPosition, photo);
                adapter.setItems(photos);
            }

            @Override
            public void onMoveLeft(int position) {
                movePhotoFromTo(position, position - 1);
            }

            @Override
            public void onMoveAllLeft(int position) {
                movePhotoFromTo(position, 0);
            }

            @Override
            public void onMoveRight(int position) {
                movePhotoFromTo(position, position + 1);
            }

            @Override
            public void onMoveAllRight(int position) {
                movePhotoFromTo(position, photos.size() - 1);
            }
        });

        progressListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int newProgress = position * 5;
                displayProgress(newProgress);
                JsonObject updateData = new JsonObject();
                updateData.addProperty("progress", newProgress);
                executeUpdate(updateData);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // do nothing
            }
        };
    }

    private void setupRatingSpinner() {
        ratingListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int newRating = position;
                JsonObject updateData = new JsonObject();
                updateData.addProperty("rating", newRating);
                executeUpdate(updateData);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // do nothing
            }
        };
        SimpleImageArrayAdapter ratingAdapter = new SimpleImageArrayAdapter(getActivity(), new Integer[]{R.drawable.smiley1, R.drawable.smiley2, R.drawable.smiley3, R.drawable.smiley4, R.drawable.smiley5});
        rating.setAdapter(ratingAdapter);
    }

    private void setGalleryEditable(boolean shouldBeEditable) {
        boolean editable = isEditable && shouldBeEditable;
        adapter.setEditable(editable);
        galleryEditDone.setVisibility(editable ? View.VISIBLE : View.GONE);
        galleryEditCancel.setVisibility(editable ? View.VISIBLE : View.GONE);
    }

    private void executeUpdate(JsonObject updateData) {
        spiceManager.execute(new UpdateProjectRequest(prefs, getActivity().getApplication(), projectId, updateData), new ProjectListener(listener));
        spiceManager.removeDataFromCache(ProjectResult.class, new GetProjectRequest(getActivity().getApplication(), prefs, projectId, prefs.username().get()).getCacheKey());
    }

    private void savePhotoOrder() {
        spiceManager.execute(new ReorderProjectPhotosRequest(prefs, getActivity().getApplication(), projectId, photos), new RavelryResultListener<PhotoResult>(listener) {
            @Override
            public void onRequestSuccess(PhotoResult photoResult) {
                photos = photoResult.photos;
                adapter.setItems(photos);
            }
        });
        spiceManager.removeDataFromCache(ProjectResult.class, new GetProjectRequest(getActivity().getApplication(), prefs, projectId, prefs.username().get()).getCacheKey());
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

    public void onProjectSelected(final int projectId, final String username) {
        clearProject();
        if (projectId != 0) {
            GetProjectRequest request = new GetProjectRequest(this.getActivity().getApplication(), prefs, projectId, username);
            spiceManager.execute(request, request.getCacheKey(), AbstractRavelryGetRequest.CACHE_DURATION, new ProjectListener(listener));
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        spiceManager.start(this.getActivity());

        onProjectSelected(projectId, username);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStop() {
        if (spiceManager.isStarted()) {
            spiceManager.shouldStop();
        }
        super.onStop();
    }

    @UiThread
    protected void clearProject() {
        getView().setVisibility(View.GONE);
    }

    protected void displayProject(final ProjectResult projectResult) {
        final Project project = projectResult.project;
        getActivity().setTitle(project.name);
        name.setText(project.name);
        rating.setOnItemSelectedListener(null);
        rating.setSelection(project.rating, false);
        setPatternName(project);
        status.setText(project.status);
        started.setText(getCompletedDateText(project.started, project.startedDaySet));
        completed.setText(getCompletedDateText(project.completed, project.completedDaySet));
        notes.setBodyText(project.notes);
        adapter.clear();
        photos = project.photos;
        photosOriginal = new ArrayList<Photo>(project.photos);
        adapter.setItems(photos);
        setGalleryEditable(false);
        displayProgress(project.progress);
        progressSpinner.setOnItemSelectedListener(null);
        getView().setVisibility(View.VISIBLE);
        if (project.user != null && prefs.username().get().equals(project.user.username)) {
            setEditable();
        } else {
            setNonEditable();
        }
    }

    private void setPatternName(final Project project) {
        String name = project.patternName == null ? "" : project.patternName;
        SpannableString patternNameText = new SpannableString(name);
        if (project.patternId != null) {
            patternNameText.setSpan(new UnderlineSpan(), 0, name.length(), 0);
            patternName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onPatternSelected(project.patternId);
                }
            });
        } else {
            patternName.setOnClickListener(null);
        }
        patternName.setText(patternNameText);
    }

    private void setEditable() {
        progressSpinner.setOnItemSelectedListener(progressListener);
        progressBar.setOnClickListener(progressBarListener);
        notes.setOnSaveListener(notesListener);
        rating.setOnItemSelectedListener(ratingListener);
        isEditable = true;
        setFieldsEditable();
    }

    private void setNonEditable() {
        progressSpinner.setOnItemSelectedListener(null);
        progressSpinner.setOnItemSelectedListener(null);
        progressBar.setOnClickListener(null);
        notes.setOnSaveListener(null);
        rating.setOnItemSelectedListener(null);
        isEditable = false;
        setFieldsEditable();
    }

    private void setFieldsEditable() {
        getSherlockActivity().invalidateOptionsMenu();
        progressSpinner.setEnabled(isEditable);
        progressSpinner.setClickable(isEditable);
        progressSpinner.setFocusable(isEditable);
        rating.setClickable(isEditable);
        rating.setEnabled(isEditable);
        notes.setEditable(isEditable);
    }

    private String getCompletedDateText(Date date, boolean withDay) {
        if (date == null) {
            return getActivity().getString(R.string.date_unknown);
        }
        DateFormat dateFormat;
        if (withDay) {
            dateFormat = SimpleDateFormat.getDateInstance();
        } else {
            dateFormat = new SimpleDateFormat("MMMM yyyy");
        }
        return dateFormat.format(date);
    }

    private void displayProgress(int progress) {
        progressBar.setProgress(progress);
        progressSpinner.setOnItemSelectedListener(null);
        progressSpinner.setSelection(progress / 5, false);
        progressSpinner.setOnItemSelectedListener(progressListener);
    }

    public void uploadPhotoToProject(final Uri photoUri) {
        Toast.makeText(getActivity(), getActivity().getString(R.string.upload_started), Toast.LENGTH_LONG).show();
        spiceManager.execute(new PhotoUploadRequest(getActivity().getApplication(), prefs, photoUri, projectId), new RequestListener<String>() {

            @Override
            public void onRequestFailure(SpiceException spiceException) {
                AQUtility.report(spiceException);
            }

            @Override
            public void onRequestSuccess(String s) {
                onPhotoUploadSuccess();
            }
        });
    }

    @UiThread
    public void onPhotoUploadSuccess() {
        Toast.makeText(getActivity(), getActivity().getString(R.string.upload_successful), Toast.LENGTH_LONG).show();
        spiceManager.removeDataFromCache(ProjectResult.class, new GetProjectRequest(getActivity().getApplication(), prefs, projectId, prefs.username().get()).getCacheKey());
    }

    @Click(R.id.gallery_edit_done)
    public void onGalleryEditDoneClicked() {
        savePhotoOrder();
        setGalleryEditable(false);
    }

    @Click(R.id.gallery_edit_cancel)
    public void onGalleryEditCancelClicked() {
        photos = new ArrayList<Photo>(photosOriginal);
        adapter.setItems(photos);
        setGalleryEditable(false);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.findItem(R.id.menu_add_photo).setVisible(isEditable);
        menu.findItem(R.id.menu_take_photo).setVisible(isEditable);
        menu.findItem(R.id.menu_reorder_photos).setVisible(isEditable);
    }

    @OptionsItem(R.id.menu_add_photo)
    public void onAddPhotoClicked() {
        listener.pickImage();
    }

    @OptionsItem(R.id.menu_take_photo)
    public void onTakePhotoClicked() {
        listener.takePhoto();
    }

    @OptionsItem(R.id.menu_refresh)
    public void menuRefresh() {
        onProjectSelected(projectId, username);
    }

    @OptionsItem(R.id.menu_reorder_photos)
    public void menuReorderPhotos() {
        setGalleryEditable(true);
    }

    class ProjectListener extends RavelryResultListener<ProjectResult> {

        protected ProjectListener(IRavelryActivity activity) {
            super(activity);
        }

        @Override
        public void onRequestSuccess(ProjectResult result) {
            displayProject(result);
        }
    }
}