package net.bitcheese.dump;

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import ch.boye.httpclientandroidlib.Header;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.ClientProtocolException;
import ch.boye.httpclientandroidlib.client.HttpClient;
import ch.boye.httpclientandroidlib.client.methods.HttpPost;
import ch.boye.httpclientandroidlib.entity.mime.HttpMultipartMode;
import ch.boye.httpclientandroidlib.entity.mime.MultipartEntity;
import ch.boye.httpclientandroidlib.entity.mime.content.FileBody;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;
import ch.boye.httpclientandroidlib.message.BasicHeader;

public class MainActivity extends Activity {

	public static final String TAG = MainActivity.class.getSimpleName();
	private TextView mTxtUrl;
	private Button mBtnCopy;
	private Button mBtnShare;
	private ClipboardManager clipboard;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		clipboard = (ClipboardManager)getSystemService(
				Context.CLIPBOARD_SERVICE);

		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		String action = intent.getAction();
		
		if (Intent.ACTION_SEND.equals(action)) {
			if (extras.containsKey(Intent.EXTRA_STREAM)) {
				Uri uri = (Uri) extras.getParcelable(Intent.EXTRA_STREAM);
				String filePath = uri.toString(); //parseUriToFilename(uri);
				new UploadImageTask().execute(filePath);
			}
		}
		
		mTxtUrl = (TextView)findViewById(R.id.txtUrl);
		mBtnCopy = (Button)findViewById(R.id.btnCopy);
		mBtnShare = (Button)findViewById(R.id.btnShare);
	}

	
	private class UploadImageTask extends AsyncTask<String, Integer, String> implements OnCancelListener {
		
		private ProgressDialog mDialog;
		private Long totalSize;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			mDialog = new ProgressDialog(MainActivity.this);
			mDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			mDialog.setMessage("Uploading File...");
			mDialog.setCancelable(true);
			mDialog.show();
			mDialog.setOnCancelListener(this);
		}

		@Override
		protected String doInBackground(String... params) {
			String fileUri = params[0];
			HttpClient httpClient = new DefaultHttpClient();
			

			try {				
				HttpPost request = new HttpPost("http://dump.bitcheese.net/upload-file?simple");
				
				MultipartEntity form = new CustomMultiPartEntity(HttpMultipartMode.STRICT, new CustomMultiPartEntity.ProgressListener() {
					
					@Override
					public void transferred(long num) {
						publishProgress((int) ((num / (float) totalSize) * 100));
					}
				});
				String fileName = Utils.getPath(MainActivity.this, Uri.parse(fileUri));
				File fileToUpload = new File(fileName);
				totalSize = fileToUpload.length();
				if(totalSize > 20971520){
					Toast.makeText(MainActivity.this, "File is too large", Toast.LENGTH_SHORT).show();
					mDialog.dismiss();
					finish();
				}
				form.addPart("file", new FileBody(fileToUpload));
				Header[] headers = new Header[]{new BasicHeader("User-Agent", "curl/9000.1"),
						new BasicHeader("Host", "dump.bitcheese.net"),
						new BasicHeader("Accept", "*/*"),
						new BasicHeader("Connection", "close")
				};
						
				
				
				request.setHeaders(headers);
				request.setEntity(form);
				
				HttpResponse uploadResponse = httpClient.execute(request);
				
				if (uploadResponse.getStatusLine().getStatusCode() == 302) {
					  String redirectURL = uploadResponse.getFirstHeader("Location").getValue();
					  //Log.i(TAG, "Redirect catched");
					  return redirectURL;
				}
				
				String response = Utils.streamToString(uploadResponse.getEntity().getContent());
				Log.i(TAG, response);
				
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return "";
		}
		
		@Override
		protected void onProgressUpdate(Integer... values) {
			super.onProgressUpdate(values);
			mDialog.setProgress(values[0]);
		}
		
		@Override
		protected void onPostExecute(String result) {
			mDialog.dismiss();
			Log.i(TAG, result);
			super.onPostExecute(result);
			if(!result.equals("")){
				processResult(result);
			} else {
				Toast.makeText(MainActivity.this, "Не удалось загрузить изображение", Toast.LENGTH_SHORT).show();
				finish();
			}
		}

		@Override
		public void onCancel(DialogInterface dialog) {
			this.cancel(true);
			mDialog.dismiss();
		}
		
	}
	
	private void processResult(final String result){
		mTxtUrl.setText(result);
		Linkify.addLinks(mTxtUrl, Linkify.ALL);
		mBtnCopy.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				clipboard.setText(result);
				Toast.makeText(MainActivity.this,
						"Ссылка скопирована в буфер обмена", Toast.LENGTH_SHORT)
						.show();
			}
		});
		
		mBtnShare.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent sendIntent = new Intent();
				sendIntent.setAction(Intent.ACTION_SEND);
				sendIntent.putExtra(Intent.EXTRA_TEXT, result);
				sendIntent.setType("text/plain");
				startActivity(sendIntent);
			}
		});
	}
}
