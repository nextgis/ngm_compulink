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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.nextgis.ngm_clink_monitoring.GISApplication;
import com.nextgis.ngm_clink_monitoring.R;
import com.nextgis.ngm_clink_monitoring.activities.MainActivity;
import com.nextgis.ngm_clink_monitoring.map.FoclProject;
import com.nextgis.ngm_clink_monitoring.map.FoclStruct;
import com.nextgis.ngm_clink_monitoring.util.FoclConstants;


public class ObjectTypesFragment
        extends Fragment
{
    protected Integer mLineId;

    protected TextView mLineName;
    protected Button   mBtnCableLaying;
    protected Button   mBtnFoscMounting;
    protected Button   mBtnCrossMounting;
    protected Button   mBtnAccessPointMounting;
    protected Button mBtnHidMounting;


    public void setParams(Integer lineId)
    {
        mLineId = lineId;
    }


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }


    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState)
    {
        final MainActivity activity = (MainActivity) getActivity();
        activity.setBarsView(null);

        final View view = inflater.inflate(R.layout.fragment_object_types, null);

        mLineName = (TextView) view.findViewById(R.id.line_name_ot);

        mBtnCableLaying = (Button) view.findViewById(R.id.btn_cable_laying);
        mBtnFoscMounting = (Button) view.findViewById(R.id.btn_fosc_mounting);
        mBtnCrossMounting = (Button) view.findViewById(R.id.btn_cross_mounting);
        mBtnAccessPointMounting = (Button) view.findViewById(R.id.btn_access_point_mounting);
        mBtnHidMounting = (Button) view.findViewById(R.id.btn_hid_mounting);

        GISApplication app = (GISApplication) getActivity().getApplication();
        FoclProject foclProject = app.getFoclProject();

        if (null == foclProject) {
            setBlockedView();
            return view;
        }

        FoclStruct foclStruct;
        try {
            foclStruct = (FoclStruct) foclProject.getLayer(mLineId);
        } catch (Exception e) {
            foclStruct = null;
        }

        if (null == foclStruct) {
            setBlockedView();
            return view;
        }

        mLineName.setText(Html.fromHtml(foclStruct.getHtmlFormattedName()));

        View.OnClickListener buttonOnClickListener = new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                int foclStructLayerType;

                switch (v.getId()) {
                    case R.id.btn_cable_laying:
                        foclStructLayerType = FoclConstants.LAYERTYPE_FOCL_OPTICAL_CABLE;
                        break;
                    case R.id.btn_fosc_mounting:
                        foclStructLayerType = FoclConstants.LAYERTYPE_FOCL_FOSC;
                        break;
                    case R.id.btn_cross_mounting:
                        foclStructLayerType = FoclConstants.LAYERTYPE_FOCL_OPTICAL_CROSS;
                        break;
                    case R.id.btn_access_point_mounting:
                        foclStructLayerType = FoclConstants.LAYERTYPE_FOCL_ACCESS_POINT;
                        break;
                    case R.id.btn_hid_mounting:
                        foclStructLayerType = FoclConstants.LAYERTYPE_FOCL_HID;
                        break;
                    default:
                        foclStructLayerType = FoclConstants.LAYERTYPE_FOCL_UNKNOWN;
                        break;
                }

                onButtonClick(foclStructLayerType);
            }
        };

        mBtnCableLaying.setOnClickListener(buttonOnClickListener);
        mBtnFoscMounting.setOnClickListener(buttonOnClickListener);
        mBtnCrossMounting.setOnClickListener(buttonOnClickListener);
        mBtnAccessPointMounting.setOnClickListener(buttonOnClickListener);
        mBtnHidMounting.setOnClickListener(buttonOnClickListener);

        return view;
    }


    protected void setBlockedView()
    {
        mLineName.setText("");
        mBtnCableLaying.setEnabled(false);
        mBtnFoscMounting.setEnabled(false);
        mBtnCrossMounting.setEnabled(false);
        mBtnAccessPointMounting.setEnabled(false);
        mBtnHidMounting.setEnabled(false);
    }


    public void onButtonClick(int foclStructLayerType)
    {
        final FragmentManager fm = getActivity().getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        ObjectListFragment objectListFragment =
                (ObjectListFragment) fm.findFragmentByTag(FoclConstants.FRAGMENT_OBJECT_LIST);

        if (objectListFragment == null) {
            objectListFragment = new ObjectListFragment();
        }

        objectListFragment.setParams(mLineId, foclStructLayerType);

        ft.replace(R.id.main_fragment, objectListFragment, FoclConstants.FRAGMENT_OBJECT_LIST);
        ft.addToBackStack(null);
        ft.commit();
    }
}
