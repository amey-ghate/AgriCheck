package com.daiict.agricheck;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveClient;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResourceClient;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.OpenFileActivityOptions;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;

public class imageDetails extends AppCompatActivity {

    EditText etCropName,etDiseaseName,etTypeName;
    Button btnsave;
    private DriveClient mDriveClient;
    private DriveResourceClient mDriveResourceClient;
    String TAG = "Save Text";
    private static final int REQUEST_CODE_SIGN_IN = 0;
    private static final int REQUEST_CODE_CAPTURE_IMAGE = 1;
    private static final int REQUEST_CODE_CREATOR = 2;
    protected static final int REQUEST_CODE_OPEN_ITEM = 3;
    private GoogleSignInClient mGoogleSignInClient;
    private String mFileNameToSave;
    private MainActivity mainActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_details);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        etCropName=(EditText) findViewById(R.id.CropName);
        etDiseaseName=(EditText) findViewById(R.id.DiseaseName);
        etTypeName=(EditText) findViewById(R.id.CropType);

        btnsave=(Button) findViewById(R.id.submitText);

        btnsave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SaveInfo();
            }
        });
     }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CODE_SIGN_IN:
                Log.i(TAG, "Sign in request code");
                // Called after user is signed in.
                if (resultCode == RESULT_OK) {
                    Log.i(TAG, "Signed in successfully.");
                    // Use the last signed in account here since it already have a Drive scope.
                    mDriveClient = Drive.getDriveClient(this, GoogleSignIn.getLastSignedInAccount(this));
                    // Build a drive resource client.
                    mDriveResourceClient =
                            Drive.getDriveResourceClient(this, GoogleSignIn.getLastSignedInAccount(this));
                    // Save file on GoogleDrive
                    SaveFileToGoogle();

                }
                else{
                    Log.d(TAG, "Requestcode "+ requestCode + " resultcode="+ resultCode + " data="+ data.getBundleExtra("Email"));

                }
                break;


        }
    }


    void SaveInfo()
    {
        Log.i(TAG, "Get Information from server");
        signIn();
    }

    /** Start sign in activity. */
    private void signIn() {
        Log.i(TAG, "Start sign in");
        mGoogleSignInClient = buildGoogleSignInClient();
        startActivityForResult(mGoogleSignInClient.getSignInIntent(), REQUEST_CODE_SIGN_IN);
    }

    /** Build a Google SignIn client. */
    private GoogleSignInClient buildGoogleSignInClient() {
        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestScopes(Drive.SCOPE_FILE)
                        .requestScopes(Drive.SCOPE_APPFOLDER)
                        .build();
        return GoogleSignIn.getClient(this, signInOptions);
    }

    void SaveFileToGoogle()
    {
        mainActivity = MainActivity.getInstance();
        //Check There is image and Text entered
        if(mainActivity.mBitmapToSave == null || etCropName.getText().toString() =="" || etTypeName.getText().toString()=="" | etDiseaseName.getText().toString()=="" )
        {
            Toast.makeText(this, "Need details before loading to Server", Toast.LENGTH_LONG).show();
        }
        else {
            mFileNameToSave =CreateFileName();
            SaveTextFileToGoogle();
            SaveImageFileToGoogle();
        }
    }

    private String CreateFileName()
    {
        String fileName;
        String timeStamp = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date());
        fileName = AgriCheckUser.getInstance().getUser() + timeStamp;
        return fileName;
    }

    void SaveTextFileToGoogle(){
         // [START create_file]
 /*        mDriveClient = Drive.getDriveClient(this, GoogleSignIn.getLastSignedInAccount(this));
         // Build a drive resource client.
         mDriveResourceClient =
                 Drive.getDriveResourceClient(this, GoogleSignIn.getLastSignedInAccount(this));*/
         final Task<DriveFolder> rootFolderTask = mDriveResourceClient.getRootFolder();
         final Task<DriveContents> createContentsTask = mDriveResourceClient.createContents();
         Tasks.whenAll(rootFolderTask, createContentsTask)
                 .continueWithTask(new Continuation<Void, Task<DriveFile>>() {
                     @Override
                     public Task<DriveFile> then(@NonNull Task<Void> task) throws Exception {
                         DriveFolder parent = rootFolderTask.getResult();
                         DriveContents contents = createContentsTask.getResult();
                         OutputStream outputStream = contents.getOutputStream();
                         try (Writer writer = new OutputStreamWriter(outputStream)) {
                             writer.write("CropName:"+etCropName.getText()+"\n");
                             writer.write("CropType:"+ etTypeName.getText()+"\n");
                             writer.write("CropDisease:"+etDiseaseName.getText()+"\n");
                         }

                         MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                                 .setTitle(mFileNameToSave+".txt")
                                 .setMimeType("text/plain")
                                 .setStarred(true)
                                 .build();

                         return mDriveResourceClient.createFile(parent, changeSet, contents);
                     }
                 })
                 .addOnSuccessListener(this,
                         new OnSuccessListener<DriveFile>() {
                             @Override
                             public void onSuccess(DriveFile driveFile) {
                                 //Toast.makeText(this, "File saved", Toast.LENGTH_SHORT).show();

                                        // driveFile.getDriveId().encodeToString()));
                                 finish();
                             }
                         })
                 .addOnFailureListener(this, new OnFailureListener() {
                     @Override
                     public void onFailure(@NonNull Exception e) {
                         Log.e(TAG, "Unable to create file", e);
                         //showMessage(getString(R.string.file_create_error));
                         finish();
                     }
                 });
         // [END create_file]
     }
    void SaveImageFileToGoogle(){
        // [START create_file]
 /*        mDriveClient = Drive.getDriveClient(this, GoogleSignIn.getLastSignedInAccount(this));
         // Build a drive resource client.
         mDriveResourceClient =
                 Drive.getDriveResourceClient(this, GoogleSignIn.getLastSignedInAccount(this));*/
        final Task<DriveFolder> rootFolderTask = mDriveResourceClient.getRootFolder();
        final Task<DriveContents> createContentsTask = mDriveResourceClient.createContents();
        Tasks.whenAll(rootFolderTask, createContentsTask)
                .continueWithTask(new Continuation<Void, Task<DriveFile>>() {
                    @Override
                    public Task<DriveFile> then(@NonNull Task<Void> task) throws Exception {
                        DriveFolder parent = rootFolderTask.getResult();
                        DriveContents contents = createContentsTask.getResult();
                        OutputStream outputStream = contents.getOutputStream();
                        try (Writer writer = new OutputStreamWriter(outputStream)) {
                            ByteArrayOutputStream bitmapStream = new ByteArrayOutputStream();
                            mainActivity.mBitmapToSave.compress(Bitmap.CompressFormat.PNG, 100, bitmapStream);
                            try {
                                outputStream.write(bitmapStream.toByteArray());
                            } catch (IOException e) {
                                Log.w(TAG, "Unable to write file contents.", e);
                            }

                        }

                        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                                .setTitle(mFileNameToSave+".png")
                                .setMimeType("text/plain")
                                .setStarred(true)
                                .build();

                        return mDriveResourceClient.createFile(parent, changeSet, contents);
                    }
                })
                .addOnSuccessListener(this,
                        new OnSuccessListener<DriveFile>() {
                            @Override
                            public void onSuccess(DriveFile driveFile) {
                                //Toast.makeText(this, "File saved", Toast.LENGTH_SHORT).show();

                                // driveFile.getDriveId().encodeToString()));
                                finish();
                            }
                        })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Unable to create file", e);
                        //showMessage(getString(R.string.file_create_error));
                        finish();
                    }
                });
        // [END create_file]
    }

}


