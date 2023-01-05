package arko.chatapp;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.sinch.android.rtc.ClientRegistration;
import com.sinch.android.rtc.PushTokenRegistrationCallback;
import com.sinch.android.rtc.Sinch;
import com.sinch.android.rtc.SinchError;
import com.sinch.android.rtc.UserController;
import com.sinch.android.rtc.UserRegistrationCallback;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import arko.chatapp.Adapter.MessageAdapter;
import arko.chatapp.Encryption.AESCryptoChat;
import arko.chatapp.Model.Chat;
import arko.chatapp.Model.User;
import arko.chatapp.Notifications.APIService;
import arko.chatapp.Notifications.Client;
import arko.chatapp.Notifications.Data;
import arko.chatapp.Notifications.MyResponse;
import arko.chatapp.Notifications.Sender;
import arko.chatapp.Notifications.Token;
import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static arko.chatapp.SinchService.APP_KEY;
import static arko.chatapp.SinchService.APP_SECRET;
import static arko.chatapp.SinchService.ENVIRONMENT;

public class MessageActivity extends BaseActivity implements SinchService.StartFailedListener, PushTokenRegistrationCallback, UserRegistrationCallback {

    CircleImageView profile_image, blockIv;
    TextView username, userStatus;
    ImageView btn_send, send_file_button;
    EditText text_send;
    RecyclerView recyclerView;
    Intent intent;
    ValueEventListener seenListener;

    FirebaseUser fuser;
    DatabaseReference reference;

    MessageAdapter messageAdapter;
    List<Chat> mChat;

    String userid;

    APIService apiService;

    boolean notify = false;
    boolean isBlocked = false;

    private RequestQueue requestQueue;
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_REQUEST_CODE = 200;

    private static final int IMAGE_PICK_CAMERA_CODE = 300;
    private static final int IMAGE_PICK_GALLERY_CODE = 400;

    String[] cameraPermissions;
    String[] storagePermissions;
    Uri image_uri = null;

    AESCryptoChat aes = new AESCryptoChat("lv39eptlvuhaqqsr");

