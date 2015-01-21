/*******************************************************************************
 * Project:  NextGIS mobile apps for Compulink
 * Purpose:  Mobile GIS for Android
 * Authors:  Dmitry Baryshnikov (aka Bishop), polimax@mail.ru
 *           NikitaFeodonit, nfeodonit@yandex.com
 * *****************************************************************************
 * Copyright (C) 2014-2015 NextGIS
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package com.nextgis.ngm_clink_monitoring.map;

import android.content.Context;
import com.nextgis.maplib.map.NGWVectorLayer;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import static com.nextgis.ngm_clink_monitoring.util.FoclConstants.LAYERTYPE_FOCL_VECTOR;


public class FoclVectorLayer
        extends NGWVectorLayer
{
    protected static final String JSON_FOCL_TYPE_KEY = "focl_type";

    protected String mFoclLayerType;


    public FoclVectorLayer(
            Context context,
            File path)
    {
        super(context, path);
    }


    public String getFoclLayerType()
    {
        return mFoclLayerType;
    }


    public void setFoclLayerType(String foclLayerType)
    {
        mFoclLayerType = foclLayerType;
    }


    @Override
    public int getType()
    {
        return LAYERTYPE_FOCL_VECTOR;
    }


    @Override
    public JSONObject toJSON()
            throws JSONException
    {
        JSONObject rootConfig = super.toJSON();
        rootConfig.put(JSON_FOCL_TYPE_KEY, mFoclLayerType);
        return rootConfig;
    }


    @Override
    public void fromJSON(JSONObject jsonObject)
            throws JSONException
    {
        super.fromJSON(jsonObject);
        mFoclLayerType = jsonObject.getString(JSON_FOCL_TYPE_KEY);
    }
}
