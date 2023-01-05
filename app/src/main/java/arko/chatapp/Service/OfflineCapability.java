package arko.chatapp.Service;

import android.app.Application;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class OfflineCapability extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users");
        usersRef.keepSynced(true);

        DatabaseReference storyRef = FirebaseDatabase.getInstance().getReference("Story");
        storyRef.keepSynced(true);

        DatabaseReference postRef = FirebaseDatabase.getInstance().getReference("Posts");
        postRef.keepSynced(true);

        DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference("Chats");
        chatRef.keepSynced(true);
    }
}
