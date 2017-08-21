package com.mikewong.tool.tesseract;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.googlecode.tesseract.android.TessBaseAPI;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.ClipboardManager;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class TesMainActivity extends Activity {

	private static final int PHOTO_CAPTURE = 0x11;// 拍照
	private static final int PHOTO_RESULT = 0x12;// 结果
	private static final int PHOTO_REQUEST_GALLERY = 0x13;// 相册

	private static String LANGUAGE = "eng";
	private static String IMG_PATH = getSDPath() + java.io.File.separator
			+ "ocrtest";

	private static EditText tvResult;
	private static TextView tvResult1;
	private static ImageView ivSelected;
	private static ImageView ivTreated;
	private static Button btnCamera;
	private static Button btnSelect;
	private static Button btnCapy;
	private static CheckBox chPreTreat;
	private static RadioGroup radioGroup;
	private static String textResult;
	private static Bitmap bitmapSelected;
	private static Bitmap bitmapTreated;
	private static final int SHOWRESULT = 0x101;
	private static final int SHOWTREATEDIMG = 0x102;

	// 该handler用于处理修改结果的任务
	public static Handler myHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			tvResult.setText("");
			switch (msg.what) {
			case SHOWRESULT:
				if (textResult.equals(""))
					tvResult1.setText("识别失败");
				else
					{
						tvResult.setText(textResult);
						tvResult1.setText("识别完成");
					}
				break;
			case SHOWTREATEDIMG:
				tvResult1.setText("识别中......");
				showPicture(ivTreated, bitmapTreated);
				break;
			}
			super.handleMessage(msg);
		}

	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tesmain);

		// 若文件夹不存在 首先创建文件夹
		File path = new File(IMG_PATH);
		if (!path.exists()) {
			path.mkdirs();
		}

		tvResult = (EditText) findViewById(R.id.tv_result);
		tvResult1 = (TextView) findViewById(R.id.tv_result1);
		ivSelected = (ImageView) findViewById(R.id.iv_selected);
		ivTreated = (ImageView) findViewById(R.id.iv_treated);
		btnCamera = (Button) findViewById(R.id.btn_camera);
		btnSelect = (Button) findViewById(R.id.btn_select);
		btnCapy = (Button) findViewById(R.id.btn_capy);
		chPreTreat = (CheckBox) findViewById(R.id.ch_pretreat);
		radioGroup = (RadioGroup) findViewById(R.id.radiogroup);

		btnCamera.setOnClickListener(new cameraButtonListener());
		btnSelect.setOnClickListener(new selectButtonListener());
		btnCapy.setOnClickListener(new capyButtonListener());
		
    	if(!isDirExist("tessdata")){
    		Toast.makeText(getApplicationContext(), "SD卡缺少语言包，复制中。。。",Toast.LENGTH_LONG).show();
    		new SaveFile_Thread().start();
    	}
		// 用于设置解析语言
		radioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				switch (checkedId) {
				case R.id.rb_en:
					LANGUAGE = "eng";
					break;
				case R.id.rb_ch:
					LANGUAGE = "chi_sim";
					break;
				}
			}

		});

	}


	@Override  
	public boolean dispatchTouchEvent(MotionEvent ev) {  //点击编辑框以外的地方退出输入法
	    if (ev.getAction() == MotionEvent.ACTION_DOWN) {  
	        View v = getCurrentFocus();  
//	        OnTouch = v.getId();
	        if (isShouldHideInput(v, ev)) {  
	  
	            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);  
	            if (imm != null) {  
	                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);  
	            }  
	        }  
	        return super.dispatchTouchEvent(ev);  
	    }  
	    // 必不可少，否则所有的组件都不会有TouchEvent了  
	    if (getWindow().superDispatchTouchEvent(ev)) {  
	        return true;  
	    }  
	    return onTouchEvent(ev);  
	}
	public  boolean isShouldHideInput(View v, MotionEvent event) {  
	    if (v != null && (v instanceof EditText)) {  
	        int[] leftTop = { 0, 0 };  
	        //获取输入框当前的location位置  
	        
	        v.getLocationInWindow(leftTop);  
	        
	        int left = leftTop[0];  
	        int top = leftTop[1];  
	        int bottom = top + v.getHeight();  
	        int right = left + v.getWidth();  
	        if (event.getX() > left && event.getX() < right  
	                && event.getY() > top && event.getY() < bottom) {  
	            // 点击的是输入框区域，保留点击EditText的事件  
	            return false;  
	        } else {  
	            return true;  
	        }  
	    }  
	    return false;  
	}  
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (resultCode == Activity.RESULT_CANCELED)
			return;
		

		if (requestCode == PHOTO_CAPTURE) {
			tvResult1.setText("abc");
			startPhotoCrop(Uri.fromFile(new File(IMG_PATH, "temp.jpg")));
		}

		if (requestCode == PHOTO_REQUEST_GALLERY) {
			startPhotoCrop(data.getData());
		}
		
		// 处理结果
		if (requestCode == PHOTO_RESULT) {
			bitmapSelected = decodeUriAsBitmap(Uri.fromFile(new File(IMG_PATH,
					"temp_cropped.jpg")));
			if (chPreTreat.isChecked())
				tvResult1.setText("预处理中......");
			else
				tvResult1.setText("识别中......");
			// 显示选择的图片
			showPicture(ivSelected, bitmapSelected);
			
			// 新线程来处理识别
			new Thread(new Runnable() {
				@Override
				public void run() {
					if (chPreTreat.isChecked()) {
						bitmapTreated = ImgPretreatment
								.doPretreatment(bitmapSelected);
						Message msg = new Message();
						msg.what = SHOWTREATEDIMG;
						myHandler.sendMessage(msg);
						textResult = doOcr(bitmapTreated, LANGUAGE);
					} else {
						bitmapTreated = ImgPretreatment
								.converyToGrayImg(bitmapSelected);
						Message msg = new Message();
						msg.what = SHOWTREATEDIMG;
						myHandler.sendMessage(msg);
						textResult = doOcr(bitmapTreated, LANGUAGE);
					}
					Message msg2 = new Message();
					msg2.what = SHOWRESULT;
					myHandler.sendMessage(msg2);
				}

			}).start();

		}

		super.onActivityResult(requestCode, resultCode, data);
	}
	
	// 拍照识别
	class cameraButtonListener implements OnClickListener {

		@Override
		public void onClick(View arg0) {
			Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			intent.putExtra(MediaStore.EXTRA_OUTPUT,
					Uri.fromFile(new File(IMG_PATH, "temp.jpg")));
			startActivityForResult(intent, PHOTO_CAPTURE);
		}
	};

	
	// 复制数据到剪切板
	class capyButtonListener implements OnClickListener {

		@Override
		public void onClick(View arg0) {
	        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
	        // 将文本内容放到系统剪贴板里。
	        if(tvResult.length() == 0){
	        	Toast.makeText(getApplicationContext(), "无数据", Toast.LENGTH_SHORT).show();
	        	return;
	        }
	        cm.setText(tvResult.getText());
	        Toast.makeText(getApplicationContext(), "复制成功", Toast.LENGTH_SHORT).show();
		}
	};
	
	// 从相册选取照片并裁剪
	class selectButtonListener implements OnClickListener {

		@Override
		public void onClick(View v) {
//			Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//			intent.addCategory(Intent.CATEGORY_OPENABLE);
//			intent.setType("image/*");
//			intent.putExtra("crop", "true");
//			intent.putExtra("scale", true);
//			intent.putExtra("return-data", false);
//			intent.putExtra(MediaStore.EXTRA_OUTPUT,
//					Uri.fromFile(new File(IMG_PATH, "temp_cropped.jpg")));
//			intent.putExtra("outputFormat",
//					Bitmap.CompressFormat.JPEG.toString());
//			intent.putExtra("noFaceDetection", true); // no face detection
//			startActivityForResult(intent, PHOTO_RESULT);
			
			 // 激活系统图库，选择一张图片
	        Intent intent = new Intent(Intent.ACTION_PICK);
	        intent.setType("image/*");
	        // 开启一个带有返回值的Activity，请求码为PHOTO_REQUEST_GALLERY
	        boolean dele= delete(new File(IMG_PATH));
	        startActivityForResult(intent, PHOTO_REQUEST_GALLERY);
		}

	}
	
	// 将图片显示在view中
	public static void showPicture(ImageView iv, Bitmap bmp){
		iv.setImageBitmap(bmp);
	}
	
	/**
	 * 进行图片识别
	 * 
	 * @param bitmap
	 *            待识别图片
	 * @param language
	 *            识别语言
	 * @return 识别结果字符串
	 */
 	public String doOcr(Bitmap bitmap, String language) {
		TessBaseAPI baseApi = new TessBaseAPI();

		baseApi.init(getSDPath(), language);

		// 必须加此行，tess-two要求BMP必须为此配置
		bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

		baseApi.setImage(bitmap);

		String text = baseApi.getUTF8Text();

		baseApi.clear();
		baseApi.end();

		return text;
	}

	/**
	 * 获取sd卡的路径
	 * 
	 * @return 路径的字符串
	 */
	public static String getSDPath() {
		File sdDir = null;
		boolean sdCardExist = Environment.getExternalStorageState().equals(
				android.os.Environment.MEDIA_MOUNTED); // 判断sd卡是否存在
		if (sdCardExist) {
			sdDir = Environment.getExternalStorageDirectory();// 获取外存目录
		}
		return sdDir.toString();
	}

	/**
	 * 调用系统图片编辑进行裁剪
	 */
	public void startPhotoCrop(Uri uri) {
		Intent intent = new Intent("com.android.camera.action.CROP");
		intent.setDataAndType(uri, "image/*");
		intent.putExtra("crop", "true");
		intent.putExtra("scale", true);
		intent.putExtra(MediaStore.EXTRA_OUTPUT,
				Uri.fromFile(new File(IMG_PATH, "temp_cropped.jpg")));
		intent.putExtra("return-data", false);
		intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
		intent.putExtra("noFaceDetection", true); // no face detection
		startActivityForResult(intent, PHOTO_RESULT);
	}

	/**
	 * 根据URI获取位图
	 * 
	 * @param uri
	 * @return 对应的位图
	 */
	private Bitmap decodeUriAsBitmap(Uri uri) {
		Bitmap bitmap = null;
		try {
			bitmap = BitmapFactory.decodeStream(getContentResolver()
					.openInputStream(uri));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
		return bitmap;
	}
	public static boolean delete(File file){
		
//		File file = new File(path);
        if (file.exists()) { //指定文件是否存在  
            if (file.isFile()) { //该路径名表示的文件是否是一个标准文件  
                file.delete(); //删除该文件  
            } else if (file.isDirectory()) { //该路径名表示的文件是否是一个目录（文件夹）  
                File[] files = file.listFiles(); //列出当前文件夹下的所有文件  
                for (File f : files) {  
                	delete(f); //递归删除  
                    //Log.d("fileName", f.getName()); //打印文件名  
                }  
            }  
            //file.delete(); //删除文件夹（song,art,lyric）  
        }
        return true;  
	}
	
	
    /* 
     * 判断SD卡dir目录是否存在 
     */  
    public boolean isDirExist(String dir){  
    		
    	//获得当前外部储存设备的目录  
    	String SDCardRoot = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator;
    	   
        File file = new File(SDCardRoot + dir + File.separator);  
        if(!file.exists())  
            return false;  //如果返回false
        else
        	return true;
    } 
    
    public boolean SaveFileToSDCard(){
    	
    	SDUtils sdutils_Chinese = new SDUtils("tessdata","chi_sim.traineddata",this,R.raw.chi_sim);
    	SDUtils sdutils_English = new SDUtils("tessdata","eng.traineddata",this,R.raw.eng);
    	try {
    		sdutils_Chinese.getSQLiteDatabase();
    		sdutils_English.getSQLiteDatabase();
		} catch (IOException e) {
			return false;
		}
    	return true;
    }
    public class SaveFile_Thread extends Thread {
  		
  		public SaveFile_Thread(){
  		}
  		
  		public void run(){
  			synchronized (this) {
  				boolean iret;
  				do {
  					iret = SaveFileToSDCard();
  				} while (false);
  				if(iret){
  					ShowMsg(1);
  				}else
  					ShowMsg(2);
  			}
  		}
  	}
      public void ShowMsg(int what) {
  		mLoadKeyHandler.sendEmptyMessage(what);
  	}
  	
  	public Handler mLoadKeyHandler = new Handler() {
  		@Override
  		public void handleMessage(Message msg) {
  			if(msg.what==1){
  				Toast.makeText(getApplicationContext(), "复制成功",Toast.LENGTH_LONG).show();
  				}
  			else if(msg.what==2)
  				Toast.makeText(getApplicationContext(), "复制失败",Toast.LENGTH_LONG).show();
  		}
  	};
}
