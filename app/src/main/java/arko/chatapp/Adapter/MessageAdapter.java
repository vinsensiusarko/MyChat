package arko.chatapp.Adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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
import com.squareup.picasso.Picasso;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import arko.chatapp.Encryption.AESCryptoChat;
import arko.chatapp.Model.Chat;
import arko.chatapp.Model.User;
import arko.chatapp.R;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {

    public static final int MSG_TYPE_LEFT = 0;
    public static final int MSG_TYPE_RIGHT = 1;

    Context mContext;
    List<Chat> mChat;
    String imageUrl;

    FirebaseUser fuser;
    FirebaseAuth firebaseAuth;
    String myUid;

    AESCryptoChat aes = new AESCryptoChat("lv39eptlvuhaqqsr");

    public MessageAdapter(Context mContext, List<Chat> mChat, String imageUrl) {

        this.mChat = mChat;
        this.mContext = mContext;
        this.imageUrl = imageUrl;

        firebaseAuth = FirebaseAuth.getInstance();
        myUid = firebaseAuth.getUid();
    }

    @NonNull
    @Override
    public MessageAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view;
        if (viewType == MSG_TYPE_RIGHT) {

            view = LayoutInflater.from(mContext).inflate(R.layout.chat_item_right, parent, false);
        } else {

            view = LayoutInflater.from(mContext).inflate(R.layout.chat_item_left, parent, false);
        }
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {

        Chat chat = mChat.get(position);

        String message = mChat.get(position).getMessage();
        String type = mChat.get(position).getType();

        String decryptedMessage = null;

        try {

            decryptedMessage = aes.decrypt(message);

        } catch (Exception e) {
//            Logger.getLogger(AESCrypt.class.getName()).log(Level.SEVERE, null, e);
            e.printStackTrace();
        }

        holder.show_message.setText(message);

        try {

            Picasso.get().load(imageUrl).fit().centerCrop().placeholder(R.drawable.profile_image).into(holder.profile_image);
        }
        catch (Exception e) {

        }

        if (type.equals("text")) {

            holder.show_message.setVisibility(View.VISIBLE);
            holder.messageIv.setVisibility(View.GONE);

            holder.show_message.setText(message);
        } else {

            holder.show_message.setVisibility(View.GONE);
            holder.messageIv.setVisibility(View.VISIBLE);

            Picasso.get().load(message).fit().centerCrop().placeholder(R.drawable.ic_image_black).into(holder.messageIv);
        }

        if (position == mChat.size() - 1) {

            if (chat.isIsseen()) {

                holder.txt_seen.setText("Seen");
            } else {

                holder.txt_seen.setText("Delivered");
            }
        } else {

            holder.txt_seen.setVisibility(View.GONE);
        }

        holder.messageIv.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setTitle("Delete");
                builder.setMessage("Are you sure to delete the message");

                builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        deleteMessageImage(position);
                    }
                });

                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        dialogInterface.dismiss();
                    }
                });

                builder.create().show();
                return false;
            }
        });

        holder.show_message.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setTitle("Delete");
                builder.setMessage("Are you sure to delete the message");

                builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        deleteMessage(position);
                    }
                });

                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        dialogInterface.dismiss();
                    }
                });

                builder.create().show();
                return false;
            }
        });
    }

    private void deleteMessageImage(int position) {

        String timeStamp = mChat.get(position).getTimeStamp();
        StorageReference picRed = FirebaseStorage.getInstance().getReferenceFromUrl(mChat.get(position).getMessage());
        picRed.delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                        Query fquery = FirebaseDatabase.getInstance()
                                .getReference("Chats").orderByChild("timeStamp").equalTo(timeStamp);
                        fquery.addListenerForSingleValueEvent(new ValueEventListener() {

                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                for (DataSnapshot ds: dataSnapshot.getChildren()) {

                                    ds.getRef().removeValue();
                                    mChat.remove(mChat.get(position));
                                    notifyDataSetChanged();

                                    Toast.makeText(mContext, "Message Deleted", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                                Toast.makeText(mContext, "You don't have permission to delete this message", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });
    }

    private void deleteMessage(int position) {

        final String myUID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String msgTimeStamp = mChat.get(position).getTimeStamp();

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Chats");
        Query query = dbRef.orderByChild("timeStamp").equalTo(msgTimeStamp);
        query.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                for (DataSnapshot ds: dataSnapshot.getChildren()) {

                    if (ds.child("sender").getValue().equals(myUID)) {

                        ds.getRef().removeValue();
                        mChat.remove(mChat.get(position));
                        notifyDataSetChanged();

                        Toast.makeText(mContext, "Message Deleted", Toast.LENGTH_SHORT).show();
                    } else {

                        Toast.makeText(mContext, "You don't have permission to delete this message", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public int getItemCount() {
        return mChat.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public TextView show_message;
        public ImageView profile_image, messageIv;
        public TextView txt_seen;
        public RelativeLayout messageLayout;

        public ViewHolder(View itemView) {
            super(itemView);

            show_message = itemView.findViewById(R.id.show_message);
            profile_image = itemView.findViewById(R.id.profile_image);
            txt_seen = itemView.findViewById(R.id.txt_seen);
            messageIv = itemView.findViewById(R.id.messageIv);
            messageLayout = itemView.findViewById(R.id.messageLayout);
        }
    }

    @Override
    public int getItemViewType(int position) {

        fuser = FirebaseAuth.getInstance().getCurrentUser();
        if (mChat.get(position).getSender().equals(fuser.getUid())) {

            return MSG_TYPE_RIGHT;
        } else {

            return MSG_TYPE_LEFT;
        }
    }
}