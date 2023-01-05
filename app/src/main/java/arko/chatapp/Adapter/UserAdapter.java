package arko.chatapp.Adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.List;

import arko.chatapp.Encryption.AESCryptoChat;
import arko.chatapp.MessageActivity;
import arko.chatapp.Model.Chat;
import arko.chatapp.Model.User;
import arko.chatapp.R;
import arko.chatapp.ThereProfileActivity;
import de.hdodenhof.circleimageview.CircleImageView;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {

    Context mContext;
    List<User> mUsers;
    boolean isChat;

    String theLastMessage;

    FirebaseAuth firebaseAuth;
    String myUid;

    AESCryptoChat aes = new AESCryptoChat("lv39eptlvuhaqqsr");

    public UserAdapter(Context mContext, List<User> mUsers, boolean isChat) {

        this.mUsers = mUsers;
        this.mContext = mContext;
        this.isChat = isChat;

        firebaseAuth = FirebaseAuth.getInstance();
        myUid = firebaseAuth.getUid();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(mContext).inflate(R.layout.user_item, parent, false);
        return new UserAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {

        final User user = mUsers.get(position);
        final String hisUID = mUsers.get(position).getId();
        String username = mUsers.get(position).getUsername();
        String image = mUsers.get(position).getImageURL();

        holder.username.setText(username);
        holder.userStatus.setText(user.getUser_status());

        try {

            Picasso.get().load(image).fit().centerCrop().placeholder(R.drawable.profile_image).into(holder.profile_image);
        }
        catch (Exception e) {

        }

        if (isChat) {

            holder.username.setText(user.getUsername());
            holder.userStatus.setText(user.getUser_status());
            lastMessage(user.getId(), holder.last_msg);

            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {

                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    builder.setItems(new String[]{"Delete Chat"}, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if (i == 0) {

                                deleteChatList(hisUID);
                            }
                        }
                    });

                    builder.create().show();
                    return false;
                }
            });
        } else {

            holder.userStatus.setVisibility(View.VISIBLE);
            holder.last_msg.setVisibility(View.GONE);
        }

        if (isChat) {

            if (user.getStatus().equals("online")) {

                holder.img_on.setVisibility(View.VISIBLE);
                holder.img_off.setVisibility(View.GONE);
            } else {

                holder.img_on.setVisibility(View.GONE);
                holder.img_off.setVisibility(View.VISIBLE);
            }
            holder.blockIV.setVisibility(View.GONE);
        } else {

            holder.img_on.setVisibility(View.GONE);
            holder.img_off.setVisibility(View.GONE);

            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {

                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    builder.setItems(new String[]{"Profile", "Chat"}, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if (i == 0) {

                                Intent intent = new Intent(mContext, ThereProfileActivity.class);
                                intent.putExtra("uid", hisUID);
                                mContext.startActivity(intent);
                            }
                            if (i == 1) {

                                imBlockedOrNot(hisUID);
                            }
                        }
                    });

                    builder.create().show();
                    return false;
                }
            });
        }

        holder.blockIV.setImageResource(R.drawable.ic_unblocked_green);

        holder.blockIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (mUsers.get(position).isBlocked()) {

                    unBlockUser(hisUID);
                } else {

                    blockUser(hisUID);
                }
            }
        });

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                imBlockedOrNot(hisUID);
            }
        });

        checkIsBlocked(hisUID, holder, position);
    }

    @Override
    public int getItemCount() {
        return mUsers.size();
    }

    public  class ViewHolder extends RecyclerView.ViewHolder{

        public TextView username, userStatus;
        public ImageView profile_image;
        private ImageView img_on;
        private ImageView img_off;
        private TextView last_msg;

        CircleImageView blockIV;

        public ViewHolder(View itemView) {
            super(itemView);

            username = itemView.findViewById(R.id.username);
            userStatus = itemView.findViewById(R.id.user_status);
            profile_image = itemView.findViewById(R.id.profile_image);
            img_on = itemView.findViewById(R.id.img_on);
            img_off = itemView.findViewById(R.id.img_off);
            last_msg = itemView.findViewById(R.id.last_msg);

            blockIV = itemView.findViewById(R.id.blockIv);
        }
    }

    //check for last message
    private void lastMessage(final String userid, final TextView last_msg) {

        theLastMessage = "default";
        final FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Chats");

        reference.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                    Chat chat = snapshot.getValue(Chat.class);
                   if (firebaseUser != null && chat != null) {

                        if (chat.getReceiver().equals(firebaseUser.getUid()) && chat.getSender().equals(userid) ||
                                chat.getReceiver().equals(userid) && chat.getSender().equals(firebaseUser.getUid())) {

                            theLastMessage = chat.getMessage();
                        }
                    }
                }

                String decryptedMessage = null;

                try {
                    decryptedMessage = aes.decrypt(theLastMessage);
                } catch (Exception e) {
//            Logger.getLogger(AESCrypt.class.getName()).log(Level.SEVERE, null, e);
                    e.printStackTrace();
                }

                switch (theLastMessage) {

                    case  "default":
                        last_msg.setText("No Message");
                        break;

                    default:
                        last_msg.setText(decryptedMessage);
                        break;
                }

                theLastMessage = "default";
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void imBlockedOrNot(final String hisUID) {

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(hisUID).child("BlockedUser").orderByChild("id").equalTo(myUid)
                .addValueEventListener(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        for (DataSnapshot ds: dataSnapshot.getChildren()) {

                            if (ds.exists()) {

                                Toast.makeText(mContext, "You're blocked by that user, can't send message.", Toast.LENGTH_LONG).show();
                                return;
                            }
                        }

                        Intent intent = new Intent(mContext, MessageActivity.class);
                        intent.putExtra("visit_user", hisUID);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        mContext.startActivity(intent);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void checkIsBlocked(String hisUID, final ViewHolder myHolder, final int position) {

        if (myUid != null) {

            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
            ref.child(myUid).child("BlockedUser").orderByChild("id").equalTo(hisUID)
                    .addValueEventListener(new ValueEventListener() {

                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                            for (DataSnapshot ds : dataSnapshot.getChildren()) {

                                if (ds.exists()) {

                                    myHolder.blockIV.setImageResource(R.drawable.ic_blocked_red);
                                    mUsers.get(position).setBlocked(true);
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
        }
    }

    private void blockUser(String hisUID) {

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("id", hisUID);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(myUid).child("BlockedUser").child(hisUID).setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {

                    @Override
                    public void onSuccess(Void aVoid) {

                        Toast.makeText(mContext, "Blocked Successfully...", Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(new OnFailureListener() {

            @Override
            public void onFailure(@NonNull Exception e) {

                Toast.makeText(mContext, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void unBlockUser(String hisUID) {

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(myUid).child("BlockedUser").child(hisUID)
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        for (DataSnapshot ds: dataSnapshot.getChildren()) {

                            if (ds.exists()) {

                                ds.getRef().removeValue()
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {

                                            @Override
                                            public void onSuccess(Void aVoid) {

                                                Toast.makeText(mContext, "Unblocked Successfully...", Toast.LENGTH_SHORT).show();
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {

                                            @Override
                                            public void onFailure(@NonNull Exception e) {

                                                Toast.makeText(mContext, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
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

    private void deleteChatList(String hisUID) {

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Chat List");
        ref.child(myUid).child(hisUID)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        snapshot.getRef().removeValue();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }
}
