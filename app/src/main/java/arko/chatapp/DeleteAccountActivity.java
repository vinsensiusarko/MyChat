package arko.chatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class DeleteAccountActivity extends AppCompatActivity {

    Button DeleteYes, DeleteNo;
    ProgressDialog loadingBar;

    FirebaseAuth mAuth;
    StorageReference UserProfileImageRef;
    DatabaseReference RootReef, UserRef;
    FirebaseUser firebaseUser;

    String currentUserID;
    String TAG;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_account);

        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();

        RootReef = FirebaseDatabase.getInstance().getReference();
        UserProfileImageRef = FirebaseStorage.getInstance().getReference("Profile Images");

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        UserRef = FirebaseDatabase.getInstance().getReference("Users").child(firebaseUser.getUid());

        InitializeFields();

        DeleteYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                loadingBar.dismiss();

                DeleteAuth();
                DeleteUser();
                DeleteImage();

                SendUserToRegister();
            }
        });

        DeleteNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                SendUserToSettingActivity();
            }
        });
    }

    private void InitializeFields() {

        DeleteYes = (Button) findViewById(R.id.delete_yes);
        DeleteNo = (Button) findViewById(R.id.delete_no);
        loadingBar = new ProgressDialog(this);
    }

    private void DeleteAuth() {

        FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        AuthCredential authCredential = EmailAuthProvider.getCredential("user@example.com", "password1234");

        assert fUser != null;
        fUser.reauthenticate(authCredential).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                fUser.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {

                        if (task.isSuccessful()) {

                            loadingBar.setTitle("Delete Account");
                            loadingBar.setMessage("Please wait, while deleting your account");
                            loadingBar.setCanceledOnTouchOutside(false);
                            loadingBar.show();

                            Log.d(TAG, "User account deleted!");
                        }
                    }
                });
            }
        });
    }

    private void DeleteUser(){

        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();

        rootRef.child("Users").child(currentUserID).removeValue();
        rootRef.child("Chat List").child(currentUserID).removeValue();
        rootRef.child("Tokens").child(currentUserID).child("token").removeValue();
    }

    private void DeleteImage() {

        StorageReference storageReference = UserProfileImageRef.child(currentUserID + ".jpg");
        StorageReference storageReference2 = UserProfileImageRef.child(currentUserID + ".png");

        storageReference.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {

                Log.d(TAG, "onSuccess: deleted file successfully");
            }
        });

        storageReference2.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {

                Log.d(TAG, "onSuccess: deleted file successfully");
            }
        });
    }

    private void SendUserToRegister() {

        Intent registerIntent = new Intent(DeleteAccountActivity.this, RegisterActivity.class);
        registerIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(registerIntent);
        finish();
    }

    private void SendUserToSettingActivity() {

        Intent settingIntent = new Intent(DeleteAccountActivity.this, SettingActivity.class);
        settingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(settingIntent);
        finish();
    }

    private void status(String status) {

        UserRef = FirebaseDatabase.getInstance().getReference("Users").child(firebaseUser.getUid());

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("status", status);

        UserRef.updateChildren(hashMap);
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