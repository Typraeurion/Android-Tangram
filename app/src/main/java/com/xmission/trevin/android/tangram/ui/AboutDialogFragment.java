/*
 * Copyright © 2026 Trevin Beattie
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.xmission.trevin.android.tangram.ui;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.xmission.trevin.android.tangram.R;

/**
 * The &ldquo;About&hellip;&rdquo; dialog, shown as a {@link DialogFragment}
 * &mdash; the modern replacement for the deprecated
 * {@code Activity.showDialog} / {@code onCreateDialog(int)} pair.
 *
 * <p>Show it from an {@code AppCompatActivity} with:</p>
 * <pre>
 * new AboutDialogFragment().show(getSupportFragmentManager(),
 *         AboutDialogFragment.TAG);
 * </pre>
 *
 * <p>The {@link androidx.fragment.app.FragmentManager} re-creates this
 * fragment (calling {@link #onCreateDialog}) automatically across
 * configuration changes such as rotation, so the dialog stays up on its
 * own.  There is no saved state to preserve here since the content is
 * static.</p>
 */
public class AboutDialogFragment extends DialogFragment {

    /** Tag identifying this fragment within the {@code FragmentManager}. */
    public static final String TAG = "about";

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        // A null button listener is fine: an AlertDialog button dismisses
        // the dialog by default when tapped.
        return new AlertDialog.Builder(requireContext())
                .setTitle(R.string.MenuAbout)
                .setMessage(requireContext().getText(R.string.InfoPopupText))
                .setNeutralButton(R.string.InfoButtonOK, null)
                .create();
    }
}