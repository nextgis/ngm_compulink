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

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import com.nextgis.ngm_clink_monitoring.map.FoclDictItem;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;


public class ComboboxControl
        extends Spinner

{
    protected Context             mContext;
    protected Map<String, String> mAliasValueMap;


    public ComboboxControl(Context context)
    {
        super(context);
        mContext = context;
    }


    public ComboboxControl(
            Context context,
            AttributeSet attrs)
    {
        super(context, attrs);
        mContext = context;
    }


    public ComboboxControl(
            Context context,
            AttributeSet attrs,
            int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        mContext = context;
    }


    public void setValues(FoclDictItem dictItem)
    {
        mAliasValueMap = new HashMap<>();

        ArrayAdapter<String> spinnerArrayAdapter =
                new ArrayAdapter<>(mContext, android.R.layout.simple_spinner_item);
        setAdapter(spinnerArrayAdapter);

        for (Map.Entry<String, String> entry : dictItem.entrySet()) {
            String value = entry.getKey();
            String value_alias = entry.getValue();

            mAliasValueMap.put(value_alias, value);
            spinnerArrayAdapter.add(value_alias);
        }

        spinnerArrayAdapter.sort(
                new Comparator<String>()
                {
                    @Override
                    public int compare(
                            String lhs,
                            String rhs)
                    {
                        if (null == lhs && null == rhs) {
                            return 0;
                        }

                        if (null == lhs) {
                            return -1;
                        }

                        if (null == rhs) {
                            return 1;
                        }

                        return lhs.compareToIgnoreCase(rhs);
                    }
                });

        // The drop down view
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        float minHeight = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 14, getResources().getDisplayMetrics());
        setPadding(0, (int) minHeight, 0, (int) minHeight);
    }


    public Object getValue()
    {
        String valueAlias = (String) getSelectedItem();
        return mAliasValueMap.get(valueAlias);
    }
}
