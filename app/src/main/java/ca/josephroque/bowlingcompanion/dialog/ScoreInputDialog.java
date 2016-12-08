package ca.josephroque.bowlingcompanion.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.InputFilter;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;

import ca.josephroque.bowlingcompanion.R;

/**
 * Created by Joseph Roque on 15-03-19.
 *
 * Provides a dialog and a callback interface for the user to enter a score.
 */
public class ScoreInputDialog
        extends DialogFragment {

    /** Identifies output from this class in Logcat. */
    @SuppressWarnings("unused")
    private static final String TAG = "ManualScoreDialog";

    /** Maximum number of characters in a bowling score. */
    private static final byte MAX_SCORE_LENGTH = 3;

    /** Instance of callback listener. */
    private ScoreInputDialogListener mDialogListener;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());
        final View dialogView = View.inflate(getContext(), R.layout.dialog_set_score, null);

        final EditText editTextScore = (EditText) dialogView.findViewById(R.id.et_score);
        editTextScore.setHint(R.string.text_score_450);
        editTextScore.setFilters(new InputFilter[]{new InputFilter.LengthFilter(MAX_SCORE_LENGTH)});

        if (savedInstanceState != null)
            editTextScore.setText(savedInstanceState.getCharSequence("GameScore"));

        dialogBuilder.setView(dialogView)
                .setPositiveButton(R.string.dialog_okay, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (editTextScore.length() > 0) {
                            short gameScore;
                            try {
                                gameScore = Short.parseShort(editTextScore.getText().toString());
                            } catch (NumberFormatException ex) {
                                gameScore = -1;
                            }
                            mDialogListener.onSetScore(gameScore);
                        }
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        AlertDialog dialog = dialogBuilder.create();
        Window dialogWindow = dialog.getWindow();
        if (dialogWindow != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        return dialog;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putCharSequence("GameScore", ((EditText) getDialog().findViewById(R.id.et_score)).getText());
    }

    /**
     * Callback interface which executes methods in activity upon user interaction.
     */
    public interface ScoreInputDialogListener {

        /**
         * Invoked when user finishes inputting a score.
         *
         * @param gameScore score input from user
         */
        void onSetScore(short gameScore);
    }

    /**
     * Creates a new instance of ScoreInputDialog, sets listener member variable and returns the new instance.
     *
     * @param listener instance of callback interface
     * @return new instance of ScoreInputDialog
     */
    public static ScoreInputDialog newInstance(ScoreInputDialogListener listener) {
        ScoreInputDialog dialogFragment = new ScoreInputDialog();
        dialogFragment.mDialogListener = listener;
        return dialogFragment;
    }
}
