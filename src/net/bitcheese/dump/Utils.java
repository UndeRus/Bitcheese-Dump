package net.bitcheese.dump;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;

public class Utils {

	public static String streamToString(InputStream stream) {

		InputStreamReader reader = new InputStreamReader(stream);
		BufferedReader buff = new BufferedReader(reader);
		StringBuffer strBuff = new StringBuffer();

		String s;
		try {
			while ((s = buff.readLine()) != null) {
				strBuff.append(s);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return strBuff.toString();
	}
	
	public static String getPath(Context context, Uri uri) {
	    String[] projection = { MediaStore.Images.Media.DATA };
	    Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
	    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
	    cursor.moveToFirst();
	    return cursor.getString(column_index);
	}
	
	public static String getMimeType(String url){
	    String type = null;
	    String extension = MimeTypeMap.getFileExtensionFromUrl(url);
	    if (extension != null) {
	        MimeTypeMap mime = MimeTypeMap.getSingleton();
	        type = mime.getMimeTypeFromExtension(extension);
	    }
	    return type;
	}
}
