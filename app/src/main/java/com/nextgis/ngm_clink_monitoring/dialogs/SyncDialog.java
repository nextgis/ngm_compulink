/*
 * Project:  NextGIS mobile apps for Compulink
 * Purpose:  Mobile GIS for Android
 * Authors:  Dmitry Baryshnikov (aka Bishop), polimax@mail.ru
 *           NikitaFeodonit, nfeodonit@yandex.com
 * *****************************************************************************
 * Copyright (C) 2014-2015 NextGIS
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

package com.nextgis.ngm_clink_monitoring.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.RadioButton;
import com.nextgis.ngm_clink_monitoring.GISApplication;
import com.nextgis.ngm_clink_monitoring.R;
import com.nextgis.ngm_clink_monitoring.activities.MainActivity;


public class SyncDialog
        extends YesNoDialog
{
    protected static final int SYNC = 0;
    protected static final int FULL_SYNC = 1;
    protected static final int SEND_WORK_DATA = 2;

    protected int mSelectedStatus = SYNC;

    protected RadioButton mSync;
    protected RadioButton mFullSync;
    protected RadioButton mSendWorkData;


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        View view = View.inflate(getActivity(), R.layout.dialog_sync, null);
        mSync = (RadioButton) view.findViewById(R.id.sync_sy);
        mFullSync = (RadioButton) view.findViewById(R.id.full_sync_sy);
        mSendWorkData = (RadioButton) view.findViewById(R.id.send_work_data_sy);


        final MainActivity activity = (MainActivity) getActivity();
        final GISApplication app = (GISApplication) activity.getApplication();


        View.OnClickListener radioListener = new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                RadioButton rb = (RadioButton) v;

                switch (rb.getId()) {
                    case R.id.sync_sy:
                    default:
                        mSelectedStatus = SYNC;
                        break;

                    case R.id.full_sync_sy:
                        mSelectedStatus = FULL_SYNC;
                        break;

                    case R.id.send_work_data_sy:
                        mSelectedStatus = SEND_WORK_DATA;
                        break;
                }
            }
        };

        mSync.setOnClickListener(radioListener);
        mFullSync.setOnClickListener(radioListener);
        mSendWorkData.setOnClickListener(radioListener);


        setIcon(R.drawable.ic_action_refresh_light);
        setTitle(R.string.synchronization);
        setView(view, true);

        setPositiveText(R.string.ok);
        setNegativeText(R.string.cancel);


        setOnPositiveClickedListener(
                new YesNoDialog.OnPositiveClickedListener()
                {
                    @Override
                    public void onPositiveClicked()
                    {
                        switch (mSelectedStatus) {
                            case SYNC:
                            default:
                                app.runSyncManually(false);
                                break;

                            case FULL_SYNC:
                                app.runSyncManually(true);
                                break;

                            case SEND_WORK_DATA:
                                activity.onMenuSendWorkDataClick();
                                break;
                        }
                    }
                });

        setOnNegativeClickedListener(
                new YesNoDialog.OnNegativeClickedListener()
                {
                    @Override
                    public void onNegativeClicked()
                    {
                        // cancel
                    }
                });


        return super.onCreateDialog(savedInstanceState);
    }
}
