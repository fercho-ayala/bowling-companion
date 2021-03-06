package ca.josephroque.bowlingcompanion.utilities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.text.Html;
import android.view.View;
import android.widget.TextView;

import ca.josephroque.bowlingcompanion.R;

/**
 * Created by Joseph Roque on 2015-11-19. Provides utilities to handle displaying the changelog of the application to
 * the user each time a new update becomes available.
 */
public final class Changelog {

    /** Identifies output from this class in Logcat. */
    @SuppressWarnings("unused")
    private static final String TAG = "Changelog";

    /** Identifier for preference to indicate if changelog should be shown. */
    private static final String PREF_SHOW_CHANGELOG = "pref_show_changelog";

    /** Indicates whether the application should show the changelog or not. */
    private static boolean sShowChangelog = false;
    /** Indicates if the value of {@code sShowChangelog} has been loaded from the shared preferences yet. */
    private static boolean sShowChangelogLoaded = false;

    /**
     * Defines the amount of time which must have passed since the application was installed before the changelog can be
     * shown, so that new installations are not shown it.
     */
    private static final long MINIMUM_TIME_REQUIRED_FOR_CHANGELOG = 1000 * 60 * 60 * 24;

    /**
     * Returns the value of {@code sShowChangelog} to indicate if changelog should be shown.
     *
     * @param context to get the preferences
     * @return true if the changelog dialog should be shown
     */
    public static boolean shouldShowChangelog(Context context) {
        if (!sShowChangelogLoaded) {
            sShowChangelog = PreferenceManager.getDefaultSharedPreferences(context)
                    .getBoolean(PREF_SHOW_CHANGELOG, false);
            sShowChangelogLoaded = true;
        }

        long installed;
        try {
            installed = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).firstInstallTime;
        } catch (PackageManager.NameNotFoundException ex) {
            installed = System.currentTimeMillis();
        }

        return sShowChangelog && System.currentTimeMillis() - installed > MINIMUM_TIME_REQUIRED_FOR_CHANGELOG;
    }

    /**
     * Sets whether the changelog will be shown.
     *
     * @param context to get the preferences
     * @param changelog true to show changelog, false to prevent it being shown
     */
    public static void setShowChangelog(Context context, boolean changelog) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(PREF_SHOW_CHANGELOG, changelog)
                .apply();
        sShowChangelog = changelog;
        sShowChangelogLoaded = true;
    }

    /**
     * Invoked when the application is launched to check if the dialog should be shown.
     *
     * @param context to show dialog
     */
    public static void appLaunched(Context context) {
        // Return if I won't be showing changelog
        if (!shouldShowChangelog(context))
            return;

        String changelog = FileUtils.retrieveTextFileAsset(context, "changelog.txt");
        if (changelog == null)
            return;

        // Creating alert dialog
        final AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        View rootView = View.inflate(context, R.layout.dialog_scrollable_text, null);

        dialog.setView(rootView);
        final AlertDialog alertDialog = dialog.create();

        // Setting text of changelog
        ((TextView) rootView.findViewById(R.id.tv_scrollable_text)).setText(
                Html.fromHtml(changelog.replace("\n", "<br />")));
        ((TextView) rootView.findViewById(R.id.tv_scrollable_text_dialog_title)).setText(context.getResources()
                .getString(R.string.text_changelog));

        rootView.findViewById(R.id.btn_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });

        alertDialog.show();
    }

    /**
     * Default private constructor.
     */
    private Changelog() {
        // does nothing
    }
}
