package arko.chatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.sinch.android.rtc.ClientRegistration;
import com.sinch.android.rtc.PushTokenRegistrationCallback;
import com.sinch.android.rtc.Sinch;
import com.sinch.android.rtc.SinchError;
import com.sinch.android.rtc.UserController;
import com.sinch.android.rtc.UserRegistrationCallback;
import com.squareup.picasso.Picasso;

import java.security.MessageDigest;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

import static arko.chatapp.SinchService.APP_KEY;
import static arko.chatapp.SinchService.APP_SECRET;
import static arko.chatapp.SinchService.ENVIRONMENT;

public class ThereProfileActivity extends BaseActivity implements SinchService.StartFailedListener, PushTokenRegistrationCallback, UserRegistrationCallback {

    CardView myAccount;
    CircleImageView avatarIv;
    TextView nameTv, statusTv;
    ImageView message, audio_call, video_call;

    FirebaseAuth firebaseAuth;
    FirebaseUser fuser;
    DatabaseReference reference;
    String uid, myUid;

    private static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 1;
    private long mSigningSequence = 1;
    private static String callType="";
    ProgressDialog loadingBar;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_there_profile);

        Toolbar toolbar = findViewById(R.id.profile_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Profile");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        avatarIv = findViewById(R.id.avatarIv);
        nameTv = findViewById(R.id.nameTv);
        statusTv = findViewById(R.id.user_status);
        myAccount = findViewById(R.id.friend);

        message = findViewById(R.id.send_message_profile);
        audio_call = findViewById(R.id.voice_call_profile);
        video_call = findViewById(R.id.video_call_profile);

        progressBar = findViewById(R.id.progressbar);
        loadingBar = new ProgressDialog(ThereProfileActivity.this);

        fuser = FirebaseAuth.getInstance().getCurrentUser();
        firebaseAuth = FirebaseAuth.getInstance();
        myUid = firebaseAuth.getUid();

        Intent intent = getIntent();
        uid = intent.getStringExtra("uid");

        Query query = FirebaseDatabase.getInstance().getReference("Users").orderByChild("id").equalTo(uid);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                for (DataSnapshot ds: dataSnapshot.getChildren()) {

                    String name = ""+ ds.child("username").getValue();
                    String image = ""+ ds.child("imageURL").getValue();
                    String status = ""+ ds.child("user_status").getValue();

                    nameTv.setText(name);
                    statusTv.setText(status);

                    try {

                        Picasso.get().load(image).fit().centerCrop().placeholder(R.drawable.profile_image).into(avatarIv);

                    } catch (Exception e) {

                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        checkUserStatus();

        if (!myUid.equals(uid)) {

            message.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    imBlockedOrNot(uid);
                }
            });

            audio_call.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    AudioCall();
                }
            });

            video_call.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    VideoCall();
                }
            });
        } else {

            myAccount.setVisibility(View.GONE);
        }
    }

    private void imBlockedOrNot(final String hisUID) {

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(hisUID).child("BlockedUser").orderByChild("id").equalTo(myUid)
                .addValueEventListener(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        for (DataSnapshot ds: dataSnapshot.getChildren()) {

                            if (ds.exists()) {

                                Toast.makeText(getApplicationContext(), "You're blocked by that user, can't send message.", Toast.LENGTH_LONG).show();
                                return;
                            }
                        }

                        Intent intent = new Intent(getApplicationContext(), MessageActivity.class);
                        intent.putExtra("visit_user", hisUID);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void AudioCall() {

        checkForPermission();

        callType = "voice";

        loadingBar.setTitle("Voice Call");
        loadingBar.setMessage("Please wait, while we are configuring voice call...");
        loadingBar.setCanceledOnTouchOutside(false);
        loadingBar.show();

        SharedPreferences prefs = getSharedPreferences("sinch_service", MODE_PRIVATE);
        if (!prefs.getBoolean("isLogin",false)) {


            if (!fuser.getUid().equals(getSinchServiceInterface().getUsername())) {
                getSinchServiceInterface().stopClient();
            }

            getSinchServiceInterface().setUsername(fuser.getUid());

            UserController uc = Sinch.getUserControllerBuilder()
                    .context(getApplicationContext())
                    .applicationKey(APP_KEY)
                    .userId(fuser.getUid())
                    .environmentHost(ENVIRONMENT)
                    .build();
            uc.registerUser(this, this);
        }
        else {

            makeCall();
        }
    }

    private void VideoCall() {

        checkForPermission();

        loadingBar.setTitle("Video Call");
        loadingBar.setMessage("Please wait, while we are configuring video call...");
        loadingBar.setCanceledOnTouchOutside(false);
        loadingBar.show();

        callType="video";

        SharedPreferences prefs2 = getSharedPreferences("sinch_service",MODE_PRIVATE);
        if (!prefs2.getBoolean("isLogin",false)) {


            if (!fuser.getUid().equals(getSinchServiceInterface().getUsername())) {
                getSinchServiceInterface().stopClient();
            }

            getSinchServiceInterface().setUsername(fuser.getUid());

            UserController uc = Sinch.getUserControllerBuilder()
                    .context(getApplicationContext())
                    .applicationKey(APP_KEY)
                    .userId(fuser.getUid())
                    .environmentHost(ENVIRONMENT)
                    .build();
            uc.registerUser(this, this);
        }
        else {
            makeCall();
        }
    }

    @Override
    protected void onServiceConnected() {
        getSinchServiceInterface().setStartListener(this);
    }

    @Override
    public void onStartFailed(SinchError error) {
        Toast.makeText(this, error.toString(), Toast.LENGTH_LONG).show();
        loadingBar.dismiss();
    }

    @Override
    public void onStarted() {
        SharedPreferences.Editor ed = getSharedPreferences("sinch_service", MODE_PRIVATE).edit();
        ed.putBoolean("isLogin", true);
        ed.apply();
        makeCall();
    }

    private void startClientAndMakeCall() {
        // start Sinch Client, it'll result onStarted() callback from where the place call activity will be started
        if (!getSinchServiceInterface().isStarted()) {
            getSinchServiceInterface().startClient();
        }
    }

    @Override
    public void tokenRegistered() {
        startClientAndMakeCall();
    }

    @Override
    public void tokenRegistrationFailed(SinchError sinchError) {
        loadingBar.dismiss();
        Toast.makeText(this, "Push token registration failed - incoming calls can't be received!", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onCredentialsRequired(ClientRegistration clientRegistration) {
        String toSign = fuser.getUid() + APP_KEY + mSigningSequence + APP_SECRET;
        String signature;
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA-1");
            byte[] hash = messageDigest.digest(toSign.getBytes("UTF-8"));
            signature = Base64.encodeToString(hash, Base64.DEFAULT).trim();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e.getCause());
        }

        clientRegistration.register(signature, mSigningSequence++);
    }

    @Override
    public void onUserRegistered() {
        // Instance is registered, but we'll wait for another callback, assuring that the push token is
        // registered as well, meaning we can receive incoming calls.
    }

    @Override
    public void onUserRegistrationFailed(SinchError sinchError) {
        loadingBar.dismiss();
        Toast.makeText(this, "Registration failed!", Toast.LENGTH_LONG).show();
    }

    private void makeCall() {

        com.sinch.android.rtc.calling.Call call;
        String callId;

        loadingBar.dismiss();

        if (callType.equals("voice")) {
            call = getSinchServiceInterface().callUser(uid);
            callId = call.getCallId();

            Intent voiceCallIntent;
            voiceCallIntent = new Intent(ThereProfileActivity.this, VoiceCallScreenActivity.class);
            voiceCallIntent.putExtra(SinchService.CALL_ID,callId);
            voiceCallIntent.putExtra("userid", uid);
            startActivity(voiceCallIntent);
        }
        else {
            call = getSinchServiceInterface().callUserVideo(uid);
            callId = call.getCallId();

            Intent videoCallIntent;
            videoCallIntent = new Intent(this, VideoCallScreenActivity.class);
            videoCallIntent.putExtra(SinchService.CALL_ID, callId);
            videoCallIntent.putExtra("userid", uid);
            startActivity(videoCallIntent);
        }
    }

    private void checkForPermission() {

        String[] permission_list=new String[3];
        permission_list[0]= Manifest.permission.CAMERA;
        permission_list[1]=Manifest.permission.RECORD_AUDIO;
        permission_list[2]=Manifest.permission.READ_PHONE_STATE;

        String[] granted_permissions = new String[3];
        int index=0;
        int grant;
        for(int i=0;i<3;i++) {
            grant= ContextCompat.checkSelfPermission(getApplicationContext(),permission_list[i]);
            if (grant!= PackageManager.PERMISSION_GRANTED) {
                granted_permissions[index++]=permission_list[i];
            }
        }
        if(index!=0)
            ActivityCompat.requestPermissions(ThereProfileActivity.this, granted_permissions, REQUEST_ID_MULTIPLE_PERMISSIONS);
    }

    private void checkUserStatus() {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {

        } else {

            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    private void status(String status) {

        reference = FirebaseDatabase.getInstance().getReference("Users").child(fuser.getUid());

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("status", status);

        reference.updateChildren(hashMap);
    }

    @Override
    protected void onResume() {
        super.onResume();

        status("online");
    }

    @Override
    protected void onPause() {
        super.onPause();

        status("offline");
    }
}