package com.example.micthebick.livecamera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.List;

/**
 * Created by micth on 2016-07-03.
 */
public class CameraView extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback{

    private Camera  mCamera;
    private byte[]  mVideoSource;
    private Bitmap  mBackBuffer;
    private Paint   mPaint;

    static {
        System.loadLibrary("livecamera");
    }

    public native void decode(Bitmap pTarget, byte[] pSource);

    public CameraView(Context context) {
        super(context);
        getHolder().addCallback(this);
        setWillNotDraw(false);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {

        try {
            mCamera =   Camera.open();
            mCamera.setDisplayOrientation(0);
            mCamera.setPreviewDisplay(null);
            mCamera.setPreviewCallbackWithBuffer(this);
        }catch (IOException e){
            mCamera.release();
            mCamera=null;
            throw new IllegalStateException();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int pFormat, int pWidth, int pHeight) {
        mCamera.stopPreview();
        Camera.Size lSize = findBestResolution(pWidth, pHeight);
        PixelFormat lPixelFormat    =   new PixelFormat();
        PixelFormat.getPixelFormatInfo(mCamera.getParameters().getPreviewFormat(), lPixelFormat);

        int lSourceSize =   lSize.width * lSize.height * lPixelFormat.bitsPerPixel / 8;

        mVideoSource    =   new byte[lSourceSize];
        mBackBuffer =   Bitmap.createBitmap(lSize.width, lSize.height, Bitmap.Config.ARGB_8888);

        Camera.Parameters   lParameters =   mCamera.getParameters();
        lParameters.setPreviewSize(lSize.width, lSize.height);
        lParameters.setPreviewFormat(PixelFormat.YCbCr_420_SP);

        mCamera.setParameters(lParameters);
        mCamera.addCallbackBuffer(mVideoSource);
        mCamera.startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        if (mCamera != null){
            mCamera.stopPreview();
            mCamera.release();
            mCamera =   null;
            mVideoSource    =   null;
            mBackBuffer =   null;
        }
    }

    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {

        decode(mBackBuffer, bytes);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mCamera != null){
            canvas.drawBitmap(mBackBuffer, 0, 0, mPaint);
            mCamera.addCallbackBuffer(mVideoSource);
        }
    }

    private Camera.Size findBestResolution(int pWidth, int pHeight){

        List<Camera.Size>   lSizes  =   mCamera.getParameters().getSupportedPreviewSizes();
        Camera.Size lSelectedSize   =   mCamera.new Size(0,0);
        for (Camera.Size lSize: lSizes){
            if ((lSize.width <= pWidth) && (lSize.width >= lSelectedSize.width) && (lSize.height >= lSelectedSize.height)){
                lSelectedSize   =   lSize;
            }
        }

        if ((lSelectedSize.width == 0) || (lSelectedSize.height == 0)){
            lSelectedSize   =   lSizes.get(0);
        }
        return lSelectedSize;
    }
}
