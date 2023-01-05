package arko.chatapp.Model;

public class User {

    private String id;
    private String username;
    private String user_status;
    private String imageURL;
    private String status;
    private String search;
    boolean isBlocked = false;

    public User(String id, String username, String user_status, String imageURL, String status, String search, boolean isBlocked) {
        this.id = id;
        this.username = username;
        this.user_status = user_status;
        this.imageURL = imageURL;
        this.status = status;
        this.search = search;
        this.isBlocked = isBlocked;
    }

    public User() {

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUser_status() {
        return user_status;
    }

    public void setUser_status(String user_status) {
        this.user_status = user_status;
    }

    public String getImageURL() {
        return imageURL;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public boolean isBlocked() {
        return isBlocked;
    }

    public void setBlocked(boolean blocked) {
        isBlocked = blocked;
    }
}
