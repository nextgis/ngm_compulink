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

package com.nextgis.ngm_clink_monitoring.fragments;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.nextgis.maplib.datasource.GeoGeometry;
import com.nextgis.maplib.datasource.GeoGeometryFactory;
import com.nextgis.maplib.datasource.GeoMultiPoint;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.location.AccurateLocationTaker;
import com.nextgis.maplib.location.GpsEventSource;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.ngm_clink_monitoring.GISApplication;
import com.nextgis.ngm_clink_monitoring.R;
import com.nextgis.ngm_clink_monitoring.activities.MainActivity;
import com.nextgis.ngm_clink_monitoring.adapters.ObjectPhotoFileAdapter;
import com.nextgis.ngm_clink_monitoring.dialogs.DistanceExceededDialog;
import com.nextgis.ngm_clink_monitoring.dialogs.YesNoDialog;
import com.nextgis.ngm_clink_monitoring.map.FoclDictItem;
import com.nextgis.ngm_clink_monitoring.map.FoclProject;
import com.nextgis.ngm_clink_monitoring.map.FoclStruct;
import com.nextgis.ngm_clink_monitoring.map.FoclVectorLayer;
import com.nextgis.ngm_clink_monitoring.util.BitmapUtil;
import com.nextgis.ngm_clink_monitoring.util.FoclConstants;
import com.nextgis.ngm_clink_monitoring.util.FoclFileUtil;
import com.nextgis.ngm_clink_monitoring.util.FoclLocationUtil;
import com.nextgis.ngm_clink_monitoring.util.FoclSettingsConstantsUI;
import com.nextgis.ngm_clink_monitoring.util.LogcatWriter;
import com.nextgis.ngm_clink_monitoring.util.ViewUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import static com.nextgis.maplib.util.Constants.*;
import static com.nextgis.maplib.util.GeoConstants.CRS_WEB_MERCATOR;
import static com.nextgis.maplib.util.GeoConstants.CRS_WGS84;
import static com.nextgis.ngm_clink_monitoring.util.FoclConstants.*;


