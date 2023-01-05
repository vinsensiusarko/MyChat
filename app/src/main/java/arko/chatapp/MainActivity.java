package arko.chatapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.sinch.android.rtc.Sinch;
import com.sinch.android.rtc.UserController;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import arko.chatapp.Adapter.TopStatusAdapter;
import arko.chatapp.Adapter.UserAdapter;
import arko.chatapp.Fragment.ChatsFragment;
import arko.chatapp.Fragment.PostFragment;
import arko.chatapp.Fragment.ProfileFragment;
import arko.chatapp.Fragment.UsersFragment;
import arko.chatapp.Model.Chat;
import arko.chatapp.Model.Status;
import arko.chatapp.Model.User;
import arko.chatapp.Model.UserStatus;
import arko.chatapp.databinding.ActivityMainBinding;
import de.hdodenhof.circleimageview.CircleImageView;

import static arko.chatapp.SinchService.APP_KEY;
import static arko.chatapp.SinchService.ENVIRONMENT;

public class MainActivity extends BaseActivity {

    CircleImageView profile_image;
    TextView username;

    FirebaseUser firebaseUser;
    DatabaseReference reference;

    ActivityMainBinding binding;
    FirebaseDatabase database;
    ArrayList<User> users;
    ArrayList<UserStatus> userStatuses;
    UserAdapter usersAdapter;
    TopStatusAdapter statusAdapter;
    ProgressDialog dialog;
    User user;

    String currentUser;

    private static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Toolbar toolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("MyChat");

        profile_image = findViewById(R.id.profile_image);
        username = findViewById(R.id.username);

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        currentUser = firebaseUser.getUid();

        reference = FirebaseDatabase.getInstance().getReference("Users").child(firebaseUser.getUid());
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (!dataSnapshot.exists()) {
                    return;
                }
                User user = dataSnapshot.getValue(User.class);
                username.setText(user.getUsername());

                try {

                    Picasso.get().load(user.getImageURL()).fit().centerCrop().placeholder(R.drawable.profile_image).into(profile_image);
                }
                catch (Exception e) {

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        dialog = new ProgressDialog(this);
        dialog.setMessage("Uploading Image...");
        dialog.setCancelable(false);

        database = FirebaseDatabase.getInstance();
        users = new ArrayList<>();
        userStatuses = new ArrayList<>();

        database.getReference().child("Users").child(FirebaseAuth.getInstance().getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        user = snapshot.getValue(User.class);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

        usersAdapter = new UserAdapter(this, users, false);
        statusAdapter = new TopStatusAdapter(this, userStatuses);

        //binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(RecyclerView.HORIZONTAL);
        binding.statusList.setLayoutManager(linearLayoutManager);

        binding.statusList.setAdapter(statusAdapter);
        binding.recyclerView.setAdapter(usersAdapter);

        binding.recyclerView.showShimmerAdapter();
        binding.statusList.showShimmerAdapter();

        binding.recyclerView.setHasFixedSize(true);
        binding.statusList.setHasFixedSize(true);

        binding.recyclerView.setNestedScrollingEnabled(false);
        binding.statusList.setNestedScrollingEnabled(false);

        database.getReference().child("Users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                users.clear();
                for(DataSnapshot snapshot1 : snapshot.getChildren()) {

                    User user = snapshot1.getValue(User.class);
                    if (!user.getId().equals(FirebaseAuth.getInstance().getUid())) {

                        users.add(user);
                    }
                }

                binding.recyclerView.hideShimmerAdapter();
                usersAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        database.getReference().child("Story").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {

                    userStatuses.clear();
                    for (DataSnapshot storySnapshot : snapshot.getChildren()) {

                        UserStatus status = new UserStatus();
                        status.setName(storySnapshot.child("username").getValue(String.class));
                        status.setProfileImage(storySnapshot.child("imageURL").getValue(String.class));
                        status.setLastUpdated(storySnapshot.child("lastUpdated").getValue(Long.class));

                        ArrayList<Status> statuses = new ArrayList<>();

                        for (DataSnapshot statusSnapshot : storySnapshot.child("statuses").getChildren()) {

                            Status sampleStatus = statusSnapshot.getValue(Status.class);
                            statuses.add(sampleStatus);
                        }

                        status.setStatuses(statuses);
                        userStatuses.add(status);
                    }

                    binding.statusList.hideShimmerAdapter();
                    binding.statusList.setHasFixedSize(true);
                    binding.statusList.setNestedScrollingEnabled(false);
                    statusAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        final TabLayout tabLayout = findViewById(R.id.tab_layout);
        final ViewPager viewPager = findViewById(R.id.view_pager);

        reference = FirebaseDatabase.getInstance().getReference("Chats");
        reference.addValueEventListener(new ValueEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
                int unread = 0;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                    Chat chat = snapshot.getValue(Chat.class);
                    if (chat.getReceiver().equals(firebaseUser.getUid()) && !chat.isIsseen()) {

                        unread++;
                    }
                }

                if (unread == 0) {

                    viewPagerAdapter.addFragment(new ChatsFragment(), "Chats");
                } else {

                    viewPagerAdapter.addFragment(new ChatsFragment(), "("+unread+") Chats");
                }

                viewPagerAdapter.addFragment(new UsersFragment(), "Users");
                viewPagerAdapter.addFragment(new PostFragment(), "Post");
                viewPagerAdapter.addFragment(new ProfileFragment(), "Profile");

                viewPager.setAdapter(viewPagerAdapter);
                viewPager.setNestedScrollingEnabled(false);
                tabLayout.setupWithViewPager(viewPager);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data != null) {
            if (data.getData() != null) {

                dialog.show();
                FirebaseStorage storage = FirebaseStorage.getInstance();
                Date date = new Date();
                StorageReference storyReference = storage.getReference().child("Status").child(date.getTime() + "");

                storyReference.putFile(data.getData()).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {

                        if(task.isSuccessful()) {

                            storyReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {

                                    UserStatus userStatus = new UserStatus();
                                    userStatus.setName(user.getUsername());
                                    userStatus.setProfileImage(user.getImageURL());
                                    userStatus.setLastUpdated(date.getTime());

                                    HashMap<String, Object> obj = new HashMap<>();
                                    obj.put("username", userStatus.getName());
                                    obj.put("imageURL", userStatus.getProfileImage());
                                    obj.put("lastUpdated", userStatus.getLastUpdated());

                                    String imageUrl = uri.toString();
                                    Status status = new Status(imageUrl, userStatus.getLastUpdated());

                                    database.getReference()
                                            .child("Story")
                                            .child(firebaseUser.getUid())
                                            .updateChildren(obj);

                                    database.getReference().child("Story")
                                            .child(firebaseUser.getUid())
                                            .child("statuses")
                                            .push()
                                            .setValue(status);

                                    long cutoff = new Date().getTime() - TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS);
                                    Query oldItems = database.getReference().child("Story")
                                            .child(firebaseUser.getUid()).child("statuses").orderByChild("timeStamp").endAt(cutoff);
                                    oldItems.addListenerForSingleValueEvent(new ValueEventListener() {

                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {

                                            for (DataSnapshot itemSnapshot: snapshot.getChildren()) {

                                                itemSnapshot.getRef().removeValue();
                                            }

                                            binding.statusList.setNestedScrollingEnabled(true);
                                            statusAdapter.notifyDataSetChanged();
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                            throw databaseError.toException();
                                        }
                                    });

                                    dialog.dismiss();
                                }
                            });
                        }
                    }
                });
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        super.onOptionsItemSelected(item);

