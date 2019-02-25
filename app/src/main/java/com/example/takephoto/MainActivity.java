package com.example.takephoto;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
  public String TAG = MainActivity.class.getName();
  /**
   * requestCode 请求权限
   **/
  public final static int PERMISSION_REQUEST_TAKE_PHOTO = 2000;
  public static final int REQUEST_CODE_OPEN_CAMERA = 0x100;
  public static final int REQUEST_CODE_OPEN_CAMERA_CROP = 0x101;
  public static final int REQUEST_CODE_RC_CROP = 0x102;
  private Context mContext;
  private ArrayList<String> mPermissions = new ArrayList<>();
  private Uri tempUri;
  private ImageView mImgResult;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mContext = this;
    setContentView(R.layout.activity_main);
    mImgResult = findViewById(R.id.iv_result);
    final Button mOpenCamera = findViewById(R.id.open_camera);
    mOpenCamera.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        openCamera(REQUEST_CODE_OPEN_CAMERA);
      }
    });

    Button mOpenCameraCrop = findViewById(R.id.open_camera_crop);
    mOpenCameraCrop.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        openCameraCrop();
      }
    });

    final Button mOpenCameraCropOther = findViewById(R.id.open_camera_crop_other);
    mOpenCameraCropOther.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        openCameraCropOther();
      }
    });

  }


  private void openCamera(int requestCode) {
    File file = new File(Environment.getExternalStorageDirectory(), "/temp/" + System.currentTimeMillis() + ".jpg");
    if (!file.getParentFile().exists()) {
      file.getParentFile().mkdirs();
    }
    Uri imageUri = Uri.fromFile(file);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      tempUri = UriParse.getTempUri(this);
    } else {
      tempUri = imageUri;
    }
    //检查权限
    boolean cameraGranted = true, storageGranted = true, storageReadGranted = true;
    //Android 8.0 行为变更 https://developer.android.com/about/versions/oreo/android-8.0-changes#rmp
    storageGranted = ContextCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    storageReadGranted = ContextCompat.checkSelfPermission(mContext, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    cameraGranted = ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    if (!storageGranted) mPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
    if (!storageReadGranted) mPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
    if (!cameraGranted) mPermissions.add(Manifest.permission.CAMERA);

    if (cameraGranted && storageGranted && storageReadGranted) {
      //检查是否有相机
      final Intent captureIntent = IntentUtils.getCaptureIntent(tempUri);
      List<ResolveInfo> result = getPackageManager().queryIntentActivities(captureIntent, PackageManager.MATCH_DEFAULT_ONLY);
      if (result.isEmpty()) {
        Toast.makeText(mContext, getString(R.string.no_camera), Toast.LENGTH_SHORT).show();
      } else {
        //开启相机
        startActivityForResult(captureIntent, requestCode);
      }
    } else {
      //没有权限，请求权限
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        requestPermissions(mPermissions.toArray(new String[mPermissions.size()]),
                PERMISSION_REQUEST_TAKE_PHOTO);
      } else {
        ActivityCompat.requestPermissions(MainActivity.this,
                mPermissions.toArray(new String[mPermissions.size()]),
                PERMISSION_REQUEST_TAKE_PHOTO);
      }
    }
  }


  private void openCameraCrop() {
    openCamera(REQUEST_CODE_OPEN_CAMERA_CROP);
  }

  private void openCameraCropOther() {

  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == PERMISSION_REQUEST_TAKE_PHOTO) {
      boolean cameraGranted = true;
      boolean storageGranted = true;
      for (int i = 0, j = permissions.length; i < j; i++) {
        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
          if (TextUtils.equals(Manifest.permission_group.STORAGE, permissions[i])) {
            storageGranted = false;
          } else if (TextUtils.equals(Manifest.permission.CAMERA, permissions[i])) {
            cameraGranted = false;
          }
        }
      }
      if (cameraGranted && storageGranted) {
        Toast.makeText(mContext, getString(R.string.authorized), Toast.LENGTH_SHORT).show();
      }
      if (!cameraGranted && storageGranted) {
        Toast.makeText(mContext, getString(R.string.no_photo_permission), Toast.LENGTH_SHORT).show();
      }
      if (!storageGranted && cameraGranted) {
        Toast.makeText(mContext, getString(R.string.no_sd_card_permissions), Toast.LENGTH_SHORT).show();
      }
      if (!storageGranted && !cameraGranted) {
        Toast.makeText(mContext, getString(R.string.no_sd_card_and_photo_permissions), Toast.LENGTH_SHORT).show();
      }
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    switch (requestCode) {
      case REQUEST_CODE_OPEN_CAMERA:
        if (resultCode == Activity.RESULT_OK) {
          try {
            String filePathWithUri = UriParse.getFilePathWithUri(tempUri, MainActivity.this);
            Log.e(TAG, "文件路径: " + filePathWithUri);
            mImgResult.setImageURI(tempUri);
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
        break;

      case REQUEST_CODE_OPEN_CAMERA_CROP:
        //裁剪的输出 Uri 必须使用 Uri.fromFile(File file) ,否则会系统会提示无法保存经过裁剪的照片
        Uri outPutUriFromFile = Uri.fromFile(new File(UriParse.parseOwnUri(MainActivity.this, tempUri)));
        onCrop(tempUri, outPutUriFromFile, getCropOptions());
        break;
      case REQUEST_CODE_RC_CROP:
        if (resultCode == Activity.RESULT_OK) {
          try {
            String path = UriParse.getFilePathWithUri(tempUri, MainActivity.this);
            Log.e(TAG, "裁剪完成之后的图片地址: " + path);
          } catch (Exception e) {
            e.printStackTrace();
          }
        } else {
          Toast.makeText(mContext, getString(R.string.cancel), Toast.LENGTH_SHORT).show();
        }
        break;
    }

  }


  public void onCrop(Uri imageUri, Uri outPutUri, CropOptions options) {
    if (!ImageFiles.checkMimeType(mContext, ImageFiles.getMimeType(MainActivity.this, imageUri))) {
      Toast.makeText(mContext, getString(R.string.selected_is_not_an_image), Toast.LENGTH_SHORT).show();
      return;
    }
    cropWithApp(imageUri, outPutUri, options);
  }

  private void cropWithApp(Uri imageUri, Uri outPutUri, CropOptions options) {
    Intent nativeCropIntent = IntentUtils.getCropIntentWithOtherApp(imageUri, outPutUri, options);
    List result = getPackageManager().queryIntentActivities(nativeCropIntent, PackageManager.MATCH_DEFAULT_ONLY);
    if (!result.isEmpty()) {
      startActivityForResult(IntentUtils.getCropIntentWithOtherApp(imageUri, outPutUri, options), REQUEST_CODE_RC_CROP);
    }
  }


  private CropOptions getCropOptions() {
    CropOptions.Builder options = new CropOptions.Builder();
    options.setWithOwnCrop(false)
            .setAspectX(0)
            .setAspectY(0)
            .setOutputX(800)
            .setOutputY(800);
    return options.create();
  }
}