public class CreateObjectFragment
        extends Fragment
        implements DistanceExceededDialog.OnNewPointClickedListener,
                   DistanceExceededDialog.OnRepeatClickedListener
{
    protected static final int REQUEST_TAKE_PHOTO = 1;

    protected final static int CREATE_OBJECT_DONE   = 0;
    protected final static int CREATE_OBJECT_OK     = 1;
    protected final static int CREATE_OBJECT_FAILED = 2;

    protected TextView mTypeWorkTitle;
    protected TextView mLineName;

    protected ProgressBar mRefiningProgress;

    protected TextView       mCoordinates;
    protected RelativeLayout mDistanceLayout;
    protected TextView       mDistanceFromPrevPointCaption;
    protected TextView       mDistanceFromPrevPoint;

    protected AccurateLocationTaker mAccurateLocationTaker;
    protected              int     mTakeCount                = 0;
    protected              int     mTakeCountPct             = 0;
    protected              int     mTakeTimePct              = 0;
    protected              int     mTakingLoopCount          = 0;
    protected static final int     MAX_PCT                   = 100;
    protected              Float   mDistance                 = null;
    protected              boolean mNewStartPoint            = false;
    protected              boolean mShowChangeLocationDialog = false;

    protected TextView        mLayingMethodCaption;
    protected ComboboxControl mLayingMethod;

    protected TextView        mFoscTypeCaption;
    protected ComboboxControl mFoscType;
    protected TextView        mFoscPlacementCaption;
    protected ComboboxControl mFoscPlacement;

    protected TextView        mOpticalCrossTypeCaption;
    protected ComboboxControl mOpticalCrossType;

    protected TextView        mSpecialLayingMethodCaption;
    protected ComboboxControl mSpecialLayingMethod;
    protected TextView        mMarkTypeCaption;
    protected ComboboxControl mMarkType;

    protected EditText     mDescription;
    protected TextView     mPhotoHintText;
    protected RecyclerView mPhotoGallery;

    protected Integer mFoclStructLayerType = FoclConstants.LAYERTYPE_FOCL_UNKNOWN;

    protected Long   mLineRemoteId;
    protected String mObjectLayerName;
    protected Long   mObjectId;

    protected Location mAccurateLocation;

    protected ObjectPhotoFileAdapter mObjectPhotoFileAdapter;

    protected String mTempPhotoPath = null;

    protected GpsEventSource mGpsEventSource;

    protected int mObjectCount;

    protected FoclProject     mFoclProject;
    protected FoclStruct      mFoclStruct;
    protected FoclVectorLayer mFoclVectorLayer;

    protected LogcatWriter mLogcatWriter;


    public void setParams(
            Long lineRemoteId,
            Integer foclStructLayerType)
    {
        mLineRemoteId = lineRemoteId;
        mFoclStructLayerType = foclStructLayerType;
    }


    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);

        if (null != mLineRemoteId) {
            outState.putLong(FoclConstants.FOCL_STRUCT_REMOTE_ID, mLineRemoteId);
        }

        outState.putInt(FoclConstants.FOCL_STRUCT_LAYER_TYPE, mFoclStructLayerType);

        if (null != mTempPhotoPath) {
            outState.putString(FoclConstants.TEMP_PHOTO_PATH, mTempPhotoPath);
        }

        if (null != mAccurateLocation) {
            outState.putParcelable(FoclConstants.ACCURATE_LOCATION, mAccurateLocation);
        }

        if (null != mAccurateLocationTaker) {
            outState.putBoolean(
                    FoclConstants.IS_ACCURATE_TAKING, mAccurateLocationTaker.isTaking());
        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        Boolean isAccurateTaking = null;

        if (null != savedInstanceState) {

            if (savedInstanceState.containsKey(FoclConstants.FOCL_STRUCT_REMOTE_ID)) {
                mLineRemoteId = savedInstanceState.getLong(FoclConstants.FOCL_STRUCT_REMOTE_ID);
            }

            if (savedInstanceState.containsKey(FoclConstants.FOCL_STRUCT_LAYER_TYPE)) {
                mFoclStructLayerType =
                        savedInstanceState.getInt(FoclConstants.FOCL_STRUCT_LAYER_TYPE);
            }

            mTempPhotoPath = savedInstanceState.getString(FoclConstants.TEMP_PHOTO_PATH);
            mAccurateLocation = savedInstanceState.getParcelable(FoclConstants.ACCURATE_LOCATION);

            if (savedInstanceState.containsKey(FoclConstants.IS_ACCURATE_TAKING)) {
                isAccurateTaking = savedInstanceState.getBoolean(FoclConstants.IS_ACCURATE_TAKING);
            }
        }


        GISApplication app = (GISApplication) getActivity().getApplication();

        mLogcatWriter = new LogcatWriter(app);
        try {
            mLogcatWriter.startLogcat();
        } catch (IOException e) {
            e.printStackTrace();
        }


        mGpsEventSource = app.getGpsEventSource();

        if (FoclConstants.LAYERTYPE_FOCL_REAL_OPTICAL_CABLE_POINT == mFoclStructLayerType) {
            setObjectCount();
        }

        mAccurateLocationTaker = new AccurateLocationTaker(
                getActivity(), MAX_TAKEN_ACCURACY, MAX_ACCURACY_TAKE_COUNT, MAX_ACCURACY_TAKE_TIME,
                ACCURACY_PUBLISH_PROGRESS_DELAY, ACCURACY_CIRCULAR_ERROR_STR);

        // TODO: setTakeOnBestLocation(true)
//        mAccurateLocationTaker.setTakeOnBestLocation(true);

        mAccurateLocationTaker.setOnGetCurrentAccurateLocationListener(
                new AccurateLocationTaker.OnGetCurrentAccurateLocationListener()
                {
                    @Override
                    public void onGetCurrentAccurateLocation(Location currentAccurateLocation)
                    {
                        if (MIN_ACCURACY_TAKE_COUNT <= mTakeCount &&
                                null != currentAccurateLocation &&
                                // Form getAccuracy() docs:
                                // "If this location does not have an accuracy, then 0.0 is returned."
                                // We must check for 0.
                                0 < currentAccurateLocation.getAccuracy() &&
                                MAX_ACCURACY > currentAccurateLocation.getAccuracy()) {

                            mAccurateLocationTaker.stopTaking();
                        }
                    }
                });

        if (null == isAccurateTaking || isAccurateTaking) {
            mAccurateLocationTaker.startTaking();
        }
    }


    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_create_object, null);

        // Common
        mTypeWorkTitle = (TextView) view.findViewById(R.id.type_work_title_cr);
        mLineName = (TextView) view.findViewById(R.id.line_name_cr);

        setCoordinatesRefiningView(view);

        // Optical cable
        mLayingMethodCaption = (TextView) view.findViewById(R.id.laying_method_caption_cr);
        mLayingMethod = (ComboboxControl) view.findViewById(R.id.laying_method_cr);

        // FOSC
        mFoscTypeCaption = (TextView) view.findViewById(R.id.fosc_type_caption_cr);
        mFoscType = (ComboboxControl) view.findViewById(R.id.fosc_type_cr);
        mFoscPlacementCaption = (TextView) view.findViewById(R.id.fosc_placement_caption_cr);
        mFoscPlacement = (ComboboxControl) view.findViewById(R.id.fosc_placement_cr);

        // Optical cross
        mOpticalCrossTypeCaption = (TextView) view.findViewById(R.id.optical_cross_type_caption_cr);
        mOpticalCrossType = (ComboboxControl) view.findViewById(R.id.optical_cross_type_cr);

        // Access point
        // nothing

        // Special transition
        mSpecialLayingMethodCaption =
                (TextView) view.findViewById(R.id.special_laying_method_caption_cr);
        mSpecialLayingMethod = (ComboboxControl) view.findViewById(R.id.special_laying_method_cr);
        mMarkTypeCaption = (TextView) view.findViewById(R.id.mark_type_caption_cr);
        mMarkType = (ComboboxControl) view.findViewById(R.id.mark_type_cr);


        // Common
        mDescription = (EditText) view.findViewById(R.id.description_cr);
        mPhotoHintText = (TextView) view.findViewById(R.id.photo_hint_text_cr);
        mPhotoGallery = (RecyclerView) view.findViewById(R.id.photo_gallery_cr);

        MainActivity activity = (MainActivity) getActivity();

        setBarsView(activity);
        setTitleView(activity);
        setFieldVisibility();
        registerForContextMenu(mPhotoGallery);

        mPhotoHintText.setText(R.string.take_photos_to_confirm);

        final GISApplication app = (GISApplication) activity.getApplication();

        if (!setFoclProjectData(app)) {
            setBlockedView();
            return view;
        }

        setObjectCount();
        mLineName.setText(Html.fromHtml(mFoclStruct.getHtmlFormattedNameTwoStringsSmall()));
        setFieldDicts();


        View.OnClickListener doneButtonOnClickListener = new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                ViewUtil.hideSoftKeyboard(getActivity());

                if (mAccurateLocationTaker.isTaking()) {
                    YesNoDialog dialog = new YesNoDialog();
                    dialog.setKeepInstance(true)
                            .setIcon(R.drawable.ic_action_warning)
                            .setTitle(R.string.warning)
                            .setMessage(R.string.coordinates_refining_process)
                            .setPositiveText(R.string.ok)
                            .setOnPositiveClickedListener(
                                    new YesNoDialog.OnPositiveClickedListener()
                                    {
                                        @Override
                                        public void onPositiveClicked()
                                        {
                                            // cancel
                                        }
                                    })
                            .show(
                                    getActivity().getSupportFragmentManager(),
                                    FoclConstants.FRAGMENT_YES_NO_DIALOG + "CoordRefiningProcess");

                } else if (null == mAccurateLocation) {
                    YesNoDialog dialog = new YesNoDialog();
                    dialog.setKeepInstance(true)
                            .setIcon(R.drawable.ic_action_warning)
                            .setTitle(R.string.warning)
                            .setMessage(R.string.coordinates_not_defined_try_again)
                            .setPositiveText(R.string.determine_coordinates)
                            .setOnPositiveClickedListener(
                                    new YesNoDialog.OnPositiveClickedListener()
                                    {
                                        @Override
                                        public void onPositiveClicked()
                                        {
                                            startLocationTaking();
                                        }
                                    })
                            .show(
                                    getActivity().getSupportFragmentManager(),
                                    FoclConstants.FRAGMENT_YES_NO_DIALOG + "CoordinatesTryAgain");

                } else {
                    createObject();
                }
            }
        };

        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            View customActionBarView = actionBar.getCustomView();
            View saveMenuItem = customActionBarView.findViewById(R.id.custom_toolbar_button_layout);
            saveMenuItem.setOnClickListener(doneButtonOnClickListener);
        }

        Toolbar bottomToolbar = activity.getBottomToolbar();
        ImageButton RefreshCoordBtn =
                (ImageButton) bottomToolbar.findViewById(R.id.btn_refresh_coordinates_cl);
        ImageButton CameraBtn = (ImageButton) bottomToolbar.findViewById(R.id.btn_camera_cl);

        RefreshCoordBtn.setOnClickListener(
                new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        startLocationTaking();
                    }
                });

        CameraBtn.setOnClickListener(
                new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        if (mAccurateLocationTaker.isTaking() || null == mAccurateLocation) {
                            YesNoDialog dialog = new YesNoDialog();
                            dialog.setKeepInstance(true)
                                    .setIcon(R.drawable.ic_action_warning)
                                    .setTitle(R.string.warning)
                                    .setMessage(R.string.coordinates_refining_process)
                                    .setPositiveText(R.string.ok)
                                    .setOnPositiveClickedListener(
                                            new YesNoDialog.OnPositiveClickedListener()
                                            {
                                                @Override
                                                public void onPositiveClicked()
                                                {
                                                    // cancel
                                                }
                                            })
                                    .show(
                                            getActivity().getSupportFragmentManager(),
                                            FoclConstants.FRAGMENT_YES_NO_DIALOG
                                                    + "CoordRefiningProcess");
                        } else {
                            showCameraActivity(app);
                        }
                    }
                });

        RecyclerView.LayoutManager layoutManager =
                new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);

        mPhotoGallery.setLayoutManager(layoutManager);
        mPhotoGallery.setHasFixedSize(true);
        setPhotoGalleryAdapter();

        setPhotoGalleryVisibility(true);

        return view;
    }


    @Override
    public void onDestroyView()
    {
        mAccurateLocationTaker.setOnProgressUpdateListener(null);
        mAccurateLocationTaker.setOnGetAccurateLocationListener(null);
        super.onDestroyView();
    }


    @Override
    public void onDestroy()
    {
        mAccurateLocationTaker.stopTaking();
        deleteTempFiles();

        super.onDestroy();
    }


    @Override
    public void onResume()
    {
        super.onResume();

        if (mShowChangeLocationDialog) {
            mShowChangeLocationDialog = false;
            showChangeLocationDialog();
        }
    }


    protected void setBarsView(MainActivity activity)
    {
        activity.setBarsView("");
        activity.switchMenuView();
    }


    protected void setTitleView(MainActivity activity)
    {
        String title = "";

        switch (mFoclStructLayerType) {
            case FoclConstants.LAYERTYPE_FOCL_REAL_OPTICAL_CABLE_POINT:
                title = activity.getString(R.string.cable_laying);
                break;

            case FoclConstants.LAYERTYPE_FOCL_REAL_FOSC:
                title = activity.getString(R.string.fosc_mounting);
                break;

            case FoclConstants.LAYERTYPE_FOCL_REAL_OPTICAL_CROSS:
                title = activity.getString(R.string.cross_mounting);
                break;

            case FoclConstants.LAYERTYPE_FOCL_REAL_ACCESS_POINT:
                title = activity.getString(R.string.access_point_mounting);
                break;

            case FoclConstants.LAYERTYPE_FOCL_REAL_SPECIAL_TRANSITION_POINT:
                title = activity.getString(R.string.special_transition_laying);
                break;
        }

        mTypeWorkTitle.setText(title);
    }


    protected void setBlockedView()
    {
        mLineName.setText("");

        mLayingMethod.setEnabled(false);
        mFoscType.setEnabled(false);
        mFoscPlacement.setEnabled(false);
        mOpticalCrossType.setEnabled(false);
        mSpecialLayingMethod.setEnabled(false);
        mMarkType.setEnabled(false);

        mDescription.setText("");
        mDescription.setEnabled(false);
        mPhotoGallery.setEnabled(false);
        mPhotoGallery.setAdapter(null);
        setPhotoGalleryVisibility(false);
    }


    protected void setFieldVisibility()
    {
        mDistanceLayout.setVisibility(View.GONE);
        mLayingMethodCaption.setVisibility(View.GONE);
        mLayingMethod.setVisibility(View.GONE);
        mFoscTypeCaption.setVisibility(View.GONE);
        mFoscType.setVisibility(View.GONE);
        mFoscPlacementCaption.setVisibility(View.GONE);
        mFoscPlacement.setVisibility(View.GONE);
        mOpticalCrossTypeCaption.setVisibility(View.GONE);
        mOpticalCrossType.setVisibility(View.GONE);
        mSpecialLayingMethodCaption.setVisibility(View.GONE);
        mSpecialLayingMethod.setVisibility(View.GONE);
        mMarkTypeCaption.setVisibility(View.GONE);
        mMarkType.setVisibility(View.GONE);

        switch (mFoclStructLayerType) {
            case FoclConstants.LAYERTYPE_FOCL_REAL_OPTICAL_CABLE_POINT:
                mDistanceLayout.setVisibility(View.VISIBLE);
                mLayingMethodCaption.setVisibility(View.VISIBLE);
                mLayingMethod.setVisibility(View.VISIBLE);
                break;

            case FoclConstants.LAYERTYPE_FOCL_REAL_FOSC:
                mFoscTypeCaption.setVisibility(View.VISIBLE);
                mFoscType.setVisibility(View.VISIBLE);
                mFoscPlacementCaption.setVisibility(View.VISIBLE);
                mFoscPlacement.setVisibility(View.VISIBLE);
                break;

            case FoclConstants.LAYERTYPE_FOCL_REAL_OPTICAL_CROSS:
//                mOpticalCrossTypeCaption.setVisibility(View.VISIBLE);
//                mOpticalCrossType.setVisibility(View.VISIBLE);
                break;

            case FoclConstants.LAYERTYPE_FOCL_REAL_ACCESS_POINT:
                break;

            case FoclConstants.LAYERTYPE_FOCL_REAL_SPECIAL_TRANSITION_POINT:
                mSpecialLayingMethodCaption.setVisibility(View.VISIBLE);
                mSpecialLayingMethod.setVisibility(View.VISIBLE);
                mMarkTypeCaption.setVisibility(View.VISIBLE);
                mMarkType.setVisibility(View.VISIBLE);
                break;
        }
    }


    protected void setFieldDicts()
    {
        FoclDictItem dictItem;

        switch (mFoclStructLayerType) {
            case FoclConstants.LAYERTYPE_FOCL_REAL_OPTICAL_CABLE_POINT:
                dictItem = mFoclProject.getFoclDitcs().get(FoclConstants.FIELD_LAYING_METHOD);
                mLayingMethod.setValues(dictItem);
                break;

            case FoclConstants.LAYERTYPE_FOCL_REAL_FOSC:
                dictItem = mFoclProject.getFoclDitcs().get(FoclConstants.FIELD_FOSC_TYPE);
                mFoscType.setValues(dictItem);

                dictItem = mFoclProject.getFoclDitcs().get(FoclConstants.FIELD_FOSC_PLACEMENT);
                mFoscPlacement.setValues(dictItem);
                break;

            case FoclConstants.LAYERTYPE_FOCL_REAL_OPTICAL_CROSS:
                dictItem = mFoclProject.getFoclDitcs().get(FoclConstants.FIELD_OPTICAL_CROSS_TYPE);
                mOpticalCrossType.setValues(dictItem);
                break;

            case FoclConstants.LAYERTYPE_FOCL_REAL_ACCESS_POINT:
                break;

            case FoclConstants.LAYERTYPE_FOCL_REAL_SPECIAL_TRANSITION_POINT:
                dictItem =
                        mFoclProject.getFoclDitcs().get(FoclConstants.FIELD_SPECIAL_LAYING_METHOD);
                mSpecialLayingMethod.setValues(dictItem);

                dictItem = mFoclProject.getFoclDitcs().get(FoclConstants.FIELD_MARK_TYPE);
                mMarkType.setValues(dictItem);
                break;
        }
    }


    protected void setCoordinatesRefiningView(View paretntView)
    {
        mRefiningProgress = (ProgressBar) paretntView.findViewById(R.id.refining_progress_cr);

        mCoordinates = (TextView) paretntView.findViewById(R.id.coordinates_cr);
        mDistanceLayout = (RelativeLayout) paretntView.findViewById(R.id.distance_layout_cr);
        mDistanceFromPrevPointCaption =
                (TextView) paretntView.findViewById(R.id.distance_from_prev_point_caption_cr);
        mDistanceFromPrevPoint =
                (TextView) paretntView.findViewById(R.id.distance_from_prev_point_cr);

        mRefiningProgress.setMax(MAX_PCT);
        mRefiningProgress.setSecondaryProgress(mTakeCountPct);
        mRefiningProgress.setProgress(mTakeTimePct);

        setCoordinatesText();
        setCoordinatesVisibility(!mAccurateLocationTaker.isTaking());

        mAccurateLocationTaker.setOnProgressUpdateListener(
                new AccurateLocationTaker.OnProgressUpdateListener()
                {
                    @Override
                    public void onProgressUpdate(Long... values)
                    {
                        mTakeCount = values[0].intValue();
                        mTakeCountPct = mTakeCount * MAX_PCT / MAX_ACCURACY_TAKE_COUNT;
                        mTakeTimePct = (int) (values[1] * MAX_PCT / MAX_ACCURACY_TAKE_TIME);

                        mRefiningProgress.setSecondaryProgress(mTakeCountPct);
                        mRefiningProgress.setProgress(mTakeTimePct);
                    }
                });

        mAccurateLocationTaker.setOnGetAccurateLocationListener(
                new AccurateLocationTaker.OnGetAccurateLocationListener()
                {
                    @Override
                    public void onGetAccurateLocation(
                            Location accurateLocation,
                            Long... values)
                    {
                        ++mTakingLoopCount;

                        if (null != accurateLocation) {
                            mAccurateLocation = accurateLocation;
                            setCoordinatesText();
                            setCoordinatesVisibility(true);

                        } else {
                            mAccurateLocation = null;

// fix for #158
//                            if (1 == mTakingLoopCount) {
                            if (CreateObjectFragment.this.isResumed()) {
                                showChangeLocationDialog();
                            } else {
                                mShowChangeLocationDialog = true;
                            }
//                            }

                            startLocationTaking();
                        }
                    }
                });
    }


    protected void showChangeLocationDialog()
    {
        YesNoDialog changeLocationDialog = new YesNoDialog();
        changeLocationDialog.setKeepInstance(true)
                .setIcon(R.drawable.ic_action_warning)
                .setTitle(R.string.warning)
                .setMessage(
                        R.string.coordinates_not_defined_change_location)
                .setPositiveText(R.string.ok)
                .setOnPositiveClickedListener(
                        new YesNoDialog.OnPositiveClickedListener()
                        {
                            @Override
                            public void onPositiveClicked()
                            {
                                // close
                            }
                        })
                .show(
                        getActivity().getSupportFragmentManager(),
                        FoclConstants.FRAGMENT_YES_NO_DIALOG + "ChangeLocation");
    }


    protected void setCoordinatesText()
    {
        if (null != mAccurateLocation) {

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                    getActivity());

            int nFormat = prefs.getInt(
                    FoclSettingsConstantsUI.KEY_PREF_COORD_FORMAT + "_int",
                    Location.FORMAT_DEGREES);

            String latText = getString(R.string.latitude_caption) + " " +
                    FoclLocationUtil.formatLatitude(
                            mAccurateLocation.getLatitude(), nFormat, getResources()) +
                    getString(R.string.coord_lat);

            String longText = getString(R.string.longitude_caption) + " " +
                    FoclLocationUtil.formatLongitude(
                            mAccurateLocation.getLongitude(), nFormat, getResources()) +
                    getString(R.string.coord_lon);

            mCoordinates.setText(latText + ",  " + longText);

            if (FoclConstants.LAYERTYPE_FOCL_REAL_OPTICAL_CABLE_POINT == mFoclStructLayerType) {
                mDistance = getMinDistanceFromPrevPoints(
                        getActivity(), mObjectLayerName, mAccurateLocation);
                mDistanceFromPrevPoint.setText(getDistanceText(getActivity(), mDistance));
                mDistanceFromPrevPoint.setTextColor(getDistanceTextColor(mDistance));
            }

        } else {
            mDistance = null;
            mCoordinates.setText(getText(R.string.coordinates_not_defined));
//            mCoordinates.setGravity(Gravity.CENTER);
            mDistanceFromPrevPoint.setText("---");
            mDistanceFromPrevPoint.setTextColor(
                    getResources().getColor(R.color.selected_object_text_color));
        }
    }


    protected void setCoordinatesVisibility(boolean isRefined)
    {
        if (isRefined) {
            mRefiningProgress.setVisibility(View.INVISIBLE);
//            mCoordinates.setGravity(Gravity.LEFT);
        } else {
            mRefiningProgress.setVisibility(View.VISIBLE);
            mCoordinates.setText(getText(R.string.coordinate_refining));
//            mCoordinates.setGravity(Gravity.CENTER);
        }
    }


    protected boolean setFoclProjectData(GISApplication app)
    {
        mFoclProject = app.getFoclProject();
        if (null == mFoclProject) {
            return false;
        }

        try {
            mFoclStruct = mFoclProject.getFoclStructByRemoteId(mLineRemoteId);
        } catch (Exception e) {
            mFoclStruct = null;
        }
        app.setSelectedFoclStruct(mFoclStruct);
        if (null == mFoclStruct) {
            return false;
        }

        mFoclVectorLayer = (FoclVectorLayer) mFoclStruct.getLayerByFoclType(
                mFoclStructLayerType);

        if (null == mFoclVectorLayer) {
            return false;
        }

        mObjectLayerName = mFoclVectorLayer.getPath().getName();
        return true;
    }


    protected void setObjectCount()
    {
        final GISApplication app = (GISApplication) getActivity().getApplication();
        mObjectCount = 0;

        if (setFoclProjectData(app)) {

            Uri uri = Uri.parse(
                    "content://" + FoclSettingsConstantsUI.AUTHORITY + "/" + mObjectLayerName);
            String proj[] = {FIELD_ID};

            Cursor objectCursor;

            try {
                objectCursor =
                        getActivity().getContentResolver().query(uri, proj, null, null, null);
            } catch (Exception e) {
                Log.d(TAG, e.getLocalizedMessage());
                objectCursor = null;
            }

            if (null != objectCursor) {
                mObjectCount = objectCursor.getCount();
                objectCursor.close();
            }
        }
    }


    protected void startLocationTaking()
    {
        mAccurateLocation = null;
        mDistance = null;

        mTakeCount = 0;
        mTakeCountPct = 0;
        mTakeTimePct = 0;
        mRefiningProgress.setSecondaryProgress(mTakeCountPct);
        mRefiningProgress.setProgress(mTakeTimePct);

        setCoordinatesText();
        setCoordinatesVisibility(false);

        mAccurateLocationTaker.startTaking();
    }


    public boolean onContextItemSelected(MenuItem menuItem)
    {
        final long itemId;

        try {
            itemId = ((ObjectPhotoFileAdapter) mPhotoGallery.getAdapter()).getSelectedItemId();

        } catch (Exception e) {
            Log.d(TAG, e.getLocalizedMessage());
            return super.onContextItemSelected(menuItem);
        }

        switch (menuItem.getItemId()) {
            case R.id.menu_show_photo:
                showPhoto(itemId);
                break;

            case R.id.menu_delete_photo:
                YesNoDialog dialog = new YesNoDialog();
                dialog.setKeepInstance(true)
                        .setIcon(R.drawable.ic_action_warning)
                        .setTitle(R.string.delete_photo_ask)
                        .setMessage(R.string.delete_photo_message_2)
                        .setPositiveText(R.string.ok)
                        .setNegativeText(R.string.cancel)
                        .setOnPositiveClickedListener(
                                new YesNoDialog.OnPositiveClickedListener()
                                {
                                    @Override
                                    public void onPositiveClicked()
                                    {
                                        deletePhoto(itemId);
                                    }
                                })
                        .setOnNegativeClickedListener(
                                new YesNoDialog.OnNegativeClickedListener()
                                {
                                    @Override
                                    public void onNegativeClicked()
                                    {
                                        // cancel
                                    }
                                })
                        .show(
                                getActivity().getSupportFragmentManager(),
                                FoclConstants.FRAGMENT_YES_NO_DIALOG + "DeletePhoto");
                break;
        }

        return super.onContextItemSelected(menuItem);
    }


    protected void showPhoto(long itemId)
    {
        // get file path of photo file
        ObjectPhotoFileAdapter adapter = (ObjectPhotoFileAdapter) mPhotoGallery.getAdapter();
        File photoFile = adapter.getItemPhotoFile((int) itemId);
        String absolutePath = photoFile.getAbsolutePath();

        if (TextUtils.isEmpty(absolutePath)) {
            return;
        }

        // show photo in system program
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse("file://" + absolutePath), "image/*");

        startActivity(intent);
    }


    protected void deletePhoto(long itemId)
    {
        // get file path of photo file
        ObjectPhotoFileAdapter adapter = (ObjectPhotoFileAdapter) mPhotoGallery.getAdapter();
        File photoFile = adapter.getItemPhotoFile((int) itemId);

        photoFile.delete();

        setPhotoGalleryAdapter();
        setPhotoGalleryVisibility(true);
    }


    protected void setPhotoGalleryAdapter()
    {
        GISApplication app = (GISApplication) getActivity().getApplication();

        try {
            mObjectPhotoFileAdapter = new ObjectPhotoFileAdapter(getActivity(), app.getDataDir());

            mObjectPhotoFileAdapter.setOnPhotoClickListener(
                    new ObjectPhotoFileAdapter.OnPhotoClickListener()
                    {
                        @Override
                        public void onPhotoClick(long itemId)
                        {
                            showPhoto(itemId);
                        }
                    });

        } catch (IOException e) {
            Log.d(TAG, e.getLocalizedMessage());
            mObjectPhotoFileAdapter = null;
            Toast.makeText(
                    getActivity(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }

        mPhotoGallery.setAdapter(mObjectPhotoFileAdapter);
    }


    protected void setPhotoGalleryVisibility(boolean visible)
    {
        if (visible) {

            if (null != mObjectPhotoFileAdapter && mObjectPhotoFileAdapter.getItemCount() > 0) {
                mPhotoHintText.setVisibility(View.GONE);
                mPhotoGallery.setVisibility(View.VISIBLE);
            } else {
                mPhotoHintText.setVisibility(View.VISIBLE);
                mPhotoGallery.setVisibility(View.GONE);
            }

        } else {
            mPhotoHintText.setVisibility(View.GONE);
            mPhotoGallery.setVisibility(View.GONE);
        }
    }


    protected void showCameraActivity(GISApplication app)
    {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Ensure that there's a camera activity to handle the intent
        if (null != cameraIntent.resolveActivity(getActivity().getPackageManager())) {

            try {
                String timeStamp =
                        new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
                File tempFile = new File(
                        app.getDataDir(),
                        FoclConstants.TEMP_PHOTO_FILE_PREFIX + timeStamp + ".jpg");

                if (!tempFile.exists() && tempFile.createNewFile()
                        || tempFile.exists() && tempFile.delete() &&
                        tempFile.createNewFile()) {

                    mTempPhotoPath = tempFile.getAbsolutePath();
                    Log.d(TAG, "mTempPhotoPath: " + mTempPhotoPath);

                    cameraIntent.putExtra(
                            MediaStore.EXTRA_OUTPUT, Uri.fromFile(tempFile));
                    startActivityForResult(cameraIntent, REQUEST_TAKE_PHOTO);
                }

            } catch (IOException e) {
                Toast.makeText(
                        getActivity(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }


    @Override
    public void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data)
    {
        File tempPhotoFile = new File(mTempPhotoPath);

        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == Activity.RESULT_OK) {

            Location currLocation = mGpsEventSource.getLastKnownLocation();
            float dist = mAccurateLocation.distanceTo(currLocation);

            if (dist > FoclConstants.MAX_DISTANCE_FROM_OBJECT_TO_PHOTO) {

                if (tempPhotoFile.delete()) {
                    Log.d(
                            TAG,
                            "tempPhotoFile deleted on Activity.RESULT_OK and bad coordinates, path: "
                                    + tempPhotoFile.getAbsolutePath());

                    YesNoDialog dialog = new YesNoDialog();
                    dialog.setKeepInstance(true)
                            .setIcon(R.drawable.ic_action_warning)
                            .setTitle(R.string.photo_not_saved)
                            .setMessage(R.string.photo_not_saved_distance_exceed)
                            .setPositiveText(R.string.ok)
                            .setOnPositiveClickedListener(
                                    new YesNoDialog.OnPositiveClickedListener()
                                    {
                                        @Override
                                        public void onPositiveClicked()
                                        {
                                            // cancel
                                        }
                                    })
                            .show(
                                    getActivity().getSupportFragmentManager(),
                                    FoclConstants.FRAGMENT_YES_NO_DIALOG + "PhotoNotSaved");

                } else {
                    Log.d(
                            TAG,
                            "tempPhotoFile delete FAILED on Activity.RESULT_OK and bad coordinates, path: "
                                    + tempPhotoFile.getAbsolutePath());
                }

            } else {
                setPhotoGalleryAdapter();
                setPhotoGalleryVisibility(true);
            }
        }

        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == Activity.RESULT_CANCELED) {
            if (tempPhotoFile.delete()) {
                Log.d(
                        TAG, "tempPhotoFile deleted on Activity.RESULT_CANCELED, path: "
                                + tempPhotoFile.getAbsolutePath());
            } else {
                Log.d(
                        TAG, "tempPhotoFile delete FAILED on Activity.RESULT_CANCELED, path: "
                                + tempPhotoFile.getAbsolutePath());
            }
        }
    }


    protected void writePhotoAttach(File tempPhotoFile)
            throws IOException
    {
        GISApplication app = (GISApplication) getActivity().getApplication();

        ContentResolver contentResolver = app.getContentResolver();
        String photoFileName = getPhotoFileName(tempPhotoFile);
        Log.d(TAG, "CreateObjectFragment, writePhotoAttach(), photoFileName: " + photoFileName);

        Uri allAttachesUri = Uri.parse(
                "content://" + FoclSettingsConstantsUI.AUTHORITY +
                        "/" + mObjectLayerName + "/" + mObjectId + "/attach");
        Log.d(TAG, "CreateObjectFragment, writePhotoAttach(), allAttachesUri: " + allAttachesUri);

        ContentValues values = new ContentValues();
        values.put(VectorLayer.ATTACH_DISPLAY_NAME, photoFileName);
        values.put(VectorLayer.ATTACH_MIME_TYPE, "image/jpeg");
        //values.put(VectorLayer.ATTACH_DESCRIPTION, photoFileName);

        Uri attachUri = null;
        String insertAttachError = null;
        try {
            attachUri = contentResolver.insert(allAttachesUri, values);
            Log.d(TAG, "CreateObjectFragment, writePhotoAttach(), insert: " + attachUri.toString());
        } catch (Exception e) {
            Log.d(
                    TAG, "CreateObjectFragment, writePhotoAttach(), Insert attach failed: "
                            + e.getLocalizedMessage());
            insertAttachError = "Insert attach failed: " + e.getLocalizedMessage();
        }

        if (null != attachUri) {
            int exifOrientation = BitmapUtil.getOrientationFromExif(tempPhotoFile);

            // resize and rotate
            Bitmap sourceBitmap = BitmapFactory.decodeFile(tempPhotoFile.getPath());
            Bitmap resizedBitmap = BitmapUtil.getResizedBitmap(
                    sourceBitmap, FoclConstants.PHOTO_MAX_SIZE_PX, FoclConstants.PHOTO_MAX_SIZE_PX);
            Bitmap rotatedBitmap = BitmapUtil.rotateBitmap(resizedBitmap, exifOrientation);

            // jpeg compress
            File tempAttachFile = File.createTempFile("attach", null, app.getCacheDir());
            OutputStream tempOutStream = new FileOutputStream(tempAttachFile);
            rotatedBitmap.compress(
                    Bitmap.CompressFormat.JPEG, FoclConstants.PHOTO_JPEG_COMPRESS_QUALITY,
                    tempOutStream);
            tempOutStream.close();

            int newHeight = rotatedBitmap.getHeight();
            int newWidth = rotatedBitmap.getWidth();

            rotatedBitmap.recycle();

            // write EXIF to new file
            BitmapUtil.copyExifData(tempPhotoFile, tempAttachFile);
            BitmapUtil.writeLocationToExif(
                    tempAttachFile, mAccurateLocation, app.getGpsTimeOffset());

            ExifInterface attachExif = new ExifInterface(tempAttachFile.getCanonicalPath());

            attachExif.setAttribute(
                    ExifInterface.TAG_ORIENTATION, "" + ExifInterface.ORIENTATION_NORMAL);
            attachExif.setAttribute(
                    ExifInterface.TAG_IMAGE_LENGTH, "" + newHeight);
            attachExif.setAttribute(
                    ExifInterface.TAG_IMAGE_WIDTH, "" + newWidth);

            attachExif.saveAttributes();

            // attach data from tempAttachFile
            OutputStream attachOutStream = contentResolver.openOutputStream(attachUri);
            if (attachOutStream != null) {
                FoclFileUtil.copy(new FileInputStream(tempAttachFile), attachOutStream);
                attachOutStream.close();
            } else {
                Log.d(
                        TAG,
                        "CreateObjectFragment, writePhotoAttach(), attachOutStream == null, attachUri"
                                + attachUri.toString());
            }

            if (!tempAttachFile.delete()) {
                Log.d(
                        TAG,
                        "CreateObjectFragment, writePhotoAttach(), tempAttachFile.delete() failed, tempAttachFile:"
                                + tempAttachFile.getAbsolutePath());
            }
        }

        if (app.isOriginalPhotoSaving()) {
            BitmapUtil.writeLocationToExif(
                    tempPhotoFile, mAccurateLocation, app.getGpsTimeOffset());
            File origPhotoFile = new File(getDailyPhotoFolder(), photoFileName);

            if (!com.nextgis.maplib.util.FileUtil.move(tempPhotoFile, origPhotoFile)) {
                Log.d(
                        TAG,
                        "CreateObjectFragment, writePhotoAttach(), move original failed, tempPhotoFile:"
                                +
                                tempPhotoFile.getAbsolutePath() + ", origPhotoFile: " +
                                origPhotoFile.getAbsolutePath());
                throw new IOException(
                        "Save original photo failed, tempPhotoFile: "
                                + tempPhotoFile.getAbsolutePath());
            }

        } else {
            if (!tempPhotoFile.delete()) {
                Log.d(
                        TAG,
                        "CreateObjectFragment, writePhotoAttach(), tempPhotoFile.delete() failed, tempPhotoFile:"
                                + tempPhotoFile.getAbsolutePath());
            }
        }

        if (null != insertAttachError) {
            throw new IOException(insertAttachError);
        }
    }


    protected void writePhotoAttaches()
    {
        GISApplication app = (GISApplication) getActivity().getApplication();

        try {
            File dataDir = app.getDataDir();

            for (File tempPhotoFile : dataDir.listFiles()) {
                if (tempPhotoFile.getName().matches(
                        FoclConstants.TEMP_PHOTO_FILE_PREFIX + ".*\\.jpg")) {

                    writePhotoAttach(tempPhotoFile);
                }
            }

        } catch (IOException e) {
            String msg = "Write photo attaches failed, " + e.getLocalizedMessage();
            Log.d(TAG, "CreateObjectFragment, writePhotoAttaches(), " + msg);
            Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG).show();
        }
    }


    protected void deleteTempFiles()
    {
        GISApplication app = (GISApplication) getActivity().getApplication();

        try {
            File dataDir = app.getDataDir();

            for (File tempPhotoFile : dataDir.listFiles()) {
                if (tempPhotoFile.getName().matches(
                        FoclConstants.TEMP_PHOTO_FILE_PREFIX + ".*\\.jpg")) {

                    tempPhotoFile.delete();
                }
            }

        } catch (IOException e) {
            Log.d(TAG, e.getLocalizedMessage());
            Toast.makeText(
                    getActivity(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }


    protected String getPhotoFileName(File tempPhotoFile)
            throws IOException
    {
        String prefix = "";

        switch (mFoclStructLayerType) {
            case FoclConstants.LAYERTYPE_FOCL_REAL_OPTICAL_CABLE_POINT:
                prefix = "Optical_Cable_Laying_";
                break;

            case FoclConstants.LAYERTYPE_FOCL_REAL_FOSC:
                prefix = "FOSC_Mounting_";
                break;

            case FoclConstants.LAYERTYPE_FOCL_REAL_OPTICAL_CROSS:
                prefix = "Cross_Mounting_";
                break;

            case FoclConstants.LAYERTYPE_FOCL_REAL_ACCESS_POINT:
                prefix = "Access_Point_Mounting_";
                break;

            case FoclConstants.LAYERTYPE_FOCL_REAL_SPECIAL_TRANSITION_POINT:
                prefix = "Special_Transition_Point_";
                break;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
        Date date = BitmapUtil.getExifDate(tempPhotoFile);

        if (null == date) {
            date = new Date();
            date.setTime(System.currentTimeMillis());
        }

        String timeStamp = sdf.format(date);

        return prefix + timeStamp + ".jpg";
    }


    protected File getDailyPhotoFolder()
            throws IOException
    {
        final GISApplication app = (GISApplication) getActivity().getApplication();
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        return FoclFileUtil.getDirWithCreate(app.getPhotoPath() + File.separator + timeStamp);
    }


    protected void createObject()
    {
        String fieldName = null;

        switch (mFoclStructLayerType) {
            case FoclConstants.LAYERTYPE_FOCL_REAL_OPTICAL_CABLE_POINT:
                if (null == mLayingMethod.getValue()) {
                    fieldName = getActivity().getString(R.string.laying_method);
                }
                break;

            case FoclConstants.LAYERTYPE_FOCL_REAL_FOSC:
//                if (null == mFoscType.getValue()) {
//                    fieldName = getActivity().getString(R.string.fosc_type);
//                    break;
//                }
                if (null == mFoscPlacement.getValue()) {
                    fieldName = getActivity().getString(R.string.fosc_placement);
                }
                break;

            case FoclConstants.LAYERTYPE_FOCL_REAL_OPTICAL_CROSS:
//                if (null == mOpticalCrossType.getValue()) {
//                    fieldName = getActivity().getString(R.string.optical_cross_type);
//                }
                break;

            case FoclConstants.LAYERTYPE_FOCL_REAL_ACCESS_POINT:
                break;

            case FoclConstants.LAYERTYPE_FOCL_REAL_SPECIAL_TRANSITION_POINT:
                if (null == mSpecialLayingMethod.getValue()) {
                    fieldName = getActivity().getString(R.string.special_laying_method);
                    break;
                }
                if (null == mMarkType.getValue()) {
                    fieldName = getActivity().getString(R.string.mark_type);
                }
                break;
        }

        if (null != fieldName) {
            YesNoDialog dialog = new YesNoDialog();
            dialog.setKeepInstance(true)
                    .setIcon(R.drawable.ic_action_warning)
                    .setTitle(R.string.warning)
                    .setMessage(String.format(getString(R.string.empty_field_warning), fieldName))
                    .setPositiveText(R.string.ok)
                    .setOnPositiveClickedListener(
                            new YesNoDialog.OnPositiveClickedListener()
                            {
                                @Override
                                public void onPositiveClicked()
                                {
                                    // cancel
                                }
                            })
                    .show(
                            getActivity().getSupportFragmentManager(),
                            FoclConstants.FRAGMENT_YES_NO_DIALOG + "FieldsNotNull");
            return; // we do not need logcat here
        }


        if (!mNewStartPoint && 0 < mObjectCount &&
                FoclConstants.LAYERTYPE_FOCL_REAL_OPTICAL_CABLE_POINT == mFoclStructLayerType &&
                null != mDistance &&
                FoclConstants.MAX_DISTANCE_FROM_PREV_POINT < mDistance) {

            showDistanceExceededDialog();
            return; // we do not need logcat here
        }


        if (null == mObjectPhotoFileAdapter || mObjectPhotoFileAdapter.getItemCount() == 0) {
            YesNoDialog dialog = new YesNoDialog();
            dialog.setKeepInstance(true)
                    .setIcon(R.drawable.ic_action_warning)
                    .setTitle(R.string.warning)
                    .setMessage(getString(R.string.take_photos_to_confirm))
                    .setPositiveText(R.string.ok)
                    .setOnPositiveClickedListener(
                            new YesNoDialog.OnPositiveClickedListener()
                            {
                                @Override
                                public void onPositiveClicked()
                                {
                                    // cancel
                                }
                            })
                    .show(
                            getActivity().getSupportFragmentManager(),
                            FoclConstants.FRAGMENT_YES_NO_DIALOG + "TakePhotos");
            return; // we do not need logcat here
        }


        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View view = inflater.inflate(R.layout.dialog_waiting, null);

        final YesNoDialog waitProgressDialog = new YesNoDialog();
        waitProgressDialog.setKeepInstance(true)
                .setIcon(R.drawable.ic_action_data_usage)
                .setTitle(R.string.waiting)
                .setView(view, true);

        waitProgressDialog.setCancelable(false);
        waitProgressDialog.show(
                getActivity().getSupportFragmentManager(),
                FoclConstants.FRAGMENT_YES_NO_DIALOG + "WaitProgress");


        final Handler handler = new Handler()
        {
            public void handleMessage(Message msg)
            {
                switch (msg.what) {
                    case CREATE_OBJECT_DONE:
                        waitProgressDialog.dismiss();
                        break;
                    case CREATE_OBJECT_OK:
                        getActivity().getSupportFragmentManager().popBackStack();
                        break;
                    case CREATE_OBJECT_FAILED:
                        Toast.makeText(getActivity(), (String) msg.obj, Toast.LENGTH_LONG).show();
                        waitProgressDialog.dismiss();
                        break;
                }
            }
        };

        RunnableFuture<Void> future = new FutureTask<Void>(
                new Callable<Void>()
                {
                    @Override
                    public Void call()
                            throws Exception
                    {
                        createObjectTask();
                        return null;
                    }
                })
        {
            @Override
            protected void done()
            {
                super.done();
                handler.sendEmptyMessage(CREATE_OBJECT_DONE);
            }


            @Override
            protected void set(Void aVoid)
            {
                super.set(aVoid);
                handler.sendEmptyMessage(CREATE_OBJECT_OK);
            }


            @Override
            protected void setException(Throwable t)
            {
                super.setException(t);
                Message msg = handler.obtainMessage(CREATE_OBJECT_FAILED, t.getLocalizedMessage());
                msg.sendToTarget();
            }
        };

        new Thread(future).start();
    }


    protected void createObjectTask()
            throws IOException
    {
        GISApplication app = (GISApplication) getActivity().getApplication();

        Uri uri = Uri.parse(
                "content://" + FoclSettingsConstantsUI.AUTHORITY + "/" +
                        mObjectLayerName);
        Log.d(TAG, "CreateObjectFragment, createObject(), uri: " + uri.toString());

        ContentValues values = new ContentValues();

        long builtDate = mAccurateLocation.getTime() + app.getGpsTimeOffset();
        values.put(FoclConstants.FIELD_BUILT_DATE, builtDate);
        Log.d(TAG, "CreateObjectFragment, createObject(), builtDate: " + builtDate);

        String descriptionText = mDescription.getText().toString();
        values.put(FoclConstants.FIELD_DESCRIPTION, descriptionText);
        Log.d(TAG, "CreateObjectFragment, createObject(), descriptionText: " + descriptionText);

        try {
            GeoPoint pt = new GeoPoint(
                    mAccurateLocation.getLongitude(), mAccurateLocation.getLatitude());
            pt.setCRS(CRS_WGS84);
            pt.project(CRS_WEB_MERCATOR);
            GeoMultiPoint mpt = new GeoMultiPoint();
            mpt.add(pt);
            values.put(FIELD_GEOM, mpt.toBlob());
            Log.d(TAG, "CreateObjectFragment, createObject(), pt: " + pt.toString());

        } catch (IOException e) {
            e.printStackTrace();
        }

        String value;

        switch (mFoclStructLayerType) {
            case FoclConstants.LAYERTYPE_FOCL_REAL_OPTICAL_CABLE_POINT:
                if (0 == mObjectCount || mNewStartPoint) {
                    values.put(FIELD_START_POINT, true);
                }
                value = mLayingMethod.getValue();
                values.put(FIELD_LAYING_METHOD, value);
                Log.d(TAG, "CreateObjectFragment, createObject(), FIELD_LAYING_METHOD: " + value);
                break;

            case FoclConstants.LAYERTYPE_FOCL_REAL_FOSC:
                value = mFoscType.getValue();
                values.put(FIELD_FOSC_TYPE, value);
                Log.d(TAG, "CreateObjectFragment, createObject(), FIELD_FOSC_TYPE: " + value);

                value = mFoscPlacement.getValue();
                values.put(FIELD_FOSC_PLACEMENT, value);
                Log.d(TAG, "CreateObjectFragment, createObject(), FIELD_FOSC_PLACEMENT: " + value);
                break;

            case FoclConstants.LAYERTYPE_FOCL_REAL_OPTICAL_CROSS:
//                value = mOpticalCrossType.getValue();
//                values.put(FIELD_OPTICAL_CROSS_TYPE, value);
//                Log.d(TAG, "CreateObjectFragment, createObject(), FIELD_OPTICAL_CROSS_TYPE: " + value);
                break;

            case FoclConstants.LAYERTYPE_FOCL_REAL_ACCESS_POINT:
                break;

            case FoclConstants.LAYERTYPE_FOCL_REAL_SPECIAL_TRANSITION_POINT:
                value = mSpecialLayingMethod.getValue();
                values.put(FIELD_SPECIAL_LAYING_METHOD, value);
                Log.d(
                        TAG, "CreateObjectFragment, createObject(), FIELD_SPECIAL_LAYING_METHOD: "
                                + value);

                value = mMarkType.getValue();
                values.put(FIELD_MARK_TYPE, value);
                Log.d(TAG, "CreateObjectFragment, createObject(), FIELD_MARK_TYPE: " + value);
                break;
        }

        Uri result = getActivity().getContentResolver().insert(uri, values);

        if (result == null) {
            Log.d(
                    TAG, "CreateObjectFragment, createObject(), Layer: " + mObjectLayerName +
                            ", insert FAILED");
            throw new IOException(getString(R.string.object_creation_error));

        } else {

            if (mFoclStruct.getStatus().equals(FoclConstants.FIELD_VALUE_STATUS_PROJECT)) {
                mFoclStruct.setStatus(FoclConstants.FIELD_VALUE_STATUS_IN_PROGRESS);
                mFoclStruct.setIsStatusChanged(true);
                mFoclStruct.setStatusUpdateTime(builtDate);
                mFoclStruct.save();
            }

            mObjectId = Long.parseLong(result.getLastPathSegment());
            Log.d(
                    TAG,
                    "CreateObjectFragment, createObject(), Layer: " + mObjectLayerName + ", id: " +
                            mObjectId +
                            ", insert result: " + result);
            writePhotoAttaches();
        }


        try {
            mLogcatWriter.writeLogcat(app.getMainLogcatFilePath());
            mLogcatWriter.stopLogcat();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static Float getMinDistanceFromPrevPoints(
            FragmentActivity activity,
            String objectLayerName,
            Location location)
    {
        Float minDist = null;

        Uri uri = Uri.parse(
                "content://" + FoclSettingsConstantsUI.AUTHORITY + "/" + objectLayerName);
        String[] columns = new String[] {FIELD_GEOM};

        Cursor cursor;
        try {
            cursor = activity.getContentResolver().query(uri, columns, null, null, null);
        } catch (Exception e) {
            Log.d(TAG, e.getLocalizedMessage());
            cursor = null;
        }

        if (null != cursor) {
            try {
                if (cursor.moveToFirst()) {
                    int columnGeom = cursor.getColumnIndex(FIELD_GEOM);

                    do {
                        try {
                            GeoGeometry geometry =
                                    GeoGeometryFactory.fromBlob(cursor.getBlob(columnGeom));

                            if (null != geometry && geometry instanceof GeoMultiPoint) {
                                GeoMultiPoint mpt = (GeoMultiPoint) geometry;

                                if (0 < mpt.size()) {

                                    int kk = 0;
                                    do {
                                        GeoPoint pt = new GeoPoint(mpt.get(kk));
                                        pt.setCRS(GeoConstants.CRS_WEB_MERCATOR);
                                        pt.project(GeoConstants.CRS_WGS84);

                                        Location dstLocation = new Location("");
                                        dstLocation.setLatitude(pt.getY());
                                        dstLocation.setLongitude(pt.getX());

                                        float dist = location.distanceTo(dstLocation);
                                        minDist = null == minDist ? dist : Math.min(minDist, dist);

                                        ++kk;
                                    } while (kk < mpt.size());
                                }
                            }

                        } catch (IOException | ClassNotFoundException e) {
                            // e.printStackTrace();
                        }

                    } while (cursor.moveToNext());
                }
            } catch (Exception e) {
                //Log.d(TAG, e.getLocalizedMessage());
            } finally {
                cursor.close();
            }
        }

        return minDist;
    }


    public static String getDistanceText(
            Context context,
            Float distance)
    {
        if (null == distance) {
            return "--";
        }

        DecimalFormat df = new DecimalFormat("0");
        return df.format(distance) + " " + context.getString(R.string.distance_unit);
    }


    public static int getDistanceTextColor(Float distance)
    {
        if (null == distance) {
            return 0xFF000000;
        }

        return FoclConstants.MAX_DISTANCE_FROM_PREV_POINT < distance ? 0xFF880000 : 0xFF008800;
    }


    protected void showDistanceExceededDialog()
    {
        DistanceExceededDialog distanceExceededDialog = new DistanceExceededDialog();
        distanceExceededDialog.setParams(mObjectLayerName, mDistance);
        distanceExceededDialog.show(
                getActivity().getSupportFragmentManager(),
                FoclConstants.FRAGMENT_DISTANCE_EXCEEDED);
    }


    @Override
    public void onNewPointClicked()
    {
        YesNoDialog newPointDialog = new YesNoDialog();
        newPointDialog.setKeepInstance(true)
                .setIcon(R.drawable.ic_action_warning)
                .setTitle(R.string.confirmation)
                .setMessage(R.string.confirm_new_start_point_creating)
                .setPositiveText(R.string.yes)
                .setNegativeText(R.string.no)
                .setOnPositiveClickedListener(
                        new YesNoDialog.OnPositiveClickedListener()
                        {
                            @Override
                            public void onPositiveClicked()
                            {
                                mNewStartPoint = true;
                                createObject();
                            }
                        })
                .setOnNegativeClickedListener(
                        new YesNoDialog.OnNegativeClickedListener()
                        {
                            @Override
                            public void onNegativeClicked()
                            {
                                // cancel
                            }
                        });

        newPointDialog.show(
                getActivity().getSupportFragmentManager(),
                FoclConstants.FRAGMENT_YES_NO_DIALOG + "NewPointDialog");
    }


    @Override
    public void onRepeatClicked()
    {
        startLocationTaking();
    }
}