        if (item.getItemId() == R.id.main_logout_option) {

            FirebaseAuth.getInstance().signOut();

            if (getSinchServiceInterface() != null) {

                UserController uc = Sinch.getUserControllerBuilder()
                        .context(getApplicationContext())
                        .applicationKey(APP_KEY)
                        .userId(currentUser)
                        .environmentHost(ENVIRONMENT)
                        .build();
                uc.unregisterPushToken();
                getSinchServiceInterface().stopClient();
            }

            SendUserToLoginActivity();
            Toast.makeText(this, "Logout", Toast.LENGTH_SHORT).show();
        }
        if (item.getItemId() == R.id.main_setting_option) {

            SendUserToSettingActivity();
        }
        if (item.getItemId() == R.id.main_add_story) {

            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(intent, 75);
        }
        if (item.getItemId() == R.id.main_about_option) {

            SendUserToAboutActivity();
        }
        if (item.getItemId() == R.id.main_change_password_option) {

            SendUserToChangePasswordActivity();
        }

        if (item.getItemId() == R.id.main_permission_option) {

            SendUserToPermission();
        }
        return false;
    }

    class ViewPagerAdapter extends FragmentPagerAdapter {

        private ArrayList<Fragment> fragments;
        private ArrayList<String> titles;

        ViewPagerAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
            this.fragments = new ArrayList<>();
            this.titles = new ArrayList<>();
        }

        @Override
        public Fragment getItem(int position) {
            return fragments.get(position);
        }

        @Override
        public int getCount() {
            return fragments.size();
        }

        public void addFragment(Fragment fragment, String title){
            fragments.add(fragment);
            titles.add(title);
        }

        // Ctrl + O

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return titles.get(position);
        }
    }

    private void status(String status) {

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

    private void SendUserToPermission() {

        Intent permissionIntent = new Intent(MainActivity.this, SetUpActivity.class);
        startActivity(permissionIntent);
    }

    private void SendUserToLoginActivity() {

        Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginIntent);
        finish();
    }

    private void SendUserToAboutActivity() {

        Intent aboutIntent = new Intent(MainActivity.this, AboutActivity.class);
        startActivity(aboutIntent);
    }

    private void SendUserToSettingActivity() {

        Intent settingIntent = new Intent(MainActivity.this, SettingActivity.class);
        startActivity(settingIntent);
    }

    private void SendUserToChangePasswordActivity() {

        Intent changePasswordIntent = new Intent(MainActivity.this, ChangePasswordActivity.class);
        startActivity(changePasswordIntent);
    }
}