package de.vanmar.android.yarrn.requests;

import android.app.Application;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;

import de.vanmar.android.yarrn.R;
import de.vanmar.android.yarrn.YarrnPrefs_;
import de.vanmar.android.yarrn.ravelry.dts.BookmarkShort;

public class EditFavoriteRequest extends AbstractRavelryRequest<BookmarkShort> {
    private String favoriteId;
    private JsonObject updateData;

    public EditFavoriteRequest(YarrnPrefs_ prefs, Application application, String favoriteId, JsonObject updateData) {
        super(BookmarkShort.class, prefs, application);
        this.favoriteId = favoriteId;
        this.updateData = updateData;
    }

    public JsonObject getUpdateData() {
        return updateData;
    }

    @Override
    public BookmarkShort loadDataFromNetwork() throws Exception {
        final OAuthRequest request = new OAuthRequest(Verb.POST,
                application.getString(R.string.ravelry_url)
                        + String.format("/people/%s/favorites/%s.json",
                        prefs.username().get(), favoriteId)
        );
        request.addBodyParameter("data", updateData.toString());
        Response response = executeRequest(request);
        return new Gson().fromJson(response.getBody(), BookmarkShort.class);
    }
}