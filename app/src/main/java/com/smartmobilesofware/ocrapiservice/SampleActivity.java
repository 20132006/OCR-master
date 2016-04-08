package com.smartmobilesofware.ocrapiservice;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

public class SampleActivity extends Activity implements OnClickListener {
	private final int RESPONSE_OK = 200;
	
	private final int IMAGE_PICKER_REQUEST = 1;
	int TAKE_PHOTO_CODE = 0;
	public static int count = 0;
	
	private TextView picNameText;
	private EditText langCodeField;
	private EditText apiKeyFiled;
	
	private String apiKey;
	private String langCode;
	private String fileName;

	static final int REQUEST_IMAGE_CAPTURE = 1;

	Uri outputFileUri;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.main);

		//picNameText = (TextView) findViewById(R.id.imageName);
		langCodeField = (EditText) findViewById(R.id.lanuageCode);
		apiKeyFiled = (EditText) findViewById(R.id.apiKey);

		final Button pickButton = (Button) findViewById(R.id.picImagebutton);
		pickButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// Starting image picker activity
				startActivityForResult(new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI), IMAGE_PICKER_REQUEST);
			}
		});


		// Here, we are making a folder named picFolder to store
		// pics taken by the camera using this application.
		final String dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/picFolder/";
		File newdir = new File(dir);
		newdir.mkdirs();

		Button capture = (Button) findViewById(R.id.take_pic);
		capture.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {

				// Here, the counter will be incremented each time, and the
				// picture taken by camera will be stored as 1.jpg,2.jpg
				// and likewise.
				count++;
				String file = dir+count+".jpg";
				File newfile = new File(file);
				try {
					newfile.createNewFile();
				}
				catch (IOException e)
				{
				}

				outputFileUri = Uri.fromFile(newfile);

				Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
				cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);

				startActivityForResult(cameraIntent, TAKE_PHOTO_CODE);
			}
		});

		final Button convertButton = (Button) findViewById(R.id.convert);
		convertButton.setOnClickListener(this);
	}
	
	@Override
	public void onClick(View v) {
		apiKey = apiKeyFiled.getText().toString();
		langCode = langCodeField.getText().toString();
		
		// Checking are all fields set
		if (fileName != null && !apiKey.equals("") && !langCode.equals(""))
		{
			final ProgressDialog dialog = ProgressDialog.show( SampleActivity.this, "Checking ingredients ...", "", true, false);
			final Thread thread = new Thread(new Runnable()
			{
				@Override
				public void run() {
					final OCRServiceAPI apiClient = new OCRServiceAPI(apiKey);
					apiClient.convertToText(langCode, fileName);
					
					// Doing UI related code in UI thread
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							dialog.dismiss();
							
							// Showing response dialog
							final AlertDialog.Builder alert = new AlertDialog.Builder(SampleActivity.this);
							String ingredients = apiClient.getResponseText();

							int found = ingredients.indexOf("돼지");

							if (found>=0)
							{
								alert.setMessage("Unfortunately, this product is not halal");
							}
							else
							{
								alert.setMessage("HALAL, Bon appetit!");
							}

							//alert.setMessage(apiClient.getResponseText());
							alert.setPositiveButton(
								"OK",
								new DialogInterface.OnClickListener() {
									public void onClick( DialogInterface dialog, int id) {
									}
								});
							
							// Setting dialog title related from response code
							if (apiClient.getResponseCode() == RESPONSE_OK) {
								alert.setTitle("Success");
							} else {
								alert.setTitle("Faild");
							}
							
							alert.show();
						}
					});
				}
			});
			thread.start();
		} else {
			Toast.makeText(SampleActivity.this, "All data are required.", Toast.LENGTH_SHORT).show();
		}
	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == IMAGE_PICKER_REQUEST && resultCode == RESULT_OK) {
			fileName = getRealPathFromURI(data.getData());
//			picNameText.setText("Selected: en" + getStringNameFromRealPath(fileName));
		}

		if (requestCode == TAKE_PHOTO_CODE && resultCode == RESULT_OK) {

			String file_path = outputFileUri.toString();
			int index = file_path.indexOf("//");
			fileName = file_path.substring(index+2);
//			picNameText.setText("Selected: en" + getStringNameFromRealPath(fileName));
			Log.d("CameraDemo", "Pic saved");
		}
	}


	/*
	 * Returns image real path.
	 */
	private String getRealPathFromURI(final Uri contentUri) {
		final String[] proj = { MediaStore.Images.Media.DATA };
		final Cursor cursor = managedQuery(contentUri, proj, null, null, null);
		int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
		cursor.moveToFirst();
		
		return cursor.getString(column_index);
	}

	/*
	 * Cuts selected file name from real path to show in screen.
	 */
	private String getStringNameFromRealPath(final String bucketName)
	{
		return bucketName.lastIndexOf('/') > 0 ? bucketName.substring(bucketName.lastIndexOf('/') + 1) : bucketName;
	}
}

