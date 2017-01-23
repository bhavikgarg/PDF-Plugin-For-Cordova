package com.mantra.plugin;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.mantra.mfs100.FingerData;
import com.mantra.mfs100.MFS100;
import com.mantra.mfs100.MFS100Event;

import java.io.ByteArrayOutputStream;
import java.io.File;

public class MFS100Sample extends CordovaPlugin implements MFS100Event {

    private int mfsVer = 41;
    private int minQuality = 60;
    private int timeout = 10000;

    private MFS100 mfs100 = null;
    private CallbackContext callbackContext;
    private Context context;

    private String isoTemplate = "";
    private boolean isMatched = false;

    private ParcelFileDescriptor mFileDescriptor;
    private PdfRenderer mPdfRenderer;
    PdfRenderer.Page mCurrentPage;

    public MFS100Sample() {

    }

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        if (mfs100 == null) {
            mfs100 = new MFS100(this, mfsVer);
            mfs100.SetApplicationContext(cordova.getActivity());
        } else {
            InitScanner();
        }
    }

    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;
        context = this.cordova.getActivity().getApplicationContext();

        if(action.equals("init")){
            InitScanner();
            return true;
        } else if(action.equals("uninit")){
            UnInitScanner();
            return true;
        } else if(action.equals("startcapture")){
            isMatched = false;
            StartAsyncCapture();
            return true;
        } else if(action.equals("stopcapture")){
            isMatched = false;
            StopAsynCapture();
            return true;
        } else if(action.equals("autocapture")){
            isMatched = false;
            StartSyncCapture();
            return true;
        } else if(action.equals("matchiso")){
            if (data != null) {
                isoTemplate = (String) data.get(0);
                isMatched = true;
                StartAsyncCapture();
            } else {
                onFailedRes("ISO Template not found.");
                isMatched = false;
            }
            return true;
        } else if(action.equals("openPdf")){
            String val = data.get(0).toString();
            if (val.startsWith("openRenderer")) {
                String[] strings = val.split("\\|");
                openRenderer(strings[1]);
            } else if (val.equals("getPageCount")) {
                getPageCount();
            } else if (val.startsWith("getPageImage")) {
                String[] strings = val.split("\\|");
                getBitmap(Integer.parseInt(strings[1]));
            } else if (val.equals("closeRender")) {
                closeRenderer();
            }
            return true;
        }

        return false;
    }

    private void closeRenderer() {
        try{
			if(mCurrentPage!=null)
				mCurrentPage.close();
			mCurrentPage = null;
            mPdfRenderer.close();
            mFileDescriptor.close();
        }catch (Exception e){
        }
    }

    private void openRenderer(String path) {
        try {
            File file = new File(path);
            mFileDescriptor = ParcelFileDescriptor.open(file,
                    ParcelFileDescriptor.MODE_READ_ONLY);
            mPdfRenderer = new PdfRenderer(mFileDescriptor);
			JSONObject object = new JSONObject();
            object.put("init", true);
            onSuccessRes(object);
        } catch (Exception e) {
			onFailedRes("not init");
        }
    }

    private void getPageCount() {
        if (mPdfRenderer != null) {
            try{
                JSONObject object = new JSONObject();
                object.put("count", mPdfRenderer.getPageCount());
                onSuccessRes(object);
            }catch(Exception e){}
        }
    }

    private void getBitmap(int index) {
        if (mPdfRenderer.getPageCount() <= index) {
            onFailedRes("GetBitmap: ");
        }
       
        try {
			 // Make sure to close the current page before opening another one.
        if (null != mCurrentPage) {
            mCurrentPage.close();
        }
        // Use `openPage` to open a specific page in PDF.
        mCurrentPage = mPdfRenderer.openPage(index);
        // Important: the destination bitmap must be ARGB (not RGB).
        Bitmap bitmap = Bitmap.createBitmap(mCurrentPage.getWidth(), mCurrentPage.getHeight(),
                Bitmap.Config.ARGB_8888);
        // Here, we render the page onto the Bitmap.
        // To render a portion of the page, use the second and third parameter. Pass nulls to get
        // the default result.
        // Pass either RENDER_MODE_FOR_DISPLAY or RENDER_MODE_FOR_PRINT for the last parameter.
        mCurrentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        // We are ready to show the Bitmap to user.

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream .toByteArray();

            JSONObject object = new JSONObject();
            object.put("image", Base64.encodeToString(byteArray, Base64.NO_WRAP));
            onSuccessRes(object);
        }catch (Exception e){
            onFailedRes("GetBitmap: " + e.getMessage());
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mfs100 != null) {
            mfs100.UnInit();
            mfs100.Dispose();
            mfs100 = null;
        }
        closeRenderer();
    }

    private void InitScanner() {
        try {
            int ret = mfs100.Init();
            if (ret != 0) {
                if(ret != -1001){
                    onFailedRes(mfs100.GetErrorMsg(ret));
                }
            } else {
                try {
                    JSONObject object = new JSONObject();
                    object.put("errorcode", 0);
                    object.put("errormsg", "Init Success");
                    object.put("make", mfs100.GetDeviceInfo().Make());
                    object.put("model", mfs100.GetDeviceInfo().Model());
                    object.put("serialno", mfs100.GetDeviceInfo().SerialNo());
                    object.put("width", mfs100.GetDeviceInfo().Width());
                    object.put("height", mfs100.GetDeviceInfo().Height());
                    object.put("certificate", mfs100.GetCertification());
                    onSuccessRes(object);
                } catch (Exception e) {
                    onFailedRes("Init: " + e.toString());
                }
            }
        } catch (Exception ex) {
            onFailedRes("Init failed, unhandled exception");
        }
    }

    private void UnInitScanner() {
        try {
            int ret = mfs100.UnInit();
            if (ret != 0) {
                onFailedRes(mfs100.GetErrorMsg(ret));
            } else {
                try {
                    JSONObject object = new JSONObject();
                    object.put("errorcode", 0);
                    object.put("errormsg", "Uninit Success");
                    onSuccessRes(object);
                } catch (Exception e) {
                    onFailedRes("UnInitScanner: " + e.toString());
                }
            }
        } catch (Exception e) {
            Log.e("UnInitScanner.EX", e.toString());
            onFailedRes("UnInitScanner.EX: " + e.toString());
        }
    }

    private void StartAsyncCapture() {
        try {
            int ret = mfs100.StartCapture(minQuality, timeout, true);
            if (ret != 0) {
                onFailedRes(mfs100.GetErrorMsg(ret));
            }
        } catch (Exception ex) {
            onFailedRes(ex.toString());
        }
    }

    private void StopAsynCapture() {
        try {
            mfs100.StopCapture();
            JSONObject object = new JSONObject();
            object.put("errorcode", 0);
            object.put("errormsg", "Capture stop");
            onSuccessRes(object);
        } catch (Exception e) {
            onFailedRes("StopAsynCapture: " + e.toString());
        }
    }

    private void StartSyncCapture() {
        try {
            FingerData fingerData = new FingerData();
            int ret = mfs100.AutoCapture(fingerData, minQuality, timeout, false);
            if (ret != 0) {
                onFailedRes("AutoCapture: " + mfs100.GetErrorMsg(ret));
            } else {
                JSONObject object = new JSONObject();
                object.put("errorcode", 0);
                object.put("errormsg", "Capture Success");
                object.put("quality", fingerData.Quality());
                object.put("nfiq", fingerData.Nfiq());
                object.put("rawdata", Base64.encodeToString(fingerData.RawData(), Base64.NO_WRAP));
                object.put("fingerimage", Base64.encodeToString(fingerData.FingerImage(), Base64.NO_WRAP));
                object.put("isoimage", Base64.encodeToString(fingerData.ISOImage(), Base64.NO_WRAP));
                object.put("isotemplate", Base64.encodeToString(fingerData.ISOTemplate(), Base64.NO_WRAP));
                object.put("ansitemplate", Base64.encodeToString(fingerData.ANSITemplate(), Base64.NO_WRAP));
                object.put("wsqimage", Base64.encodeToString(fingerData.WSQImage(), Base64.NO_WRAP));
                onSuccessRes(object);
            }
        } catch (Exception ex) {
            onFailedRes("AutoCapture.Error: " + ex.toString());
        }
    }

    @Override
    public void OnPreview(final FingerData fingerData) {
        try {
            JSONObject object = new JSONObject();
            object.put("errorcode", 0);
            object.put("errormsg", "Preview");
            object.put("quality", fingerData.Quality());
            object.put("fingerimage", Base64.encodeToString(fingerData.FingerImage(), Base64.NO_WRAP));
            onSuccessRes(object);
        } catch (Exception e) {
            onFailedRes("OnPreview.Error: " + e.toString());
        }
    }

    @Override
    public void OnCaptureCompleted(boolean status, int errorCode, String errorMsg, FingerData fingerData) {
        if (status) {
            try {
                JSONObject object = new JSONObject();
                object.put("errorcode", 0);
                if (isMatched) {
                    byte[] galleryTemp = Base64.decode(isoTemplate, Base64.DEFAULT);
                    int ret = mfs100.MatchISO(galleryTemp, fingerData.ISOTemplate());
                    isoTemplate = "";
                    if (ret < 0) {
                        onFailedRes("Error: " + ret + "(" + mfs100.GetErrorMsg(ret) + ")");
                    } else {
                        if (ret >= 1400) {
                            object.put("errormsg", "Matched Success");
                            object.put("matchscore", ret);
                            object.put("isotemplate", Base64.encodeToString(fingerData.ISOTemplate(), Base64.NO_WRAP));
                            onSuccessRes(object);
                        } else {
                            object.put("errormsg", "Matched Failed");
                            object.put("matchscore", ret);
                            object.put("isotemplate", Base64.encodeToString(fingerData.ISOTemplate(), Base64.NO_WRAP));
                            onSuccessRes(object);
                        }
                    }
                } else {
                    object.put("errormsg", "Capture Success");
                    object.put("quality", fingerData.Quality());
                    object.put("nfiq", fingerData.Nfiq());
                    object.put("rawdata", Base64.encodeToString(fingerData.RawData(), Base64.NO_WRAP));
                    object.put("fingerimage", Base64.encodeToString(fingerData.FingerImage(), Base64.NO_WRAP));
                    object.put("isoimage", Base64.encodeToString(fingerData.ISOImage(), Base64.NO_WRAP));
                    object.put("isotemplate", Base64.encodeToString(fingerData.ISOTemplate(), Base64.NO_WRAP));
                    object.put("ansitemplate", Base64.encodeToString(fingerData.ANSITemplate(), Base64.NO_WRAP));
                    object.put("wsqimage", Base64.encodeToString(fingerData.WSQImage(), Base64.NO_WRAP));
                    onSuccessRes(object);
                }

            } catch (Exception e) {
                onFailedRes("OnCaptureCompleted: " + e.toString());
            }
        } else {
            onFailedRes(errorCode + ", Error Message: " + errorMsg);
        }
    }

    @Override
    public void OnDeviceAttached(int vid, int pid, boolean hasPermission) {
        int ret = 0;
        if (!hasPermission) {
            onFailedRes("Permission denied");
            return;
        }
        if (vid == 1204 || vid == 11279) {

            if (pid == 34323) {
                ret = mfs100.LoadFirmware();
                if (ret != 0) {
                    onFailedRes(mfs100.GetErrorMsg(ret));
                } else {
                    Log.e("Message", "Loadfirmware success");
                }
            } else if (pid == 4101) {
                ret = mfs100.Init();
                if (ret != 0) {
                    onFailedRes(mfs100.GetErrorMsg(ret));
                } else {
                    try {
                        JSONObject object = new JSONObject();
                        object.put("errorcode", 0);
                        object.put("errormsg", "Init Success");
                        object.put("make", mfs100.GetDeviceInfo().Make());
                        object.put("model", mfs100.GetDeviceInfo().Model());
                        object.put("serialno", mfs100.GetDeviceInfo().SerialNo());
                        object.put("width", mfs100.GetDeviceInfo().Width());
                        object.put("height", mfs100.GetDeviceInfo().Height());
                        object.put("certificate", mfs100.GetCertification());
                        onSuccessRes(object);
                    } catch (Exception e) {
                        onFailedRes("Init: " + e.toString());
                    }
                }
            }

        }
    }

    @Override
    public void OnDeviceDetached() {
        UnInitScanner();
        Toast.makeText(context, "Device remove", Toast.LENGTH_SHORT).show();
        onFailedRes("Device remove");
    }

    @Override
    public void OnHostCheckFailed(String err) {
        onFailedRes(err);
    }

    private void onSuccessRes(JSONObject response) {
        if(callbackContext != null) {
            PluginResult result = new PluginResult(PluginResult.Status.OK, response);
            result.setKeepCallback(true);
            callbackContext.sendPluginResult(result);
        }
    }

    private void onFailedRes(String error) {
        if(callbackContext != null) {
            PluginResult result = new PluginResult(PluginResult.Status.ERROR, error);
            result.setKeepCallback(true);
            callbackContext.sendPluginResult(result);
        }
    }
}
