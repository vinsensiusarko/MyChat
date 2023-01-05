package arko.chatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class ChangePasswordActivity extends AppCompatActivity {

    private ProgressDialog loadingBar;

    private Button buttonSubmit;
    private EditText currentPassword, newPassword;
    private String TAG;

    FirebaseUser firebaseUser;
    DatabaseReference reference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        Toolbar toolbar = findViewById(R.id.change_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Change Password");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        reference = FirebaseDatabase.getInstance().getReference("Users").child(firebaseUser.getUid());

        buttonSubmit = (Button) findViewById(R.id.submit_password);
        currentPassword = (EditText) findViewById(R.id.current_password_text);
        newPassword = (EditText) findViewById(R.id.new_password_text);
        loadingBar = new ProgressDialog(this);

        buttonSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                ChangePassword();
            }
        });
    }

    private void ChangePassword(){

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        String currentPass = currentPassword.getText().toString();
        String newPass = newPassword.getText().toString();

        if (TextUtils.isEmpty(currentPass) || TextUtils.isEmpty(newPass)) {

            loadingBar.dismiss();
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();

        } else {
            loadingBar.setTitle("Change Password");
            loadingBar.setMessage("Please wait...");
            loadingBar.setCanceledOnTouchOutside(false);
            loadingBar.show();

            assert user != null;
            user.updatePassword(newPass)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {

                                loadingBar.dismiss();

                                SendUserToMainActivity();
                                Toast.makeText(ChangePasswordActivity.this, "Password Updated...", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }

    private void status(String status){
        reference = FirebaseDatabase.getInstance().getReference("Users").child(firebaseUser.getUid());

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

    private void SendUserToMainActivity() {

        Intent mainIntent = new Intent(ChangePasswordActivity.this, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }
}