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

package com.nextgis.ngm_clink_monitoring.services;

import android.accounts.Account;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.NGWUtil;
import com.nextgis.maplib.util.NetworkUtil;
import com.nextgis.ngm_clink_monitoring.GISApplication;
import com.nextgis.ngm_clink_monitoring.R;
import com.nextgis.ngm_clink_monitoring.activities.MainActivity;
import com.nextgis.ngm_clink_monitoring.util.FoclConstants;
import com.nextgis.ngm_clink_monitoring.util.FoclFileUtil;
import com.nextgis.ngm_clink_monitoring.util.Utils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import static com.nextgis.maplib.util.Constants.TAG;
import static com.nextgis.ngm_clink_monitoring.util.FoclConstants.*;


public class FoclReportService
        extends Service
{
    public static final int NOTIFICATION_START    = 1;
    public static final int NOTIFICATION_FINISH   = 2;
    public static final int NOTIFICATION_CANCELED = 3;
    public static final int NOTIFICATION_ERROR    = 4;

    private static final int NOTIFY_ID = 2;

    protected boolean mSendReportFromMain = false;
    protected boolean mSendWorkData       = false;

    protected NetworkUtil mNet;

    protected boolean mIsRunning = false;
    protected Thread  mThread    = null;

    protected GISApplication mApplication;

    protected String  mDeviceUuid;
    protected String  mDeviceModelName;
    protected Account mAccount;
    protected String  mAccountUrl;
    protected String  mAccountLogin;
    protected String  mAccountPassword;


    @Override
    public void onCreate()
    {
        super.onCreate();

        // for debug
//        android.os.Debug.waitForDebugger();

        mApplication = (GISApplication) getApplicationContext();
        mNet = new NetworkUtil(this);
        mDeviceUuid = Utils.getDeviceUniqueID(mApplication);
        mDeviceModelName = mApplication.getDeviceModelName();

        mAccount = mApplication.getAccount();
        mAccountUrl = mApplication.getAccountUrl(mAccount);
        mAccountLogin = mApplication.getAccountLogin(mAccount);
        mAccountPassword = mApplication.getAccountPassword(mAccount);
    }


    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId)
    {
        if (!mIsRunning) {
            mIsRunning = true;

            if (null != intent) {
                mSendReportFromMain = intent.getBooleanExtra(FOCL_SEND_REPORT_FROM_MAIN, false);
                mSendWorkData = intent.getBooleanExtra(FOCL_SEND_WORK_DATA, false);
            }

            Log.d(TAG, "Report service started");
            if (mSendWorkData) {
                startForeground(NOTIFY_ID, getNotification(this, NOTIFICATION_START, null));
            }
            runTask();
        }

        return START_STICKY;
    }


    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }


    @Override
    public void onDestroy()
    {
        super.onDestroy();

        if (mThread != null) {
            mThread.interrupt();
        }
    }


    protected void runTask()
    {
        mThread = new Thread(
                new Runnable()
                {
                    public void run()
                    {
                        try {
                            Log.d(TAG, "Report service, runTask() started");
                            String reportsDirPath = mApplication.getReportsDirPath();

                            if (mSendWorkData) {
                                sendWorkData();
                            }

                            sendMainErrorReports(reportsDirPath);
                            sendSyncErrorReports(reportsDirPath);

                            if (mSendReportFromMain) {
                                sendAllErrorReports(reportsDirPath);
                            }

                            sendMainReports(reportsDirPath);
                            sendSyncReports(reportsDirPath);

                        } catch (JSONException | IOException e) {
                            e.printStackTrace();
                            if (mSendWorkData) {
                                sendNotification(
                                        FoclReportService.this, NOTIFICATION_ERROR,
                                        e.getLocalizedMessage());
                            }
                        }

                        if (mSendWorkData) {
                            sendNotification(FoclReportService.this, NOTIFICATION_FINISH, null);
                        }
                        Log.d(TAG, "Report service, runTask() stopped");
                        stopSelf();
                    }
                });

        mThread.start();
    }


    protected boolean sendAllErrorReports(String reportsDirPath)
            throws IOException, JSONException
    {
        String errorFileMask =
                "(?!" + FOCL_ZIP_WORK_DATA_FILE_NAME + ".*\\" + FOCL_ZIP_WORK_DATA_FILE_EXT + ")";
        return sendReports(reportsDirPath, errorFileMask, JSON_MAIN_ERROR_REPORT_TYPE_VALUE);
    }


    protected boolean sendMainReports(String reportsDirPath)
            throws IOException, JSONException
    {
        String errorFileMask = FOCL_MAIN_LOGCAT_FILE_NAME + ".*\\" + FOCL_REPORT_FILE_EXT;
        return sendReports(reportsDirPath, errorFileMask, JSON_MAIN_REPORT_TYPE_VALUE);
    }


    protected boolean sendMainErrorReports(String reportsDirPath)
            throws IOException, JSONException
    {
        String errorFileMask = FOCL_MAIN_ERROR_LOGCAT_FILE_NAME + ".*\\" + FOCL_REPORT_FILE_EXT;
        return sendReports(reportsDirPath, errorFileMask, JSON_MAIN_ERROR_REPORT_TYPE_VALUE);
    }


    protected boolean sendSyncReports(String reportsDirPath)
            throws IOException, JSONException
    {
        String errorFileMask = FOCL_SYNC_LOGCAT_FILE_NAME + ".*\\" + FOCL_REPORT_FILE_EXT;
        return sendReports(reportsDirPath, errorFileMask, JSON_SYNC_REPORT_TYPE_VALUE);
    }


    protected boolean sendSyncErrorReports(String reportsDirPath)
            throws IOException, JSONException
    {
        String errorFileMask = FOCL_SYNC_ERROR_LOGCAT_FILE_NAME + ".*\\" + FOCL_REPORT_FILE_EXT;
        return sendReports(reportsDirPath, errorFileMask, JSON_SYNC_ERROR_REPORT_TYPE_VALUE);
    }


    protected boolean sendWorkData()
            throws IOException, JSONException
    {
        getZippedMap();

        final String zipFileMask =
                FOCL_ZIP_WORK_DATA_FILE_NAME + ".*\\" + FOCL_ZIP_WORK_DATA_FILE_EXT;
        File reportsDir = new File(mApplication.getReportsDirPath());

        if (!reportsDir.isDirectory()) {
            throw new IOException("reportsDir is not directory");
        }

        Log.d(TAG, "Report service, reportsDir: " + reportsDir.getAbsolutePath());

        File[] zipFiles = reportsDir.listFiles(
                new FilenameFilter()
                {
                    @Override
                    public boolean accept(
                            final File dir,
                            final String name)
                    {
                        if (name.matches(zipFileMask)) {
                            Log.d(
                                    TAG, "Report service, reportsDir: " +
                                            dir.getAbsolutePath() + ", name: " + name);
                            return true;
                        } else {
                            return false;
                        }
                    }
                });

        Log.d(TAG, "Report service, zipFiles.length: " + zipFiles.length);

        for (File zipFile : zipFiles) {
            Log.d(TAG, "Report service, zipFile: " + zipFile.getAbsolutePath());

            if (sendZipFile(zipFile.getAbsolutePath())) {
                if (!zipFile.delete()) {
                    Log.d(
                            TAG,
                            "Report service, zipFile do not deleted: " + zipFile.getAbsolutePath());
                }

            } else {
                Log.d(
                        TAG,
                        "Report service, sending zipFile FAILED: " + zipFile.getAbsolutePath());
            }
        }

        return true;
    }


    protected boolean sendReports(
            String reportsDirPath,
            final String fileMask,
            String reportType)
            throws IOException, JSONException
    {
        File reportsDir = new File(reportsDirPath);

        if (!reportsDir.isDirectory()) {
            throw new IOException("reportsDir is not directory");
        }

        Log.d(TAG, "Report service, reportsDir: " + reportsDir.getAbsolutePath());

        File[] repFiles = reportsDir.listFiles(
                new FilenameFilter()
                {
                    @Override
                    public boolean accept(
                            final File dir,
                            final String name)
                    {
                        if (name.matches(fileMask)) {
                            Log.d(
                                    TAG, "Report service, reportsDir: " +
                                            dir.getAbsolutePath() + ", name: " + name);
                            return true;
                        } else {
                            return false;
                        }
                    }
                });

        Log.d(TAG, "Report service, repFiles.length: " + repFiles.length);

        for (File repFile : repFiles) {
            Log.d(TAG, "Report service, repFile: " + repFile.getAbsolutePath());
            JSONArray arrayLines = FoclFileUtil.readJsonArrayLinesFromFile(repFile);
            JSONObject logcatObject = new JSONObject();
            logcatObject.put("logcat_str", arrayLines);

            if (sendReport(reportType, logcatObject)) {
                if (!repFile.delete()) {
                    Log.d(
                            TAG,
                            "Report service, repFile do not deleted: " + repFile.getAbsolutePath());
                }

            } else {
                Log.d(
                        TAG,
                        "Report service, sending repFile FAILED: " + repFile.getAbsolutePath());
            }
        }

        return true;
    }


    protected boolean sendReport(
            String reportType,
            JSONObject logcatObject)
    {
        if (!mNet.isNetworkAvailable()) {
            return false;
        }

        try {
            String data = sendJsonData(reportType, logcatObject, null);

            if (null == data) {
                Log.d(Constants.TAG, "send report FAILED, null == data");
                return false;
            } else {
                return true;
            }

        } catch (JSONException | IOException e) {
            e.printStackTrace();
            return false;
        }
    }


    protected boolean sendZipFile(String filePath)
            throws IOException, JSONException
    {
        if (!mNet.isNetworkAvailable()) {
            return false;
        }

        // 1. send file
        File sendFile = new File(filePath);
        String fileName = sendFile.getName();
        String fileMime = "application/zip";

        String data = mNet.postFile(
                NGWUtil.getFileUploadUrl(mAccountUrl), fileName, sendFile, fileMime, mAccountLogin,
                mAccountPassword);
        Log.d(Constants.TAG, "send file: " + filePath); // TODO: comment it?
        Log.d(Constants.TAG, "send file, data: " + data); // TODO: comment it?

        if (null == data) {
            Log.d(Constants.TAG, "send file FAILED, null == data");
            return false;
        }

        JSONObject result = new JSONObject(data);
        if (!result.has("upload_meta")) {
            Log.d(Constants.TAG, "send file FAILED, data has not upload_meta");
            return false;
        }

        JSONArray uploadMetaArray = result.getJSONArray("upload_meta");
        if (uploadMetaArray.length() == 0) {
            Log.d(Constants.TAG, "send file FAILED, uploadMetaArray.length() == 0");
            return false;
        }


        // 2. send POST data
        JSONObject jsonFileName = new JSONObject();
        jsonFileName.put("file_name", fileName);

        data = sendJsonData(
                JSON_WORK_DATA_REPORT_TYPE_VALUE, jsonFileName, uploadMetaArray.get(0));

        if (null == data) {
            Log.d(Constants.TAG, "send file FAILED, null == data");
            return false;
        } else {
            return true;
        }
    }


    protected String sendJsonData(
            String reportType,
            JSONObject logcatObject,
            Object fileUpload)
            throws JSONException, IOException
    {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(JSON_DEVICE_UUID_KEY, mDeviceUuid);
        jsonObject.put(JSON_MODEL_NAME_KEY, mDeviceModelName);
        jsonObject.put(JSON_DATE_KEY, System.currentTimeMillis() / 1000);
        jsonObject.put(JSON_SERVER_URL_KEY, mAccountUrl);
        jsonObject.put(JSON_LOGIN_KEY, mAccountLogin);
        jsonObject.put(JSON_REPORT_TYPE_KEY, reportType);
        jsonObject.put(JSON_LOGCAT_KEY, logcatObject);

        if (null != fileUpload) {
            jsonObject.put(JSON_FILE_UPLOAD_KEY, fileUpload);
        }

        String payload = jsonObject.toString();
        Log.d(Constants.TAG, "send report, payload: " + payload); // TODO: comment it

        return mNet.post(getReportUrl(mAccountUrl), payload, mAccountLogin, mAccountPassword);
    }


    public static String getReportUrl(String server)
    {
        if (!server.startsWith("http")) {
            server = "http://" + server;
        }
        return server + FoclConstants.FOCL_REPORT_URL;
    }


    protected File getZippedMap()
            throws IOException
    {
        String mapPath = mApplication.getMapPath();
        String zipFilePath = mApplication.getWorkDataZipFilePath();

        FoclFileUtil.zipFileAtPath(mapPath, zipFilePath);
        File zipFile = new File(zipFilePath);

        File readyZipFile = new File(zipFilePath + FoclConstants.FOCL_ZIP_WORK_DATA_FILE_EXT);

        if (!zipFile.renameTo(readyZipFile)) {
            throw new IOException(
                    "can not rename zipFile: " + zipFile.getAbsolutePath() +
                            " to readyZipFile: " + readyZipFile.getAbsolutePath());
        }

        return readyZipFile;
    }


    protected static Notification getNotification(
            Context context,
            int notificationType,
            String errorMsg)
    {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

        builder.setLargeIcon(
                BitmapFactory.decodeResource(
                        context.getResources(), R.drawable.ic_launcher))
                .setContentIntent(contentIntent)
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true)
                .setOngoing(false);

        switch (notificationType) {
            case NOTIFICATION_START:
                builder.setProgress(0, 0, true)
                        .setSmallIcon(R.drawable.ic_sync_started)
                        .setTicker(context.getString(R.string.report_started))
                        .setContentTitle(context.getString(R.string.report))
                        .setContentText(context.getString(R.string.report_progress));
                break;

            case NOTIFICATION_FINISH:
                builder.setProgress(0, 0, false)
                        .setSmallIcon(R.drawable.ic_sync_finished)
                        .setTicker(context.getString(R.string.report_finished))
                        .setContentTitle(context.getString(R.string.report))
                        .setContentText(context.getString(R.string.report_finished));
                break;

            case NOTIFICATION_CANCELED:
                builder.setProgress(0, 0, false)
                        .setSmallIcon(R.drawable.ic_sync_error)
                        .setTicker(context.getString(R.string.report_canceled))
                        .setContentTitle(context.getString(R.string.report))
                        .setContentText(context.getString(R.string.report_canceled));
                break;

            case NOTIFICATION_ERROR:
                builder.setProgress(0, 0, false)
                        .setSmallIcon(R.drawable.ic_sync_error)
                        .setTicker(context.getString(R.string.report_error))
                        .setContentTitle(context.getString(R.string.report_error))
                        .setContentText(errorMsg);
                break;
        }

        return builder.build();
    }


    protected static void sendNotification(
            Context context,
            int notificationType,
            String errorMsg)
    {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFY_ID, getNotification(context, notificationType, errorMsg));
    }
}
