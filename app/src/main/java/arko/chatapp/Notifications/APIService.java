package arko.chatapp.Notifications;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface APIService {

    @Headers(
            {
                    "Content-Type:application/json",
                    "Authorization:key=AAAACUr2HLM:APA91bHEZP2O93W1InZHetkoF2UjaLsjvc6HvEtRXtLFxSnWWmI5pKGFrDjEfYVX8keuGjW96xhns-3V5BuL6Fcfj2G3XWpu7r4wiZ1s8F4vilBqyZ4gtmGl9LUbDB7M7nFzLQey8KOK"
            }
    )

    @POST("fcm/send")
    Call<MyResponse> sendNotification(@Body Sender body);
}
