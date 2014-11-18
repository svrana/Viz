/*
 * Copyright 2012-2014, First Three LLC
 *
 * This file is a part of Viz.
 *
 * Viz is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * Viz is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Viz.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.first3.viz.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.app.DialogFragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.first3.viz.Preferences;
import com.first3.viz.R;
import com.first3.viz.VizApp;
import com.first3.viz.utils.Log;
import com.first3.viz.utils.Utils;

public class PinSelectorDialogFragment extends DialogFragment {
    public static String PIN_SELECTOR_DIALOG_TAG = "PinSelectorDialogFragment";
    public static String MESSAGE_CODE = "message";
    public static String EDITING_CODE = "purpose";

    public interface ConfirmNewPinListener {
        public void confirmedNewPin(boolean confirmed);
    }

    public interface DismissPinDialogListener {
        /**
         * Callback when dialog is dismissed.
         */
        public void pinDialogDismissed();
    }

    Button[] keypad;
    Button deleteButton, cancelButton;
    TextView[] pinDigits;
    LinearLayout keypadLinearLayout, pinDigitLinearLayout;
    String testPin, confirmPin, message;
    boolean editing;
    int currentDigit;
    ConfirmNewPinListener newPinConfirmedlistener;
    DismissPinDialogListener dialogDismissedListener;

    /**
     * A static psuedo-constructor for the class. (Do not call the constructor directly.)
     *
     * @param message
     *            The title of the dialog
     * @param editing
     *            True if the user will be setting a new pin, false if entering the current pin to unlock the app
     * @return
     */
    public static PinSelectorDialogFragment newInstance(String message, boolean editing) {
        PinSelectorDialogFragment f = new PinSelectorDialogFragment();

        Bundle args = new Bundle();
        args.putString(MESSAGE_CODE, message);
        args.putBoolean(EDITING_CODE, editing);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        testPin = "";
        confirmPin = "";
        message = getArguments().getString(MESSAGE_CODE);
        editing = getArguments().getBoolean(EDITING_CODE);
        currentDigit = 0;

        // Otherwise the keypad might be too small on small screens
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater = VizApp.getInflator();
        View dialogView = inflater.inflate(R.layout.pin_selector_dialog, null);

        Utils.maximizeDialog(getActivity(), dialogView);

        keypad = new Button[10];
        keypad[0] = (Button) dialogView.findViewById(R.id.key0);
        keypad[1] = (Button) dialogView.findViewById(R.id.key1);
        keypad[2] = (Button) dialogView.findViewById(R.id.key2);
        keypad[3] = (Button) dialogView.findViewById(R.id.key3);
        keypad[4] = (Button) dialogView.findViewById(R.id.key4);
        keypad[5] = (Button) dialogView.findViewById(R.id.key5);
        keypad[6] = (Button) dialogView.findViewById(R.id.key6);
        keypad[7] = (Button) dialogView.findViewById(R.id.key7);
        keypad[8] = (Button) dialogView.findViewById(R.id.key8);
        keypad[9] = (Button) dialogView.findViewById(R.id.key9);
        pinDigits = new TextView[4];
        pinDigits[0] = (TextView) dialogView.findViewById(R.id.pinDigit1);
        pinDigits[1] = (TextView) dialogView.findViewById(R.id.pinDigit2);
        pinDigits[2] = (TextView) dialogView.findViewById(R.id.pinDigit3);
        pinDigits[3] = (TextView) dialogView.findViewById(R.id.pinDigit4);
        deleteButton = (Button) dialogView.findViewById(R.id.keyDelete);
        cancelButton = (Button) dialogView.findViewById(R.id.keyCancel);
        pinDigitLinearLayout = (LinearLayout) dialogView.findViewById(R.id.pinDigitLinearLayout);
        keypadLinearLayout = (LinearLayout) dialogView.findViewById(R.id.keypadLinearLayout);

        for (int i = 0; i < 10; i++) {
            keypad[i].setOnClickListener(new KeypadButtonListener(i));
        }

        // A listener is needed to get the size of the buttons after they've
        // been measured.
        // We want to set the width == height for nice big, square buttons
        dialogView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            public void onGlobalLayout() {
                int buttonHeight = Math.max(keypad[1].getMeasuredHeight(), 35); // Ensure a minimum size (35 is just a
                // guess!)
                ViewGroup.LayoutParams params = keypad[1].getLayoutParams();
                if (params.width != buttonHeight) { // prevents an
                    // endless loop of
                    // re-sizing
                    params.width = buttonHeight;

                    for (int i = 0; i < 10; i++) {
                        keypad[i].setLayoutParams(params);
                    }
                    cancelButton.setLayoutParams(params);
                    deleteButton.setLayoutParams(params);
                }

                params = pinDigitLinearLayout.getLayoutParams();
                if (params.width != keypadLinearLayout.getMeasuredWidth()) {
                    params.width = keypadLinearLayout.getMeasuredWidth();
                    pinDigitLinearLayout.setLayoutParams(params);
                }
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // There is an (on-purpose) pause before dismissing the dialog.
                // Therefore, we need to make sure that the user doesn't bugger
                // it up in the meantime ;)
                if (currentDigit < 4) {
                    testPin = "";
                    for (TextView tv : pinDigits) {
                        tv.setText("");
                    }
                    currentDigit = 0;
                }
            }
        });

        if (!editing) {
            cancelButton.setEnabled(false);
        } else {
            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                }
            });
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(dialogView).setTitle(message).setIcon(R.drawable.ic_launcher).setCancelable(false)
        // Disable user to hit anything but the home key
        .setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                return true; // This consumes the event
            }
        });

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false); // this is crucial

        return dialog;
    }

    @Override
    public void dismiss() {
        super.dismiss();
        // Release orientation lock
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
    }

    public void registerConfirmPinListener(Object listener) {
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the
            // host
            this.newPinConfirmedlistener = (ConfirmNewPinListener) listener;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            Log.e(listener.getClass().getName() + " must implement PinSelectorDialogListener interface.");
        }
    }

    public void registerDialogDismissedListener(Object listener) {
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the
            // host
            this.dialogDismissedListener = (DismissPinDialogListener) listener;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            Log.e(listener.getClass().getName() + " must implement DismissDialogListener interface.");
        }
    }

    private void dismissIfComplete() {
        if (currentDigit == 4) {
            Handler h = VizApp.getHandler();
            h.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (editing) {
                        if (confirmPin.equals("")) {
                            // confirm the new pin before committing
                            confirmPin = String.valueOf(testPin);
                            clearDisplay();
                            getDialog().setTitle(R.string.confirm_pin);
                        } else {
                            String message = "";
                            boolean confirmed = testPin.equals(confirmPin);
                            if (confirmed) {
                                message = getString(R.string.pin_enabled);
                                VizApp.getPrefs().edit().putString(Preferences.PIN, testPin).commit();
                                VizApp.getPrefs().edit().putBoolean(Preferences.PIN_LOCKED, true).commit();
                            } else {
                                message = getString(R.string.pin_not_confirmed);
                            }

                            Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
                            if (newPinConfirmedlistener != null) {
                                newPinConfirmedlistener.confirmedNewPin(confirmed);
                            } else {
                                Log.e("No one's listening, preference will not be updated!");
                            }
                            dismiss();
                        }
                    } else { // Confirming current PIN
                        String correctPin = VizApp.getPrefs().getString(Preferences.PIN, "");
                        if (testPin.equals(correctPin)) {
                            dismiss();
                            if (dialogDismissedListener != null) {
                                dialogDismissedListener.pinDialogDismissed();
                            }
                        } else {
                            clearDisplay();
                            Toast.makeText(getActivity(), getString(R.string.pin_incorrect), Toast.LENGTH_LONG).show();
                        }
                    }
                }
            }, 250);
        }

    }

    private void clearDisplay() {
        testPin = "";
        currentDigit = 0;
        for (int i = 0; i < 4; i++) {
            pinDigits[i].setText("");
        }
    }

    private class KeypadButtonListener implements Button.OnClickListener {

        int keyValue;

        public KeypadButtonListener(int keyValue) {
            this.keyValue = keyValue;
        }

        @Override
        public void onClick(View v) {
            // There is an (on-purpose) pause before dismissing the dialog.
            // Therefore, we need to make sure that the user doesn't bugger it
            // up in the meantime ;)
            if (currentDigit < 4) {
                pinDigits[currentDigit++].setText(String.valueOf(keyValue));
                testPin += String.valueOf(keyValue);
                dismissIfComplete();
            }
        }
    }
}
