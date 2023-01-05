package arko.chatapp.Fragment;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import arko.chatapp.Adapter.UserAdapter;
import arko.chatapp.Model.ChatList;
import arko.chatapp.Model.User;
import arko.chatapp.Notifications.Token;
import arko.chatapp.R;

public class ChatsFragment extends Fragment {

    private RecyclerView recyclerView;

    private UserAdapter userAdapter;
    private List<User> mUsers;

    FirebaseUser fuser;
    DatabaseReference reference;

    private boolean animated = false;
    private List<ChatList> usersList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chats, container, false);

        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        fuser = FirebaseAuth.getInstance().getCurrentUser();

        usersList = new ArrayList<>();

        DividerItemDecoration dividerItemDecorationVertical = new DividerItemDecoration(getContext(), LinearLayoutManager.VERTICAL);
        dividerItemDecorationVertical.setDrawable(getContext().getResources().getDrawable(R.drawable.divider_layer));
        recyclerView.addItemDecoration(dividerItemDecorationVertical);

        reference = FirebaseDatabase.getInstance().getReference("Chat List").child(fuser.getUid());
        reference.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                usersList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                    ChatList chatlist = snapshot.getValue(ChatList.class);
                    usersList.add(chatlist);
                }

                chatList();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        updateToken(FirebaseInstanceId.getInstance().getToken());

        return view;
    }

    private void updateToken(String token) {

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Tokens");
        Token token1 = new Token(token);
        reference.child(fuser.getUid()).setValue(token1);
    }

    private void chatList() {

        AsyncTask.execute(() -> {

            if (getActivity() == null) {
                return;
            }

            new Handler(Looper.getMainLooper()).post(() -> {

                if (getActivity() == null) {
                    return;
                }

                mUsers = new ArrayList<>();
                reference = FirebaseDatabase.getInstance().getReference("Users");
                reference.addValueEventListener(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        mUsers.clear();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                            User user = snapshot.getValue(User.class);
                            for (ChatList chatlist : usersList) {

                                if (user.getId() != null && user.getId().equals(chatlist.getId())) {

                                    mUsers.add(user);
                                    break;
                                }
                            }

                            userAdapter = new UserAdapter(getContext(), mUsers, true);
                            recyclerView.setAdapter(userAdapter);
                            recyclerView.setHasFixedSize(true);
                            recyclerView.setNestedScrollingEnabled(false);
                            recyclerView.smoothScrollToPosition(recyclerView.getAdapter().getItemCount());

                            if (!animated) {
                                LayoutAnimationController animation = AnimationUtils.loadLayoutAnimation(getActivity(), R.anim.layout_animation_fall_down);
                                recyclerView.setLayoutAnimation(animation);
                                animated = true;
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            });
        });
    }
}
