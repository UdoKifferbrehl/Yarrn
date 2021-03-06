package de.vanmar.android.yarrn;

import org.androidannotations.annotations.sharedpreferences.DefaultBoolean;
import org.androidannotations.annotations.sharedpreferences.SharedPref;
import org.androidannotations.annotations.sharedpreferences.SharedPref.Scope;

@SharedPref(Scope.APPLICATION_DEFAULT)
public interface YarrnPrefs {

    String accessSecret();

    String accessToken();

    String requestToken();

    String username();

    @DefaultBoolean(false)
    boolean sendErrorReports();

    int projectSort();

    boolean projectSortReverse();

    int favoriteSearchOption();

    int stashSort();

    boolean stashSortReverse();
}