    ProgressDialog loadingBar;
    ProgressBar progressBar;
    private long mSigningSequence = 1;
    private static String callType="";
    private static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        Toolbar toolbar = findViewById(R.id.message_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                SendToMainActivity();
            }
        });

        blockIv = findViewById(R.id.blockIv);

        apiService = Client.getClient("https://fcm.googleapis.com/").create(APIService.class);

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setNestedScrollingEnabled(false);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        profile_image = findViewById(R.id.profile_image);
        username = findViewById(R.id.username);
        userStatus = findViewById(R.id.status_user);
        btn_send = findViewById(R.id.btn_send);
        text_send = findViewById(R.id.text_send);
        send_file_button = findViewById(R.id.send_file_btn);

        progressBar=findViewById(R.id.progressbar);
        loadingBar = new ProgressDialog(MessageActivity.this);

        intent = getIntent();
        userid = intent.getStringExtra("visit_user");
        fuser = FirebaseAuth.getInstance().getCurrentUser();

        requestQueue = Volley.newRequestQueue(getApplicationContext());

        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                notify = true;
                String msg = text_send.getText().toString();
                if (!msg.equals("")) {

                    sendMessage(fuser.getUid(), userid, msg);
                } else {

                    Toast.makeText(MessageActivity.this, "You can't send empty message", Toast.LENGTH_SHORT).show();
                }

                text_send.setText("");
            }
        });

        send_file_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                showImagePickDialog();
            }
        });

        blockIv.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                if (isBlocked) {

                    unBlockUser();
                } else {

                    blockUser();
                }
            }
        });

        reference = FirebaseDatabase.getInstance().getReference("Users").child(userid);
        reference.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                User user = dataSnapshot.getValue(User.class);
                username.setText(user.getUsername());
                userStatus.setText(user.getStatus());

                try {

                    Picasso.get().load(user.getImageURL()).fit().centerCrop().placeholder(R.drawable.profile_image).into(profile_image);
                }
                catch (Exception e) {

                }

                readMesagges(fuser.getUid(), userid, user.getImageURL());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        checkIsBlocked();

        seenMessage(userid);
    }

    private void checkIsBlocked() {

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(fuser.getUid()).child("BlockedUser").orderByChild("id").equalTo(userid)
                .addValueEventListener(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        for (DataSnapshot ds: dataSnapshot.getChildren()) {

                            if (ds.exists()) {

                                blockIv.setImageResource(R.drawable.ic_blocked_red);
                                isBlocked = true;
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void blockUser() {

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("id", userid);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(fuser.getUid()).child("BlockedUser").child(userid).setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {

                    @Override
                    public void onSuccess(Void aVoid) {

                        Toast.makeText(MessageActivity.this, "Blocked Successfully...", Toast.LENGTH_SHORT).show();

                        blockIv.setImageResource(R.drawable.ic_blocked_red);
                    }
                }).addOnFailureListener(new OnFailureListener() {

            @Override
            public void onFailure(@NonNull Exception e) {

                Toast.makeText(MessageActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void unBlockUser() {

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(fuser.getUid()).child("BlockedUser").child(userid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        for (DataSnapshot ds: dataSnapshot.getChildren()) {

                            if (ds.exists()) {

                                ds.getRef().removeValue()
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {

                                            @Override
                                            public void onSuccess(Void aVoid) {

                                                Toast.makeText(MessageActivity.this, "Unblocked Successfully...", Toast.LENGTH_SHORT).show();

                                                blockIv.setImageResource(R.drawable.ic_unblocked_green);
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {

                                            @Override
                                            public void onFailure(@NonNull Exception e) {

                                                Toast.makeText(MessageActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void showImagePickDialog() {
        String[]  options = {"Camera", "Gallery"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Image from");

        builder.setItems(options, new DialogInterface.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {

                if (which == 0) {

                    if (!checkCameraPermission()) {

                        requestCameraPermission();
                    } else{
                        pickFromCamera();
                    }
                }
                if (which == 1) {

                    if (!checkStoragePermission()) {

                        requestStoragePermission();
                    } else {

                        pickFromGallery();
                    }
                }
            }
        });

        builder.create().show();
    }

    private void pickFromCamera() {

        ContentValues cv = new ContentValues();
        cv.put(MediaStore.Images.Media.TITLE, "Temp Pick");
        cv.put(MediaStore.Images.Media.DESCRIPTION, "Temp Descr");
        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(intent, IMAGE_PICK_CAMERA_CODE);
    }

    private void pickFromGallery() {

        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK_GALLERY_CODE);
    }

    private boolean checkStoragePermission() {

        boolean result = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return result;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestStoragePermission() {

        requestPermissions(storagePermissions, STORAGE_REQUEST_CODE);
    }

    private boolean checkCameraPermission() {

        boolean result = ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);
        boolean result1 = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return result && result1;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestCameraPermission() {

        requestPermissions(cameraPermissions, CAMERA_REQUEST_CODE);
    }

    private void seenMessage(final String userid) {

        reference = FirebaseDatabase.getInstance().getReference("Chats");
        seenListener = reference.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                    Chat chat = snapshot.getValue(Chat.class);
                    if (chat.getReceiver().equals(fuser.getUid()) && chat.getSender().equals(userid)) {

                        HashMap<String, Object> hashMap = new HashMap<>();
                        hashMap.put("isseen", true);
                        snapshot.getRef().updateChildren(hashMap);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void sendMessage(final String sender, final String receiver, final String message) {

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

        String timestamp = String.valueOf(System.currentTimeMillis());

        String encryptedMessage = null;

        try {
            encryptedMessage = aes.encrypt(message);
        } catch (Exception e) {
//            Logger.getLogger(AESCrypt.class.getName()).log(Level.SEVERE, null, e);
            e.printStackTrace();
        }

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("sender", sender);
        hashMap.put("receiver", receiver);
        hashMap.put("message", encryptedMessage);
        hashMap.put("timeStamp", timestamp);
        hashMap.put("type", "text");
        hashMap.put("isseen", false);

        reference.child("Chats").push().setValue(hashMap);


        // add user to chat fragment
        final DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference("Chat List")
                .child(fuser.getUid())
                .child(userid);

        chatRef.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (!dataSnapshot.exists()) {

                    chatRef.child("id").setValue(userid);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        
        final DatabaseReference chatRefReceiver = FirebaseDatabase.getInstance().getReference("Chat List")
                .child(userid)
                .child(fuser.getUid());
        chatRefReceiver.child("id").setValue(fuser.getUid());

        final String msg = message;

        reference = FirebaseDatabase.getInstance().getReference("Users").child(fuser.getUid());
        reference.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                User user = dataSnapshot.getValue(User.class);
                if (notify) {

                    sendNotification(receiver, user.getUsername(), msg);
                }

                notify = false;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void sendImageMessage(Uri image_uri) throws IOException {

        notify = true;

        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Sending image..");
        progressDialog.show();

        final String timeStamp = ""+System.currentTimeMillis();

        String fileNameAndPath = "Chat Images/"+"post_"+ timeStamp;

        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), image_uri);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG,100, baos);
        final byte[] data = baos.toByteArray();

        StorageReference ref = FirebaseStorage.getInstance().getReference().child(fileNameAndPath);
        ref.putBytes(data)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {

                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                        progressDialog.dismiss();

                        Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                        while (!uriTask.isSuccessful());
                        String downloadUri = uriTask.getResult().toString();

                        if (uriTask.isSuccessful()) {

                            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();

                            HashMap<String, Object> hashMap = new HashMap<>();

                            hashMap.put("sender", fuser.getUid());
                            hashMap.put("receiver", userid);
                            hashMap.put("message", downloadUri);
                            hashMap.put("timeStamp", timeStamp);
                            hashMap.put("type", "image");
                            hashMap.put("isseen", false);

                            databaseReference.child("Chats").push().setValue(hashMap);

                            final DatabaseReference chatRefReceiver = FirebaseDatabase.getInstance().getReference("Chat List")
                                    .child(userid)
                                    .child(fuser.getUid());
                            chatRefReceiver.child("id").setValue(fuser.getUid());

                            databaseReference = FirebaseDatabase.getInstance().getReference("Users").child(fuser.getUid());
                            databaseReference.addValueEventListener(new ValueEventListener() {

                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                    User user  = dataSnapshot.getValue(User.class);
                                    if (notify) {

                                        sendNotification(userid, user.getUsername(), "Sent you a photo...");
                                    }

                                    notify = false;
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {

                    @Override
                    public void onFailure(@NonNull Exception e) {

                        progressDialog.dismiss();
                        Toast.makeText(MessageActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

    }



    private void sendNotification(String receiver, final String username, final String message) {

        DatabaseReference tokens = FirebaseDatabase.getInstance().getReference("Tokens");
        Query query = tokens.orderByKey().equalTo(receiver);
        query.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                    Token token = snapshot.getValue(Token.class);
                    Data data = new Data(fuser.getUid(), R.drawable.logo, username+" : "+message, "New Message",
                            userid);

                    Sender sender = new Sender(data, token.getToken());

                    apiService.sendNotification(sender)
                            .enqueue(new Callback<MyResponse>() {

                                @Override
                                public void onResponse(Call<MyResponse> call, Response<MyResponse> response) {

                                    if (response.code() == 200){
                                        if (response.body().success != 1) {

                                            Toast.makeText(MessageActivity.this, "Failed!", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }

                                @Override
                                public void onFailure(Call<MyResponse> call, Throwable t) {

                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void readMesagges(final String myid, final String userid, final String imageurl) {

        mChat = new ArrayList<>();

        reference = FirebaseDatabase.getInstance().getReference("Chats");
        reference.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                mChat.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                    Chat chat = snapshot.getValue(Chat.class);
                    if (chat.getReceiver().equals(myid) && chat.getSender().equals(userid) ||
                            chat.getReceiver().equals(userid) && chat.getSender().equals(myid)) {

                        mChat.add(chat);
                    }

                    messageAdapter = new MessageAdapter(MessageActivity.this, mChat, imageurl);
                    messageAdapter.notifyDataSetChanged();
                    recyclerView.setAdapter(messageAdapter);
                    recyclerView.smoothScrollToPosition(recyclerView.getAdapter().getItemCount());

                    recyclerView.setHasFixedSize(true);
                    recyclerView.setNestedScrollingEnabled(false);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode){
            case CAMERA_REQUEST_CODE:{

                if (grantResults.length > 0) {

                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                    if(cameraAccepted && storageAccepted) {

                        pickFromCamera();
                    } else {

                        Toast.makeText(this, "Permissions are necessary", Toast.LENGTH_SHORT).show();
                    }
                } else {

                }
            }
            break;
            case STORAGE_REQUEST_CODE: {

                if (grantResults.length > 0) {

                    boolean storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if(storageAccepted) {

                        pickFromGallery();
                    } else {

                        Toast.makeText(this, "Permissions are necessary", Toast.LENGTH_SHORT).show();
                    }
                } else {

                }
            }
            break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        if(resultCode == RESULT_OK ) {

            if(requestCode == IMAGE_PICK_GALLERY_CODE) {

                image_uri = data.getData();

                try {

                    sendImageMessage(image_uri);
                } catch (IOException e) {

                    e.printStackTrace();
                }
            }
            else if(requestCode == IMAGE_PICK_CAMERA_CODE) {

                try {

                    sendImageMessage(image_uri);
                } catch (IOException e) {

                    e.printStackTrace();
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void currentUser(String userid) {

        SharedPreferences.Editor editor = getSharedPreferences("PREFS", MODE_PRIVATE).edit();
        editor.putString("currentuser", userid);
        editor.apply();
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
        currentUser(userid);
    }

    @Override
    protected void onPause() {
        super.onPause();

        reference.removeEventListener(seenListener);
        status("offline");
        currentUser("none");
    }

    private void SendToMainActivity() {

        Intent intent = new Intent(MessageActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {

            case R.id.voice_call:

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
                break;

            case R.id.video_call:

                checkForPermission();

                loadingBar.setTitle("Video Call");
                loadingBar.setMessage("Please wait, while we are configuring video call...");
                loadingBar.setCanceledOnTouchOutside(false);
                loadingBar.show();

                callType="video";

                SharedPreferences prefs2 = getSharedPreferences("sinch_service", MODE_PRIVATE);
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
                break;
        }

        return false;
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
            call = getSinchServiceInterface().callUser(userid);
            callId = call.getCallId();

            Intent voiceCallIntent;
            voiceCallIntent = new Intent(MessageActivity.this,VoiceCallScreenActivity.class);
            voiceCallIntent.putExtra(SinchService.CALL_ID,callId);
            voiceCallIntent.putExtra("userid", userid);
            startActivity(voiceCallIntent);
        }
        else {
            call = getSinchServiceInterface().callUserVideo(userid);
            callId = call.getCallId();

            Intent videoCallIntent;
            videoCallIntent = new Intent(this, VideoCallScreenActivity.class);
            videoCallIntent.putExtra(SinchService.CALL_ID, callId);
            videoCallIntent.putExtra("userid", userid);
            startActivity(videoCallIntent);
        }
    }

    private void checkForPermission() {

        String[] permission_list = new String[3];
        permission_list[0] = Manifest.permission.CAMERA;
        permission_list[1] = Manifest.permission.RECORD_AUDIO;
        permission_list[2] = Manifest.permission.READ_PHONE_STATE;

        String[] granted_permissions = new String[3];
        int index = 0;
        int grant;
        for(int i = 0; i < 3; i++) {

            grant = ContextCompat.checkSelfPermission(getApplicationContext(), permission_list[i]);
            if (grant != PackageManager.PERMISSION_GRANTED) {

                granted_permissions[index++] = permission_list[i];
            }
        }
        if(index != 0)
            ActivityCompat.requestPermissions(MessageActivity.this, granted_permissions, REQUEST_ID_MULTIPLE_PERMISSIONS);
    }
}
