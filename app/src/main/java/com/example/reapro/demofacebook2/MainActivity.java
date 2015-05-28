package com.example.reapro.demofacebook2;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookAuthorizationException;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.Profile;
import com.facebook.ProfileTracker;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.share.ShareApi;
import com.facebook.share.Sharer;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;
import com.facebook.share.widget.ShareDialog;

import java.util.ArrayList;
import java.util.Arrays;

import roboguice.activity.RoboActivity;
import roboguice.inject.ContentView;
import roboguice.inject.InjectView;

@ContentView(R.layout.activity_main)
public class MainActivity extends RoboActivity {

    CallbackManager callbackManager;
    @InjectView(R.id.txtSaludo)  TextView txtSaludo;
    @InjectView(R.id.postStatusUpdateButton)
    Button btnActualizarStatus;
    private boolean canPresentShareDialog;
    private boolean canPresentShareDialogWithPhotos;


    private enum PendingAction {
        NONE,
        POST_PHOTO,
        POST_STATUS_UPDATE
    }

    private FacebookCallback<Sharer.Result> shareCallback= new FacebookCallback<Sharer.Result>() {
        @Override
        public void onSuccess(Sharer.Result result) {
            Log.d("111","shareCallback onSuccess");
        }

        @Override
        public void onCancel() {
            Log.d("111","shareCallback onCancel");

        }

        @Override
        public void onError(FacebookException e) {
            Log.d("111","shareCallback onError");

        }
    };

    private ShareDialog shareDialog;
    private ProfileTracker profileTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        FacebookSdk.sdkInitialize(this.getApplicationContext());
        super.onCreate(savedInstanceState);
       callbackManager = CallbackManager.Factory.create();
        Log.d("111", "after super.oncreate");

       LoginManager.getInstance().registerCallback(callbackManager,
               new FacebookCallback<LoginResult>() {
                   @Override
                   public void onSuccess(LoginResult loginResult) {
                       Log.d("111", "onSuccess");
                       updateUI();
                   }

                   @Override
                   public void onCancel() {
                       Log.d("111", "onCancel");

                       updateUI();
                   }

                   @Override
                   public void onError(FacebookException exception) {
                       exception.printStackTrace();
                       Log.e("111", "onError" + exception.getMessage());


                       updateUI();
                   }

               });

        shareDialog= new ShareDialog(this);
        shareDialog.registerCallback(callbackManager,shareCallback);

        canPresentShareDialog = ShareDialog.canShow(
                ShareLinkContent.class);

        canPresentShareDialogWithPhotos = ShareDialog.canShow(
                SharePhotoContent.class);

        profileTracker = new ProfileTracker() {
            @Override
            protected void onCurrentProfileChanged(Profile oldProfile, Profile currentProfile) {
                Log.d("111","ProfileTracker:onCurrentProfileChanged");
                updateUI();
                // It's possible that we were waiting for Profile to be populated in order to
                // post a status update.
                //handlePendingAction();
            }
        };
        updateUI();
    }

    private void updateUI() {
        Log.d("111", "updateUI");
        boolean enableButtons = AccessToken.getCurrentAccessToken() != null;
        Profile profile = Profile.getCurrentProfile();
        Log.d("111", "enableButtons:" + enableButtons);
        Log.d("111", "profile:" + profile);
        if(enableButtons && profile!=null){
            txtSaludo.setText(getString(R.string.hello_user, profile.getFirstName()));
            txtSaludo.setVisibility(View.VISIBLE);

        }else{
            txtSaludo.setVisibility(View.INVISIBLE);
        }
    }

   @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("111", "onActivityResult");
        Log.d("111", "requestCode:"+requestCode);
        Log.d("111", "resultCode:"+resultCode);
        Log.d("111", "data:"+data.getExtras().toString());



        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();



        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void actualizarStatus(View view) {

        performPublish(PendingAction.POST_STATUS_UPDATE, canPresentShareDialog);
    }


    public void actualizarFoto(View view) {
        performPublish(PendingAction.POST_PHOTO, canPresentShareDialogWithPhotos);
    }

    private void onClickPostStatusUpdate() {
        performPublish(PendingAction.POST_STATUS_UPDATE, canPresentShareDialog);
    }

    private PendingAction pendingAction = PendingAction.NONE;
    private static final String PERMISSION = "publish_actions";

    private void performPublish(PendingAction action, boolean canPresentShareDialog) {
        AccessToken accessToken= AccessToken.getCurrentAccessToken();
        if(accessToken!=null){
            Log.d("111","accessToken!=null");
            pendingAction=action;
            if(accessToken.getPermissions().contains("publish_actions")){
                Log.d("111","publish_actions: yes");
                handlePendingAction();
                return;

            }else{
                Log.d("111","publish_actions: not");
                LoginManager.getInstance().logInWithPublishPermissions(this, Arrays.asList(PERMISSION));
                return;
            }
        }
    }

    private void handlePendingAction() {
        PendingAction previos=pendingAction;
        pendingAction=PendingAction.NONE;
        switch (previos){
            case NONE:break;
            case POST_PHOTO:postearFoto(); break;
            case POST_STATUS_UPDATE: postearStatus(); break;
        }
    }

    private void postearFoto() {
        Log.d("111","postearFoto");
        Bitmap image= BitmapFactory.decodeResource(this.getResources(),R.drawable.abc_btn_check_material);
        SharePhoto sharePhoto= new SharePhoto.Builder().setBitmap(image).build();
        ArrayList<SharePhoto> photos=new ArrayList<>();
        photos.add(sharePhoto);
        SharePhotoContent sharePhotoContent= new SharePhotoContent.Builder().setPhotos(photos).build();
        AccessToken accessToken= AccessToken.getCurrentAccessToken();
        if(canPresentShareDialog){
            shareDialog.show(sharePhotoContent);
        }else if(accessToken.getPermissions().contains("publish_actions")){
            ShareApi.share(sharePhotoContent,shareCallback);
        }

    }

    private void postearStatus(){
        Profile profile= Profile.getCurrentProfile();
        ShareLinkContent linkContent=new ShareLinkContent.Builder().setContentTitle("Hello Facebook").
                setContentDescription("aaa")
                .setContentUrl(Uri.parse("http://developers.facebook.com/docs/android"))
                .build();

        if(canPresentShareDialog){
            Log.d("111","canPresentShareDialog");
            shareDialog.show(linkContent);
        }
        else if (profile!=null){
            Log.d("111","share");
            ShareApi.share(linkContent,shareCallback);
        }
    }
}
